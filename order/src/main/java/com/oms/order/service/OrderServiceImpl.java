package com.oms.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oms.common.kafka.KafkaEventPublisher;
import com.oms.order.client.InventoryClient;
import com.oms.order.dto.InventoryResponse;
import com.oms.order.dto.OrderDto;
import com.oms.order.dto.ReservationRequest;
import com.oms.order.exception.OrderNotFoundException;
import com.oms.order.exception.ProductNotFoundException;
import com.oms.order.exception.UnauthorizedAccessException;
import com.oms.order.model.Order;
import com.oms.order.model.OrderItem;
import com.oms.order.model.OrderStatus;
import com.oms.order.repository.OrderRepository;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class OrderServiceImpl implements OrderService {

        private final OrderRepository orderRepository;
        private final InventoryClient inventoryClient;
        private final KafkaEventPublisher kafkaEventPublisher;

        @Override
        @Transactional
        @Timed(value = "order.create", description = "Time to create order end-to-end")
        public OrderDto.OrderResponse createOrder(Long userId, OrderDto.CreateOrderRequest request) {
                BigDecimal totalAmount = BigDecimal.ZERO;

                Order order = Order.builder()
                                .userId(userId)
                                .status(OrderStatus.CREATED)
                                .currency("USD")
                                .build();

                List<ReservationRequest.ItemRequest> reservationItems = new ArrayList<>();

                for (OrderDto.OrderItemRequest itemRequest : request.items()) {
                        log.info("Processing item: {}", itemRequest.productId());
                        // 1. Fetch Product from Inventory (Source of Truth)
                        InventoryResponse product = inventoryClient.getProductById(itemRequest.productId());

                        if (product == null) {
                                log.error("Product null for id: {}", itemRequest.productId());
                                throw new ProductNotFoundException("Product not found: " + itemRequest.productId());
                        }

                        if (!"ACTIVE".equals(product.status())) {
                                throw new RuntimeException("Product is not active: " + product.productName());
                        }

                        // 2. Use Price from Inventory, NOT from Client Request
                        BigDecimal itemTotal = product.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));
                        totalAmount = totalAmount.add(itemTotal);

                        // 3. Prepare Order Item
                        OrderItem item = OrderItem.builder()
                                        .order(order)
                                        .productId(itemRequest.productId())
                                        .productName(product.productName()) // Name from Inventory
                                        .unitPrice(product.price()) // Price from Inventory
                                        .quantity(itemRequest.quantity())
                                        .totalPrice(itemTotal)
                                        .build();

                        order.addItem(item);

                        // 4. Prepare Reservation Item
                        reservationItems.add(new ReservationRequest.ItemRequest(itemRequest.productId(),
                                        itemRequest.quantity()));
                }

                order.setTotalAmount(totalAmount);
                Order savedOrder = orderRepository.save(order);

                // 5. Reserve Inventory
                // Use the real saved order ID now.
                ReservationRequest reservationRequest = new ReservationRequest(savedOrder.getId(), reservationItems);
                inventoryClient.reserveInventory(reservationRequest);

                // Publish ORDER_CREATED event
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", savedOrder.getId());
                payload.put("userId", savedOrder.getUserId());
                payload.put("totalAmount", savedOrder.getTotalAmount());
                payload.put("orderStatus", savedOrder.getStatus().name());

                kafkaEventPublisher.publishEvent(
                                "order-events",
                                "ORDER_CREATED",
                                "ORDER",
                                savedOrder.getId(),
                                "order-service",
                                payload);

                return mapToResponse(savedOrder);
        }

        @Override
        @Transactional(readOnly = true)
        public OrderDto.OrderResponse getOrderById(Long userId, Long orderId) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

                if (!order.getUserId().equals(userId)) {
                        throw new UnauthorizedAccessException("You are not authorized to view this order");
                }

                return mapToResponse(order);
        }

        @Override
        @Transactional(readOnly = true)
        public OrderDto.OrderListResponse listUserOrders(Long userId, Pageable pageable) {
                Page<Order> ordersPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

                List<OrderDto.OrderResponse> content = ordersPage.getContent().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());

                return new OrderDto.OrderListResponse(
                                content,
                                ordersPage.getNumber(),
                                ordersPage.getTotalPages(),
                                ordersPage.getTotalElements());
        }

        @Override
        @Transactional
        @Timed(value = "order.inventory.reserved", description = "Handle inventory reserved event")
        public void handleInventoryReserved(Long orderId) {
                log.info("Handling INVENTORY_RESERVED for order: {}", orderId);
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

                if (order.getStatus() != OrderStatus.CREATED) {
                        log.warn("Order {} is not in CREATED status, skipping inventory reserved handling", orderId);
                        return;
                }

                order.setStatus(OrderStatus.AWAITING_PAYMENT); // Use a more descriptive status if possible, or keep
                                                               // INVENTORY_RESERVED
                orderRepository.save(order);

                log.info("Order {} is now AWAITING_PAYMENT. Manual payment is required.", orderId);
        }

        @Override
        @Transactional
        @Timed(value = "order.inventory.failed", description = "Handle inventory failed event")
        public void handleInventoryFailed(Long orderId) {
                log.info("Handling INVENTORY_FAILED for order: {}", orderId);
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

                order.setStatus(OrderStatus.INVENTORY_FAILED);
                orderRepository.save(order);
        }

        @Override
        @Transactional
        @Timed(value = "order.payment.success", description = "Handle payment success event")
        public void handlePaymentSuccess(Long orderId) {
                log.info("Handling PAYMENT_SUCCESSFUL for order: {}", orderId);
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);

                // Publish ORDER_COMPLETED event
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", order.getId());
                payload.put("userId", order.getUserId());
                payload.put("totalAmount", order.getTotalAmount());
                payload.put("orderStatus", order.getStatus().name());

                kafkaEventPublisher.publishEvent(
                                "order-events",
                                "ORDER_COMPLETED",
                                "ORDER",
                                order.getId(),
                                "order-service",
                                payload);
        }

        @Override
        @Transactional
        @Timed(value = "order.payment.failed", description = "Handle payment failed event")
        public void handlePaymentFailed(Long orderId) {
                log.info("Handling PAYMENT_FAILED for order: {}", orderId);
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

                order.setStatus(OrderStatus.PAYMENT_FAILED);
                orderRepository.save(order);

                // In a real system, we would trigger inventory release here
                // For now, we'll just log it.
                log.warn("Payment failed for order {}, inventory should be released", orderId);
        }

        private OrderDto.OrderResponse mapToResponse(Order order) {
                List<OrderDto.OrderItemResponse> items = order.getItems().stream()
                                .map(item -> new OrderDto.OrderItemResponse(
                                                item.getId(),
                                                item.getProductId(),
                                                item.getProductName(),
                                                item.getUnitPrice(),
                                                item.getQuantity(),
                                                item.getTotalPrice()))
                                .collect(Collectors.toList());

                return new OrderDto.OrderResponse(
                                order.getId(),
                                order.getUserId(),
                                order.getStatus(),
                                order.getTotalAmount(),
                                order.getCurrency(),
                                order.getCreatedAt(),
                                order.getUpdatedAt(),
                                items);
        }
}


package com.oms.order.service;

import com.oms.order.dto.OrderDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderDto.OrderResponse createOrder(Long userId, OrderDto.CreateOrderRequest request);

    OrderDto.OrderResponse getOrderById(Long userId, Long orderId);

    OrderDto.OrderListResponse listUserOrders(Long userId, Pageable pageable);

    void handleInventoryReserved(Long orderId);

    void handleInventoryFailed(Long orderId);

    void handlePaymentSuccess(Long orderId);

    void handlePaymentFailed(Long orderId);
}

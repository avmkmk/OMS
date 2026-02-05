package com.oms.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.oms.common.kafka.KafkaEventPublisher;
import com.oms.order.client.InventoryClient;
import com.oms.order.dto.InventoryResponse;
import com.oms.order.dto.OrderDto;
import com.oms.order.dto.ReservationRequest;
import com.oms.order.model.Order;
import com.oms.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private InventoryResponse mockProduct;

    @BeforeEach
    void setUp() {
        mockProduct = new InventoryResponse(1L, "Laptop", new BigDecimal("1000.00"), 10, "ACTIVE");
    }

    @Test
    void createOrder_Success() {
        // Arrange
        OrderDto.OrderItemRequest itemRequest = new OrderDto.OrderItemRequest(1L, "Laptop", new BigDecimal("500"), 1); // Client
                                                                                                                       // price
                                                                                                                       // 500
                                                                                                                       // should
                                                                                                                       // be
                                                                                                                       // ignored
        OrderDto.CreateOrderRequest request = new OrderDto.CreateOrderRequest(List.of(itemRequest));

        when(inventoryClient.getProductById(1L)).thenReturn(mockProduct);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        // Act
        OrderDto.OrderResponse response = orderService.createOrder(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("1000.00"), response.totalAmount()); // Should use Inventory price 1000
        assertEquals(1, response.items().size());
        assertEquals("Laptop", response.items().get(0).productName());

        verify(inventoryClient).getProductById(1L);
        verify(inventoryClient).reserveInventory(any(ReservationRequest.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_ProductNotFound() {
        // Arrange
        OrderDto.OrderItemRequest itemRequest = new OrderDto.OrderItemRequest(999L, "Unknown", new BigDecimal("100"),
                1);
        OrderDto.CreateOrderRequest request = new OrderDto.CreateOrderRequest(List.of(itemRequest));

        when(inventoryClient.getProductById(999L)).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createOrder(1L, request));
        verify(inventoryClient, never()).reserveInventory(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_ProductInactive() {
        // Arrange
        InventoryResponse inactiveProduct = new InventoryResponse(1L, "Laptop", new BigDecimal("1000.00"), 10,
                "INACTIVE");
        OrderDto.OrderItemRequest itemRequest = new OrderDto.OrderItemRequest(1L, "Laptop", new BigDecimal("1000"), 1);
        OrderDto.CreateOrderRequest request = new OrderDto.CreateOrderRequest(List.of(itemRequest));

        when(inventoryClient.getProductById(1L)).thenReturn(inactiveProduct);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createOrder(1L, request));
        verify(inventoryClient, never()).reserveInventory(any());
    }
}


package com.oms.order.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oms.order.dto.OrderDto;
import com.oms.order.service.OrderService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MeterRegistry meterRegistry;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<OrderDto.OrderResponse> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrderDto.CreateOrderRequest createRequest) {

        Long userId = jwt.getClaim("userId");
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId, createRequest));
        } finally {
            sample.stop(Timer.builder("order.create")
                    .description("Create order endpoint")
                    .register(meterRegistry));
        }
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<OrderDto.OrderResponse> getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId) {

        Long userId = jwt.getClaim("userId");
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.ok(orderService.getOrderById(userId, orderId));
        } finally {
            sample.stop(Timer.builder("order.get")
                    .description("Get order by id endpoint")
                    .register(meterRegistry));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<OrderDto.OrderListResponse> listOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = jwt.getClaim("userId");
        Pageable pageable = PageRequest.of(page, size);
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.ok(orderService.listUserOrders(userId, pageable));
        } finally {
            sample.stop(Timer.builder("order.list")
                    .description("List user orders endpoint")
                    .register(meterRegistry));
        }
    }
}

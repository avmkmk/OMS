package com.oms.inventory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oms.inventory.dto.ReservationRequest;
import com.oms.inventory.dto.ReservationResponse;
import com.oms.inventory.service.InventoryService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final MeterRegistry meterRegistry;

    @PostMapping("/reserve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> reserveInventory(@RequestBody ReservationRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.ok(inventoryService.reserveInventory(request));
        } finally {
            sample.stop(Timer.builder("inventory.reserve")
                    .description("Reserve inventory endpoint")
                    .register(meterRegistry));
        }
    }

    @PostMapping("/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> releaseInventory(@RequestBody ReservationRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.ok(inventoryService.releaseInventory(request));
        } finally {
            sample.stop(Timer.builder("inventory.release")
                    .description("Release inventory endpoint")
                    .register(meterRegistry));
        }
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.oms.inventory.dto.InventoryDto> addProduct(
            @RequestBody com.oms.inventory.dto.InventoryDto inventoryDto) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.status(201).body(inventoryService.addProduct(inventoryDto));
        } finally {
            sample.stop(Timer.builder("inventory.add")
                    .description("Add product endpoint")
                    .register(meterRegistry));
        }
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.oms.inventory.dto.InventoryDto> updateProduct(
            @PathVariable Long id,
            @RequestBody com.oms.inventory.dto.InventoryDto inventoryDto) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.ok(inventoryService.updateProduct(id, inventoryDto));
        } finally {
            sample.stop(Timer.builder("inventory.update")
                    .description("Update product endpoint")
                    .register(meterRegistry));
        }
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<com.oms.inventory.dto.InventoryDto> getProduct(
            @PathVariable Long id) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.ok(inventoryService.getProduct(id));
        } finally {
            sample.stop(Timer.builder("inventory.get")
                    .description("Get product endpoint")
                    .register(meterRegistry));
        }
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<com.oms.inventory.dto.InventoryDto>> getAllAvailableProducts() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.ok(inventoryService.getAllAvailableProducts());
        } finally {
            sample.stop(Timer.builder("inventory.list")
                    .description("List products endpoint")
                    .register(meterRegistry));
        }
    }
}

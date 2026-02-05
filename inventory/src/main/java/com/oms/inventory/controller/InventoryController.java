package com.oms.inventory.controller;

import com.oms.inventory.dto.ReservationRequest;
import com.oms.inventory.dto.ReservationResponse;
import com.oms.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/reserve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> reserveInventory(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(inventoryService.reserveInventory(request));
    }

    @PostMapping("/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> releaseInventory(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(inventoryService.releaseInventory(request));
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.oms.inventory.dto.InventoryDto> addProduct(
            @RequestBody com.oms.inventory.dto.InventoryDto inventoryDto) {
        return ResponseEntity.status(201).body(inventoryService.addProduct(inventoryDto));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.oms.inventory.dto.InventoryDto> updateProduct(
            @PathVariable Long id,
            @RequestBody com.oms.inventory.dto.InventoryDto inventoryDto) {
        return ResponseEntity.ok(inventoryService.updateProduct(id, inventoryDto));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<com.oms.inventory.dto.InventoryDto> getProduct(
            @PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getProduct(id));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<com.oms.inventory.dto.InventoryDto>> getAllAvailableProducts() {
        return ResponseEntity.ok(inventoryService.getAllAvailableProducts());
    }
}

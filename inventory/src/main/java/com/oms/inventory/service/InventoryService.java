package com.oms.inventory.service;

import com.oms.inventory.dto.ReservationRequest;
import com.oms.inventory.dto.ReservationResponse;
import com.oms.inventory.exception.InsufficientStockException;
import com.oms.inventory.exception.InventoryNotFoundException;
import com.oms.inventory.model.Inventory;
import com.oms.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.oms.inventory.event.KafkaEventPublisher;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional
    public ReservationResponse reserveInventory(ReservationRequest request) {
        log.info("Reserving inventory for order: {}", request.getOrderId());

        for (ReservationRequest.ItemRequest item : request.getItems()) {
            Inventory inventory = inventoryRepository.findById(item.getProductId())
                    .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + item.getProductId()));

            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + inventory.getProductName());
            }

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());

            inventoryRepository.save(inventory);
        }

        // Publish event
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", request.getOrderId());
        payload.put("status", "RESERVED");

        kafkaEventPublisher.publishEvent(
                "inventory-events",
                "INVENTORY_RESERVED",
                "ORDER",
                request.getOrderId(),
                "inventory-service",
                payload);

        return new ReservationResponse("RESERVED");
    }

    @Transactional
    public ReservationResponse releaseInventory(ReservationRequest request) {
        log.info("Releasing inventory for order: {}", request.getOrderId());

        for (ReservationRequest.ItemRequest item : request.getItems()) {
            Inventory inventory = inventoryRepository.findById(item.getProductId())
                    .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + item.getProductId()));

            // In a real scenario, we might want to check if the reservation actually exists
            // but for this v1, we just move stock back.
            inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - item.getQuantity()));
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + item.getQuantity());

            inventoryRepository.save(inventory);
        }

        // Publish event
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", request.getOrderId());
        payload.put("status", "RELEASED");

        kafkaEventPublisher.publishEvent(
                "inventory-events",
                "INVENTORY_RELEASED",
                "ORDER",
                request.getOrderId(),
                "inventory-service",
                payload);

        return new ReservationResponse("RELEASED");
    }

    @Transactional
    public com.oms.inventory.dto.InventoryDto addProduct(com.oms.inventory.dto.InventoryDto inventoryDto) {
        log.info("Adding new product: {}", inventoryDto.productName());
        Inventory inventory = Inventory.builder()
                .productName(inventoryDto.productName())
                .price(inventoryDto.price())
                .availableQuantity(inventoryDto.quantity())
                .reservedQuantity(0)
                .status("ACTIVE")
                .build();
        Inventory saved = inventoryRepository.save(inventory);
        return mapToDto(saved);
    }

    @Transactional
    public com.oms.inventory.dto.InventoryDto updateProduct(Long id, com.oms.inventory.dto.InventoryDto inventoryDto) {
        log.info("Updating product: {}", id);
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + id));

        if (inventoryDto.productName() != null)
            inventory.setProductName(inventoryDto.productName());
        if (inventoryDto.price() != null)
            inventory.setPrice(inventoryDto.price());
        if (inventoryDto.quantity() != null)
            inventory.setAvailableQuantity(inventoryDto.quantity());
        if (inventoryDto.status() != null)
            inventory.setStatus(inventoryDto.status());

        Inventory saved = inventoryRepository.save(inventory);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public com.oms.inventory.dto.InventoryDto getProduct(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + id));
        return mapToDto(inventory);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.oms.inventory.dto.InventoryDto> getAllAvailableProducts() {
        return inventoryRepository.findAll().stream()
                .filter(i -> "ACTIVE".equals(i.getStatus()) && i.getAvailableQuantity() > 0)
                .map(this::mapToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    private com.oms.inventory.dto.InventoryDto mapToDto(Inventory inventory) {
        return new com.oms.inventory.dto.InventoryDto(
                inventory.getProductId(),
                inventory.getProductName(),
                inventory.getPrice(),
                inventory.getAvailableQuantity(),
                inventory.getStatus());
    }
}

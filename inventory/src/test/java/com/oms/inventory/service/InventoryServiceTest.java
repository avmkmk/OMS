package com.oms.inventory.service;

import com.oms.inventory.dto.ReservationRequest;
import com.oms.inventory.dto.ReservationResponse;
import com.oms.inventory.event.KafkaEventPublisher;
import com.oms.inventory.exception.InsufficientStockException;
import com.oms.inventory.exception.InventoryNotFoundException;
import com.oms.inventory.model.Inventory;
import com.oms.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory mockInventory;

    @BeforeEach
    void setUp() {
        mockInventory = Inventory.builder()
                .productId(1L)
                .productName("Laptop")
                .price(new BigDecimal("1000.00"))
                .availableQuantity(10)
                .reservedQuantity(0)
                .status("ACTIVE")
                .build();
    }

    @Test
    void testReserveInventory_Success() {
        // Arrange
        ReservationRequest.ItemRequest item = new ReservationRequest.ItemRequest(1L, 2);
        ReservationRequest request = new ReservationRequest(100L, List.of(item));

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);

        // Act
        ReservationResponse response = inventoryService.reserveInventory(request);

        // Assert
        assertNotNull(response);
        assertEquals("RESERVED", response.getStatus());
        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository).save(any(Inventory.class));
        verify(kafkaEventPublisher).publishEvent(
                anyString(), anyString(), anyString(), anyLong(), anyString(), anyMap());
    }

    @Test
    void testReserveInventory_InsufficientStock() {
        // Arrange
        ReservationRequest.ItemRequest item = new ReservationRequest.ItemRequest(1L, 20); // Request more than available
        ReservationRequest request = new ReservationRequest(100L, List.of(item));

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));

        // Act & Assert
        assertThrows(InsufficientStockException.class,
                () -> inventoryService.reserveInventory(request));

        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(kafkaEventPublisher, never()).publishEvent(
                anyString(), anyString(), anyString(), anyLong(), anyString(), anyMap());
    }

    @Test
    void testReserveInventory_ProductNotFound() {
        // Arrange
        ReservationRequest.ItemRequest item = new ReservationRequest.ItemRequest(999L, 2);
        ReservationRequest request = new ReservationRequest(100L, List.of(item));

        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InventoryNotFoundException.class,
                () -> inventoryService.reserveInventory(request));

        verify(inventoryRepository).findById(999L);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testReleaseInventory_Success() {
        // Arrange
        mockInventory.setAvailableQuantity(8);
        mockInventory.setReservedQuantity(2);

        ReservationRequest.ItemRequest item = new ReservationRequest.ItemRequest(1L, 2);
        ReservationRequest request = new ReservationRequest(100L, List.of(item));

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);

        // Act
        ReservationResponse response = inventoryService.releaseInventory(request);

        // Assert
        assertNotNull(response);
        assertEquals("RELEASED", response.getStatus());
        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository).save(any(Inventory.class));
        verify(kafkaEventPublisher).publishEvent(
                anyString(), anyString(), anyString(), anyLong(), anyString(), anyMap());
    }

    @Test
    void testUpdateProduct_Success() {
        // Arrange
        com.oms.inventory.dto.InventoryDto updateDto = new com.oms.inventory.dto.InventoryDto(
                1L, "Updated Laptop", new BigDecimal("1200.00"), 15, "ACTIVE");

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory savedInventory = invocation.getArgument(0);
            return savedInventory;
        });

        // Act
        com.oms.inventory.dto.InventoryDto result = inventoryService.updateProduct(1L, updateDto);

        // Assert
        assertNotNull(result);
        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void testUpdateProduct_NotFound() {
        // Arrange
        com.oms.inventory.dto.InventoryDto updateDto = new com.oms.inventory.dto.InventoryDto(
                999L, "Laptop", new BigDecimal("1000.00"), 10, "ACTIVE");

        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InventoryNotFoundException.class,
                () -> inventoryService.updateProduct(999L, updateDto));

        verify(inventoryRepository).findById(999L);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }
}

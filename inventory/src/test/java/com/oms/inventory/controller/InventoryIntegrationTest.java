package com.oms.inventory.controller;

import com.oms.inventory.dto.InventoryDto;
import com.oms.inventory.dto.ReservationRequest;
import com.oms.inventory.model.Inventory;
import com.oms.inventory.repository.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("null")
public class InventoryIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private InventoryRepository inventoryRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @BeforeEach
        void setup() {
                inventoryRepository.deleteAll();
        }

        @Test
        void testCreateAndGetProduct() throws Exception {
                InventoryDto product = new InventoryDto(null, "Test Product", new BigDecimal("99.99"), 10, "ACTIVE");

                mockMvc.perform(post("/inventory/products")
                                .with(jwt().authorities(() -> "ROLE_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(product)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.productName").value("Test Product"));

                List<Inventory> products = inventoryRepository.findAll();
                assertEquals(1, products.size());
                assertEquals("Test Product", products.get(0).getProductName());
        }

        @Test
        void testReserveInventory_Success() throws Exception {
                Inventory item = Inventory.builder()
                                .productName("Item")
                                .price(new BigDecimal("10.00"))
                                .availableQuantity(5)
                                .reservedQuantity(0)
                                .status("ACTIVE")
                                .build();
                item = inventoryRepository.save(item);

                ReservationRequest request = new ReservationRequest(100L, Arrays.asList(
                                new ReservationRequest.ItemRequest(item.getProductId(), 2)));

                mockMvc.perform(post("/inventory/reserve")
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                Inventory updated = inventoryRepository.findById(item.getProductId()).get();
                assertEquals(3, updated.getAvailableQuantity());
                assertEquals(2, updated.getReservedQuantity());
        }

        @Test
        void testReserveInventory_InsufficientStock() throws Exception {
                Inventory item = Inventory.builder()
                                .productName("Item")
                                .price(new BigDecimal("10.00"))
                                .availableQuantity(1)
                                .reservedQuantity(0)
                                .status("ACTIVE")
                                .build();
                item = inventoryRepository.save(item);

                ReservationRequest request = new ReservationRequest(101L, Arrays.asList(
                                new ReservationRequest.ItemRequest(item.getProductId(), 10)));

                mockMvc.perform(post("/inventory/reserve")
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("Insufficient stock for product: " + item.getProductName()));
        }
}

package com.oms.order.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.oms.order.dto.OrderDto;
import com.oms.order.repository.OrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WireMockTest(httpPort = 9082)
@EmbeddedKafka(partitions = 1, topics = { "order-events", "inventory-events", "payment-events" })
@SuppressWarnings("null")
public class OrderIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @BeforeEach
        void setup() {
                orderRepository.deleteAll();
        }

        private RequestPostProcessor userJwt(Long userId, String role) {
                return jwt()
                                .jwt(j -> j.claim("sub", String.valueOf(userId))
                                                .claim("email", "user" + userId + "@test.com")
                                                .claim("userId", userId)
                                                .claim("role", role))
                                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
        }

        @Test
        void testCreateOrder_Success() throws Exception {
                // Mock Inventory Response (matching InventoryResponse DTO fields)
                stubFor(get(urlEqualTo("/inventory/products/1"))
                                .willReturn(aResponse()
                                                .withHeader("Content-Type", "application/json")
                                                .withBody(
                                                                "{\"id\":1, \"productName\":\"Test\", \"price\":10.00, \"status\":\"ACTIVE\", \"quantity\":10}")));

                stubFor(post(urlEqualTo("/inventory/reserve"))
                                .willReturn(aResponse().withStatus(200)));

                OrderDto.CreateOrderRequest request = new OrderDto.CreateOrderRequest(List.of(
                                new OrderDto.OrderItemRequest(1L, "Test", new BigDecimal("10.00"), 2)));

                mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                                .with(userJwt(123L, "USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.totalAmount").value(20.00));
        }

        @Test
        void testCreateOrder_ProductNotFound_ReturnsNotFound() throws Exception {
                stubFor(get(urlEqualTo("/inventory/products/404"))
                                .willReturn(aResponse().withStatus(404)));

                OrderDto.CreateOrderRequest request = new OrderDto.CreateOrderRequest(List.of(
                                new OrderDto.OrderItemRequest(404L, "Unknown", new BigDecimal("10.00"), 1)));

                mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                                .with(userJwt(123L, "USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }
}

package com.example.checkout.controller;

import com.example.checkout.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stage 4 — integration tier. Real Spring context, real H2, real
 * DummyPaymentGateway. Only the network is faked (MockMvc).
 */
@SpringBootTest
@TestPropertySource(properties = "spring.main.banner-mode=off")
class CheckoutControllerIT {

    @Autowired WebApplicationContext context;
    @Autowired OrderRepository orders;
    @Autowired ObjectMapper json;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String body(String card) throws Exception {
        return json.writeValueAsString(Map.of(
                "cartId",         "c-1",
                "cardNumber",     card,
                "expiry",         "12/29",
                "cvv",            "123",
                "cardholderName", "Satish K"
        ));
    }

    @Test
    void payHappyPath() throws Exception {                                  // AC-1
        long before = orders.count();

        mvc.perform(post("/api/checkout/pay")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.last4").value("1111"));

        assertThat(orders.count()).isEqualTo(before + 1);
    }

    @Test
    void payDeclined() throws Exception {                                   // AC-2
        long before = orders.count();

        mvc.perform(post("/api/checkout/pay")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("4000000000000002")))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.reason").value("insufficient_funds"));

        assertThat(orders.count()).isEqualTo(before);
    }

    @Test
    void emptyCartReturns400() throws Exception {                           // AC-3
        mvc.perform(get("/api/checkout/c-empty"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_CART"));
    }

    @Test
    void idempotentReplay_returnsSameOrder_andDoesNotDoubleSave() throws Exception {  // AC-4
        String key = UUID.randomUUID().toString();

        MvcResult first = mvc.perform(post("/api/checkout/pay")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111")))
                .andExpect(status().isOk())
                .andReturn();
        String firstOrderId = json.readTree(first.getResponse().getContentAsString())
                .get("orderId").asText();
        long countAfterFirst = orders.count();

        MvcResult second = mvc.perform(post("/api/checkout/pay")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111")))
                .andExpect(status().isOk())
                .andReturn();
        String secondOrderId = json.readTree(second.getResponse().getContentAsString())
                .get("orderId").asText();

        assertThat(secondOrderId).isEqualTo(firstOrderId);
        assertThat(orders.count()).isEqualTo(countAfterFirst);
    }

    @Test
    void invalidCardFormatReturns400() throws Exception {                   // AC-5
        String bad = json.writeValueAsString(Map.of(
                "cartId",         "c-1",
                "cardNumber",     "1234",                       // too short
                "expiry",         "12/29",
                "cvv",            "123",
                "cardholderName", "Satish K"
        ));

        mvc.perform(post("/api/checkout/pay")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.fields.cardNumber").exists());
    }
}

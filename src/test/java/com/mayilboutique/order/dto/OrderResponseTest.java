package com.mayilboutique.order.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentStatus;
import com.mayilboutique.order.dto.response.OrderResponse;
import com.mayilboutique.order.entity.Order;

class OrderResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private Order orderWithNote(String note) {
        Order o = new Order("MB1", "9876543210", "Priya", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, OrderStatus.PENDING, PaymentStatus.COD_PENDING);
        o.setInternalNote(note);
        return o;
    }

    @Test
    void customerViewNeverCarriesTheInternalNote() throws Exception {
        OrderResponse response = OrderResponse.from(orderWithNote("call before delivery"), List.of());

        assertNull(response.internalNote());
        assertFalse(mapper.writeValueAsString(response).contains("internalNote"));
        assertFalse(mapper.writeValueAsString(response).contains("call before delivery"));
    }

    @Test
    void staffViewCarriesTheInternalNote() throws Exception {
        OrderResponse response = OrderResponse.forAdmin(orderWithNote("call before delivery"), List.of());

        assertEquals("call before delivery", response.internalNote());
        assertTrue(mapper.writeValueAsString(response).contains("\"internalNote\":\"call before delivery\""));
    }

    @Test
    void staffViewOmitsTheFieldWhileThereIsNoNote() throws Exception {
        OrderResponse response = OrderResponse.forAdmin(orderWithNote(null), List.of());

        assertFalse(mapper.writeValueAsString(response).contains("internalNote"));
    }
}

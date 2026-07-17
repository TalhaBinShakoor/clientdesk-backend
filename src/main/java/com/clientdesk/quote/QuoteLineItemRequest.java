package com.clientdesk.quote;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record QuoteLineItemRequest(
        @NotBlank
        @Size(max = 300)
        String description,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal quantity,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal unitPrice
) {
}

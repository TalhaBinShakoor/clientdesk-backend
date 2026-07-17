package com.clientdesk.quote;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuoteUpdateRequest(
        @NotNull
        UUID clientId,

        UUID workRequestId,

        @NotBlank
        @Size(max = 50)
        String quoteNumber,

        @NotBlank
        @Size(max = 200)
        String title,

        QuoteStatus status,

        @Size(min = 3, max = 3)
        String currency,

        @DecimalMin(value = "0.00")
        BigDecimal taxAmount,

        LocalDate validUntil,

        String notes,

        @NotEmpty
        List<@Valid QuoteLineItemRequest> lineItems
) {
}

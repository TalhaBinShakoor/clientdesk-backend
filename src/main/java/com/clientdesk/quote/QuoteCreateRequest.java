package com.clientdesk.quote;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuoteCreateRequest(
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

        @Pattern(regexp = "USD|EUR|GBP|SEK")
        String currency,

        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal taxAmount,

        LocalDate validUntil,

        @Size(max = 10000)
        String notes,

        @NotEmpty
        @Size(max = 50)
        List<@Valid QuoteLineItemRequest> lineItems
) {
}

package com.clientdesk.quote;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuoteResponse(
        UUID id,
        UUID clientId,
        String clientCompanyName,
        UUID workRequestId,
        String workRequestTitle,
        String quoteNumber,
        String title,
        QuoteStatus status,
        String currency,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        LocalDate validUntil,
        String notes,
        List<QuoteLineItemResponse> lineItems,
        Instant createdAt,
        Instant updatedAt
) {
    static QuoteResponse from(Quote quote) {
        return new QuoteResponse(
                quote.getId(),
                quote.getClient().getId(),
                quote.getClient().getCompanyName(),
                quote.getWorkRequest() == null ? null : quote.getWorkRequest().getId(),
                quote.getWorkRequest() == null ? null : quote.getWorkRequest().getTitle(),
                quote.getQuoteNumber(),
                quote.getTitle(),
                quote.getStatus(),
                quote.getCurrency(),
                quote.getSubtotal(),
                quote.getTaxAmount(),
                quote.getTotalAmount(),
                quote.getValidUntil(),
                quote.getNotes(),
                quote.getLineItems().stream()
                        .map(QuoteLineItemResponse::from)
                        .toList(),
                quote.getCreatedAt(),
                quote.getUpdatedAt()
        );
    }
}

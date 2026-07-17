package com.clientdesk.quote;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteLineItemResponse(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        int sortOrder
) {
    static QuoteLineItemResponse from(QuoteLineItem lineItem) {
        return new QuoteLineItemResponse(
                lineItem.getId(),
                lineItem.getDescription(),
                lineItem.getQuantity(),
                lineItem.getUnitPrice(),
                lineItem.getLineTotal(),
                lineItem.getSortOrder()
        );
    }
}

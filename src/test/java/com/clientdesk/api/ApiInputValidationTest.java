package com.clientdesk.api;

import com.clientdesk.client.ClientRequest;
import com.clientdesk.comment.CommentCreateRequest;
import com.clientdesk.quote.QuoteCreateRequest;
import com.clientdesk.quote.QuoteLineItemRequest;
import com.clientdesk.workrequest.WorkRequestCreateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiInputValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsOversizedTextInputs() {
        ClientRequest client = new ClientRequest("Client", null, null, null, null, "x".repeat(10001));
        WorkRequestCreateRequest workRequest = new WorkRequestCreateRequest(
                UUID.randomUUID(), "Request", "x".repeat(10001), null, null, null, null
        );
        CommentCreateRequest comment = new CommentCreateRequest(
                UUID.randomUUID(), null, null, "x".repeat(5001)
        );

        assertThat(validator.validate(client)).isNotEmpty();
        assertThat(validator.validate(workRequest)).isNotEmpty();
        assertThat(validator.validate(comment)).isNotEmpty();
    }

    @Test
    void rejectsUnsupportedCurrencyAndExcessiveLineItems() {
        QuoteLineItemRequest lineItem = new QuoteLineItemRequest(
                "Delivery", BigDecimal.ONE, BigDecimal.TEN
        );
        QuoteCreateRequest request = quoteRequest("CAD", java.util.Collections.nCopies(51, lineItem));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("currency", "lineItems");
    }

    @Test
    void rejectsQuoteValuesBeyondDatabasePrecision() {
        QuoteLineItemRequest lineItem = new QuoteLineItemRequest(
                "Delivery",
                new BigDecimal("123456789.00"),
                new BigDecimal("12345678901.00")
        );
        QuoteCreateRequest request = quoteRequest("USD", List.of(lineItem));

        assertThat(validator.validate(request)).hasSizeGreaterThanOrEqualTo(2);
    }

    private QuoteCreateRequest quoteRequest(String currency, List<QuoteLineItemRequest> lineItems) {
        return new QuoteCreateRequest(
                UUID.randomUUID(),
                null,
                "Q-100",
                "Quote",
                null,
                currency,
                BigDecimal.ZERO,
                null,
                null,
                lineItems
        );
    }
}

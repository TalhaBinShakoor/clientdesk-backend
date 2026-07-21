package com.clientdesk.quote;

import com.clientdesk.api.ApiPage;
import com.clientdesk.client.Client;
import com.clientdesk.client.ClientRepository;
import com.clientdesk.security.AccessService;
import com.clientdesk.workrequest.WorkRequest;
import com.clientdesk.workrequest.WorkRequestRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class QuoteService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal MAX_MONEY_VALUE = new BigDecimal("9999999999.99");

    private final QuoteRepository quoteRepository;
    private final ClientRepository clientRepository;
    private final WorkRequestRepository workRequestRepository;
    private final AccessService accessService;

    public QuoteService(
            QuoteRepository quoteRepository,
            ClientRepository clientRepository,
            WorkRequestRepository workRequestRepository,
            AccessService accessService
    ) {
        this.quoteRepository = quoteRepository;
        this.clientRepository = clientRepository;
        this.workRequestRepository = workRequestRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public ApiPage<QuoteResponse> findAll(
            QuoteStatus status,
            UUID clientId,
            UUID workRequestId,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ApiPage.from(
                quoteRepository.findAll(matchingFilters(status, clientId, workRequestId), pageRequest),
                QuoteResponse::from
        );
    }

    @Transactional(readOnly = true)
    public QuoteResponse findById(UUID id) {
        return QuoteResponse.from(findQuote(id));
    }

    public QuoteResponse create(QuoteCreateRequest request) {
        Client client = findClient(request.clientId());
        WorkRequest workRequest = findOptionalWorkRequest(request.workRequestId(), client);
        Quote quote = new Quote(
                client,
                workRequest,
                request.quoteNumber(),
                request.title(),
                request.status(),
                request.currency(),
                scaledMoney(request.taxAmount()),
                request.validUntil(),
                request.notes()
        );

        applyLineItemsAndTotals(quote, request.lineItems());

        return QuoteResponse.from(quoteRepository.save(quote));
    }

    public QuoteResponse update(UUID id, QuoteUpdateRequest request) {
        Quote quote = findQuote(id);
        Client client = findClient(request.clientId());
        WorkRequest workRequest = findOptionalWorkRequest(request.workRequestId(), client);

        quote.setClient(client);
        quote.setWorkRequest(workRequest);
        quote.setQuoteNumber(request.quoteNumber());
        quote.setTitle(request.title());
        quote.setStatus(request.status());
        quote.setCurrency(request.currency());
        quote.setTaxAmount(scaledMoney(request.taxAmount()));
        quote.setValidUntil(request.validUntil());
        quote.setNotes(request.notes());
        applyLineItemsAndTotals(quote, request.lineItems());
        quote.markUpdated();

        return QuoteResponse.from(quote);
    }

    public QuoteResponse updateStatus(UUID id, QuoteStatusUpdateRequest request) {
        Quote quote = findQuote(id);
        quote.setStatus(request.status());
        quote.markUpdated();

        return QuoteResponse.from(quote);
    }

    public void delete(UUID id) {
        Quote quote = findQuote(id);
        quoteRepository.delete(quote);
    }

    private Specification<Quote> matchingFilters(QuoteStatus status, UUID clientId, UUID workRequestId) {
        return Specification.allOf(
                accessService.scopeByClientPath("client"),
                status == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status),
                clientId == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("client").get("id"), clientId),
                workRequestId == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("workRequest").get("id"), workRequestId)
        );
    }

    private void applyLineItemsAndTotals(Quote quote, List<QuoteLineItemRequest> lineItemRequests) {
        List<QuoteLineItem> lineItems = IntStream.range(0, lineItemRequests.size())
                .mapToObj(index -> toLineItem(lineItemRequests.get(index), index))
                .toList();

        quote.replaceLineItems(lineItems);

        BigDecimal subtotal = lineItems.stream()
                .map(QuoteLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal taxAmount = scaledMoney(quote.getTaxAmount());
        requireMoneyRange(subtotal);
        requireMoneyRange(taxAmount);
        requireMoneyRange(subtotal.add(taxAmount));

        quote.setSubtotal(subtotal);
        quote.setTaxAmount(taxAmount);
        quote.setTotalAmount(subtotal.add(taxAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private QuoteLineItem toLineItem(QuoteLineItemRequest request, int sortOrder) {
        BigDecimal quantity = scaledQuantity(request.quantity());
        BigDecimal unitPrice = scaledMoney(request.unitPrice());
        BigDecimal lineTotal = quantity.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        requireMoneyRange(lineTotal);

        return new QuoteLineItem(
                request.description(),
                quantity,
                unitPrice,
                lineTotal,
                sortOrder
        );
    }

    private BigDecimal scaledQuantity(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaledMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private void requireMoneyRange(BigDecimal value) {
        if (value.compareTo(MAX_MONEY_VALUE) > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Quote amount exceeds the supported range");
        }
    }

    private Client findClient(UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Client not found"));
        accessService.requireClientAccess(client, "Client");
        return client;
    }

    private WorkRequest findOptionalWorkRequest(UUID id, Client client) {
        if (id == null) {
            return null;
        }

        WorkRequest workRequest = workRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work request not found"));
        accessService.requireClientAccess(workRequest.getClient(), "Work request");

        if (!workRequest.getClient().getId().equals(client.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Work request does not belong to client");
        }

        return workRequest;
    }

    private Quote findQuote(UUID id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Quote not found"));
        accessService.requireClientAccess(quote.getClient(), "Quote");
        return quote;
    }
}

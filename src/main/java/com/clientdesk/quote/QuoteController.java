package com.clientdesk.quote;

import com.clientdesk.api.ApiPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@Validated
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public ApiPage<QuoteResponse> findAll(
            @RequestParam(required = false) QuoteStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID workRequestId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        return quoteService.findAll(status, clientId, workRequestId, page, size);
    }

    @GetMapping("/{id}")
    public QuoteResponse findById(@PathVariable UUID id) {
        return quoteService.findById(id);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public QuoteResponse create(@Valid @RequestBody QuoteCreateRequest request) {
        return quoteService.create(request);
    }

    @PutMapping("/{id}")
    public QuoteResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody QuoteUpdateRequest request
    ) {
        return quoteService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public QuoteResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody QuoteStatusUpdateRequest request
    ) {
        return quoteService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        quoteService.delete(id);
    }
}

package com.clientdesk.quote;

import com.clientdesk.client.Client;
import com.clientdesk.workrequest.WorkRequest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_request_id")
    private WorkRequest workRequest;

    @NotBlank
    @Size(max = 50)
    @Column(name = "quote_number", nullable = false, unique = true, length = 50)
    private String quoteNumber;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuoteStatus status = QuoteStatus.DRAFT;

    @NotBlank
    @Pattern(regexp = "USD|EUR|GBP|SEK")
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Size(max = 10000)
    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<QuoteLineItem> lineItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Quote() {
    }

    public Quote(
            Client client,
            WorkRequest workRequest,
            String quoteNumber,
            String title,
            QuoteStatus status,
            String currency,
            BigDecimal taxAmount,
            LocalDate validUntil,
            String notes
    ) {
        this.client = client;
        this.workRequest = workRequest;
        this.quoteNumber = quoteNumber;
        this.title = title;
        this.status = status == null ? QuoteStatus.DRAFT : status;
        this.currency = currency == null || currency.isBlank() ? "USD" : currency.toUpperCase();
        this.taxAmount = taxAmount == null ? BigDecimal.ZERO : taxAmount;
        this.validUntil = validUntil;
        this.notes = notes;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        markUpdated();
    }

    void markUpdated() {
        updatedAt = Instant.now();
    }

    public void replaceLineItems(List<QuoteLineItem> replacementLineItems) {
        lineItems.clear();
        replacementLineItems.forEach(this::addLineItem);
    }

    public void addLineItem(QuoteLineItem lineItem) {
        lineItem.setQuote(this);
        lineItems.add(lineItem);
    }

    public UUID getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public WorkRequest getWorkRequest() {
        return workRequest;
    }

    public void setWorkRequest(WorkRequest workRequest) {
        this.workRequest = workRequest;
    }

    public String getQuoteNumber() {
        return quoteNumber;
    }

    public void setQuoteNumber(String quoteNumber) {
        this.quoteNumber = quoteNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public void setStatus(QuoteStatus status) {
        this.status = status == null ? QuoteStatus.DRAFT : status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency == null || currency.isBlank() ? "USD" : currency.toUpperCase();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount == null ? BigDecimal.ZERO : taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<QuoteLineItem> getLineItems() {
        return lineItems;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

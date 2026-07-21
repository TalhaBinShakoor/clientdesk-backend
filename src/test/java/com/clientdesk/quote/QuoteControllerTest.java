package com.clientdesk.quote;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithUserDetails("admin@clientdesk.test")
class QuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAndListQuotes() throws Exception {
        String clientId = createClient("Quote Client " + UUID.randomUUID());
        String workRequestId = createWorkRequest(clientId, "Quote request " + UUID.randomUUID());
        String quoteNumber = uniqueQuoteNumber();

        mockMvc.perform(post("/api/quotes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "workRequestId": "%s",
                                  "quoteNumber": "%s",
                                  "title": "Website refresh quote",
                                  "status": "DRAFT",
                                  "currency": "USD",
                                  "taxAmount": 25.00,
                                  "validUntil": "2026-09-30",
                                  "notes": "Valid for 30 days.",
                                  "lineItems": [
                                    {
                                      "description": "Discovery workshop",
                                      "quantity": 2,
                                      "unitPrice": 150.00
                                    },
                                    {
                                      "description": "Design implementation",
                                      "quantity": 8,
                                      "unitPrice": 100.00
                                    }
                                  ]
                                }
                                """.formatted(clientId, workRequestId, quoteNumber)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.clientCompanyName").exists())
                .andExpect(jsonPath("$.workRequestId").value(workRequestId))
                .andExpect(jsonPath("$.workRequestTitle").exists())
                .andExpect(jsonPath("$.quoteNumber").value(quoteNumber))
                .andExpect(jsonPath("$.title").value("Website refresh quote"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.subtotal").value(1100.00))
                .andExpect(jsonPath("$.taxAmount").value(25.00))
                .andExpect(jsonPath("$.totalAmount").value(1125.00))
                .andExpect(jsonPath("$.validUntil").value("2026-09-30"))
                .andExpect(jsonPath("$.lineItems[0].description").value("Discovery workshop"))
                .andExpect(jsonPath("$.lineItems[0].lineTotal").value(300.00))
                .andExpect(jsonPath("$.lineItems[0].sortOrder").value(0))
                .andExpect(jsonPath("$.lineItems[1].description").value("Design implementation"))
                .andExpect(jsonPath("$.lineItems[1].lineTotal").value(800.00))
                .andExpect(jsonPath("$.lineItems[1].sortOrder").value(1));

        mockMvc.perform(get("/api/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].quoteNumber", hasItem(quoteNumber)));
    }

    @Test
    void filterQuotesByStatusClientAndWorkRequest() throws Exception {
        String firstClientId = createClient("Quote Filter Client A " + UUID.randomUUID());
        String secondClientId = createClient("Quote Filter Client B " + UUID.randomUUID());
        String firstWorkRequestId = createWorkRequest(firstClientId, "Filter request A " + UUID.randomUUID());
        String secondWorkRequestId = createWorkRequest(secondClientId, "Filter request B " + UUID.randomUUID());
        String matchingQuoteNumber = uniqueQuoteNumber();
        String otherQuoteNumber = uniqueQuoteNumber();

        createQuote(firstClientId, firstWorkRequestId, matchingQuoteNumber, "SENT");
        createQuote(secondClientId, secondWorkRequestId, otherQuoteNumber, "DRAFT");

        mockMvc.perform(get("/api/quotes")
                        .param("status", "SENT")
                        .param("clientId", firstClientId)
                        .param("workRequestId", firstWorkRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].quoteNumber", hasItem(matchingQuoteNumber)))
                .andExpect(jsonPath("$.content[*].quoteNumber", not(hasItem(otherQuoteNumber))));
    }

    @Test
    void updateQuoteDetails() throws Exception {
        String firstClientId = createClient("Original Quote Client " + UUID.randomUUID());
        String secondClientId = createClient("Updated Quote Client " + UUID.randomUUID());
        String secondWorkRequestId = createWorkRequest(secondClientId, "Updated quote request " + UUID.randomUUID());
        String quoteId = createQuote(firstClientId, null, uniqueQuoteNumber(), "DRAFT");
        String updatedQuoteNumber = uniqueQuoteNumber();

        mockMvc.perform(put("/api/quotes/{id}", quoteId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "workRequestId": "%s",
                                  "quoteNumber": "%s",
                                  "title": "Updated quote",
                                  "status": "SENT",
                                  "currency": "EUR",
                                  "taxAmount": 40.00,
                                  "validUntil": "2026-10-15",
                                  "notes": "Updated scope.",
                                  "lineItems": [
                                    {
                                      "description": "Updated delivery",
                                      "quantity": 4,
                                      "unitPrice": 200.00
                                    }
                                  ]
                                }
                                """.formatted(secondClientId, secondWorkRequestId, updatedQuoteNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(secondClientId))
                .andExpect(jsonPath("$.workRequestId").value(secondWorkRequestId))
                .andExpect(jsonPath("$.quoteNumber").value(updatedQuoteNumber))
                .andExpect(jsonPath("$.title").value("Updated quote"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.subtotal").value(800.00))
                .andExpect(jsonPath("$.taxAmount").value(40.00))
                .andExpect(jsonPath("$.totalAmount").value(840.00))
                .andExpect(jsonPath("$.lineItems[0].description").value("Updated delivery"));
    }

    @Test
    void updateStatusAndDeleteQuote() throws Exception {
        String clientId = createClient("Quote Status Client " + UUID.randomUUID());
        String quoteId = createQuote(clientId, null, uniqueQuoteNumber(), "DRAFT");

        mockMvc.perform(patch("/api/quotes/{id}/status", quoteId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "APPROVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(delete("/api/quotes/{id}", quoteId).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/quotes/{id}", quoteId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createQuoteRequiresClientTitleNumberAndLineItems() throws Exception {
        mockMvc.perform(post("/api/quotes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DRAFT"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createQuoteReturnsNotFoundForMissingClient() throws Exception {
        mockMvc.perform(post("/api/quotes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "quoteNumber": "%s",
                                  "title": "Quote for missing client",
                                  "lineItems": [
                                    {
                                      "description": "Planning",
                                      "quantity": 1,
                                      "unitPrice": 100.00
                                    }
                                  ]
                                }
                                """.formatted(UUID.randomUUID(), uniqueQuoteNumber())))
                .andExpect(status().isNotFound());
    }

    @Test
    void createQuoteRejectsWorkRequestFromDifferentClient() throws Exception {
        String firstClientId = createClient("Quote Relationship Client A " + UUID.randomUUID());
        String secondClientId = createClient("Quote Relationship Client B " + UUID.randomUUID());
        String secondWorkRequestId = createWorkRequest(secondClientId, "Other client request " + UUID.randomUUID());

        mockMvc.perform(post("/api/quotes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "workRequestId": "%s",
                                  "quoteNumber": "%s",
                                  "title": "Mismatched quote",
                                  "lineItems": [
                                    {
                                      "description": "Planning",
                                      "quantity": 1,
                                      "unitPrice": 100.00
                                    }
                                  ]
                                }
                                """.formatted(firstClientId, secondWorkRequestId, uniqueQuoteNumber())))
                .andExpect(status().isBadRequest());
    }

    private String createClient(String companyName) throws Exception {
        String responseBody = mockMvc.perform(post("/api/clients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(companyName)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractId(responseBody);
    }

    private String createWorkRequest(String clientId, String title) throws Exception {
        String responseBody = mockMvc.perform(post("/api/work-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "%s",
                                  "status": "NEW",
                                  "priority": "MEDIUM"
                                }
                                """.formatted(clientId, title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractId(responseBody);
    }

    private String createQuote(
            String clientId,
            String workRequestId,
            String quoteNumber,
            String status
    ) throws Exception {
        String workRequestField = workRequestId == null
                ? ""
                : """
                                  "workRequestId": "%s",
                        """.formatted(workRequestId);
        String responseBody = mockMvc.perform(post("/api/quotes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                        %s          "quoteNumber": "%s",
                                  "title": "Service quote",
                                  "status": "%s",
                                  "currency": "USD",
                                  "taxAmount": 10.00,
                                  "lineItems": [
                                    {
                                      "description": "Implementation",
                                      "quantity": 3,
                                      "unitPrice": 100.00
                                    }
                                  ]
                                }
                                """.formatted(clientId, workRequestField, quoteNumber, status)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractId(responseBody);
    }

    private String uniqueQuoteNumber() {
        return "Q-" + UUID.randomUUID();
    }

    private String extractId(String responseBody) {
        return responseBody.replaceFirst("(?s)^.*?\\\"id\\\":\\\"([^\\\"]+)\\\".*$", "$1");
    }
}

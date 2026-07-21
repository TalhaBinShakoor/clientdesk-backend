package com.clientdesk.client;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithUserDetails("admin@clientdesk.test")
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAndListClients() throws Exception {
        String companyName = "Acme Studio " + UUID.randomUUID();

        mockMvc.perform(post("/api/clients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "%s",
                                  "contactName": "Alex Morgan",
                                  "email": "alex@acme.example",
                                  "phone": "+1 555 0100",
                                  "status": "LEAD",
                                  "notes": "Potential design client"
                                }
                                """.formatted(companyName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value(companyName))
                .andExpect(jsonPath("$.contactName").value("Alex Morgan"))
                .andExpect(jsonPath("$.email").value("alex@acme.example"))
                .andExpect(jsonPath("$.status").value("LEAD"));

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].companyName", hasItem(companyName)));
    }

    @Test
    void listClientsEnforcesPaginationLimits() throws Exception {
        mockMvc.perform(get("/api/clients").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").isNumber());

        mockMvc.perform(get("/api/clients").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClientRequiresCompanyName() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contactName": "Alex Morgan",
                                  "email": "alex@acme.example"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDeleteClient() throws Exception {
        String responseBody = mockMvc.perform(post("/api/clients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "Northwind",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = responseBody.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(put("/api/clients/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "Northwind Agency",
                                  "contactName": "Nina Patel",
                                  "email": "nina@northwind.example",
                                  "phone": "+1 555 0199",
                                  "status": "ACTIVE",
                                  "notes": "Updated client notes"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Northwind Agency"))
                .andExpect(jsonPath("$.contactName").value("Nina Patel"));

        mockMvc.perform(delete("/api/clients/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/clients/{id}", id))
                .andExpect(status().isNotFound());
    }
}

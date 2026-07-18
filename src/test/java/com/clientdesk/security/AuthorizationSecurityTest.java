package com.clientdesk.security;

import com.clientdesk.client.Client;
import com.clientdesk.client.ClientRepository;
import com.clientdesk.client.ClientStatus;
import com.clientdesk.identity.Organization;
import com.clientdesk.identity.OrganizationRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationSecurityTest {

    private static final UUID ACME_CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID NORTHSTAR_CLIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACME_REQUEST_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @WithUserDetails("team@clientdesk.test")
    void teamMemberCanUpdateButCannotDelete() throws Exception {
        mockMvc.perform(patch("/api/work-requests/{id}/status", ACME_REQUEST_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(delete("/api/work-requests/{id}", ACME_REQUEST_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("client@clientdesk.test")
    void clientRoleCannotCreateQuote() throws Exception {
        mockMvc.perform(post("/api/quotes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("client@clientdesk.test")
    void clientCanOnlyReadAssignedClientRecords() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[*].id", hasItem(ACME_CLIENT_ID.toString())))
                .andExpect(jsonPath("$[*].id", not(hasItem(NORTHSTAR_CLIENT_ID.toString()))));

        mockMvc.perform(get("/api/clients/{id}", ACME_CLIENT_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/clients/{id}", NORTHSTAR_CLIENT_ID))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/work-requests/{id}", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("client@clientdesk.test")
    void clientCanSubmitAssignedRequestAndActorComesFromSession() throws Exception {
        String title = "Authenticated client request " + UUID.randomUUID();

        var result = mockMvc.perform(post("/api/work-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "%s",
                                  "priority": "HIGH",
                                  "requestedBy": "Payload Impostor"
                                }
                                """.formatted(ACME_CLIENT_ID, title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestedBy").value("Maya Chen"))
                .andReturn();

        String requestId = result.getResponse().getContentAsString()
                .replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workRequestId": "%s",
                                  "authorName": "Payload Impostor",
                                  "body": "Please confirm receipt."
                                }
                                """.formatted(requestId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Maya Chen"));

        mockMvc.perform(get("/api/activity-events").param("workRequestId", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actorName", hasItem("Maya Chen")));
    }

    @Test
    @WithUserDetails("client@clientdesk.test")
    void clientCannotSubmitRequestForAnotherClient() throws Exception {
        mockMvc.perform(post("/api/work-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "Unauthorized client request"
                                }
                                """.formatted(NORTHSTAR_CLIENT_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("admin@clientdesk.test")
    void crossOrganizationRecordsAreHiddenFromListsAndLookups() throws Exception {
        Organization otherOrganization = organizationRepository.save(
                new Organization("Other Workspace", "other-" + UUID.randomUUID())
        );
        Client otherClient = clientRepository.saveAndFlush(new Client(
                otherOrganization,
                "Other Organization Client",
                "Outside User",
                "outside@example.test",
                null,
                ClientStatus.ACTIVE,
                null
        ));

        mockMvc.perform(get("/api/clients/{id}", otherClient.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(otherClient.getId().toString()))));
    }
}

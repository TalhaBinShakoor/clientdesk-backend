package com.clientdesk.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank
        @Size(max = 200)
        String companyName,

        @Size(max = 200)
        String contactName,

        @Email
        @Size(max = 320)
        String email,

        @Size(max = 50)
        String phone,

        ClientStatus status,

        String notes
) {
}
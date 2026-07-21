package com.clientdesk.api;

import java.io.IOException;

public class RequestBodyTooLargeException extends IOException {

    public RequestBodyTooLargeException() {
        super("JSON request body exceeds the configured limit");
    }
}

package com.example.smartinventory.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private WebRequest webRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/products/1");
        return new ServletWebRequest(request);
    }

    @Test
    void handlesResourceNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Product not found with id: 1"), webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Product not found with id: 1");
    }

    @Test
    void handlesDuplicateEmail() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateEmail(
                new DuplicateEmailException("Email already in use: a@example.com"), webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Email already in use: a@example.com");
    }

    @Test
    void handlesAccessDenied() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("denied"), webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Access is denied");
    }

    @Test
    void handlesUnexpectedException() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("boom"), webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("boom");
    }

}

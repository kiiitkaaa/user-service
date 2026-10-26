package com.deshko.userservice.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.deshko.userservice.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/api/v1/users/1");

    @SuppressWarnings("unused")
    private static void endpoint(String param) {}

    private static MethodParameter methodParameter() throws NoSuchMethodException {
        return new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("endpoint", String.class), 0);
    }

    private void assertError(ResponseEntity<ApiError> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(status.value());
        assertThat(body.error()).isEqualTo(status.getReasonPhrase());
        assertThat(body.message()).isEqualTo(message);
        assertThat(body.path()).isEqualTo("/api/v1/users/1");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("User", 1L), request);

        assertError(response, HttpStatus.NOT_FOUND, "User with id 1 not found");
        assertThat(response.getBody().details()).isNull();
    }

    @Test
    void handleConflict_duplicateResource_returns409() {
        ResponseEntity<ApiError> response =
                handler.handleConflict(new DuplicateResourceException("already exists"), request);

        assertError(response, HttpStatus.CONFLICT, "already exists");
    }

    @Test
    void handleConflict_cardLimit_returns409() {
        ResponseEntity<ApiError> response =
                handler.handleConflict(new CardLimitExceededException(5), request);

        assertError(response, HttpStatus.CONFLICT, "User cannot have more than 5 cards");
    }

    @Test
    void handleValidation_returns400_withFieldErrors_mergingDuplicates() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "dto");
        binding.addError(new FieldError("dto", "name", "must not be blank"));
        binding.addError(new FieldError("dto", "name",
                "size must be between 1 and 100"));
        binding.addError(new FieldError("dto", "email",
                "must be a well-formed email address"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter(), binding);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertError(response, HttpStatus.BAD_REQUEST, "Validation failed");
        assertThat(response.getBody().details()).containsExactly(
                entry("name", "must not be blank; size must be between 1 and 100"),
                entry("email", "must be a well-formed email address"));
    }

    @Test
    void handleUnreadable_returns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "bad json", new MockHttpInputMessage(new byte[0]));

        assertError(handler.handleUnreadable(ex, request), HttpStatus.BAD_REQUEST,
                "Malformed JSON or invalid field format");
    }

    @Test
    void handleTypeMismatch_returns400_withParameterName() throws Exception {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", methodParameter(), null);

        assertError(handler.handleTypeMismatch(ex, request), HttpStatus.BAD_REQUEST,
                "Invalid value for parameter 'id'");
    }

    @Test
    void handleBadSort_returns400_withPropertyName() {
        PropertyReferenceException ex = mock(PropertyReferenceException.class);
        when(ex.getPropertyName()).thenReturn("nonexistent");

        assertError(handler.handleBadSort(ex, request), HttpStatus.BAD_REQUEST,
                "Unknown sort property: nonexistent");
    }

    @Test
    void handleIntegrity_returns409_withoutLeakingSqlDetails() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("duplicate key value violates constraint uq_users_email");

        assertError(handler.handleIntegrity(ex, request), HttpStatus.CONFLICT,
                "Data integrity violation");
    }

    @Test
    void handleOther_errorResponse_usesItsOwnStatus() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "no such route");

        assertError(handler.handleOther(ex, request), HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @Test
    void handleOther_missingRequestParameter_returns400() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("active", "boolean");

        assertError(handler.handleOther(ex, request), HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @Test
    void handleOther_unexpectedException_returns500_withoutLeakingMessage() {
        ResponseEntity<ApiError> response =
                handler.handleOther(new IllegalStateException("boom: internal details"), request);

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @Test
    void handleAccessDenied_returns403() {
        assertError(handler.handleAccessDenied(new AccessDeniedException("denied"), request),
                HttpStatus.FORBIDDEN, "Access denied");
    }

    @Test
    void handleAuthentication_returns401() {
        assertError(handler.handleAuthentication(new BadCredentialsException("bad"), request),
                HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}

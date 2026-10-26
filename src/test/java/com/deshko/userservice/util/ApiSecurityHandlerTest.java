package com.deshko.userservice.util;

import com.deshko.userservice.dto.ApiError;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.assertThat;

class ApiSecurityHandlerTest {
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ApiSecurityHandler handler = new ApiSecurityHandler(jsonMapper);

    @Test
    void commence_writes401JsonBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, new BadCredentialsException("bad credentials"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");

        ApiError body = jsonMapper.readValue(response.getContentAsString(), ApiError.class);
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.error()).isEqualTo("Unauthorized");
        assertThat(body.message()).isEqualTo("Unauthorized");
        assertThat(body.path()).isEqualTo("/api/v1/users/1");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.details()).isNull();
    }

    @Test
    void handle_writes403JsonBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/cards/5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);

        ApiError body = jsonMapper.readValue(response.getContentAsString(), ApiError.class);
        assertThat(body.status()).isEqualTo(403);
        assertThat(body.error()).isEqualTo("Forbidden");
        assertThat(body.message()).isEqualTo("Access denied");
        assertThat(body.path()).isEqualTo("/api/v1/cards/5");
    }
}

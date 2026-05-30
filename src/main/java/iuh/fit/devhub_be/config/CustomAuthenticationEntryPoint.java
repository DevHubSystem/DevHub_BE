package iuh.fit.devhub_be.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iuh.fit.devhub_be.common.dto.ErrorResponse;
import iuh.fit.devhub_be.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Handles authentication failures raised inside the security filter chain
 * (missing, malformed, or expired JWT) before the request reaches a controller,
 * where {@code @RestControllerAdvice} cannot intercept them. Renders the standard
 * {@link ErrorResponse} payload so clients see a consistent error shape.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // Standalone mapper with JSR-310 support so the Instant timestamp serializes;
    // the entry point runs outside the MVC stack, so it cannot rely on the
    // HttpMessageConverter-configured ObjectMapper.
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse body = ErrorResponse.of(errorCode, authException.getMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
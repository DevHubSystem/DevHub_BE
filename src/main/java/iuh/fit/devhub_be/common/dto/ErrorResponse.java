package iuh.fit.devhub_be.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.devhub_be.common.exception.ErrorCode;

import java.time.Instant;

/**
 * Standard error payload returned for failed requests. Kept separate from
 * {@link ApiResponse} so error responses can carry a machine-readable
 * {@link ErrorCode} that clients can branch on.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String errorCode,
        String message,
        String timestamp
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getDefaultMessage(), Instant.now().toString());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, Instant.now().toString());
    }
}
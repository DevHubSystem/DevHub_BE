package iuh.fit.devhub_be.common.exception;

/**
 * Thrown when an authenticated caller is known to lack permission for an action
 * on a resource they can otherwise see. Maps to HTTP 403.
 *
 * <p>Use {@link ResourceNotFoundException} (404) instead when the caller has no
 * access to the resource at all and its existence must not be leaked.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
package com.oms.iam.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Centralized exception handler for the IAM service.
 *
 * <p>Returns RFC 7807 ProblemDetail responses for all handled exceptions.</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles duplicate email registration attempts.
     *
     * @param ex the duplicate email exception
     * @return 409 Conflict ProblemDetail
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        return problem;
    }

    /**
     * Handles invalid login credentials.
     *
     * @param ex the invalid credentials exception
     * @return 401 Unauthorized ProblemDetail
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Unauthorized");
        return problem;
    }

    /**
     * Handles login attempts for inactive user accounts.
     *
     * @param ex the inactive user exception
     * @return 403 Forbidden ProblemDetail
     */
    @ExceptionHandler(InactiveUserException.class)
    public ProblemDetail handleInactiveUser(InactiveUserException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Forbidden");
        return problem;
    }

    /**
     * Catch-all handler for unexpected exceptions.
     *
     * @param ex the unhandled exception
     * @return 500 Internal Server Error ProblemDetail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error in IAM service", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        return problem;
    }
}

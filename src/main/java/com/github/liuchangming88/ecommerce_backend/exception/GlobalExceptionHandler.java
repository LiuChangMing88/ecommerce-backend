package com.github.liuchangming88.ecommerce_backend.exception;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.github.liuchangming88.ecommerce_backend.api.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    // For duplicated resources
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({DuplicatedUserException.class, DuplicatedAddressException.class})
    public ErrorResponse handleRegisterException (RuntimeException ex, HttpServletRequest request) {
        logger.error("Duplicated resource: {}", ex.getMessage());
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({IncorrectUsernameException.class, IncorrectPasswordException.class})
    public ErrorResponse handleLoginException (AuthenticationException ex, HttpServletRequest request) {
        logger.error("User login error: {}", ex.getMessage());
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad request",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // Constraints for fields (password, username, email...)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach((FieldError error) -> validationErrors.put(error.getField(), error.getDefaultMessage()));

        logger.error("Validation error: {}", ex.getMessage());
        String message = "Validation failed for one or more fields.";

        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                request.getRequestURI(),
                validationErrors
        );
    }

    // For when JavaMailSender fails
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(MailException.class)
    public ErrorResponse handleEmailException(MailException ex, HttpServletRequest request) {
        logger.error("JavaMailSender threw an exception: {}", ex.getMessage());
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // For when the user is not verified but attempted to log in
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(UserNotVerifiedException.class)
    public ErrorResponse handleUserNotVerifiedException(UserNotVerifiedException ex, HttpServletRequest request) {
        logger.error("The user attempted to log in when they haven't verified their account: {}", ex.getMessage());
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // For exceptions related to tokens (verification, password reset)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({InvalidTokenException.class, TokenExpiredException.class})
    public ErrorResponse handleTokenVerificationExceptions (RuntimeException ex, HttpServletRequest request) {
        logger.error("The token is expired or invalid: {}", ex.getMessage());
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // Conflict for when user is already email verified but still tried to verify
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserAlreadyVerifiedException.class)
    public ErrorResponse handleUserAlreadyVerifiedException (UserAlreadyVerifiedException ex, HttpServletRequest request) {
        logger.error("The user is already verified: {}", ex.getMessage());
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // For jwt token
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(JWTVerificationException.class)
    public ErrorResponse handleJwtVerificationExceptions (JWTVerificationException ex, HttpServletRequest request) {
        logger.error("The JWT token was rejected: {}", ex.getMessage());
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // Password reset
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PasswordsDoNotMatchException.class)
    public ErrorResponse handlePasswordResetException(PasswordsDoNotMatchException ex, HttpServletRequest request) {
        logger.error("Password reset error: {}", ex.getMessage());
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad request",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // For not found resources
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({AddressNotFoundException.class, UserNotFoundException.class})
    public ErrorResponse handlePasswordResetException(RuntimeException ex, HttpServletRequest request) {
        logger.error("Resource not found: {}", ex.getMessage());
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not found",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }


    // For when user try to access resources they don't have authorization for
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handlePasswordResetException(AccessDeniedException ex, HttpServletRequest request) {
        logger.error("Access denied: {}", ex.getMessage());
        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Access denied",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }
}

package com.chatapp.exception;

import com.chatapp.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Lỗi validation: @NotBlank, @Email, @Size,...
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(
            MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        return new ErrorResponse(false, message);
    }

    // Lỗi đăng nhập
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(
            AuthenticationException e) {

        return new ErrorResponse(
                false,
                e.getMessage()
        );
    }

    // Lỗi dữ liệu đã tồn tại
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateResourceException(
            DuplicateResourceException e) {

        return new ErrorResponse(
                false,
                e.getMessage()
        );
    }
}
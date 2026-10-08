package com.neueda.leap.exception;

import com.neueda.leap.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Module 6's honest gap, closed: instead of Spring's default
    // {"status":400,"error":"Bad Request",...} with no mention of WHAT was
    // wrong, every violated field and its message is listed explicitly.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        List<ErrorResponseDTO.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponseDTO.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorResponseDTO body = ErrorResponseDTO.withFieldErrors(
                HttpStatus.BAD_REQUEST, "Bad Request", "request failed validation", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }


    // The catch-all. Deliberately generic - never echo ex.getMessage() or a
    // stack trace here. An unanticipated exception might carry internal
    // detail (a SQL fragment, an internal class name) that has no business
    // reaching a client. Full detail still goes to the server log.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(Exception ex, HttpServletRequest request) {
        ErrorResponseDTO body = ErrorResponseDTO.of(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "an unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
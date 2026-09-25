package com.neueda.leap.controller;

import com.neueda.leap.dto.TradeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<TradeResponse> handleIllegalStateException(IllegalStateException ex) {
        TradeResponse response = new TradeResponse();
        response.setStatus("REJECTED");
        
        String message = ex.getMessage();
        
        if (message.contains("Insufficient quantity")) {
            response.setReason("Insufficient holding for the requested trade");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        } else if (message.contains("Insufficient funds")) {
            response.setReason("Insufficient balance for the requested trade");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        
        response.setReason(message);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<TradeResponse> handleNoSuchElementException(NoSuchElementException ex) {
        TradeResponse response = new TradeResponse();
        response.setError("404");
        response.setMessage("Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        response.put("fieldErrors", fieldErrors);
        response.put("status", "BAD_REQUEST");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}


package com.officeseatbooking.controller;

import com.officeseatbooking.dto.AuthResponse;
import com.officeseatbooking.dto.LoginRequest;
import com.officeseatbooking.dto.RegisterRequest;
import com.officeseatbooking.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login attempt for username: {}", loginRequest.getUsername());
            AuthResponse response = authService.login(loginRequest);
            logger.info("Login successful for username: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            logger.error("Invalid credentials for username: {}", loginRequest.getUsername());
            return ResponseEntity.status(401)
                .body(new ErrorResponse(401, "Invalid username or password", java.time.LocalDateTime.now()));
        } catch (Exception e) {
            logger.error("Login error for username: {}: {}", loginRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(new ErrorResponse(500, "Login failed: " + e.getMessage(), java.time.LocalDateTime.now()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            logger.info("Registration attempt for username: {}", registerRequest.getUsername());
            AuthResponse response = authService.register(registerRequest);
            logger.info("Registration successful for username: {}", registerRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Registration error for username: {}: {}", registerRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(400)
                .body(new ErrorResponse(400, e.getMessage(), java.time.LocalDateTime.now()));
        }
    }

    // Inner class for error responses
    public static class ErrorResponse {
        private int status;
        private String message;
        private java.time.LocalDateTime timestamp;

        public ErrorResponse(int status, String message, java.time.LocalDateTime timestamp) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
    }
}

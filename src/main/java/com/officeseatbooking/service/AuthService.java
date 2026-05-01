package com.officeseatbooking.service;

import com.officeseatbooking.dto.AuthResponse;
import com.officeseatbooking.dto.LoginRequest;
import com.officeseatbooking.dto.RegisterRequest;
import com.officeseatbooking.entity.User;
import com.officeseatbooking.repository.UserRepository;
import com.officeseatbooking.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            logger.debug("Attempting authentication for user: {}", loginRequest.getUsername());

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            logger.debug("Authentication successful for user: {}", loginRequest.getUsername());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            logger.debug("Generating JWT token for user: {}", loginRequest.getUsername());
            String jwt = jwtUtils.generateJwtToken(authentication);

            User user = (User) authentication.getPrincipal();

            logger.info("Login completed successfully for user: {} with role: {}", user.getUsername(), user.getRole());

            return new AuthResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), user.getRole());

        } catch (BadCredentialsException e) {
            logger.error("Bad credentials for user: {}", loginRequest.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        } catch (Exception e) {
            logger.error("Login error for user: {}: {}", loginRequest.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        try {
            logger.info("Registration attempt for user: {}", registerRequest.getUsername());

            if (userRepository.existsByUsername(registerRequest.getUsername())) {
                throw new RuntimeException("Username is already taken!");
            }

            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                throw new RuntimeException("Email is already in use!");
            }

            // Create new user account
            User user = new User(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                encoder.encode(registerRequest.getPassword()),
                registerRequest.getRole()
            );

            userRepository.save(user);
            logger.info("User created successfully: {}", user.getUsername());

            // Auto-login after registration
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(registerRequest.getUsername(), registerRequest.getPassword())
            );

            String jwt = jwtUtils.generateJwtToken(authentication);

            logger.info("Registration and auto-login completed for user: {}", user.getUsername());

            return new AuthResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), user.getRole());

        } catch (Exception e) {
            logger.error("Registration error for user: {}: {}", registerRequest.getUsername(), e.getMessage(), e);
            throw e;
        }
    }
}

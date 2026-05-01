package com.officeseatbooking.controller;

import com.officeseatbooking.entity.User;
import com.officeseatbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DebugController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = userRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", users.size());
        response.put("users", users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole());
            userMap.put("createdAt", user.getCreatedAt());
            return userMap;
        }).toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Map<String, Object>> verifyPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        Map<String, Object> response = new HashMap<>();

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            response.put("userFound", false);
            response.put("message", "User not found");
            return ResponseEntity.ok(response);
        }

        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());

        response.put("userFound", true);
        response.put("passwordMatches", passwordMatches);
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("encodedPassword", user.getPassword().substring(0, 20) + "...");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-demo-users")
    public ResponseEntity<Map<String, Object>> resetDemoUsers() {
        // Delete existing demo users
        userRepository.findByUsername("admin").ifPresent(userRepository::delete);
        userRepository.findByUsername("manager").ifPresent(userRepository::delete);
        userRepository.findByUsername("employee").ifPresent(userRepository::delete);

        // Create new demo users
        User admin = new User("admin", "admin@company.com", passwordEncoder.encode("admin123"), com.officeseatbooking.enums.Role.ADMIN);
        User manager = new User("manager", "manager@company.com", passwordEncoder.encode("manager123"), com.officeseatbooking.enums.Role.MANAGER);
        User employee = new User("employee", "employee@company.com", passwordEncoder.encode("employee123"), com.officeseatbooking.enums.Role.EMPLOYEE);

        userRepository.save(admin);
        userRepository.save(manager);
        userRepository.save(employee);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Demo users reset successfully");
        response.put("users", List.of("admin", "manager", "employee"));

        return ResponseEntity.ok(response);
    }
}

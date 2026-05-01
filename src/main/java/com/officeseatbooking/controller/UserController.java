package com.officeseatbooking.controller;

import com.officeseatbooking.entity.User;
import com.officeseatbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/employees")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getEmployees() {
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> userList = users.stream()
            .map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("role", user.getRole().toString());
                return userMap;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(userList);
    }

    @GetMapping("/employees/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> searchEmployees(@RequestParam String query) {
        List<User> users = userRepository.findAll().stream()
            .filter(user ->
                user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                user.getEmail().toLowerCase().contains(query.toLowerCase())
            )
            .collect(Collectors.toList());

        List<Map<String, Object>> userList = users.stream()
            .map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("role", user.getRole().toString());
                return userMap;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(userList);
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsersExceptCurrent() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        List<User> users = userRepository.findAll().stream()
            .filter(user -> !user.getUsername().equals(currentUsername))
            .collect(Collectors.toList());
        List<Map<String, Object>> userList = users.stream()
            .map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("role", user.getRole().toString());
                return userMap;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(userList);
    }
}

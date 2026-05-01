package com.officeseatbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*", maxAge = 3600)
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        // Check database connection
        boolean dbConnected = false;
        try (Connection connection = dataSource.getConnection()) {
            dbConnected = connection.isValid(5);
        } catch (Exception e) {
            // Database connection failed
        }

        // Check Redis connection
        boolean redisConnected = false;
        try {
            redisTemplate.opsForValue().set("health-check", "ok");
            String result = (String) redisTemplate.opsForValue().get("health-check");
            redisConnected = "ok".equals(result);
            redisTemplate.delete("health-check");
        } catch (Exception e) {
            // Redis connection failed
        }

        health.put("status", (dbConnected && redisConnected) ? "UP" : "DOWN");
        health.put("database", dbConnected ? "UP" : "DOWN");
        health.put("redis", redisConnected ? "UP" : "DOWN");
        health.put("timestamp", java.time.LocalDateTime.now());

        return ResponseEntity.ok(health);
    }
}

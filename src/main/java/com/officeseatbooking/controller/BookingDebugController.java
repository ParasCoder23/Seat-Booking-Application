package com.officeseatbooking.controller;

import com.officeseatbooking.entity.Seat;
import com.officeseatbooking.entity.User;
import com.officeseatbooking.service.BookingService;
import com.officeseatbooking.service.RedisLockService;
import com.officeseatbooking.service.SeatService;
import com.officeseatbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug/booking")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BookingDebugController {

    @Autowired
    private SeatService seatService;

    @Autowired
    private RedisLockService redisLockService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/test-flow")
    public ResponseEntity<Map<String, Object>> testBookingFlow() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                response.put("error", "User not authenticated");
                return ResponseEntity.ok(response);
            }

            String username = auth.getName();
            User currentUser = userRepository.findByUsername(username).orElse(null);
            if (currentUser == null) {
                response.put("error", "User not found: " + username);
                return ResponseEntity.ok(response);
            }

            response.put("currentUser", Map.of(
                "id", currentUser.getId(),
                "username", currentUser.getUsername(),
                "role", currentUser.getRole()
            ));

            // Check available seats
            List<Seat> availableSeats = seatService.findSeatsByStatus(com.officeseatbooking.enums.SeatStatus.AVAILABLE);
            response.put("availableSeatsCount", availableSeats.size());

            if (!availableSeats.isEmpty()) {
                Seat testSeat = availableSeats.get(0);
                response.put("testSeat", Map.of(
                    "id", testSeat.getId(),
                    "seatNumber", testSeat.getSeatNumber(),
                    "status", testSeat.getStatus(),
                    "type", testSeat.getType()
                ));

                // Test Redis connection
                try {
                    redisTemplate.opsForValue().set("test_connection", "ok");
                    String result = (String) redisTemplate.opsForValue().get("test_connection");
                    response.put("redisConnection", "ok".equals(result) ? "SUCCESS" : "FAILED");
                    redisTemplate.delete("test_connection");
                } catch (Exception e) {
                    response.put("redisConnection", "FAILED: " + e.getMessage());
                }

                // Test seat locking
                try {
                    boolean lockResult = redisLockService.acquireLock(testSeat.getId(), currentUser.getId().toString());
                    response.put("seatLockTest", lockResult ? "SUCCESS" : "FAILED");

                    if (lockResult) {
                        // Check lock status
                        String lockOwner = redisLockService.getLockOwner(testSeat.getId());
                        response.put("lockOwner", lockOwner);

                        // Release the test lock
                        redisLockService.releaseLock(testSeat.getId(), currentUser.getId().toString());
                        response.put("lockReleased", "SUCCESS");
                    }
                } catch (Exception e) {
                    response.put("seatLockTest", "FAILED: " + e.getMessage());
                }
            } else {
                response.put("error", "No available seats found");
            }

        } catch (Exception e) {
            response.put("error", "Unexpected error: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/redis-status")
    public ResponseEntity<Map<String, Object>> checkRedisStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Test basic Redis operations
            redisTemplate.opsForValue().set("health_check", "test", java.time.Duration.ofSeconds(10));
            String result = (String) redisTemplate.opsForValue().get("health_check");
            response.put("basicOperation", "test".equals(result) ? "SUCCESS" : "FAILED");

            // Check existing locks
            java.util.Set<String> lockKeys = redisTemplate.keys("seat_lock:*");
            response.put("existingLocks", lockKeys != null ? lockKeys : java.util.Collections.emptySet());
            response.put("lockCount", lockKeys != null ? lockKeys.size() : 0);

            // Test lock operations
            boolean testLock = redisTemplate.opsForValue().setIfAbsent(
                "test_lock", "test_value", java.time.Duration.ofMinutes(1)
            );
            response.put("lockOperation", testLock ? "SUCCESS" : "ALREADY_EXISTS");

            // Clean up test data
            redisTemplate.delete("health_check");
            redisTemplate.delete("test_lock");

        } catch (Exception e) {
            response.put("error", "Redis error: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-locks")
    public ResponseEntity<Map<String, Object>> resetAllLocks() {
        Map<String, Object> response = new HashMap<>();

        try {
            java.util.Set<String> lockKeys = redisTemplate.keys("seat_lock:*");
            if (lockKeys != null && !lockKeys.isEmpty()) {
                redisTemplate.delete(lockKeys);
                response.put("locksDeleted", lockKeys.size());
                response.put("deletedKeys", lockKeys);
            } else {
                response.put("locksDeleted", 0);
                response.put("message", "No locks found");
            }
        } catch (Exception e) {
            response.put("error", "Failed to reset locks: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user-permissions/{seatId}")
    public ResponseEntity<Map<String, Object>> checkUserPermissions(@PathVariable Long seatId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userRepository.findByUsername(username).orElse(null);

            if (currentUser == null) {
                response.put("error", "User not found");
                return ResponseEntity.ok(response);
            }

            Seat seat = seatService.findSeatById(seatId);

            response.put("user", Map.of(
                "username", currentUser.getUsername(),
                "role", currentUser.getRole()
            ));

            response.put("seat", Map.of(
                "id", seat.getId(),
                "seatNumber", seat.getSeatNumber(),
                "type", seat.getType(),
                "status", seat.getStatus()
            ));

            // Check permissions
            boolean canBook = true;
            String reason = "";

            if (currentUser.getRole() == com.officeseatbooking.enums.Role.EMPLOYEE &&
                seat.getType() == com.officeseatbooking.enums.SeatType.MEETING_ROOM) {
                canBook = false;
                reason = "Employees cannot book meeting rooms";
            }

            if (seat.getStatus() != com.officeseatbooking.enums.SeatStatus.AVAILABLE) {
                canBook = false;
                reason = "Seat is not available";
            }

            response.put("canBook", canBook);
            response.put("reason", reason);

        } catch (Exception e) {
            response.put("error", "Error checking permissions: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}

package com.officeseatbooking.controller;

import com.officeseatbooking.entity.Booking;
import com.officeseatbooking.entity.Seat;
import com.officeseatbooking.entity.User;
import com.officeseatbooking.repository.BookingRepository;
import com.officeseatbooking.repository.SeatRepository;
import com.officeseatbooking.repository.UserRepository;
import com.officeseatbooking.service.RedisLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug/database")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DatabaseDebugController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisLockService redisLockService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check database connection
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                response.put("databaseConnected", true);
                response.put("databaseURL", metaData.getURL());
                response.put("databaseName", conn.getCatalog());

                // Check tables exist
                List<String> tables = new ArrayList<>();
                ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (tableName.equals("users") || tableName.equals("seats") || tableName.equals("bookings")) {
                        tables.add(tableName);
                    }
                }
                response.put("requiredTables", tables);
                response.put("allTablesExist", tables.size() == 3);

            }

            // Check data counts
            long userCount = userRepository.count();
            long seatCount = seatRepository.count();
            long bookingCount = bookingRepository.count();

            response.put("userCount", userCount);
            response.put("seatCount", seatCount);
            response.put("bookingCount", bookingCount);

            // Check if demo users exist
            boolean adminExists = userRepository.findByUsername("admin").isPresent();
            boolean managerExists = userRepository.findByUsername("manager").isPresent();
            boolean employeeExists = userRepository.findByUsername("employee").isPresent();

            response.put("demoUsersExist", Map.of(
                "admin", adminExists,
                "manager", managerExists,
                "employee", employeeExists
            ));

            // Sample data
            if (userCount > 0) {
                List<User> sampleUsers = userRepository.findAll();
                response.put("sampleUsers", sampleUsers.stream().limit(3).map(user -> Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole()
                )).toList());
            }

            if (seatCount > 0) {
                List<Seat> sampleSeats = seatRepository.findAll();
                response.put("sampleSeats", sampleSeats.stream().limit(5).map(seat -> Map.of(
                    "id", seat.getId(),
                    "seatNumber", seat.getSeatNumber(),
                    "type", seat.getType(),
                    "status", seat.getStatus(),
                    "floor", seat.getFloor()
                )).toList());
            }

            response.put("status", "SUCCESS");

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("databaseConnected", false);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/columns/{tableName}")
    public ResponseEntity<Map<String, Object>> getTableColumns(@PathVariable String tableName) {
        Map<String, Object> response = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            List<Map<String, Object>> columns = new ArrayList<>();
            ResultSet rs = metaData.getColumns(null, null, tableName, null);

            while (rs.next()) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", rs.getString("COLUMN_NAME"));
                column.put("type", rs.getString("TYPE_NAME"));
                column.put("size", rs.getInt("COLUMN_SIZE"));
                column.put("nullable", rs.getString("IS_NULLABLE"));
                column.put("autoIncrement", rs.getString("IS_AUTOINCREMENT"));
                columns.add(column);
            }

            response.put("tableName", tableName);
            response.put("columns", columns);
            response.put("columnCount", columns.size());

        } catch (Exception e) {
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-demo-data")
    public ResponseEntity<Map<String, Object>> resetDemoData() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Delete all bookings first (foreign key constraints)
            bookingRepository.deleteAll();

            // Reset users if needed
            long userCount = userRepository.count();
            if (userCount == 0) {
                // Data initializer should handle this automatically
                response.put("message", "No users found - they should be auto-created on next app start");
            } else {
                response.put("message", "Demo users already exist");
            }

            // Check seat count
            long seatCount = seatRepository.count();
            response.put("userCount", userRepository.count());
            response.put("seatCount", seatCount);
            response.put("bookingCount", bookingRepository.count());

            if (seatCount == 0) {
                response.put("warning", "No seats found - they should be auto-created on app start");
            }

        } catch (Exception e) {
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/redis-status")
    public ResponseEntity<Map<String, Object>> getRedisStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Test basic Redis connectivity
            redisTemplate.opsForValue().set("health_check", "test", Duration.ofSeconds(10));
            String result = (String) redisTemplate.opsForValue().get("health_check");

            if ("test".equals(result)) {
                response.put("redis", "UP");
                response.put("connectivity", "SUCCESS");
                response.put("message", "Redis is connected and working");
            } else {
                response.put("redis", "DOWN");
                response.put("connectivity", "FAILED");
                response.put("message", "Redis connectivity test failed");
            }

            // Clean up test key
            redisTemplate.delete("health_check");

        } catch (Exception e) {
            response.put("redis", "DOWN");
            response.put("connectivity", "ERROR");
            response.put("message", "Redis connection error: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-lock")
    public ResponseEntity<Map<String, Object>> testLock(@RequestParam Long seatId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Test Redis lock functionality
            String testUserId = "test_user_" + System.currentTimeMillis();

            // Try to acquire lock
            boolean acquired = redisLockService.acquireLock(seatId, testUserId);
            response.put("lockAcquired", acquired);

            if (acquired) {
                // Check lock owner
                String owner = redisLockService.getLockOwner(seatId);
                response.put("lockOwner", owner);
                response.put("isLocked", redisLockService.isLocked(seatId));

                // Release lock
                redisLockService.releaseLock(seatId, testUserId);
                response.put("lockReleased", true);
                response.put("isLockedAfterRelease", redisLockService.isLocked(seatId));

                response.put("testResult", "SUCCESS");
                response.put("message", "Redis lock test completed successfully");
            } else {
                response.put("testResult", "FAILED");
                response.put("message", "Failed to acquire test lock");
                response.put("existingOwner", redisLockService.getLockOwner(seatId));
            }

        } catch (Exception e) {
            response.put("testResult", "ERROR");
            response.put("message", "Redis lock test error: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(response);
    }
}

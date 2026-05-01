package com.officeseatbooking.controller;

import com.officeseatbooking.dto.BookingRequest;
import com.officeseatbooking.dto.BookingResponse;
import com.officeseatbooking.enums.BookingStatus;
import com.officeseatbooking.service.BookingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private BookingService bookingService;

    @PostMapping("/lock/{seatId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> lockSeat(@PathVariable Long seatId) {
        try {
            Map<String, Object> result = bookingService.lockSeatWithInfo(seatId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error locking seat {}: {}", seatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/unlock/{seatId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> unlockSeat(@PathVariable Long seatId) {
        try {
            bookingService.unlockSeat(seatId);
            return ResponseEntity.ok(Map.of("message", "Seat unlocked successfully"));
        } catch (Exception e) {
            logger.error("Error unlocking seat {}: {}", seatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequest bookingRequest) {
        try {
            logger.info("Received booking request for seat {} from {} to {} for user(s)",
                       bookingRequest.getSeatId(), bookingRequest.getStartTime(), bookingRequest.getEndTime());

            // If bookedForUserId is null/0, set it to current user
            if (bookingRequest.getBookedForUserId() == null || bookingRequest.getBookedForUserId() == 0) {
                bookingRequest.setBookedForUserId(bookingService.getCurrentUserId());
            }

            // If bookedByUserId is null/0, set it to current user
            if (bookingRequest.getBookedByUserId() == null || bookingRequest.getBookedByUserId() == 0) {
                bookingRequest.setBookedByUserId(bookingService.getCurrentUserId());
            }

            List<BookingResponse> bookings = bookingService.createBookingForUsers(bookingRequest);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            logger.error("Error creating booking: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getUserBookings() {
        try {
            List<BookingResponse> bookings = bookingService.getUserBookings();
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            logger.error("Error getting user bookings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve bookings: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBookings() {
        try {
            List<BookingResponse> bookings = bookingService.getAllBookings();
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            logger.error("Error getting all bookings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve all bookings: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getBookingById(@PathVariable Long id) {
        try {
            BookingResponse booking = bookingService.getBookingById(id);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            logger.error("Error getting booking {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Booking not found: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        try {
            BookingResponse booking = bookingService.cancelBooking(id);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            logger.error("Error cancelling booking {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to cancel booking: " + e.getMessage()));
        }
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public List<BookingResponse> getUserCalendarBookings() {
        List<BookingResponse> bookings = bookingService.getUserCalendarBookings();
        // Mark completed bookings
        LocalDateTime now = LocalDateTime.now();
        bookings.forEach(b -> {
            if (b.getEndTime() != null && b.getEndTime().isBefore(now)) {
                b.setStatus(BookingStatus.COMPLETED);
            }
        });
        return bookings;
    }

    @PostMapping("/meeting-room")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> bookMeetingRoom(@Valid @RequestBody BookingRequest bookingRequest) {
        try {
            // bookingRequest.bookedForUserIds should contain the list of employee IDs
            if (bookingRequest.getBookedForUserIds() == null || bookingRequest.getBookedForUserIds().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "You must select at least one employee for the meeting room booking"));
            }
            if (bookingRequest.getBookedForUserIds().size() > 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "You can select a maximum of 6 employees for a meeting room booking"));
            }
            List<BookingResponse> bookings = bookingService.bookMeetingRoomForEmployees(
                bookingRequest.getSeatId(),
                bookingRequest.getStartTime(),
                bookingRequest.getEndTime(),
                bookingRequest.getBookedForUserIds()
            );
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            logger.error("Error booking meeting room: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

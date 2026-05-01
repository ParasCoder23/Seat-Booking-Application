package com.officeseatbooking.service;

import com.officeseatbooking.dto.SeatResponse;
import com.officeseatbooking.entity.Booking;
import com.officeseatbooking.entity.Seat;
import com.officeseatbooking.entity.User;
import com.officeseatbooking.enums.SeatStatus;
import com.officeseatbooking.enums.SeatType;
import com.officeseatbooking.exception.ResourceNotFoundException;
import com.officeseatbooking.repository.BookingRepository;
import com.officeseatbooking.repository.SeatRepository;
import com.officeseatbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatService {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RedisLockService redisLockService;

    @Autowired
    private UserRepository userRepository;

    public List<SeatResponse> getAllSeats() {
        return seatRepository.findAll().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<SeatResponse> getAvailableSeats() {
        return seatRepository.findByStatus(SeatStatus.AVAILABLE).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<SeatResponse> getSeatsByFloor(Integer floor) {
        return seatRepository.findByFloor(floor).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<SeatResponse> getSeatsByType(SeatType type) {
        return seatRepository.findByType(type).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<SeatResponse> getAvailableSeatsByFloor(Integer floor) {
        return seatRepository.findByFloorAndStatus(floor, SeatStatus.AVAILABLE).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<SeatResponse> getAvailableSeatsByType(SeatType type) {
        return seatRepository.findByTypeAndStatus(type, SeatStatus.AVAILABLE).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public SeatResponse getSeatById(Long id) {
        Seat seat = seatRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
        return convertToResponse(seat);
    }

    public Seat findSeatById(Long id) {
        return seatRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
    }

    public List<Seat> findSeatsByStatus(SeatStatus status) {
        return seatRepository.findByStatus(status);
    }

    public Seat updateSeatStatus(Long id, SeatStatus status) {
        Seat seat = findSeatById(id);
        seat.setStatus(status);
        return seatRepository.save(seat);
    }

    public SeatResponse createSeat(Seat seat) {
        Seat savedSeat = seatRepository.save(seat);
        return convertToResponse(savedSeat);
    }

    public void deleteSeat(Long id) {
        if (!seatRepository.existsById(id)) {
            throw new ResourceNotFoundException("Seat not found with id: " + id);
        }
        seatRepository.deleteById(id);
    }

    private SeatResponse convertToResponse(Seat seat) {
        SeatResponse response = new SeatResponse(
            seat.getId(),
            seat.getFloor(),
            seat.getSeatNumber(),
            seat.getType(),
            seat.getStatus()
        );

        // Check Redis lock status first
        String lockOwnerId = redisLockService.getLockOwner(seat.getId());
        if (lockOwnerId != null) {
            try {
                Long lockOwnerUserId = Long.parseLong(lockOwnerId);
                User lockOwnerUser = userRepository.findById(lockOwnerUserId).orElse(null);
                if (lockOwnerUser != null) {
                    response.setLocked(true);
                    response.setLockedByUsername(lockOwnerUser.getUsername());
                    response.setLockedByRole(lockOwnerUser.getRole().toString());

                    // Get lock remaining time
                    long remainingTime = redisLockService.getLockRemainingTime(seat.getId());
                    if (remainingTime > 0) {
                        response.setLockExpiresAt(System.currentTimeMillis() + (remainingTime * 1000));
                    }
                }
            } catch (NumberFormatException e) {
                // Invalid lock format, ignore
            }
        }

        // Check for ANY CONFIRMED bookings overlapping with current time
        // This ensures seats show as BOOKED even if seat status in DB hasn't been updated yet
        LocalDateTime now = LocalDateTime.now();
        List<Booking> currentBookings = bookingRepository.findActiveBookingsByTimeRange(
            seat.getId(), now, now // Look for bookings happening right now
        );

        if (!currentBookings.isEmpty()) {
            // There's an active booking for this seat RIGHT NOW
            Booking currentBooking = currentBookings.get(0);
            response.setStatus(SeatStatus.BOOKED); // Force status to BOOKED
            response.setBookedByUsername(currentBooking.getUser().getUsername());
            response.setBookedByRole(currentBooking.getUser().getRole().toString());
            response.setBookedFrom(currentBooking.getStartTime());
            response.setBookedUntil(currentBooking.getEndTime());
            response.setCurrentBookingId(currentBooking.getId());
        } else if (seat.getStatus() == SeatStatus.BOOKED) {
            // Seat status is BOOKED but no active bookings found
            // This means the booking has ended - update seat to AVAILABLE
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);

            // Update response status
            response.setStatus(SeatStatus.AVAILABLE);

            // Clear booking info since booking has ended
            response.setBookedByUsername(null);
            response.setBookedByRole(null);
            response.setBookedFrom(null);
            response.setBookedUntil(null);
            response.setCurrentBookingId(null);
        }

        return response;
    }
}

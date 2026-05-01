package com.officeseatbooking.service;

import com.officeseatbooking.entity.Booking;
import com.officeseatbooking.entity.Seat;
import com.officeseatbooking.enums.BookingStatus;
import com.officeseatbooking.enums.SeatStatus;
import com.officeseatbooking.repository.BookingRepository;
import com.officeseatbooking.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service to clean up and manage booking lifecycle
 * Automatically marks expired bookings as COMPLETED
 * Frees up seats when bookings expire
 */
@Service
public class BookingCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(BookingCleanupService.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatRepository seatRepository;

    /**
     * Scheduled task to mark expired bookings as COMPLETED
     * Runs every minute to ensure bookings are marked as completed promptly
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000) // Run every 60 seconds
    @Transactional
    public void markExpiredBookingsAsCompleted() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Find all confirmed bookings that have ended
            List<Booking> expiredBookings = bookingRepository.findExpiredConfirmedBookings(now);

            if (!expiredBookings.isEmpty()) {
                logger.info("Found {} expired bookings to mark as completed", expiredBookings.size());

                for (Booking booking : expiredBookings) {
                    try {
                        booking.setStatus(BookingStatus.COMPLETED);
                        Booking savedBooking = bookingRepository.save(booking);
                        logger.info("Marked booking {} as COMPLETED (User: {}, Seat: {}, End time: {})",
                                   savedBooking.getId(),
                                   savedBooking.getUser().getUsername(),
                                   savedBooking.getSeat().getSeatNumber(),
                                   savedBooking.getEndTime());
                    } catch (Exception e) {
                        logger.error("Error marking booking {} as completed: ", booking.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in markExpiredBookingsAsCompleted scheduled task: ", e);
        }
    }

    /**
     * Clean up expired seat locks from Redis
     * Runs every 2 minutes
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 120000)
    public void cleanupExpiredSeatLocks() {
        try {
            logger.debug("Running cleanup for expired seat locks");
            // This is handled by Redis TTL, but we can add additional cleanup logic here if needed
        } catch (Exception e) {
            logger.error("Error in cleanupExpiredSeatLocks scheduled task: ", e);
        }
    }

    /**
     * Validate and correct seat status based on bookings
     * Ensures seats are marked as BOOKED only if there are active confirmed bookings
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000) // Run every 5 minutes
    @Transactional
    public void validateAndCorrectSeatStatus() {
        try {
            logger.debug("Running seat status validation");
            LocalDateTime now = LocalDateTime.now();

            List<Seat> seats = seatRepository.findAll();
            for (Seat seat : seats) {
                // Check if seat has any active confirmed bookings
                List<Booking> activeBookings = bookingRepository.findActiveBookingsByTimeRange(
                    seat.getId(),
                    now.minusHours(1),
                    now.plusDays(30)
                );

                boolean hasActiveBookings = !activeBookings.isEmpty();

                if (hasActiveBookings && seat.getStatus() != SeatStatus.BOOKED) {
                    // Seat should be marked as BOOKED
                    seat.setStatus(SeatStatus.BOOKED);
                    seatRepository.save(seat);
                    logger.debug("Corrected seat {} status to BOOKED", seat.getSeatNumber());
                }
                // Note: We don't automatically mark as AVAILABLE because other conditions might apply
            }
        } catch (Exception e) {
            logger.error("Error in validateAndCorrectSeatStatus scheduled task: ", e);
        }
    }
}


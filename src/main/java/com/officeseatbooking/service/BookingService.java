package com.officeseatbooking.service;

import com.officeseatbooking.dto.BookingRequest;
import com.officeseatbooking.dto.BookingResponse;
import com.officeseatbooking.entity.Booking;
import com.officeseatbooking.entity.Seat;
import com.officeseatbooking.entity.User;
import com.officeseatbooking.enums.BookingStatus;
import com.officeseatbooking.enums.Role;
import com.officeseatbooking.enums.SeatStatus;
import com.officeseatbooking.enums.SeatType;
import com.officeseatbooking.exception.BookingConflictException;
import com.officeseatbooking.exception.ResourceNotFoundException;
import com.officeseatbooking.exception.SeatLockedException;
import com.officeseatbooking.exception.UnauthorizedException;
import com.officeseatbooking.repository.BookingRepository;
import com.officeseatbooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatService seatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisLockService redisLockService;

    public Map<String, Object> lockSeatWithInfo(Long seatId) {
        logger.info("Attempting to lock seat {} for user", seatId);
        User currentUser = getCurrentUser();
        Seat seat = seatService.findSeatById(seatId);
        Map<String, Object> result = new HashMap<>();
        result.put("seatId", seatId);
        result.put("seatNumber", seat.getSeatNumber());

        try {
            // Check if seat is available
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                logger.warn("Seat {} is not available for booking. Current status: {}", seat.getSeatNumber(), seat.getStatus());
                result.put("locked", false);
                result.put("success", false);
                result.put("message", "Seat is not available for booking");
                return result;
            }

            // Check if seat is already locked
            String existingLockOwner = redisLockService.getLockOwner(seatId);
            logger.debug("Checking existing lock for seat {}. Lock owner: {}", seatId, existingLockOwner);

            if (existingLockOwner != null) {
                try {
                    Long lockOwnerUserId = Long.parseLong(existingLockOwner);

                    if (lockOwnerUserId.equals(currentUser.getId())) {
                        // User already has the lock - extend it
                        logger.info("User {} already has lock on seat {}, extending lock", currentUser.getUsername(), seat.getSeatNumber());
                        boolean extended = redisLockService.extendLock(seatId, currentUser.getId().toString());

                        if (extended) {
                            result.put("locked", true);
                            result.put("success", true);
                            result.put("lockedBy", currentUser.getUsername());
                            result.put("lockedByRole", currentUser.getRole().toString());
                            result.put("message", "You already have this seat locked. Lock extended for 2 more minutes.");
                            result.put("isOwner", true);
                            result.put("expiresAt", System.currentTimeMillis() + (2 * 60 * 1000));
                        } else {
                            result.put("locked", false);
                            result.put("success", false);
                            result.put("message", "Failed to extend your lock. Please try again.");
                        }
                        return result;
                    } else {
                        // Another user has locked this seat
                        User lockOwnerUser = userRepository.findById(lockOwnerUserId).orElse(null);
                        if (lockOwnerUser != null) {
                            long remainingTime = redisLockService.getLockRemainingTime(seatId);
                            String timeMessage = remainingTime > 0 ? String.format(" (expires in %d seconds)", remainingTime) : "";

                            logger.warn("Seat {} is already locked by user: {} ({}){}",
                                       seat.getSeatNumber(), lockOwnerUser.getUsername(), lockOwnerUser.getRole(), timeMessage);

                            result.put("locked", false);
                            result.put("success", false);
                            result.put("lockedBy", lockOwnerUser.getUsername());
                            result.put("lockedByRole", lockOwnerUser.getRole().toString());
                            result.put("message", String.format("This seat is currently selected by %s (%s)%s",
                                                               lockOwnerUser.getUsername(), lockOwnerUser.getRole(), timeMessage));
                            result.put("isOwner", false);
                            result.put("remainingTime", remainingTime);
                            return result;
                        } else {
                            // Lock exists but user not found
                            logger.warn("Seat {} is locked by unknown user ID: {}", seat.getSeatNumber(), existingLockOwner);
                            result.put("locked", false);
                            result.put("success", false);
                            result.put("message", "This seat is currently being selected by another user");
                            result.put("isOwner", false);
                            return result;
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid lock format for seat {}: {}", seat.getSeatNumber(), existingLockOwner);
                    result.put("locked", false);
                    result.put("success", false);
                    result.put("message", "Seat is currently locked");
                    result.put("isOwner", false);
                    return result;
                }
            }

            // Try to acquire new lock
            logger.debug("Attempting to acquire Redis lock for seat {} by user {}", seatId, currentUser.getId());
            boolean lockAcquired = redisLockService.acquireLock(seatId, currentUser.getId().toString());

            if (!lockAcquired) {
                // Check again if someone else just locked it
                String newLockOwner = redisLockService.getLockOwner(seatId);
                if (newLockOwner != null && !newLockOwner.equals(currentUser.getId().toString())) {
                    User lockOwnerUser = userRepository.findById(Long.parseLong(newLockOwner)).orElse(null);
                    String lockOwnerName = lockOwnerUser != null ? lockOwnerUser.getUsername() : "another user";
                    result.put("locked", false);
                    result.put("success", false);
                    result.put("message", String.format("Seat was just selected by %s. Please try another seat.", lockOwnerName));
                    if (lockOwnerUser != null) {
                        result.put("lockedBy", lockOwnerUser.getUsername());
                        result.put("lockedByRole", lockOwnerUser.getRole().toString());
                    }
                } else {
                    result.put("locked", false);
                    result.put("success", false);
                    result.put("message", "Failed to select seat - please try again");
                }
                result.put("isOwner", false);
                return result;
            }

            logger.info("Successfully locked seat {} for user {} for 2 minutes", seat.getSeatNumber(), currentUser.getUsername());
            result.put("locked", true);
            result.put("success", true);
            result.put("lockedBy", currentUser.getUsername());
            result.put("lockedByRole", currentUser.getRole().toString());
            result.put("message", "Seat selected successfully! You have 2 minutes to complete booking.");
            result.put("isOwner", true);
            result.put("expiresAt", System.currentTimeMillis() + (2 * 60 * 1000)); // 2 minutes from now

            return result;

        } catch (Exception e) {
            logger.error("Error occurred while locking seat {}: ", seatId, e);
            result.put("locked", false);
            result.put("success", false);
            result.put("message", "Error occurred while selecting seat: " + e.getMessage());
            result.put("isOwner", false);
            return result;
        }
    }

    public void unlockSeat(Long seatId) {
        User currentUser = getCurrentUser();
        String lockOwner = redisLockService.getLockOwner(seatId);
        if (lockOwner != null && lockOwner.equals(currentUser.getId().toString())) {
            redisLockService.releaseLock(seatId, currentUser.getId().toString());
        }
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest bookingRequest) {
        try {
            logger.info("Creating booking for seat {} from {} to {}",
                       bookingRequest.getSeatId(), bookingRequest.getStartTime(), bookingRequest.getEndTime());

            // Validate request
            if (bookingRequest.getSeatId() == null) {
                logger.error("Booking request missing seatId");
                throw new BookingConflictException("Seat ID is required");
            }
            if (bookingRequest.getStartTime() == null) {
                logger.error("Booking request missing startTime");
                throw new BookingConflictException("Start time is required");
            }
            if (bookingRequest.getEndTime() == null) {
                logger.error("Booking request missing endTime");
                throw new BookingConflictException("End time is required");
            }

            User currentUser = getCurrentUser();
            logger.info("Current user for booking: {} (ID: {}, Role: {})",
                       currentUser.getUsername(), currentUser.getId(), currentUser.getRole());

            Seat seat = seatService.findSeatById(bookingRequest.getSeatId());
            logger.info("Found seat: {} (Status: {}, Type: {})",
                       seat.getSeatNumber(), seat.getStatus(), seat.getType());

            // CRITICAL: Double-check lock ownership before proceeding
            logger.debug("CRITICAL: Validating lock ownership before booking...");
            String lockOwner = redisLockService.getLockOwner(bookingRequest.getSeatId());
            logger.debug("Lock owner for seat {}: {}, Expected: {}",
                        bookingRequest.getSeatId(), lockOwner, currentUser.getId().toString());

            if (lockOwner == null) {
                logger.error("CRITICAL: No lock found on seat {}. User {} cannot book without lock",
                            seat.getSeatNumber(), currentUser.getUsername());
                throw new SeatLockedException("You must lock the seat first before booking. The seat lock may have expired.");
            }

            if (!lockOwner.equals(currentUser.getId().toString())) {
                logger.error("CRITICAL: User {} does not own lock on seat {}. Lock owner: {}",
                            currentUser.getUsername(), seat.getSeatNumber(), lockOwner);
                throw new SeatLockedException("You do not have the lock on this seat. Please lock the seat first.");
            }

            // Validate user permissions
            logger.debug("Validating user permissions...");
            validateUserBookingPermissions(currentUser, seat);
            logger.debug("User permissions validated for {} booking seat {}", currentUser.getRole(), seat.getType());

            // Validate booking times
            logger.debug("Validating booking times...");
            validateBookingTimes(bookingRequest.getStartTime(), bookingRequest.getEndTime());
            logger.debug("Booking times validated");

            // CRITICAL: Check for conflicts again with database-level locking
            logger.debug("CRITICAL: Checking for database-level seat conflicts...");
            List<Booking> seatConflicts = bookingRepository.findConflictingBookings(
                bookingRequest.getSeatId(),
                bookingRequest.getStartTime(),
                bookingRequest.getEndTime()
            );
            logger.debug("Found {} conflicting bookings for seat {}", seatConflicts.size(), seat.getSeatNumber());

            if (!seatConflicts.isEmpty()) {
                logger.error("CRITICAL: Seat booking conflict detected for seat {}. Conflicting bookings: {}",
                            seat.getSeatNumber(), seatConflicts.size());

                // Get the next available time after current conflicts
                Booking latestConflict = seatConflicts.stream()
                    .max((b1, b2) -> b1.getEndTime().compareTo(b2.getEndTime()))
                    .orElse(null);

                String errorMessage = "Seat is already booked for the requested time slot";
                if (latestConflict != null) {
                    errorMessage = "Seat is booked. It will be available after " +
                                 latestConflict.getEndTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }

                for (Booking conflict : seatConflicts) {
                    logger.error("Conflict: Booking {} by user {} from {} to {}",
                                conflict.getId(), conflict.getUser().getUsername(),
                                conflict.getStartTime(), conflict.getEndTime());
                }

                // Release the lock since booking failed
                redisLockService.releaseLock(bookingRequest.getSeatId(), currentUser.getId().toString());
                throw new BookingConflictException(errorMessage);
            }

            // Check user booking limits
            logger.debug("Checking user booking limits...");
            validateUserBookingLimits(currentUser);
            logger.debug("User booking limits validated for {}", currentUser.getUsername());

            // Create booking
            logger.debug("Creating booking entity...");
            Booking booking = new Booking(
                currentUser,
                seat,
                bookingRequest.getStartTime(),
                bookingRequest.getEndTime(),
                BookingStatus.CONFIRMED
            );
            logger.debug("Booking entity created, saving to database...");

            // Save booking with retry mechanism for concurrency
            try {
                booking = bookingRepository.save(booking);
                logger.info("Booking saved with ID: {}", booking.getId());

                // Update seat status to BOOKED
                seatService.updateSeatStatus(seat.getId(), SeatStatus.BOOKED);
                logger.debug("Seat {} status updated to BOOKED", seat.getSeatNumber());

            } catch (Exception e) {
                logger.error("Database error while saving booking: ", e);
                // Release the lock on database error
                redisLockService.releaseLock(bookingRequest.getSeatId(), currentUser.getId().toString());
                throw new BookingConflictException("Failed to save booking due to database conflict: " + e.getMessage());
            }

            // Only release lock AFTER successful database save
            logger.debug("Releasing lock for seat {} after successful booking...", bookingRequest.getSeatId());
            boolean lockReleased = redisLockService.releaseLock(bookingRequest.getSeatId(), currentUser.getId().toString());

            if (!lockReleased) {
                logger.warn("Failed to release lock for seat {} after booking, but booking was successful", seat.getSeatNumber());
            } else {
                logger.debug("Lock released successfully");
            }

            logger.info("Booking created successfully for user {} on seat {} (Booking ID: {})",
                       currentUser.getUsername(), seat.getSeatNumber(), booking.getId());

            return convertToResponse(booking);

        } catch (SeatLockedException | BookingConflictException | UnauthorizedException | ResourceNotFoundException e) {
            // Re-throw business logic exceptions
            logger.error("Business logic error in createBooking: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in createBooking: ", e);
            throw new RuntimeException("Failed to create booking: " + e.getMessage(), e);
        }
    }

    public List<BookingResponse> createBookingForUsers(BookingRequest request) {
        logger.info("Creating booking for seat {} for users (type: {})", request.getSeatId(), request.getSeatType());

        // Validate inputs
        if (request.getSeatId() == null) {
            throw new RuntimeException("Seat ID is required");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new RuntimeException("Start and end times are required");
        }
        if (request.getBookedByUserId() == null) {
            request.setBookedByUserId(getCurrentUserId());
        }

        // Get current user
        User currentUser = getCurrentUser();
        logger.info("Current user: {} (ID: {})", currentUser.getUsername(), currentUser.getId());

        // Get seat
        Seat seat = seatService.findSeatById(request.getSeatId());
        logger.info("Booking seat: {}", seat.getSeatNumber());

        // Check and release lock after booking
        String lockOwner = redisLockService.getLockOwner(request.getSeatId());
        logger.debug("Current lock owner for seat {}: {}", seat.getSeatNumber(), lockOwner);

        try {
            // If meeting room, book for all selected users
            if ("MEETING_ROOM".equals(request.getSeatType())) {
                List<Long> userIds = request.getBookedForUserIds();
                if (userIds == null || userIds.isEmpty()) {
                    throw new RuntimeException("No employees selected for meeting room booking. Please select at least one employee.");
                }
                logger.info("Booking meeting room for {} employees", userIds.size());

                // Check for conflicts for all users (only consider CONFIRMED bookings)
                for (Long userId : userIds) {
                    List<Booking> conflicts = bookingRepository.findConflictingBookingsForUser(userId, request.getStartTime(), request.getEndTime())
                        .stream()
                        .filter(b -> b.getStatus() == com.officeseatbooking.enums.BookingStatus.CONFIRMED)
                        .toList();
                    if (!conflicts.isEmpty()) {
                        throw new RuntimeException("User with ID " + userId + " already has a booking in this time slot");
                    }
                }

                // Create bookings for all users
                List<BookingResponse> responses = new java.util.ArrayList<>();
                for (Long userId : userIds) {
                    Booking booking = new Booking();
                    // Set the user who is making the booking (admin/manager)
                    User bookedByUser = userRepository.findById(request.getBookedByUserId()).orElseThrow(
                        () -> new RuntimeException("Booking user with ID " + request.getBookedByUserId() + " not found")
                    );
                    booking.setUser(bookedByUser);
                    // Set the seat
                    booking.setSeat(seat);
                    // Set the user for whom the booking is made
                    User bookedForUser = userRepository.findById(userId).orElseThrow(
                        () -> new RuntimeException("Employee with ID " + userId + " not found")
                    );
                    booking.setBookedForUser(bookedForUser);
                    booking.setStartTime(request.getStartTime());
                    booking.setEndTime(request.getEndTime());
                    booking.setStatus(com.officeseatbooking.enums.BookingStatus.CONFIRMED);
                    bookingRepository.save(booking);
                    logger.info("Created booking ID {} for user {} for seat {}", booking.getId(), bookedForUser.getUsername(), seat.getSeatNumber());
                    responses.add(new BookingResponse(
                        booking.getId(),
                        booking.getUser().getId(),
                        booking.getUser().getUsername(),
                        booking.getSeat().getId(),
                        booking.getSeat().getSeatNumber(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getStatus(),
                        booking.getCreatedAt(),
                        booking.getBookedForUser() != null ? booking.getBookedForUser().getId() : null,
                        booking.getBookedForUser() != null ? booking.getBookedForUser().getUsername() : null,
                        booking.getUser().getUsername()
                    ));
                }

                // Release lock after successful bookings
                if (lockOwner != null && lockOwner.equals(currentUser.getId().toString())) {
                    redisLockService.releaseLock(request.getSeatId(), lockOwner);
                    logger.info("Released lock for seat {}", seat.getSeatNumber());
                }

                return responses;
            } else {
                // Regular seat: only one user
                Long userId = request.getBookedForUserId();
                if (userId == null || userId == 0) {
                    throw new RuntimeException("User ID is required for seat booking");
                }
                logger.info("Booking regular seat for user ID: {}", userId);

                // Check for conflicts
                List<Booking> conflicts = bookingRepository.findConflictingBookingsForUser(userId, request.getStartTime(), request.getEndTime());
                if (!conflicts.isEmpty()) {
                    throw new RuntimeException("User already has a booking in this time slot");
                }

                Booking booking = new Booking();
                User bookedByUser = userRepository.findById(request.getBookedByUserId()).orElseThrow(
                    () -> new RuntimeException("Booking user with ID " + request.getBookedByUserId() + " not found")
                );
                booking.setUser(bookedByUser);
                booking.setSeat(seat);
                User bookedForUser = userRepository.findById(userId).orElseThrow(
                    () -> new RuntimeException("Employee with ID " + userId + " not found")
                );
                booking.setBookedForUser(bookedForUser);
                booking.setStartTime(request.getStartTime());
                booking.setEndTime(request.getEndTime());
                booking.setStatus(com.officeseatbooking.enums.BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                logger.info("Created booking ID {} for user {} for seat {}", booking.getId(), bookedForUser.getUsername(), seat.getSeatNumber());

                // Release lock after successful booking
                if (lockOwner != null && lockOwner.equals(currentUser.getId().toString())) {
                    redisLockService.releaseLock(request.getSeatId(), lockOwner);
                    logger.info("Released lock for seat {}", seat.getSeatNumber());
                }

                return java.util.List.of(new BookingResponse(
                    booking.getId(),
                    booking.getUser().getId(),
                    booking.getUser().getUsername(),
                    booking.getSeat().getId(),
                    booking.getSeat().getSeatNumber(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getStatus(),
                    booking.getCreatedAt(),
                    booking.getBookedForUser() != null ? booking.getBookedForUser().getId() : null,
                    booking.getBookedForUser() != null ? booking.getBookedForUser().getUsername() : null,
                    booking.getUser().getUsername()
                ));
            }
        } catch (Exception e) {
            logger.error("Error in createBookingForUsers: {}", e.getMessage(), e);
            // Try to release lock on error
            if (lockOwner != null && lockOwner.equals(currentUser.getId().toString())) {
                try {
                    redisLockService.releaseLock(request.getSeatId(), lockOwner);
                    logger.info("Released lock for seat after error");
                } catch (Exception releaseError) {
                    logger.warn("Failed to release lock after error: {}", releaseError.getMessage());
                }
            }
            throw e;
        }
    }

    // Enhanced: Get all bookings where user is owner or bookedForUser
    // Groups meeting room bookings - fetches ALL meeting members so every user sees full attendee list
    public List<BookingResponse> getUserBookings() {
        User currentUser = getCurrentUser();
        List<Booking> bookings = bookingRepository.findUserBookingsExcludingCompleted(currentUser.getId());

        // Auto-mark expired bookings as COMPLETED
        LocalDateTime now = LocalDateTime.now();
        List<Booking> updatedBookings = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED && booking.getEndTime().isBefore(now)) {
                booking.setStatus(BookingStatus.COMPLETED);
                booking = bookingRepository.save(booking);
                logger.info("Auto-marked booking {} as COMPLETED", booking.getId());
            }
            updatedBookings.add(booking);
        }

        // Track which meetingGroupIds and admin-group keys we've already processed
        java.util.Set<Long> processedMeetingGroups = new java.util.HashSet<>();
        java.util.Set<String> processedAdminGroups = new java.util.HashSet<>();
        Map<String, List<Booking>> adminBookedGroups = new HashMap<>();
        List<BookingResponse> responses = new ArrayList<>();

        for (Booking booking : updatedBookings) {
            if (booking.getIsMeetingRoom() && booking.getMeetingGroupId() != null) {
                Long groupId = booking.getMeetingGroupId();
                if (!processedMeetingGroups.contains(groupId)) {
                    processedMeetingGroups.add(groupId);
                    // Fetch ALL bookings for this meetingGroupId so every attendee is visible
                    List<Booking> allGroupBookings = bookingRepository.findByMeetingGroupId(groupId);
                    if (!allGroupBookings.isEmpty()) {
                        responses.add(convertMeetingToResponse(allGroupBookings));
                    }
                }
            } else if (booking.getBookedForUser() != null && !booking.getUser().getId().equals(booking.getBookedForUser().getId())) {
                // Admin booked for someone else - group by seat + time
                String groupKey = booking.getSeat().getId() + "_" + booking.getStartTime() + "_" + booking.getEndTime();
                if (!processedAdminGroups.contains(groupKey)) {
                    processedAdminGroups.add(groupKey);
                    // Fetch ALL bookings for this seat+time so every member is visible
                    List<Booking> allAdminBookings = bookingRepository.findBySeatAndTime(
                        booking.getSeat().getId(), booking.getStartTime(), booking.getEndTime());
                    if (!allAdminBookings.isEmpty()) {
                        responses.add(convertAdminBookedToResponse(allAdminBookings));
                    }
                }
            } else {
                // Regular self-booking
                responses.add(convertToResponse(booking));
            }
        }


        return responses.stream()
            .distinct()
            // Sort by startTime DESC (most recent first)
            .sorted((a, b) -> {
                if (a.getStartTime() == null) return 1;
                if (b.getStartTime() == null) return -1;
                return b.getStartTime().compareTo(a.getStartTime());
            })
            .collect(Collectors.toList());
    }

    // Calendar view: get all bookings for user (owner or bookedForUser)
    // Shows all bookings including completed and cancelled
    // Groups meeting room bookings and admin-booked bookings
    // For calendar: Admins see ALL meetings. Employees see only meetings they attend
    public List<BookingResponse> getUserCalendarBookings() {
        Long userId = getCurrentUserId();
        User currentUser = getCurrentUser();

        List<Booking> bookings;
        
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER) {
            // Admins/Managers see ALL bookings in calendar
            bookings = bookingRepository.findAll();
        } else {
            // Employees see only bookings where they are involved
            List<Booking> allBookings = bookingRepository.findByUserOrBookedFor(userId);
            
            // Filter: Only include if user is actually attending meetings/admin bookings
            // Regular bookings: Include all they created or are booked for
            bookings = new ArrayList<>();
            for (Booking booking : allBookings) {
                if (booking.getIsMeetingRoom() || booking.getIsAdminBooked()) {
                    // For meetings/admin bookings: Only include if user is a PARTICIPANT
                    if (booking.getBookedForUser() != null && booking.getBookedForUser().getId().equals(userId)) {
                        bookings.add(booking);
                    }
                } else {
                    // Regular booking: Include if they created it or it's for them
                    if (booking.getUser().getId().equals(userId) || 
                        (booking.getBookedForUser() != null && booking.getBookedForUser().getId().equals(userId))) {
                        bookings.add(booking);
                    }
                }
            }
        }

        // Auto-mark expired bookings as COMPLETED
        LocalDateTime now = LocalDateTime.now();
        List<Booking> processedBookings = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED && booking.getEndTime().isBefore(now)) {
                booking.setStatus(BookingStatus.COMPLETED);
                booking = bookingRepository.save(booking);
                logger.info("Auto-marked booking {} as COMPLETED in calendar view", booking.getId());
            }
            // Include ALL bookings: confirmed, completed, and cancelled
            processedBookings.add(booking);
        }

        // Group meeting room bookings and admin-booked-for bookings
        // For each meetingGroupId, fetch ALL bookings so attendees list is complete
        java.util.Set<Long> processedMeetingGroups = new java.util.HashSet<>();
        java.util.Set<String> processedAdminGroups = new java.util.HashSet<>();
        List<BookingResponse> responses = new ArrayList<>();
        
        for (Booking booking : processedBookings) {
            if (booking.getIsMeetingRoom() && booking.getMeetingGroupId() != null) {
                Long groupId = booking.getMeetingGroupId();
                if (!processedMeetingGroups.contains(groupId)) {
                    processedMeetingGroups.add(groupId);
                    // Always fetch ALL bookings for this meeting so full attendee list is shown
                    List<Booking> allGroupBookings = bookingRepository.findByMeetingGroupId(groupId);
                    if (!allGroupBookings.isEmpty()) {
                        responses.add(convertMeetingToResponse(allGroupBookings));
                    }
                }
            } else if (booking.getBookedForUser() != null && !booking.getUser().getId().equals(booking.getBookedForUser().getId())) {
                String groupKey = booking.getSeat().getId() + "_" + booking.getStartTime() + "_" + booking.getEndTime();
                if (!processedAdminGroups.contains(groupKey)) {
                    processedAdminGroups.add(groupKey);
                    List<Booking> allAdminBookings = bookingRepository.findBySeatAndTime(
                        booking.getSeat().getId(), booking.getStartTime(), booking.getEndTime());
                    if (!allAdminBookings.isEmpty()) {
                        responses.add(convertAdminBookedToResponse(allAdminBookings));
                    }
                }
            } else {
                // Regular self-booking
                responses.add(convertToResponse(booking));
            }
        }


        return responses;
    }

    // Manager: Book meeting room for up to 6 employees
    @Transactional
    public List<BookingResponse> bookMeetingRoomForEmployees(Long seatId, LocalDateTime startTime, LocalDateTime endTime, List<Long> employeeIds) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.MANAGER && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only manager or admin can book meeting rooms for multiple employees");
        }
        if (employeeIds == null || employeeIds.isEmpty() || employeeIds.size() > 6) {
            throw new BookingConflictException("You must select between 1 and 6 employees for a meeting room booking");
        }
        Seat seat = seatService.findSeatById(seatId);
        if (seat.getType() != SeatType.MEETING_ROOM) {
            throw new BookingConflictException("Selected seat is not a meeting room");
        }
        // Check for conflicts for each employee (only consider CONFIRMED bookings)
        for (Long empId : employeeIds) {
            List<Booking> conflicts = bookingRepository.findConflictingBookingsForUser(empId, startTime, endTime)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .toList();
            if (!conflicts.isEmpty()) {
                throw new BookingConflictException("Employee with ID " + empId + " already has a booking during this time");
            }
        }

        // Create a group ID for all attendees of this meeting
        // Use first booking's ID as group ID
        long groupId = System.currentTimeMillis(); // Unique group ID for this meeting

        // Create bookings for each employee
        List<BookingResponse> responses = new java.util.ArrayList<>();
        List<Booking> groupBookings = new java.util.ArrayList<>();

        for (Long empId : employeeIds) {
            User emp = userRepository.findById(empId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + empId));
            Booking booking = new Booking(currentUser, seat, startTime, endTime, BookingStatus.CONFIRMED);
            booking.setBookedForUser(emp);
            booking.setIsMeetingRoom(true); // Mark as meeting room
            booking.setMeetingGroupId(groupId); // Set same group ID for all
            booking.setMeetingOrganizerId(currentUser.getId()); // Set organizer
            booking = bookingRepository.save(booking);
            groupBookings.add(booking);
        }

        seatService.updateSeatStatus(seatId, SeatStatus.BOOKED);

        // Return only ONE response per meeting (with all attendees)
        BookingResponse meetingResponse = convertMeetingToResponse(groupBookings);
        responses.add(meetingResponse);

        return responses;
    }

    public List<BookingResponse> getAllBookings() {
        User currentUser = getCurrentUser();

        // Only ADMIN can view all bookings
        if (currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only admins can view all bookings");
        }

        List<Booking> allBookings = bookingRepository.findAll();
        
        // Sort by ID descending (newest first)
        allBookings.sort((b1, b2) -> b2.getId().compareTo(b1.getId()));
        
        // Group meeting room bookings and admin-booked-for bookings
        Map<Long, List<Booking>> meetingGroups = new HashMap<>();
        Map<String, List<Booking>> adminBookedGroups = new HashMap<>();
        List<BookingResponse> responses = new ArrayList<>();
        
        for (Booking booking : allBookings) {
            if (booking.getIsMeetingRoom() && booking.getMeetingGroupId() != null) {
                // Meeting room booking
                meetingGroups.computeIfAbsent(booking.getMeetingGroupId(), k -> new ArrayList<>()).add(booking);
            } else if (booking.getBookedForUser() != null && !booking.getUser().getId().equals(booking.getBookedForUser().getId())) {
                // Admin booked for someone else - group by seat + time
                String groupKey = booking.getSeat().getId() + "_" + booking.getStartTime() + "_" + booking.getEndTime();
                adminBookedGroups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(booking);
            } else {
                // Regular self-booking
                responses.add(convertToResponse(booking));
            }
        }
        
        // Convert meeting groups to single BookingResponse per group
        for (List<Booking> groupBookings : meetingGroups.values()) {
            if (!groupBookings.isEmpty()) {
                responses.add(convertMeetingToResponse(groupBookings));
            }
        }

        // Convert admin-booked groups to single BookingResponse per group
        for (List<Booking> adminBookings : adminBookedGroups.values()) {
            if (!adminBookings.isEmpty()) {
                responses.add(convertAdminBookedToResponse(adminBookings));
            }
        }

        // Sort response by ID descending (newest first)
        responses.sort((r1, r2) -> r2.getId().compareTo(r1.getId()));

        return responses;
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        User currentUser = getCurrentUser();

        // Users can only view their own bookings unless they are ADMIN
        if (currentUser.getRole() != Role.ADMIN && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only view your own bookings");
        }

        return convertToResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        User currentUser = getCurrentUser();

        // Users can only cancel their own bookings unless they are ADMIN
        if (currentUser.getRole() != Role.ADMIN && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only cancel your own bookings");
        }

        // Check if booking can be cancelled (not in the past)
        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BookingConflictException("Cannot cancel a booking that has already started");
        }

        // Cancel booking
        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);

        // Update seat status back to available
        seatService.updateSeatStatus(booking.getSeat().getId(), SeatStatus.AVAILABLE);

        return convertToResponse(savedBooking);
    }

    private void validateUserBookingPermissions(User user, Seat seat) {
        // EMPLOYEE can only book regular seats
        if (user.getRole() == Role.EMPLOYEE && seat.getType() == SeatType.MEETING_ROOM) {
            throw new UnauthorizedException("Employees cannot book meeting rooms");
        }
    }

    private void validateUserBookingLimits(User user) {
        if (user.getRole() == Role.EMPLOYEE) {
            long activeBookings = bookingRepository.countActiveBookingsByUser(user.getId());
            if (activeBookings >= 1) {
                throw new BookingConflictException("Employees can only have one active booking at a time");
            }
        }
    }

    private void validateBookingTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BookingConflictException("Cannot book a seat in the past");
        }

        if (endTime.isBefore(startTime)) {
            throw new BookingConflictException("End time cannot be before start time");
        }

        if (startTime.equals(endTime)) {
            throw new BookingConflictException("Start time and end time cannot be the same");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    public Long getCurrentUserId() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.officeseatbooking.exception.UnauthorizedException("User not authenticated");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new com.officeseatbooking.exception.ResourceNotFoundException("User not found: " + username));
        return user.getId();
    }

    private BookingResponse convertToResponse(Booking booking) {
        // Determine who the booking is for and who made it
        Long bookedForUserId = null;
        String bookedForUsername = null;
        String bookedByUsername = null;

        // If booking has a 'bookedFor' field, use it; otherwise, fallback to booking user
        if (booking.getBookedForUser() != null) {
            bookedForUserId = booking.getBookedForUser().getId();
            bookedForUsername = booking.getBookedForUser().getUsername();
        } else {
            bookedForUserId = booking.getUser().getId();
            bookedForUsername = booking.getUser().getUsername();
        }
        bookedByUsername = booking.getUser().getUsername();

        return new BookingResponse(
            booking.getId(),
            booking.getUser().getId(),
            booking.getUser().getUsername(),
            booking.getSeat().getId(),
            booking.getSeat().getSeatNumber(),
            booking.getStartTime(),
            booking.getEndTime(),
            booking.getStatus(),
            booking.getCreatedAt(),
            bookedForUserId,
            bookedForUsername,
            bookedByUsername
        );
    }

    private BookingResponse convertAdminBookedToResponse(List<Booking> adminBookings) {
        if (adminBookings == null || adminBookings.isEmpty()) {
            return null;
        }

        // Use first booking as template
        Booking firstBooking = adminBookings.get(0);

        // Get list of all attendees
        java.util.List<String> attendeeNames = new java.util.ArrayList<>();
        java.util.List<Long> attendeeIds = new java.util.ArrayList<>();
        for (Booking booking : adminBookings) {
            if (booking.getBookedForUser() != null) {
                attendeeNames.add(booking.getBookedForUser().getUsername());
                attendeeIds.add(booking.getBookedForUser().getId());
            }
        }

        // Create response with admin booking details
        BookingResponse response = new BookingResponse(
            firstBooking.getId(),
            firstBooking.getUser().getId(),
            firstBooking.getUser().getUsername(),
            firstBooking.getSeat().getId(),
            firstBooking.getSeat().getSeatNumber(),
            firstBooking.getStartTime(),
            firstBooking.getEndTime(),
            firstBooking.getStatus(),
            firstBooking.getCreatedAt(),
            null, // bookedForUserId (not applicable for admin-booked)
            null, // bookedForUsername (not applicable for admin-booked)
            firstBooking.getUser().getUsername() // Admin who booked
        );

        // Set admin-booking specific fields (reuse meeting fields for consistency)
        response.setMeetingGroupId(null); // Not a meeting
        response.setIsMeetingRoom(false); // Not a meeting room
        response.setMeetingOrganizerName(firstBooking.getUser().getUsername()); // Admin who booked
        response.setMeetingAttendees(attendeeNames); // Employees booked for
        response.setMeetingAttendeeIds(attendeeIds); // Employee IDs booked for
        response.setIsAdminBooked(true); // Flag to indicate this is an admin booking

        return response;
    }

    private BookingResponse convertMeetingToResponse(List<Booking> meetingBookings) {
        if (meetingBookings == null || meetingBookings.isEmpty()) {
            return null;
        }

        // Use first booking as template
        Booking firstBooking = meetingBookings.get(0);

        // Get organizer info
        User organizer = userRepository.findById(firstBooking.getMeetingOrganizerId()).orElse(null);
        String organizerName = organizer != null ? organizer.getUsername() : "Unknown";

        // Get list of all attendees and their IDs
        java.util.List<String> attendeeNames = new java.util.ArrayList<>();
        java.util.List<Long> attendeeIds = new java.util.ArrayList<>();
        for (Booking booking : meetingBookings) {
            if (booking.getBookedForUser() != null) {
                attendeeNames.add(booking.getBookedForUser().getUsername());
                attendeeIds.add(booking.getBookedForUser().getId());
            }
        }

        // Create response with meeting details
        BookingResponse response = new BookingResponse(
            firstBooking.getId(),
            firstBooking.getUser().getId(),
            firstBooking.getUser().getUsername(),
            firstBooking.getSeat().getId(),
            firstBooking.getSeat().getSeatNumber(),
            firstBooking.getStartTime(),
            firstBooking.getEndTime(),
            firstBooking.getStatus(),
            firstBooking.getCreatedAt(),
            null, // bookedForUserId (not applicable for meeting)
            null, // bookedForUsername (not applicable for meeting)
            organizerName // Use organizer as booked by
        );

        // Set meeting-specific fields
        response.setMeetingGroupId(firstBooking.getMeetingGroupId());
        response.setIsMeetingRoom(true);
        response.setMeetingOrganizerName(organizerName);
        response.setMeetingAttendees(attendeeNames);
        response.setMeetingAttendeeIds(attendeeIds);

        return response;
    }
}

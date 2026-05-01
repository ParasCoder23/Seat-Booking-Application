package com.officeseatbooking.repository;

import com.officeseatbooking.entity.Booking;
import com.officeseatbooking.entity.Seat;
import com.officeseatbooking.entity.User;
import com.officeseatbooking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUser(User user);

    List<Booking> findByUserId(Long userId);

    List<Booking> findBySeat(Seat seat);

    List<Booking> findByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.seat.id = :seatId AND b.status = 'CONFIRMED' " +
           "AND ((b.startTime <= :endTime) AND (b.endTime >= :startTime))")
    List<Booking> findConflictingBookings(@Param("seatId") Long seatId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.status = 'CONFIRMED'")
    long countActiveBookingsByUser(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = 'CONFIRMED' " +
           "AND b.endTime > :currentTime")
    List<Booking> findActiveBookingsByUser(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT b FROM Booking b WHERE b.seat.id = :seatId AND b.status = 'CONFIRMED' " +
           "AND b.startTime <= :endTime AND b.endTime >= :startTime")
    List<Booking> findActiveBookingsByTimeRange(@Param("seatId") Long seatId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.endTime < :currentTime")
    List<Booking> findExpiredBookings(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND b.startTime >= :startTime AND b.startTime <= :endTime")
    List<Booking> findUpcomingBookings(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    List<Booking> findByBookedForUserId(Long userId);

    @Query("SELECT b FROM Booking b WHERE (b.user.id = :userId OR b.bookedForUser.id = :userId) AND b.status = 'CONFIRMED' " +
           "AND ((b.startTime <= :endTime) AND (b.endTime >= :startTime))")
    List<Booking> findConflictingBookingsForUser(@Param("userId") Long userId,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId OR b.bookedForUser.id = :userId")
    List<Booking> findByUserOrBookedFor(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b WHERE (b.user.id = :userId OR b.bookedForUser.id = :userId) " +
           "AND b.status = 'CONFIRMED' AND b.endTime > :currentTime")
    List<Booking> findActiveBookingsByUserOrBookedFor(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT b FROM Booking b WHERE (b.user.id = :userId OR b.bookedForUser.id = :userId) " +
           "ORDER BY b.startTime DESC")
    List<Booking> findUserBookingsExcludingCompleted(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b WHERE b.meetingGroupId = :meetingGroupId")
    List<Booking> findByMeetingGroupId(@Param("meetingGroupId") Long meetingGroupId);

    @Query("SELECT b FROM Booking b WHERE b.seat.id = :seatId AND b.startTime = :startTime AND b.endTime = :endTime")
    List<Booking> findBySeatAndTime(@Param("seatId") Long seatId,
                                    @Param("startTime") java.time.LocalDateTime startTime,
                                    @Param("endTime") java.time.LocalDateTime endTime);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.endTime <= :currentTime")
    List<Booking> findExpiredConfirmedBookings(@Param("currentTime") java.time.LocalDateTime currentTime);
}

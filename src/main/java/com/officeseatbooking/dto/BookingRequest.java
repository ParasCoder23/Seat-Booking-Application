package com.officeseatbooking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class BookingRequest {

    @NotNull(message = "Seat ID is required")
    private Long seatId;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    // Optional: For booking on behalf of another user (Admin/Manager feature)
    private Long bookedForUserId;

    // For meeting room bookings (multiple users)
    private java.util.List<Long> bookedForUserIds;
    // The user who is making the booking (admin/manager/employee)
    private Long bookedByUserId;
    // The type of seat (REGULAR, MEETING_ROOM, etc.)
    private String seatType;

    // Constructors
    public BookingRequest() {}

    public BookingRequest(Long seatId, LocalDateTime startTime, LocalDateTime endTime) {
        this.seatId = seatId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public BookingRequest(Long seatId, LocalDateTime startTime, LocalDateTime endTime, Long bookedForUserId) {
        this.seatId = seatId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bookedForUserId = bookedForUserId;
    }

    // Getters and Setters
    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getBookedForUserId() {
        return bookedForUserId;
    }

    public void setBookedForUserId(Long bookedForUserId) {
        this.bookedForUserId = bookedForUserId;
    }

    public java.util.List<Long> getBookedForUserIds() {
        return bookedForUserIds;
    }

    public void setBookedForUserIds(java.util.List<Long> bookedForUserIds) {
        this.bookedForUserIds = bookedForUserIds;
    }

    public Long getBookedByUserId() {
        return bookedByUserId;
    }

    public void setBookedByUserId(Long bookedByUserId) {
        this.bookedByUserId = bookedByUserId;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }
}

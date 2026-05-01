package com.officeseatbooking.dto;

import com.officeseatbooking.enums.SeatStatus;
import com.officeseatbooking.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class SeatResponse {

    private Long id;
    private Integer floor;
    private String seatNumber;
    private SeatType type;
    private SeatStatus status;

    // Booking information
    private String bookedByUsername;
    private String bookedByRole;
    private LocalDateTime bookedFrom;
    private LocalDateTime bookedUntil;
    private Long currentBookingId;

    // Lock information
    @JsonProperty("isLocked")
    private boolean isLocked;
    private String lockedByUsername;
    private String lockedByRole;
    private long lockExpiresAt;

    // Constructors
    public SeatResponse() {}

    public SeatResponse(Long id, Integer floor, String seatNumber, SeatType type, SeatStatus status) {
        this.id = id;
        this.floor = floor;
        this.seatNumber = seatNumber;
        this.type = type;
        this.status = status;
    }

    public SeatResponse(Long id, Integer floor, String seatNumber, SeatType type, SeatStatus status,
                       String bookedByUsername, String bookedByRole, LocalDateTime bookedFrom,
                       LocalDateTime bookedUntil, Long currentBookingId) {
        this.id = id;
        this.floor = floor;
        this.seatNumber = seatNumber;
        this.type = type;
        this.status = status;
        this.bookedByUsername = bookedByUsername;
        this.bookedByRole = bookedByRole;
        this.bookedFrom = bookedFrom;
        this.bookedUntil = bookedUntil;
        this.currentBookingId = currentBookingId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatType getType() {
        return type;
    }

    public void setType(SeatType type) {
        this.type = type;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public String getBookedByUsername() {
        return bookedByUsername;
    }

    public void setBookedByUsername(String bookedByUsername) {
        this.bookedByUsername = bookedByUsername;
    }

    public String getBookedByRole() {
        return bookedByRole;
    }

    public void setBookedByRole(String bookedByRole) {
        this.bookedByRole = bookedByRole;
    }

    public LocalDateTime getBookedFrom() {
        return bookedFrom;
    }

    public void setBookedFrom(LocalDateTime bookedFrom) {
        this.bookedFrom = bookedFrom;
    }

    public LocalDateTime getBookedUntil() {
        return bookedUntil;
    }

    public void setBookedUntil(LocalDateTime bookedUntil) {
        this.bookedUntil = bookedUntil;
    }

    public Long getCurrentBookingId() {
        return currentBookingId;
    }

    public void setCurrentBookingId(Long currentBookingId) {
        this.currentBookingId = currentBookingId;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public String getLockedByUsername() {
        return lockedByUsername;
    }

    public void setLockedByUsername(String lockedByUsername) {
        this.lockedByUsername = lockedByUsername;
    }

    public String getLockedByRole() {
        return lockedByRole;
    }

    public void setLockedByRole(String lockedByRole) {
        this.lockedByRole = lockedByRole;
    }

    public long getLockExpiresAt() {
        return lockExpiresAt;
    }

    public void setLockExpiresAt(long lockExpiresAt) {
        this.lockExpiresAt = lockExpiresAt;
    }
}

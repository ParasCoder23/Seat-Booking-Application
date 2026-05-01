package com.officeseatbooking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.officeseatbooking.entity.Booking;
import com.officeseatbooking.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

public class BookingResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long seatId;
    private String seatNumber;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    private BookingStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // Fields for booking on behalf functionality
    private Long bookedForUserId;
    private String bookedForUsername;
    private String bookedByUsername; // Who actually made the booking

    // Meeting room fields
    private Long meetingGroupId; // Group ID for all attendees of same meeting
    private Boolean isMeetingRoom; // Is this a meeting room booking
    private String meetingOrganizerName; // Who organized the meeting
    private List<String> meetingAttendees; // List of employee names in meeting
    private List<Long> meetingAttendeeIds; // List of employee IDs in meeting
    private Boolean isAdminBooked; // Is this an admin booking for employees

    // Constructors
    public BookingResponse() {}

    public BookingResponse(Long id, Long userId, String username, Long seatId, String seatNumber,
                          LocalDateTime startTime, LocalDateTime endTime, BookingStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdAt = createdAt;
    }

    public BookingResponse(Long id, Long userId, String username, Long seatId, String seatNumber,
                          LocalDateTime startTime, LocalDateTime endTime, BookingStatus status, LocalDateTime createdAt,
                          Long bookedForUserId, String bookedForUsername, String bookedByUsername) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdAt = createdAt;
        this.bookedForUserId = bookedForUserId;
        this.bookedForUsername = bookedForUsername;
        this.bookedByUsername = bookedByUsername;
    }

    public BookingResponse(Booking booking) {
        this.id = booking.getId();
        this.userId = booking.getUser() != null ? booking.getUser().getId() : null;
        this.username = booking.getUser() != null ? booking.getUser().getUsername() : null;
        this.seatId = booking.getSeat() != null ? booking.getSeat().getId() : null;
        this.seatNumber = booking.getSeat() != null ? booking.getSeat().getSeatNumber() : null;
        this.startTime = booking.getStartTime();
        this.endTime = booking.getEndTime();
        this.status = booking.getStatus();
        this.createdAt = booking.getCreatedAt();
        this.bookedForUserId = booking.getBookedForUser() != null ? booking.getBookedForUser().getId() : null;
        this.bookedForUsername = booking.getBookedForUser() != null ? booking.getBookedForUser().getUsername() : null;
        this.bookedByUsername = booking.getUser() != null ? booking.getUser().getUsername() : null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
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

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getBookedForUserId() {
        return bookedForUserId;
    }

    public void setBookedForUserId(Long bookedForUserId) {
        this.bookedForUserId = bookedForUserId;
    }

    public String getBookedForUsername() {
        return bookedForUsername;
    }

    public void setBookedForUsername(String bookedForUsername) {
        this.bookedForUsername = bookedForUsername;
    }

    public String getBookedByUsername() {
        return bookedByUsername;
    }

    public void setBookedByUsername(String bookedByUsername) {
        this.bookedByUsername = bookedByUsername;
    }

    public Long getMeetingGroupId() {
        return meetingGroupId;
    }

    public void setMeetingGroupId(Long meetingGroupId) {
        this.meetingGroupId = meetingGroupId;
    }

    public Boolean getIsMeetingRoom() {
        return isMeetingRoom;
    }

    public void setIsMeetingRoom(Boolean meetingRoom) {
        isMeetingRoom = meetingRoom;
    }

    public String getMeetingOrganizerName() {
        return meetingOrganizerName;
    }

    public void setMeetingOrganizerName(String meetingOrganizerName) {
        this.meetingOrganizerName = meetingOrganizerName;
    }

    public List<String> getMeetingAttendees() {
        return meetingAttendees;
    }

    public void setMeetingAttendees(List<String> meetingAttendees) {
        this.meetingAttendees = meetingAttendees;
    }

    public List<Long> getMeetingAttendeeIds() {
        return meetingAttendeeIds;
    }

    public void setMeetingAttendeeIds(List<Long> meetingAttendeeIds) {
        this.meetingAttendeeIds = meetingAttendeeIds;
    }

    public Boolean getIsAdminBooked() {
        return isAdminBooked;
    }

    public void setIsAdminBooked(Boolean isAdminBooked) {
        this.isAdminBooked = isAdminBooked;
    }
}

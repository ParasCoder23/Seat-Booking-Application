package com.officeseatbooking.entity;

import com.officeseatbooking.enums.BookingStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_for_user_id")
    private User bookedForUser;

    @Column(name = "meeting_group_id")
    private Long meetingGroupId; // Group ID for meeting room bookings (all attendees have same ID)

    @Column(name = "is_meeting_room", columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isMeetingRoom = false; // True if this is a meeting room booking

    @Column(name = "is_admin_booked", columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isAdminBooked = false; // True if admin booked this for employee

    @Column(name = "meeting_organizer_id")
    private Long meetingOrganizerId; // Who created the meeting (Admin/Manager)

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Booking() {}

    public Booking(User user, Seat seat, LocalDateTime startTime, LocalDateTime endTime, BookingStatus status) {
        this.user = user;
        this.seat = seat;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getBookedForUser() {
        return bookedForUser;
    }

    public void setBookedForUser(User bookedForUser) {
        this.bookedForUser = bookedForUser;
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

    public Boolean getIsAdminBooked() {
        return isAdminBooked;
    }

    public void setIsAdminBooked(Boolean adminBooked) {
        isAdminBooked = adminBooked;
    }

    public Long getMeetingOrganizerId() {
        return meetingOrganizerId;
    }

    public void setMeetingOrganizerId(Long meetingOrganizerId) {
        this.meetingOrganizerId = meetingOrganizerId;
    }
}

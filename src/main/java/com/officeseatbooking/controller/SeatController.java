package com.officeseatbooking.controller;

import com.officeseatbooking.dto.SeatResponse;
import com.officeseatbooking.enums.SeatType;
import com.officeseatbooking.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/seats")
public class SeatController {

    @Autowired
    private SeatService seatService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<SeatResponse>> getAllSeats() {
        List<SeatResponse> seats = seatService.getAllSeats();
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats() {
        List<SeatResponse> seats = seatService.getAvailableSeats();
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/floor/{floor}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<SeatResponse>> getSeatsByFloor(@PathVariable Integer floor) {
        List<SeatResponse> seats = seatService.getSeatsByFloor(floor);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/floor/{floor}/available")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<SeatResponse>> getAvailableSeatsByFloor(@PathVariable Integer floor) {
        List<SeatResponse> seats = seatService.getAvailableSeatsByFloor(floor);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<SeatResponse>> getSeatsByType(@PathVariable SeatType type) {
        List<SeatResponse> seats = seatService.getSeatsByType(type);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/type/{type}/available")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<SeatResponse>> getAvailableSeatsByType(@PathVariable SeatType type) {
        List<SeatResponse> seats = seatService.getAvailableSeatsByType(type);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable Long id) {
        SeatResponse seat = seatService.getSeatById(id);
        return ResponseEntity.ok(seat);
    }
}

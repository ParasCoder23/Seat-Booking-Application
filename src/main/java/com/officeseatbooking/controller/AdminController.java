package com.officeseatbooking.controller;

import com.officeseatbooking.dto.SeatResponse;
import com.officeseatbooking.entity.Seat;
import com.officeseatbooking.enums.SeatStatus;
import com.officeseatbooking.enums.SeatType;
import com.officeseatbooking.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private SeatService seatService;

    @PostMapping("/seats")
    public ResponseEntity<SeatResponse> createSeat(@Valid @RequestBody CreateSeatRequest request) {
        Seat seat = new Seat(request.getFloor(), request.getSeatNumber(), request.getType(), SeatStatus.AVAILABLE);
        SeatResponse seatResponse = seatService.createSeat(seat);
        return ResponseEntity.ok(seatResponse);
    }

    @PutMapping("/seats/{id}/status")
    public ResponseEntity<Map<String, String>> updateSeatStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        seatService.updateSeatStatus(id, request.getStatus());
        return ResponseEntity.ok(Map.of("message", "Seat status updated successfully"));
    }

    @DeleteMapping("/seats/{id}")
    public ResponseEntity<Map<String, String>> deleteSeat(@PathVariable Long id) {
        seatService.deleteSeat(id);
        return ResponseEntity.ok(Map.of("message", "Seat deleted successfully"));
    }

    // Inner classes for request DTOs
    public static class CreateSeatRequest {
        private Integer floor;
        private String seatNumber;
        private SeatType type;

        public Integer getFloor() { return floor; }
        public void setFloor(Integer floor) { this.floor = floor; }

        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

        public SeatType getType() { return type; }
        public void setType(SeatType type) { this.type = type; }
    }

    public static class UpdateStatusRequest {
        private SeatStatus status;

        public SeatStatus getStatus() { return status; }
        public void setStatus(SeatStatus status) { this.status = status; }
    }
}

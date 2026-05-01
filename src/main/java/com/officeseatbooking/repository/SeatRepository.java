package com.officeseatbooking.repository;

import com.officeseatbooking.entity.Seat;
import com.officeseatbooking.enums.SeatStatus;
import com.officeseatbooking.enums.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByStatus(SeatStatus status);

    List<Seat> findByFloor(Integer floor);

    List<Seat> findByType(SeatType type);

    List<Seat> findByFloorAndStatus(Integer floor, SeatStatus status);

    List<Seat> findByTypeAndStatus(SeatType type, SeatStatus status);
}

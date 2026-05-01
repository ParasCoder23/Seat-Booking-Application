package com.officeseatbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeatBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeatBookingSystemApplication.class, args);
    }

}
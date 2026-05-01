package com.officeseatbooking.config;

import com.officeseatbooking.entity.Seat;
import com.officeseatbooking.entity.User;
import com.officeseatbooking.enums.Role;
import com.officeseatbooking.enums.SeatStatus;
import com.officeseatbooking.enums.SeatType;
import com.officeseatbooking.repository.SeatRepository;
import com.officeseatbooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting data initialization...");
        initializeUsers();
        initializeSeats();
        logger.info("Data initialization completed.");
    }

    private void initializeUsers() {
        logger.info("Checking if users need to be initialized...");
        if (userRepository.count() == 0) {
            logger.info("Creating initial users...");

            // Create admin user
            String adminPassword = passwordEncoder.encode("admin123");
            User admin = new User("admin", "admin@company.com", adminPassword, Role.ADMIN);
            userRepository.save(admin);
            logger.info("Created admin user - Username: admin");

            // Create manager user
            String managerPassword = passwordEncoder.encode("manager123");
            User manager = new User("manager", "manager@company.com", managerPassword, Role.MANAGER);
            userRepository.save(manager);
            logger.info("Created manager user - Username: manager");

            // Create employee user
            String employeePassword = passwordEncoder.encode("employee123");
            User employee = new User("employee", "employee@company.com", employeePassword, Role.EMPLOYEE);
            userRepository.save(employee);
            logger.info("Created employee user - Username: employee");

            logger.info("=== LOGIN CREDENTIALS ===");
            logger.info("Admin - Username: admin, Password: admin123");
            logger.info("Manager - Username: manager, Password: manager123");
            logger.info("Employee - Username: employee, Password: employee123");
            logger.info("========================");
        } else {
            logger.info("Users already exist, skipping user creation. Total users: {}", userRepository.count());
        }
    }

    private void initializeSeats() {
        logger.info("Checking if seats need to be initialized...");
        if (seatRepository.count() == 0) {
            logger.info("Creating initial seats...");

            // Floor 1 - Regular seats
            for (int i = 1; i <= 10; i++) {
                Seat seat = new Seat(1, "1-" + String.format("%02d", i), SeatType.REGULAR, SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }

            // Floor 1 - Hot desks
            for (int i = 1; i <= 5; i++) {
                Seat seat = new Seat(1, "1-HD-" + String.format("%02d", i), SeatType.HOT_DESK, SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }

            // Floor 1 - Meeting rooms
            for (int i = 1; i <= 3; i++) {
                Seat seat = new Seat(1, "1-MR-" + String.format("%02d", i), SeatType.MEETING_ROOM, SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }

            // Floor 2 - Regular seats
            for (int i = 1; i <= 15; i++) {
                Seat seat = new Seat(2, "2-" + String.format("%02d", i), SeatType.REGULAR, SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }

            // Floor 2 - Meeting rooms
            for (int i = 1; i <= 2; i++) {
                Seat seat = new Seat(2, "2-MR-" + String.format("%02d", i), SeatType.MEETING_ROOM, SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }

            logger.info("Sample seats created for floors 1 and 2");
            logger.info("Total seats created: {}", seatRepository.count());
        } else {
            logger.info("Seats already exist, skipping seat creation. Total seats: {}", seatRepository.count());
        }
    }
}

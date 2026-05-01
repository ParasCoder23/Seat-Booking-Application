package com.officeseatbooking.exception;

public class SeatLockedException extends RuntimeException {

    public SeatLockedException(String message) {
        super(message);
    }

    public SeatLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}

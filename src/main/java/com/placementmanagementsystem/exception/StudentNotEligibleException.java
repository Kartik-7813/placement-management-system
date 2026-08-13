package com.placementmanagementsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class StudentNotEligibleException extends RuntimeException {
    public StudentNotEligibleException(String message) {
        super(message);
    }
}

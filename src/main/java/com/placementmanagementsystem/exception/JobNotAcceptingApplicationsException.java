package com.placementmanagementsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class JobNotAcceptingApplicationsException extends RuntimeException {
    public JobNotAcceptingApplicationsException(String message) {
        super(message);
    }
}

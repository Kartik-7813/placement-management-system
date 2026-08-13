package com.placementmanagementsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SkillAlreadyAssignedException extends RuntimeException {
    public SkillAlreadyAssignedException(String message) {
        super(message);
    }
}

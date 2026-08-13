package com.placementmanagementsystem.exception;

public class PlacementAlreadyExistsException extends RuntimeException {

    public PlacementAlreadyExistsException(String message) {
        super(message);
    }
}
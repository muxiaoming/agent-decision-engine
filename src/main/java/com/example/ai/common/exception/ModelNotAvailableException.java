package com.example.ai.common.exception;

public class ModelNotAvailableException extends RuntimeException {

    private final String model;

    public ModelNotAvailableException(String model, String message) {
        super(message);
        this.model = model;
    }

    public String getModel() {
        return model;
    }
}

package com.example.jolijob.profile.application;

public class OwnProfileNotFoundException extends IllegalStateException {
    public OwnProfileNotFoundException(String message) {
        super(message);
    }
}

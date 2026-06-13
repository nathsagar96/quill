package com.quill.event;

public record UserRegisteredEvent(String email, String token) {}

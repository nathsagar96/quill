package com.quill.event;

public record PasswordResetRequestedEvent(String email, String token) {}

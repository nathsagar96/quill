package com.quill.service;

import com.quill.dto.request.LoginRequest;
import com.quill.dto.request.RegisterRequest;
import com.quill.dto.response.AuthResponse;
import com.quill.exception.DuplicateEmailException;
import com.quill.exception.DuplicateUsernameException;
import com.quill.model.User;
import com.quill.repository.UserRepository;
import com.quill.security.CustomUserDetails;
import com.quill.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user: username='{}', email='{}'", request.username(), request.email());

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new DuplicateUsernameException(request.username());
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException(request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .bio(request.bio())
                .avatarUrl(request.avatarUrl())
                .build();

        User saved = userRepository.save(user);
        log.info("Registered user with id={}", saved.getId());

        UserDetails userDetails = new CustomUserDetails(saved);

        String token = jwtService.generateToken(userDetails);
        return toAuthResponse(token, saved);
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: username='{}'", request.username());

        Authentication authenticated = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        var principal = (CustomUserDetails) authenticated.getPrincipal();
        User user = principal.user();

        String token = jwtService.generateToken(principal);
        return toAuthResponse(token, user);
    }

    private AuthResponse toAuthResponse(String token, User user) {
        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl());
    }
}

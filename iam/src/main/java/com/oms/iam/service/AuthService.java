package com.oms.iam.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.oms.iam.dto.AuthDto;
import com.oms.iam.exception.DuplicateEmailException;
import com.oms.iam.exception.InactiveUserException;
import com.oms.iam.exception.InvalidCredentialsException;
import com.oms.iam.model.Role;
import com.oms.iam.model.Status;
import com.oms.iam.model.User;
import com.oms.iam.repository.UserRepository;
import com.oms.iam.security.JwtUtil;
import com.oms.common.kafka.KafkaEventPublisher;

import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KafkaEventPublisher kafkaEventPublisher;

    public void register(AuthDto.AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateEmailException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole() != null ? request.getRole() : Role.USER) // Better to have default role as USER
                .status(Status.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        // Publish event
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", savedUser.getId());
        payload.put("email", savedUser.getEmail());
        payload.put("name", savedUser.getName());

        kafkaEventPublisher.publishEvent(
                "user-events",
                "USER_REGISTERED",
                "USER",
                savedUser.getId(),
                "iam-service",
                payload);
    }

    public AuthDto.AuthResponse login(AuthDto.AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (user.getStatus() != Status.ACTIVE) {
            throw new InactiveUserException("User is not active");
        }

        String token = jwtUtil.generateToken(user);

        return AuthDto.AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}

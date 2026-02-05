package com.oms.iam.service;

import com.oms.iam.dto.AuthDto;
import com.oms.iam.model.Role;
import com.oms.iam.model.Status;
import com.oms.iam.model.User;
import com.oms.iam.repository.UserRepository;
import com.oms.iam.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void register(AuthDto.AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole() != null ? request.getRole() : Role.USER) // Better to have default role as USER
                .status(Status.ACTIVE)
                .build();

        userRepository.save(user);
    }

    public AuthDto.AuthResponse login(AuthDto.AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials")); // shouldnt this be only invalid email

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials"); // shouldnt this be only invalid password
        }

        if (user.getStatus() != Status.ACTIVE) {
            throw new RuntimeException("User is not active"); // Better to have global exception handler
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

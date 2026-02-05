package com.oms.iam.controller;

import com.oms.iam.dto.AuthDto;
import com.oms.iam.security.JwtUtil;
import com.oms.iam.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth") // Need to add base path along with version
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthDto.AuthRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully"); // Need http status code along with response
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@RequestBody AuthDto.AuthRequest request) {
        return ResponseEntity.ok(authService.login(request)); // Need http status code along with response
    }

    @GetMapping("/me")
    public ResponseEntity<UserDetails> validate(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails); // Need http status code along with response
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwtUtil.getJwks();
    }
}

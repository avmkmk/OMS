package com.oms.iam.dto;

import com.oms.iam.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthRequest {

        private String email;
        private String password;
        private String name;
        private Role role; // Optional for registration; defaults to USER if not specified
    }

    @Data
    @Builder
    public static class AuthResponse {
        private String token;
        private Long userId;
        private String email;
        private Role role;
    }
}

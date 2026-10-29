package com.example.onlinebooking.dto;

import com.example.onlinebooking.model.Role;
import com.example.onlinebooking.model.User;

public class AuthResponse {

    private String token;
    private UserDto user;

    public AuthResponse() {
    }

    public AuthResponse(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }

    public static AuthResponse from(String token, User user) {
        return new AuthResponse(token, new UserDto(user.getUserId(), user.getName(), user.getEmail(), user.getRole()));
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public static class UserDto {
        private Long userId;
        private String name;
        private String email;
        private Role role;

        public UserDto() {
        }

        public UserDto(Long userId, String name, String email, Role role) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }
    }
}

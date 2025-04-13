package com.thesis.backend.dto;

import com.thesis.backend.entity.User.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String email;
    private String password;
    private Role role; // Usually set to PROFESSOR by default
}

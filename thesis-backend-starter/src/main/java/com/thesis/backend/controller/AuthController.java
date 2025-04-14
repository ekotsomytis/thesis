package com.thesis.backend.controller;

import com.thesis.backend.dto.AuthRequest;
import com.thesis.backend.dto.AuthResponse;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.UserRepository;
import com.thesis.backend.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(user);

        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // set to true when using HTTPS (Minikube with TLS)
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(jwtCookie);

        return ResponseEntity.ok("Logged in");
    }

    // TEMP: register user for testing
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        // Basic null or blank check
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Email and password must not be empty.");
        }

        // Check for existing email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use.");
        }

        // Save the new user
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.PROFESSOR) // Default to professor
                .build();
        userRepository.save(user);

        // Optional: return JWT token after registration
        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token)); // auto-login behavior
    }

}

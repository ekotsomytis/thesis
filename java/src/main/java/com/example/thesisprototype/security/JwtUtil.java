package com.example.thesisprototype.security;

public interface JwtUtil {
String generateToken(String username);

String extractUsername(String token);

boolean isTokenExpired(String token);

//boolean validateToken(String token, UserDetails userDetails);
}

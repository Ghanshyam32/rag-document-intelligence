package com.ghanshyam.empty_stack.controller;

import com.ghanshyam.empty_stack.dto.AuthResponse;
import com.ghanshyam.empty_stack.dto.LoginRequest;
import com.ghanshyam.empty_stack.dto.RegisterRequest;
import com.ghanshyam.empty_stack.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
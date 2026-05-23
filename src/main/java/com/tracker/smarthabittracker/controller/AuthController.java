package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.dto.AuthRequest;
import com.tracker.smarthabittracker.dto.AuthResponse;
import com.tracker.smarthabittracker.dto.RegisterRequest;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = userService.registerUser(registerRequest);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully! You can now log in.");
            response.put("username", user.getUsername());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException ex) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", ex.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse authResponse = userService.authenticateUser(authRequest);
        return ResponseEntity.ok(authResponse);
    }
}

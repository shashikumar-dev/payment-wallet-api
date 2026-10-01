package com.paymentwallet.user_service.controller;

import com.paymentwallet.user_service.dto.LoginRequest;
import com.paymentwallet.user_service.dto.LoginResponse;
import com.paymentwallet.user_service.dto.RegisterRequest;
import com.paymentwallet.user_service.dto.RegisterResponse;
import com.paymentwallet.user_service.entity.User;
import com.paymentwallet.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentwallet.user_service.security.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService,
                          JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        RegisterResponse response =
                userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        User user = userService.authenticate(request);

        String token = jwtService.generateToken(user);

        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                3600
        );

        return ResponseEntity.ok(response);
    }
}
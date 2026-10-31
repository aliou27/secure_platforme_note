package com.project.secure_platforme.auth;

import com.project.secure_platforme.auth.dto.RegisterRequest;
import com.project.secure_platforme.user.User;
import com.project.secure_platforme.user.UserService;
import com.project.secure_platforme.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }
}

package com.project.secure_platforme.auth;

import com.project.secure_platforme.auth.dto.TokenResponse;
import com.project.secure_platforme.exception.InvalidCredentialsException;
import com.project.secure_platforme.security.JwtService;
import com.project.secure_platforme.user.User;
import com.project.secure_platforme.user.UserRepository;
import com.project.secure_platforme.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String rawUsername, String rawPassword) {
        String username = UserService.normalize(rawUsername);
        Optional<User> user = userRepository.findByUsername(username);

        String hashToCheck = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(rawPassword, hashToCheck);

        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        JwtService.IssuedToken token = jwtService.issue(user.get());
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }
}

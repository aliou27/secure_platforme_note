package com.project.secure_platforme.auth;

import com.project.secure_platforme.audit.AuditEventType;
import com.project.secure_platforme.audit.AuditService;
import com.project.secure_platforme.auth.dto.TokenResponse;
import com.project.secure_platforme.exception.InvalidCredentialsException;
import com.project.secure_platforme.security.JwtService;
import com.project.secure_platforme.security.TokenRevocationService;
import com.project.secure_platforme.user.User;
import com.project.secure_platforme.user.UserRepository;
import com.project.secure_platforme.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;
    private final AuditService auditService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenRevocationService tokenRevocationService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRevocationService = tokenRevocationService;
        this.auditService = auditService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String rawUsername, String rawPassword) {
        String username = UserService.normalize(rawUsername);
        Optional<User> user = userRepository.findByUsername(username);

        String hashToCheck = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(rawPassword, hashToCheck);

        if (user.isEmpty()) {
            auditService.record(AuditEventType.LOGIN_FAILURE, username, Map.of("reason", "unknown_user"));
            throw new InvalidCredentialsException();
        }
        if (!passwordMatches) {
            auditService.record(AuditEventType.LOGIN_FAILURE, username, Map.of("reason", "bad_password"));
            throw new InvalidCredentialsException();
        }

        JwtService.IssuedToken token = jwtService.issue(user.get());
        auditService.record(AuditEventType.LOGIN_SUCCESS, username);
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    @Transactional(readOnly = true)
    public void logout(Jwt jwt) {
        tokenRevocationService.revoke(jwt.getId(), jwt.getExpiresAt());
        String username = userRepository.findById(Long.valueOf(jwt.getSubject()))
                .map(User::getUsername)
                .orElse(null);
        auditService.record(AuditEventType.LOGOUT, username, Map.of("tokenId", jwt.getId()));
    }
}

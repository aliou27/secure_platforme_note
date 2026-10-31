package com.project.secure_platforme.user;

import com.project.secure_platforme.audit.AuditEventType;
import com.project.secure_platforme.audit.AuditService;
import com.project.secure_platforme.exception.InvalidCredentialsException;
import com.project.secure_platforme.exception.UsernameAlreadyTakenException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public User register(String rawUsername, String rawPassword) {
        String username = normalize(rawUsername);

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyTakenException();
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user;
        try {
            user = userRepository.saveAndFlush(new User(username, passwordHash));
        } catch (DataIntegrityViolationException e) {
            throw new UsernameAlreadyTakenException();
        }

        auditService.record(AuditEventType.USER_REGISTERED, username, Map.of("userId", user.getId()));
        return user;
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new InvalidCredentialsException("Authentication required"));
    }

    public static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}

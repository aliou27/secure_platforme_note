package com.project.secure_platforme.user;

import com.project.secure_platforme.exception.UsernameAlreadyTakenException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String rawUsername, String rawPassword) {
        String username = normalize(rawUsername);

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyTakenException();
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        try {
            return userRepository.saveAndFlush(new User(username, passwordHash));
        } catch (DataIntegrityViolationException e) {
            throw new UsernameAlreadyTakenException();
        }
    }

    public static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}

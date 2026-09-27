package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.AuthResponse;
import com.rameshkumar.placementsystem.dto.LoginRequest;
import com.rameshkumar.placementsystem.dto.RegisterRequest;
import com.rameshkumar.placementsystem.entity.StudentProfileDefaults;
import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.exception.ConflictException;
import com.rameshkumar.placementsystem.exception.UnauthorizedException;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import com.rameshkumar.placementsystem.repository.UserRepository;
import com.rameshkumar.placementsystem.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String SESSION_EXPIRED = "Your session has expired. Please sign in again.";

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository,
                           StudentRepository studentRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // The user and their empty student profile are created together or not at all.
    @Override
    @Transactional
    public String register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        logger.info("Register request received for email {}", email);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            logger.warn("Registration failed because email already exists: {}", email);
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");
        userRepository.save(user);

        studentRepository.save(StudentProfileDefaults.newEmptyProfile(user));

        logger.info("User registered successfully with email {}", user.getEmail());
        return "User registered successfully";
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    logger.warn("Login failed for email {}", email);
                    return new UnauthorizedException(INVALID_CREDENTIALS);
                });

        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Login failed due to invalid password for email {}", email);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        logger.info("User logged in successfully with email {} and role {}", user.getEmail(), user.getRole());
        return issueTokens(user);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String email;
        try {
            if (!jwtUtil.validateRefreshToken(refreshToken)) {
                logger.warn("Refresh token validation failed");
                throw new UnauthorizedException(SESSION_EXPIRED);
            }
            email = jwtUtil.extractUsername(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("Refresh token rejected: {}", ex.getMessage());
            throw new UnauthorizedException(SESSION_EXPIRED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Refresh token user not found for email {}", email);
                    return new UnauthorizedException(SESSION_EXPIRED);
                });

        logger.info("Refreshed tokens for email {}", user.getEmail());
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getRole());
        return new AuthResponse(accessToken, refreshToken, jwtUtil.getAccessTokenExpirationMs());
    }

    static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

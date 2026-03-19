package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.AuthResponse;
import com.rameshkumar.placementsystem.dto.LoginRequest;
import com.rameshkumar.placementsystem.dto.RegisterRequest;
import com.rameshkumar.placementsystem.entity.Student;
import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import com.rameshkumar.placementsystem.repository.UserRepository;
import com.rameshkumar.placementsystem.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

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

    @Override
    public String register(RegisterRequest request) {
        logger.info("Register request received for email {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed because email already exists: {}", request.getEmail());
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");
        userRepository.save(user);

        Student studentProfile = new Student();
        studentProfile.setUser(user);
        studentProfile.setCgpa(0.0);
        studentProfile.setSkills("Profile not updated");
        studentProfile.setResumeLink(null);
        studentRepository.save(studentProfile);

        logger.info("User registered successfully with email {}", user.getEmail());
        return "User registered successfully";
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Login failed for email {}", request.getEmail());
                    return new RuntimeException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Login failed due to invalid password for email {}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getRole());
        logger.info("User logged in successfully with email {} and role {}", user.getEmail(), user.getRole());
        return new AuthResponse(token, refreshToken, jwtUtil.getAccessTokenExpirationMs());
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            logger.warn("Refresh token validation failed");
            throw new RuntimeException("Invalid refresh token");
        }

        String email = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Refresh token user not found for email {}", email);
                    return new RuntimeException("Invalid refresh token");
                });

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getRole());
        logger.info("Refreshed tokens for email {}", user.getEmail());
        return new AuthResponse(newAccessToken, newRefreshToken, jwtUtil.getAccessTokenExpirationMs());
    }
}

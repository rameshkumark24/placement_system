package com.rameshkumar.placementsystem.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Test
    void createsAdminWhenConfiguredAndMissing() {
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());

        runner(" Admin@Example.com ", "SuperSecret1").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("admin@example.com", saved.getValue().getEmail());
        assertEquals("ADMIN", saved.getValue().getRole());
        assertEquals("Placement Admin", saved.getValue().getName());
        assertNotEquals("SuperSecret1", saved.getValue().getPassword());
    }

    @Test
    void neverPromotesAnExistingNonAdminAccount() {
        User student = new User();
        student.setEmail("admin@example.com");
        student.setRole("STUDENT");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(student));

        runner("admin@example.com", "SuperSecret1").run(null);

        assertEquals("STUDENT", student.getRole());
        verify(userRepository, never()).save(any());
    }

    @Test
    void leavesExistingAdminUntouched() {
        User admin = new User();
        admin.setRole("ADMIN");
        admin.setPassword("existing-hash");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        runner("admin@example.com", "DifferentPassword1").run(null);

        assertEquals("existing-hash", admin.getPassword());
        verify(userRepository, never()).save(any());
    }

    @Test
    void skipsWhenNotConfiguredOrPasswordTooShort() {
        when(userRepository.existsByRole("ADMIN")).thenReturn(false);
        runner("", "").run(null);
        runner("admin@example.com", "short").run(null);

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    private AdminBootstrapRunner runner(String email, String password) {
        return new AdminBootstrapRunner(userRepository, passwordEncoder, email, password, "");
    }
}

package com.rameshkumar.placementsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rameshkumar.placementsystem.dto.StudentDTO;
import com.rameshkumar.placementsystem.dto.StudentProfileUpdateRequest;
import com.rameshkumar.placementsystem.entity.Student;
import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import com.rameshkumar.placementsystem.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void updateMyProfileUpdatesAllowedFields() {
        User user = new User();
        user.setId(14L);
        user.setName("Old Name");
        user.setEmail("student@example.com");
        user.setPassword("encoded");

        Student student = new Student();
        student.setId(8L);
        student.setUser(user);
        student.setCgpa(7.2);
        student.setSkills("Old Skills");
        student.setResumeLink("https://old.example.com");

        StudentProfileUpdateRequest request = new StudentProfileUpdateRequest(
                "New Name",
                8.8,
                "Java, Spring Boot, Docker",
                "https://new.example.com"
        );

        when(studentRepository.findByUserEmail("student@example.com")).thenReturn(Optional.of(student));
        when(studentRepository.save(student)).thenReturn(student);

        StudentDTO result = studentService.updateMyProfile("student@example.com", request);

        assertEquals("New Name", result.getName());
        assertEquals(8.8, result.getCgpa());
        assertEquals("Java, Spring Boot, Docker", result.getSkills());
        assertEquals("https://new.example.com", result.getResumeLink());
        assertEquals("student@example.com", result.getEmail());
        verify(userRepository).save(user);
        verify(studentRepository).save(student);
    }
}

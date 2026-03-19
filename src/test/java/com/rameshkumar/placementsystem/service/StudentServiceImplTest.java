package com.rameshkumar.placementsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rameshkumar.placementsystem.dto.StudentDTO;
import com.rameshkumar.placementsystem.dto.StudentProfileUpdateRequest;
import com.rameshkumar.placementsystem.entity.Student;
import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.repository.ApplicationRepository;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import com.rameshkumar.placementsystem.repository.UserRepository;
import java.util.List;
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
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void filterStudentsUsesCombinedSearchWhenSkillAndCgpaAreProvided() {
        User user = new User();
        user.setId(14L);
        user.setName("Ramesh");
        user.setEmail("student@example.com");

        Student student = new Student();
        student.setId(8L);
        student.setUser(user);
        student.setCgpa(8.8);
        student.setSkills("Java, Spring Boot");

        when(studentRepository.findByUserRoleAndSkillsContainingIgnoreCaseAndCgpaGreaterThanEqual(eq("STUDENT"), eq("Java"), eq(8.0)))
                .thenReturn(List.of(student));

        List<StudentDTO> result = studentService.filterStudents("Java", 8.0);

        assertEquals(1, result.size());
        assertEquals("student@example.com", result.get(0).getEmail());
        verify(studentRepository).findByUserRoleAndSkillsContainingIgnoreCaseAndCgpaGreaterThanEqual("STUDENT", "Java", 8.0);
    }

    @Test
    void deleteStudentRemovesApplicationsBeforeStudentAndUser() {
        User user = new User();
        user.setId(14L);

        Student student = new Student();
        student.setId(8L);
        student.setUser(user);

        when(studentRepository.findById(8L)).thenReturn(Optional.of(student));

        studentService.deleteStudent(8L);

        verify(applicationRepository).deleteByStudentId(8L);
        verify(studentRepository).delete(student);
        verify(userRepository).deleteById(14L);
    }

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

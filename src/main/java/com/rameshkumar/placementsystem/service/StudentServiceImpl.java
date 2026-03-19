package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.PaginationResponse;
import com.rameshkumar.placementsystem.dto.StudentDTO;
import com.rameshkumar.placementsystem.dto.StudentProfileUpdateRequest;
import com.rameshkumar.placementsystem.entity.Student;
import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.exception.StudentNotFoundException;
import com.rameshkumar.placementsystem.repository.ApplicationRepository;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import com.rameshkumar.placementsystem.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentServiceImpl(ApplicationRepository applicationRepository,
                              StudentRepository studentRepository,
                              UserRepository userRepository,
                              PasswordEncoder passwordEncoder) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private StudentDTO mapToDTO(Student student) {
        User user = student.getUser();
        return new StudentDTO(
                student.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                null,
                student.getCgpa(),
                student.getSkills(),
                student.getResumeLink()
        );
    }

    private Student mapToEntity(StudentDTO dto) {
        Student student = new Student();
        student.setCgpa(dto.getCgpa());
        student.setSkills(dto.getSkills());
        student.setResumeLink(dto.getResumeLink());
        return student;
    }

    @Override
    public StudentDTO saveStudent(StudentDTO studentDTO) {
        if (userRepository.existsByEmail(studentDTO.getEmail())) {
            logger.warn("Student creation failed because email already exists: {}", studentDTO.getEmail());
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setName(studentDTO.getName());
        user.setEmail(studentDTO.getEmail());
        user.setPassword(passwordEncoder.encode(studentDTO.getPassword()));
        user.setRole("STUDENT");
        User savedUser = userRepository.save(user);

        Student student = mapToEntity(studentDTO);
        student.setUser(savedUser);

        Student savedStudent = studentRepository.save(student);
        logger.info("Student created with profile id {} and email {}", savedStudent.getId(), savedUser.getEmail());
        return mapToDTO(savedStudent);
    }

    @Override
    public List<StudentDTO> getAllStudents() {
        logger.info("Fetching all students");
        return studentRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<StudentDTO> filterStudents(String skill, Double cgpa) {
        boolean hasSkill = skill != null && !skill.isBlank();
        boolean hasCgpa = cgpa != null;

        if (hasSkill && hasCgpa) {
            logger.info("Filtering students by skill {} and minimum CGPA {}", skill, cgpa);
            return studentRepository.findBySkillsContainingIgnoreCaseAndCgpaGreaterThanEqual(skill, cgpa)
                    .stream()
                    .map(this::mapToDTO)
                    .toList();
        }

        if (hasSkill) {
            return searchStudentsBySkill(skill);
        }

        if (hasCgpa) {
            return filterStudentsByCgpa(cgpa);
        }

        return getAllStudents();
    }

    @Override
    public List<StudentDTO> searchStudentsBySkill(String skill) {
        logger.info("Searching students by skill {}", skill);
        return studentRepository.findBySkillsContainingIgnoreCase(skill)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<StudentDTO> filterStudentsByCgpa(double cgpa) {
        logger.info("Filtering students with CGPA greater than or equal to {}", cgpa);
        return studentRepository.findByCgpaGreaterThanEqual(cgpa)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public StudentDTO getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Student not found with id {}", id);
                    return new StudentNotFoundException("Student not found with id: " + id);
                });

        logger.info("Fetched student with id {}", id);
        return mapToDTO(student);
    }

    @Override
    public StudentDTO getMyProfile(String email) {
        Student student = studentRepository.findByUserEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Student profile not found for email {}", email);
                    return new RuntimeException("Student profile not found");
                });
        logger.info("Fetched student profile for {}", email);
        return mapToDTO(student);
    }

    @Override
    public StudentDTO updateMyProfile(String email, StudentProfileUpdateRequest profileUpdateRequest) {
        Student student = studentRepository.findByUserEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Student profile not found for self-update email {}", email);
                    return new RuntimeException("Student profile not found");
                });

        User user = student.getUser();
        if (user == null) {
            throw new RuntimeException("Student profile is not linked to a user");
        }

        user.setName(profileUpdateRequest.getName());
        userRepository.save(user);

        student.setCgpa(profileUpdateRequest.getCgpa());
        student.setSkills(profileUpdateRequest.getSkills());
        student.setResumeLink(profileUpdateRequest.getResumeLink());

        Student updatedStudent = studentRepository.save(student);
        logger.info("Student self-profile updated for {}", email);
        return mapToDTO(updatedStudent);
    }

    @Override
    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Student not found for update with id {}", id);
                    return new StudentNotFoundException("Student not found with id " + id);
                });

        User user = existingStudent.getUser();
        if (user == null) {
            throw new RuntimeException("Student profile is not linked to a user");
        }
        if (!user.getEmail().equals(studentDTO.getEmail()) && userRepository.existsByEmail(studentDTO.getEmail())) {
            logger.warn("Student update failed because email already exists: {}", studentDTO.getEmail());
            throw new RuntimeException("Email already registered");
        }

        user.setName(studentDTO.getName());
        user.setEmail(studentDTO.getEmail());
        user.setPassword(passwordEncoder.encode(studentDTO.getPassword()));
        userRepository.save(user);

        existingStudent.setCgpa(studentDTO.getCgpa());
        existingStudent.setSkills(studentDTO.getSkills());
        existingStudent.setResumeLink(studentDTO.getResumeLink());

        Student updatedStudent = studentRepository.save(existingStudent);
        logger.info("Student updated with id {}", id);
        return mapToDTO(updatedStudent);
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Student not found for delete with id {}", id);
                    return new StudentNotFoundException("Student not found with id " + id);
                });

        Long userId = student.getUser() != null ? student.getUser().getId() : null;
        applicationRepository.deleteByStudentId(id);
        studentRepository.delete(student);
        if (userId != null) {
            userRepository.deleteById(userId);
        }
        logger.info("Student deleted with id {}", id);
    }

    @Override
    public PaginationResponse<StudentDTO> getStudentsPaginated(int page, int size) {
        logger.info("Fetching students with pagination page {} and size {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Student> studentPage = studentRepository.findAll(pageable);

        List<StudentDTO> studentDTOList = studentPage.getContent()
                .stream()
                .map(this::mapToDTO)
                .toList();

        return new PaginationResponse<>(
                studentDTOList,
                page,
                size,
                studentPage.getTotalElements(),
                studentPage.getTotalPages()
        );
    }
}

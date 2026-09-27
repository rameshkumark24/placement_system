package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.PaginationResponse;
import com.rameshkumar.placementsystem.dto.StudentDTO;
import com.rameshkumar.placementsystem.dto.StudentProfileUpdateRequest;
import com.rameshkumar.placementsystem.entity.Student;
import com.rameshkumar.placementsystem.entity.StudentProfileDefaults;
import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.exception.BadRequestException;
import com.rameshkumar.placementsystem.exception.ConflictException;
import com.rameshkumar.placementsystem.exception.ResourceNotFoundException;
import com.rameshkumar.placementsystem.exception.StudentNotFoundException;
import com.rameshkumar.placementsystem.repository.ApplicationRepository;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import com.rameshkumar.placementsystem.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);
    private static final String STUDENT_ROLE = "STUDENT";
    private static final int MAX_PAGE_SIZE = 100;

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
    @Transactional
    public StudentDTO saveStudent(StudentDTO studentDTO) {
        String email = normalizeEmail(studentDTO.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            logger.warn("Student creation failed because email already exists: {}", email);
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setName(studentDTO.getName().trim());
        user.setEmail(email);
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
        return studentRepository.findByUserRole(STUDENT_ROLE)
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
            return studentRepository.findByUserRoleAndSkillsContainingIgnoreCaseAndCgpaGreaterThanEqual(STUDENT_ROLE, skill, cgpa)
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
        return studentRepository.findByUserRoleAndSkillsContainingIgnoreCase(STUDENT_ROLE, skill)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<StudentDTO> filterStudentsByCgpa(double cgpa) {
        logger.info("Filtering students with CGPA greater than or equal to {}", cgpa);
        return studentRepository.findByUserRoleAndCgpaGreaterThanEqual(STUDENT_ROLE, cgpa)
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

    // Not read-only: accounts created before profiles existed get an empty profile on first access,
    // so the student dashboard can always load.
    @Override
    @Transactional
    public StudentDTO getMyProfile(String email) {
        Student student = getOrCreateOwnProfile(email);
        logger.info("Fetched student profile for {}", email);
        return mapToDTO(student);
    }

    @Override
    @Transactional
    public StudentDTO updateMyProfile(String email, StudentProfileUpdateRequest profileUpdateRequest) {
        Student student = getOrCreateOwnProfile(email);

        User user = student.getUser();
        if (user == null) {
            throw new BadRequestException("Student profile is not linked to a user");
        }

        user.setName(profileUpdateRequest.getName().trim());
        userRepository.save(user);

        student.setCgpa(profileUpdateRequest.getCgpa());
        student.setSkills(profileUpdateRequest.getSkills().trim());
        student.setResumeLink(trimToNull(profileUpdateRequest.getResumeLink()));

        Student updatedStudent = studentRepository.save(student);
        logger.info("Student self-profile updated for {}", email);
        return mapToDTO(updatedStudent);
    }

    @Override
    @Transactional
    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Student not found for update with id {}", id);
                    return new StudentNotFoundException("Student not found with id " + id);
                });

        User user = existingStudent.getUser();
        if (user == null) {
            throw new BadRequestException("Student profile is not linked to a user");
        }
        String email = normalizeEmail(studentDTO.getEmail());
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            logger.warn("Student update failed because email already exists: {}", email);
            throw new ConflictException("Email already registered");
        }

        user.setName(studentDTO.getName().trim());
        user.setEmail(email);
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
        if (page < 0) {
            throw new BadRequestException("Page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
        logger.info("Fetching students with pagination page {} and size {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<Student> studentPage = studentRepository.findByUserRole(STUDENT_ROLE, pageable);

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

    private Student getOrCreateOwnProfile(String email) {
        return studentRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
                    if (!STUDENT_ROLE.equalsIgnoreCase(user.getRole())) {
                        throw new BadRequestException("Only students have a student profile");
                    }
                    Student created = studentRepository.save(StudentProfileDefaults.newEmptyProfile(user));
                    logger.warn("Created missing student profile {} for user {}", created.getId(), email);
                    return created;
                });
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

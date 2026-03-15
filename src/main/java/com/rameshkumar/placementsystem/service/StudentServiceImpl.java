package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.PaginationResponse;
import com.rameshkumar.placementsystem.dto.StudentDTO;
import com.rameshkumar.placementsystem.entity.Student;
import com.rameshkumar.placementsystem.exception.StudentNotFoundException;
import com.rameshkumar.placementsystem.repository.StudentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    public StudentServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // ENTITY → DTO
    private StudentDTO mapToDTO(Student student) {
        return new StudentDTO(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getPassword(),
                student.getCgpa(),
                student.getSkills()
        );
    }

    // DTO → ENTITY
    private Student mapToEntity(StudentDTO dto) {

        Student student = new Student();

        student.setName(dto.getName());
        student.setEmail(dto.getEmail());
        student.setPassword(dto.getPassword());
        student.setCgpa(dto.getCgpa());
        student.setSkills(dto.getSkills());

        return student;
    }

    // CREATE STUDENT
    @Override
    public StudentDTO saveStudent(StudentDTO studentDTO) {

        Student student = mapToEntity(studentDTO);

        Student savedStudent = studentRepository.save(student);

        return mapToDTO(savedStudent);
    }

    // GET ALL STUDENTS
    @Override
    public List<StudentDTO> getAllStudents() {

        return studentRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // GET STUDENT BY ID
    @Override
    public StudentDTO getStudentById(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + id));

        return mapToDTO(student);
    }

    // UPDATE STUDENT
    @Override
    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {

        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id " + id));

        existingStudent.setName(studentDTO.getName());
        existingStudent.setEmail(studentDTO.getEmail());
        existingStudent.setPassword(studentDTO.getPassword());
        existingStudent.setCgpa(studentDTO.getCgpa());
        existingStudent.setSkills(studentDTO.getSkills());

        Student updatedStudent = studentRepository.save(existingStudent);

        return mapToDTO(updatedStudent);
    }

    // DELETE STUDENT
    @Override
    public void deleteStudent(Long id) {

        if (!studentRepository.existsById(id)) {
            throw new StudentNotFoundException("Student not found with id " + id);
        }

        studentRepository.deleteById(id);
    }

    // PAGINATION
    @Override
    public PaginationResponse<StudentDTO> getStudentsPaginated(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Student> studentPage = studentRepository.findAll(pageable);

        List<StudentDTO> studentDTOList = studentPage
                .getContent()
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
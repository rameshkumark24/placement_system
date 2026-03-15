package com.rameshkumar.placementsystem.controller;

import com.rameshkumar.placementsystem.dto.PaginationResponse;
import com.rameshkumar.placementsystem.dto.StudentDTO;
import com.rameshkumar.placementsystem.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // CREATE STUDENT
    @PostMapping
    public StudentDTO createStudent(@Valid @RequestBody StudentDTO studentDTO) {
        return studentService.saveStudent(studentDTO);
    }

    // GET ALL STUDENTS
    @GetMapping
    public List<StudentDTO> getAllStudents() {
        return studentService.getAllStudents();
    }

    // GET STUDENT BY ID
    @GetMapping("/{id}")
    public StudentDTO getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id);
    }

    // UPDATE STUDENT
    @PutMapping("/{id}")
    public StudentDTO updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentDTO studentDTO) {

        return studentService.updateStudent(id, studentDTO);
    }

    // DELETE STUDENT
    @DeleteMapping("/{id}")
    public String deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return "Student deleted successfully";
    }

    // PAGINATION
    @GetMapping("/paginated")
    public PaginationResponse<StudentDTO> getStudentsPaginated(
            @RequestParam int page,
            @RequestParam int size) {

        return studentService.getStudentsPaginated(page, size);
    }
}
package com.rameshkumar.placementsystem.controller;

import com.rameshkumar.placementsystem.dto.PaginationResponse;
import com.rameshkumar.placementsystem.dto.StudentDTO;
import com.rameshkumar.placementsystem.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;

@Tag(name = "Student APIs", description = "Operations related to student management")
@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // CREATE STUDENT
    @Operation(summary = "Create a new student")
    @PostMapping
    public StudentDTO createStudent(@Valid @RequestBody StudentDTO studentDTO) {
        return studentService.saveStudent(studentDTO);
    }

    // GET ALL STUDENTS
    @Operation(summary = "Get all students")
    @GetMapping
    public List<StudentDTO> getAllStudents() {
        return studentService.getAllStudents();
    }

    // GET STUDENT BY ID
    @Operation(summary = "Get student by ID")
    @GetMapping("/{id}")
    public StudentDTO getStudentById(@PathVariable Long id){
        return studentService.getStudentById(id);
    }

    // UPDATE STUDENT
    @Operation(summary = "Update student details")
    @PutMapping("/{id}")
    public StudentDTO updateStudent(@PathVariable Long id,
                                    @Valid @RequestBody StudentDTO studentDTO){
        return studentService.updateStudent(id, studentDTO);
    }

    // DELETE STUDENT
    @Operation(summary = "Delete student by ID")
    @DeleteMapping("/{id}")
    public String deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return "Student deleted successfully";
    }

    // PAGINATION
    @Operation(summary = "Get students with pagination")
    @GetMapping("/paginated")
    public PaginationResponse<StudentDTO> getStudentsPaginated(
            @RequestParam int page,
            @RequestParam int size) {

        return studentService.getStudentsPaginated(page, size);
    }
}
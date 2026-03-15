package com.rameshkumar.placementsystem.controller;

import com.rameshkumar.placementsystem.dto.ApiResponse;
import com.rameshkumar.placementsystem.dto.PaginationResponse;
import com.rameshkumar.placementsystem.dto.StudentDTO;
import com.rameshkumar.placementsystem.service.StudentService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new student")
    @PostMapping
    public ApiResponse<StudentDTO> createStudent(@Valid @RequestBody StudentDTO studentDTO) {

        StudentDTO savedStudent = studentService.saveStudent(studentDTO);

        return new ApiResponse<>(
                true,
                "Student created successfully",
                savedStudent
        );
    }

    // GET ALL STUDENTS
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all students")
    @GetMapping
    public ApiResponse<List<StudentDTO>> getAllStudents() {

        List<StudentDTO> students = studentService.getAllStudents();

        return new ApiResponse<>(
                true,
                "Students fetched successfully",
                students
        );
    }

    // GET STUDENT BY ID
    @Operation(summary = "Get student by ID")
    @GetMapping("/{id}")
    public ApiResponse<StudentDTO> getStudentById(@PathVariable Long id){

        StudentDTO student = studentService.getStudentById(id);

        return new ApiResponse<>(
                true,
                "Student fetched successfully",
                student
        );
    }

    // UPDATE STUDENT
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update student details")
    @PutMapping("/{id}")
    public ApiResponse<StudentDTO> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentDTO studentDTO){

        StudentDTO updatedStudent = studentService.updateStudent(id, studentDTO);

        return new ApiResponse<>(
                true,
                "Student updated successfully",
                updatedStudent
        );
    }

    // DELETE STUDENT
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete student by ID")
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteStudent(@PathVariable Long id) {

        studentService.deleteStudent(id);

        return new ApiResponse<>(
                true,
                "Student deleted successfully",
                null
        );
    }

    // PAGINATION
    @Operation(summary = "Get students with pagination")
    @GetMapping("/paginated")
    public ApiResponse<PaginationResponse<StudentDTO>> getStudentsPaginated(
            @RequestParam int page,
            @RequestParam int size) {

        PaginationResponse<StudentDTO> response =
                studentService.getStudentsPaginated(page, size);

        return new ApiResponse<>(
                true,
                "Students fetched with pagination",
                response
        );
    }
}
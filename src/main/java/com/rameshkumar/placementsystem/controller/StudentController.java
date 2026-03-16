package com.rameshkumar.placementsystem.controller;

import com.rameshkumar.placementsystem.dto.ApiResponse;
import com.rameshkumar.placementsystem.dto.PaginationResponse;
import com.rameshkumar.placementsystem.dto.StudentDTO;
import com.rameshkumar.placementsystem.service.StudentService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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
    @Operation(
            summary = "Create a new student",
            description = "Creates a student record. Accessible only to ADMIN users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
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
    @Operation(
            summary = "Get all students",
            description = "Returns the full student list. Accessible only to ADMIN users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Students fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<List<StudentDTO>> getAllStudents(
            @RequestParam(required = false) Double cgpa) {

        List<StudentDTO> students = cgpa != null
                ? studentService.filterStudentsByCgpa(cgpa)
                : studentService.getAllStudents();

        return new ApiResponse<>(
                true,
                "Students fetched successfully",
                students
        );
    }

    // GET STUDENT BY ID
    @Operation(
            summary = "Get student by ID",
            description = "Returns a single student by id.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found")
    })
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
    @Operation(
            summary = "Update student details",
            description = "Updates an existing student record. Accessible only to ADMIN users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found")
    })
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
    @Operation(
            summary = "Delete student by ID",
            description = "Deletes a student record by id. Accessible only to ADMIN users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found")
    })
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
    @Operation(
            summary = "Get students with pagination",
            description = "Returns students in paginated format.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Students fetched with pagination"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
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

    @Operation(
            summary = "Search students by skill",
            description = "Returns students whose skills contain the provided keyword.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Students filtered by skill successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/search")
    public ApiResponse<List<StudentDTO>> searchStudentsBySkill(@RequestParam String skill) {
        List<StudentDTO> students = studentService.searchStudentsBySkill(skill);
        return new ApiResponse<>(true, "Students filtered by skill successfully", students);
    }
}

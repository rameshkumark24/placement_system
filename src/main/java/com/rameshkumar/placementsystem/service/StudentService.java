package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.PaginationResponse;
import com.rameshkumar.placementsystem.dto.StudentDTO;

import java.util.List;

public interface StudentService {

    StudentDTO saveStudent(StudentDTO studentDTO);

    List<StudentDTO> getAllStudents();

    List<StudentDTO> searchStudentsBySkill(String skill);

    List<StudentDTO> filterStudentsByCgpa(double cgpa);

    StudentDTO getStudentById(Long id);

    StudentDTO updateStudent(Long id, StudentDTO studentDTO);

    void deleteStudent(Long id);

    PaginationResponse<StudentDTO> getStudentsPaginated(int page, int size);
}

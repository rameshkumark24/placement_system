package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.PaginationResponse;
import com.rameshkumar.placementsystem.dto.StudentDTO;
import com.rameshkumar.placementsystem.dto.StudentProfileUpdateRequest;

import java.util.List;

public interface StudentService {

    StudentDTO saveStudent(StudentDTO studentDTO);

    List<StudentDTO> getAllStudents();

    List<StudentDTO> filterStudents(String skill, Double cgpa);

    List<StudentDTO> searchStudentsBySkill(String skill);

    List<StudentDTO> filterStudentsByCgpa(double cgpa);

    StudentDTO getStudentById(Long id);

    StudentDTO getMyProfile(String email);

    StudentDTO updateMyProfile(String email, StudentProfileUpdateRequest profileUpdateRequest);

    StudentDTO updateStudent(Long id, StudentDTO studentDTO);

    void deleteStudent(Long id);

    PaginationResponse<StudentDTO> getStudentsPaginated(int page, int size);
}

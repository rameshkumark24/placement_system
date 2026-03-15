package com.rameshkumar.placementsystem.repository;
import com.rameshkumar.placementsystem.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentRepository extends JpaRepository<Student, Long>{
}

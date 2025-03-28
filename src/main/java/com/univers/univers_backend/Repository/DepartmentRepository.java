package com.univers.univers_backend.Repository;


import com.univers.univers_backend.Entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long>{

}

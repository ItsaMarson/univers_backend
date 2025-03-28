package com.univers.univers_backend.Service;

import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.DepartmentRepository;
import com.univers.univers_backend.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentService(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    public String assignDepartmentHead(Long departmentId, Long userId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        //Ensure no other user is already the department head
        if (department.getDeptHead() != null) {
            throw new RuntimeException("This department already has a department head");
        }

        department.setDeptHead(user);
        departmentRepository.save(department);

        return "User " + user.getEmail() + " is now the department head of " + department.getName();
    }

    public List<Department> getAllDepartments() {

        List<Department> departments = departmentRepository.findAll();

        return departments;
    }
}

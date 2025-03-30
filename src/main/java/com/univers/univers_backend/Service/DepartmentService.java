package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.DepartmentDTO;
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.DepartmentRepository;
import com.univers.univers_backend.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

        return departmentRepository.findAll();

    }

    public String addDepartment(DepartmentDTO departmentDTO) {
        Optional<Department> existingDepartment = departmentRepository.findByNameIgnoreCase(departmentDTO.name());

        if(existingDepartment.isPresent()){
            return "Department already exists.";
        }
        Department newDepartment = new Department();
        newDepartment.setName(departmentDTO.name());
        newDepartment.setDescription(departmentDTO.description());

        if(departmentDTO.deptHead() != null){
            User deptHead = userRepository.findById(departmentDTO.deptHead()).orElse(null);

            if(deptHead == null){
                return "Invalid department head";
            }
            newDepartment.setDeptHead(deptHead);
        }
        departmentRepository.save(newDepartment);

        return "Department has been saved.";
    }
    public String updateDepartment(Long departmentId, DepartmentDTO updatedDept){

        Optional<Department> existingDept = departmentRepository.findById(departmentId);

        if(!existingDept.isPresent()){
            return "Department does not exist";
        }
        Department department = existingDept.get();

        department.setName(updatedDept.name() != null ? updatedDept.name() : existingDept.get().getName());
        department.setDescription(updatedDept.description() != null ? updatedDept.description() : existingDept.get().getDescription());
        if(updatedDept.deptHead() != null){
            User deptHead = userRepository.findById(updatedDept.deptHead()).orElse(null);

            if(deptHead == null){
                return "Invalid department head";
            }
            department.setDeptHead(deptHead);
        }else{
            department.setDeptHead(existingDept.get().getDeptHead());
        }
        departmentRepository.save(department);
        return "Department has been successfully updated.";
    }
}

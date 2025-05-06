/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.DepartmentDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Mapper.UserMapper;
import com.univers.univers_backend.Repository.DepartmentRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            UserMapper userMapper) {
        this.departmentRepository = departmentRepository;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
    }

    public String assignDepartmentHead(Long departmentId, Long userId) {
        Department department =
                departmentRepository
                        .findById(departmentId)
                        .orElseThrow(() -> new RuntimeException("Department not found"));

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        if (department.getDeptHead() != null && !department.getDeptHead().getId().equals(userId)) {
            User currentHead = department.getDeptHead();
            if (currentHead != null && !currentHead.getId().equals(userId)) {
                throw new RuntimeException(
                        "This department already has a department head: "
                                + currentHead.getFirstname()
                                + " "
                                + currentHead.getLastname());
            }
        }

        department.setDeptHead(user);
        departmentRepository.save(department);

        return "User " + user.getEmail() + " is now the department head of " + department.getName();
    }

    public List<DepartmentDTO> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return departments.stream().map(this::mapDepartmentToDTO).collect(Collectors.toList());
    }

    public String addDepartment(DepartmentDTO departmentDTO) {
        // departmentDTO.name() and departmentDTO.description() are used directly.
        // For departmentDTO.deptHead(), it's a UserDTO. If an ID for the department head
        // is provided via departmentDTO.deptHead().id(), this service will attempt to
        // find and assign that user.
        // Note: If the request payload sends deptHead as a simple ID (e.g., "deptHead": 116),
        // and DepartmentDTO expects a UserDTO object, departmentDTO.deptHead() might be null
        // after deserialization, and the department head will not be set.

        Optional<Department> existingDepartment =
                departmentRepository.findByNameIgnoreCase(departmentDTO.name());

        if (existingDepartment.isPresent()) {
            return "Department with name '" + departmentDTO.name() + "' already exists.";
        }
        Department newDepartment = new Department();
        newDepartment.setName(departmentDTO.name());
        newDepartment.setDescription(departmentDTO.description());

        // Check if department head information is provided in the DTO
        if (departmentDTO.deptHeadId() != null) {
            Long deptHeadUserId = departmentDTO.deptHeadId();
            User deptHeadUser = userRepository.findById(deptHeadUserId).orElse(null);
            if (deptHeadUser == null) {
                return "Invalid department head: User with ID " + deptHeadUserId + " not found.";
            }
            // Optional: Add further validation for the user (e.g., role) if necessary
            // e.g., if (!deptHeadUser.getRole().equals(UserRole.DEPARTMENT_HEAD) && ...) {
            //    return "User with ID " + deptHeadUserId + " is not eligible to be a department
            // head.";
            // }
            newDepartment.setDeptHead(deptHeadUser);
        }
        // If departmentDTO.deptHead() is null or departmentDTO.deptHead().id() is null,
        // the department will be created without a department head.

        departmentRepository.save(newDepartment);

        return "Department '" + newDepartment.getName() + "' has been saved successfully.";
    }

    public String updateDepartment(Long departmentId, DepartmentDTO updatedDeptDTO) {

        Optional<Department> existingDeptOpt = departmentRepository.findById(departmentId);

        if (existingDeptOpt.isEmpty()) {
            return "Department does not exist";
        }
        Department department = existingDeptOpt.get();

        department.setName(
                updatedDeptDTO.name() != null ? updatedDeptDTO.name() : department.getName());
        department.setDescription(
                updatedDeptDTO.description() != null
                        ? updatedDeptDTO.description()
                        : department.getDescription());

        // Handling deptHead update:
        // If updatedDeptDTO.deptHead() is null, it means unassign.
        // If updatedDeptDTO.deptHead() has a UserDTO, it means assign/change.
        if (updatedDeptDTO.deptHeadId() != null) {
            User deptHeadUser = userRepository.findById(updatedDeptDTO.deptHeadId()).orElse(null);
            if (deptHeadUser == null) {
                return "Invalid department head user specified for update.";
            }
            department.setDeptHead(deptHeadUser);
        } else if (updatedDeptDTO.deptHead() == null) {
            department.setDeptHead(null);
        }
        // If updatedDeptDTO.deptHead() is present but ID is null, it's ambiguous, could be an error
        // or keep existing.
        // Current logic: if deptHead DTO is there with an ID, update. If deptHead DTO is null,
        // unassign.

        departmentRepository.save(department);
        return "Department has been successfully updated.";
    }

    public Department getDepartmentById(Long departmentId) {
        return departmentRepository.findById(departmentId).orElse(null);
    }

    public String deleteDepartment(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            return "Department not found.";
        }
        // Add any business logic before deletion if necessary,
        // e.g., check if department is associated with other entities

        departmentRepository.deleteById(departmentId);
        return "Department successfully deleted.";
    }

    private DepartmentDTO mapDepartmentToDTO(Department department) {
        if (department == null) {
            return null;
        }
        UserDTO deptHeadDTO = null;
        if (department.getDeptHead() != null) {
            // Assuming userService has a method to map User to UserDTO
            // If mapUserToDTO is not public or accessible, you'll need to adjust this
            // For example, by creating a public mapper method in UserService or a separate
            // UserMapper class
            try {
                // Temporarily make mapUserToDTO public in UserService or use a shared mapper
                // For now, let's assume it's accessible or create a local helper if needed.
                // This is a placeholder for the actual mapping logic.
                // You might need to make userService.mapUserToDTO public or use a dedicated mapper.
                deptHeadDTO = userMapper.toDto(department.getDeptHead());
            } catch (Exception e) {
                // Handle cases where user service might not be fully initialized or method not
                // accessible
                System.err.println("Error mapping department head to DTO: " + e.getMessage());
            }
        }
        return new DepartmentDTO(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.getDeptHead() != null ? department.getDeptHead().getId() : null,
                deptHeadDTO,
                department.getCreatedAt(),
                department.getUpdatedAt());
    }

    public DepartmentDTO getDepartmentDtoById(Long departmentId) {
        Department department =
                departmentRepository
                        .findById(departmentId)
                        .orElseThrow(() -> new RuntimeException("Department not found"));
        return mapDepartmentToDTO(department);
    }
}

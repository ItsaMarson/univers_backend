/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.DepartmentDTO;
// UserDTO import might not be directly needed if DepartmentMapper handles User mapping
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Mapper.DepartmentMapper; // Import DepartmentMapper
// UserMapper is a dependency of DepartmentMapper, not necessarily needed directly here for DTO
// mapping
import com.univers.univers_backend.Repository.DepartmentRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // Import UUID
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final DepartmentMapper departmentMapper; // Injected DepartmentMapper

    public DepartmentService(
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            @Lazy
                    DepartmentMapper
                            departmentMapper) { // Inject DepartmentMapper, @Lazy if cycles are a
        // concern
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.departmentMapper = departmentMapper;
    }

    public String assignDepartmentHead(UUID departmentPublicId, UUID userPublicId) {
        Department department =
                departmentRepository
                        .findByPublicId(departmentPublicId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Department not found with Public ID: "
                                                        + departmentPublicId));

        User user =
                userRepository
                        .findByPublicId(userPublicId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "User not found with Public ID: " + userPublicId));

        if (department.getDeptHead() != null
                && !department.getDeptHead().getPublicId().equals(userPublicId)) {
            User currentHead = department.getDeptHead();
            throw new RuntimeException(
                    "This department already has a department head: "
                            + currentHead.getFirstname()
                            + " "
                            + currentHead.getLastname());
        }
        department.setDeptHead(user);
        departmentRepository.save(department);
        return "User " + user.getEmail() + " is now the department head of " + department.getName();
    }

    public List<DepartmentDTO> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return departments.stream().map(departmentMapper::toDto).collect(Collectors.toList());
    }

    public String addDepartment(DepartmentDTO departmentDTO) {
        Optional<Department> existingDepartment =
                departmentRepository.findByNameIgnoreCase(departmentDTO.name());

        if (existingDepartment.isPresent()) {
            return "Department with name '" + departmentDTO.name() + "' already exists.";
        }
        Department newDepartment = new Department();
        newDepartment.setName(departmentDTO.name());
        newDepartment.setDescription(departmentDTO.description());

        if (departmentDTO.deptHead() != null && departmentDTO.deptHead().publicId() != null) {
            UUID deptHeadUserPublicId = departmentDTO.deptHead().publicId();
            User deptHeadUser =
                    userRepository
                            .findByPublicId(deptHeadUserPublicId)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Invalid department head: User with Public ID "
                                                            + deptHeadUserPublicId
                                                            + " not found."));
            newDepartment.setDeptHead(deptHeadUser);
        } // If deptHead or its publicId is null, created without a head.

        departmentRepository.save(newDepartment);
        return "Department '" + newDepartment.getName() + "' has been saved successfully.";
    }

    public String updateDepartment(UUID departmentPublicId, DepartmentDTO updatedDeptDTO) {
        Department department =
                departmentRepository
                        .findByPublicId(departmentPublicId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Department not found with Public ID: "
                                                        + departmentPublicId));

        if (updatedDeptDTO.name() != null) {
            department.setName(updatedDeptDTO.name());
        }
        if (updatedDeptDTO.description() != null) {
            department.setDescription(updatedDeptDTO.description());
        }

        if (updatedDeptDTO.deptHead() != null && updatedDeptDTO.deptHead().publicId() != null) {
            UUID deptHeadUserPublicId = updatedDeptDTO.deptHead().publicId();
            User deptHeadUser =
                    userRepository
                            .findByPublicId(deptHeadUserPublicId)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Invalid department head User (Public ID: "
                                                            + deptHeadUserPublicId
                                                            + ") for update."));
            department.setDeptHead(deptHeadUser);
        } else if (updatedDeptDTO.deptHead() == null) { // Check for explicit null to unassign
            department.setDeptHead(null);
        }
        // If deptHead DTO is present but publicId is null, it implies no change to dept head unless
        // explicitly nulled.

        departmentRepository.save(department);
        return "Department has been successfully updated.";
    }

    // This method returns the ENTITY, used by EventService
    public Department getDepartmentByPublicId(UUID publicId) {
        return departmentRepository
                .findByPublicId(publicId)
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Department not found with Public ID: " + publicId));
    }

    // This method returns the DTO
    public DepartmentDTO getDepartmentDtoByPublicId(UUID publicId) {
        Department department = getDepartmentByPublicId(publicId); // Reuse the method above
        return departmentMapper.toDto(department);
    }

    @Deprecated // Prefer getDepartmentByPublicId or getDepartmentDtoByPublicId
    public Department getDepartmentById(Long departmentId) {
        return departmentRepository.findById(departmentId).orElse(null);
    }

    public String deleteDepartment(UUID departmentPublicId) {
        Department department =
                departmentRepository
                        .findByPublicId(departmentPublicId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Department not found with Public ID: "
                                                        + departmentPublicId
                                                        + ", cannot delete."));

        // TODO: Add any business logic before deletion if necessary,
        // e.g., check if department is associated with other entities (Users, Events)
        // and handle those cases (e.g., disallow deletion, reassign, nullify).
        // For example, if users are in this department, what happens?
        // if (!userRepository.findAllByDepartment(department).isEmpty()) {
        //    return "Cannot delete department: Users are still assigned to it.";
        // }

        departmentRepository.delete(department);
        return "Department successfully deleted.";
    }
    // Removed private mapDepartmentToDTO method
}

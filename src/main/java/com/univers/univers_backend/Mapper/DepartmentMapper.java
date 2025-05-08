/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.DepartmentDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Department;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    private final UserMapper userMapper;

    // Use @Lazy to help break potential circular dependency between UserMapper and DepartmentMapper
    public DepartmentMapper(@Lazy UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public DepartmentDTO toDto(Department department) {
        if (department == null) {
            return null;
        }

        UserDTO deptHeadDto = null;
        if (department.getDeptHead() != null) {
            // This is the crucial part for the cycle:
            // UserMapper.toDto will convert User to UserDTO.
            // That UserDTO contains a DepartmentDTO.
            // If UserMapper directly calls this.toDto for that department, we have a loop.
            // Using @Lazy might be enough for Spring to manage the proxying.
            // Alternatively, UserMapper might need a toDto(User user, boolean mapDepartment)
            // variant.
            deptHeadDto = userMapper.toDto(department.getDeptHead());
        }

        return new DepartmentDTO(
                department.getPublicId(),
                department.getName(),
                department.getDescription(),
                deptHeadDto,
                department.getCreatedAt(),
                department.getUpdatedAt());
    }

    // Add toEntity method if needed later
    // public Department toEntity(DepartmentDTO dto) { ... }

}

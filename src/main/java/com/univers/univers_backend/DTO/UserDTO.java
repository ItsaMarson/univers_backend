package com.univers.univers_backend.DTO;


import com.univers.univers_backend.Entity.Department;

public record UserDTO(
        Long id,
        String email,
        String firstName,
        String lastName,
        String idNumber,
        String phoneNumber,
        String role,
        Department department,
        Boolean emailVerified) {
}

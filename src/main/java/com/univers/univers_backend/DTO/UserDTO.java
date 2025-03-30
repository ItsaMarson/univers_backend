package com.univers.univers_backend.DTO;



public record UserDTO(
        Long id,
        String email,
        String firstName,
        String lastName,
        String idNumber,
        String phoneNumber,
        String role,
        Boolean emailVerified) {
}

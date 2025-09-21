package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Enum.Status;

import java.util.UUID;

public record EventPersonnelDTO (

        UUID publicId,
        String name,
        String phoneNumber
){}

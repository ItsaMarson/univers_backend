package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Enum.Task;

import java.util.UUID;

public record EventPersonnelDTO (

        UUID publicId,
//        String name,
        UserDTO personnel,
        String phoneNumber,

        Task task
){}

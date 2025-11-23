/* (C)2025 */
package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Enum.Task;
import java.util.UUID;

public record EventPersonnelDTO(UUID publicId, UserDTO personnel, String phoneNumber, Task task) {}

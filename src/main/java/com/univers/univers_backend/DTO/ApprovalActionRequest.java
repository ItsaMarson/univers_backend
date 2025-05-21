/* (C)2025 */
package com.univers.univers_backend.DTO;

// Using a record for simplicity and immutability
public record ApprovalActionRequest(
        String status, // Expecting "APPROVED" or "REJECTED"
        String remarks) {}

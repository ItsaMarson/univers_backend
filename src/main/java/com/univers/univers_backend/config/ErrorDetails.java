/* (C)2025 */
package com.univers.univers_backend.config;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Error details")
public class ErrorDetails {
    @Schema(description = "Error message", example = "Invalid input")
    private String message;

    @Schema(description = "Error code", example = "INVALID_INPUT")
    private String code;

    @Schema(description = "Detailed error information", example = "The provided email is not valid")
    private String details;

    public ErrorDetails() {}

    public ErrorDetails(String message, String code, String details) {
        this.message = message;
        this.code = code;
        this.details = details;
    }

    public ErrorDetails(String message) {
        this.message = message;
        this.code = "ERROR";
        this.details = message;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorDetails that = (ErrorDetails) o;
        return Objects.equals(message, that.message)
                && Objects.equals(code, that.code)
                && Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, code, details);
    }

    @Override
    public String toString() {
        return "ErrorDetails{"
                + "message='"
                + message
                + '\''
                + ", code='"
                + code
                + '\''
                + ", details='"
                + details
                + '\''
                + '}';
    }
}

/* (C)2025 */
package com.univers.univers_backend.config;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {
    @Schema(description = "HTTP status code", example = "200")
    private int status;

    @Schema(description = "Response message", example = "Operation successful")
    private String message;

    @Schema(description = "Response data")
    private T data;

    @Schema(description = "Error details if any")
    private ErrorDetails error;

    public ApiResponse() {}

    public ApiResponse(int status, String message, T data, ErrorDetails error) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiResponse<?> that = (ApiResponse<?>) o;
        return status == that.status
                && Objects.equals(message, that.message)
                && Objects.equals(data, that.data)
                && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, message, data, error);
    }

    @Override
    public String toString() {
        return "ApiResponse{"
                + "status="
                + status
                + ", message='"
                + message
                + '\''
                + ", data="
                + data
                + ", error="
                + error
                + '}';
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, null);
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null, new ErrorDetails(message));
    }

    public static <T> ApiResponse<T> error(int status, String message, String details) {
        return new ApiResponse<>(status, message, null, new ErrorDetails(details));
    }
}

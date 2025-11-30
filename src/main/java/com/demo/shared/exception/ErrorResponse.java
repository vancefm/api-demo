package com.demo.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Error response body")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "Error message", example = "Resource not found")
    private String message;

    @Schema(description = "Detailed error description")
    private String details;

    @Schema(description = "Timestamp of error occurrence")
    private LocalDateTime timestamp;

    @Schema(description = "Request path that caused the error")
    private String path;

    // Constructors
    public ErrorResponse() {
    }

    public ErrorResponse(int status, String message, String details, LocalDateTime timestamp, String path) {
        this.status = status;
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
        this.path = path;
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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int status;
        private String message;
        private String details;
        private LocalDateTime timestamp;
        private String path;

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(status, message, details, timestamp, path);
        }
    }
}

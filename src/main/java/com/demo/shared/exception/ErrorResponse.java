package com.demo.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "Error response body")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}

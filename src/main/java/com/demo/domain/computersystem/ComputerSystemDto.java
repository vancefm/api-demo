package com.demo.domain.computersystem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Computer System Data Transfer Object")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComputerSystemDto {

    @Schema(description = "Unique identifier", example = "1")
    private Long id;

    @NotBlank(message = "Hostname is required")
    @Schema(description = "Computer hostname", example = "SERVER-001")
    private String hostname;

    @NotBlank(message = "Manufacturer is required")
    @Schema(description = "Computer manufacturer", example = "Dell")
    private String manufacturer;

    @NotBlank(message = "Model is required")
    @Schema(description = "Computer model", example = "PowerEdge R750")
    private String model;

    @NotNull(message = "User ID is required")
    @Schema(description = "Assigned user ID", example = "1")
    private Long userId;

    @NotBlank(message = "Department is required")
    @Schema(description = "Department", example = "IT")
    private String department;

    @NotBlank(message = "MAC address is required")
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", message = "Invalid MAC address format")
    @Schema(description = "MAC address", example = "00:1A:2B:3C:4D:5E")
    private String macAddress;

    @NotBlank(message = "IP address is required")
    @Pattern(regexp = "^(?:\\d{1,3}\\.){3}\\d{1,3}$", message = "Invalid IP address format")
    @Schema(description = "IP address", example = "192.168.1.100")
    private String ipAddress;

    @NotBlank(message = "Network name is required")
    @Schema(description = "Network name", example = "PROD-NETWORK")
    private String networkName;

    @Schema(description = "ID of the user who created this record", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long createdById;

    @Schema(description = "ID of the user who last updated this record", example = "2", accessMode = Schema.AccessMode.READ_ONLY)
    private Long updatedById;

    @Schema(description = "Timestamp when this record was created", accessMode = Schema.AccessMode.READ_ONLY)
    private java.time.LocalDateTime createdAt;

    @Schema(description = "Timestamp when this record was last updated", accessMode = Schema.AccessMode.READ_ONLY)
    private java.time.LocalDateTime updatedAt;
}

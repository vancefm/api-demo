package com.demo.feature.security.rbac;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "One entity:field → operation grant on a role")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionDto {

    @Schema(description = "Permission ID", example = "12", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Permission entity is required")
    @Schema(description = "Secured entity name, or * for every entity", example = "User")
    private String entity;

    @Schema(description = "DTO property name, or * for every field (default)", example = "firstName")
    private String field;

    @NotNull(message = "Permission operation is required")
    @Schema(description = "Operation granted", example = "READ")
    private Operation operation;
}

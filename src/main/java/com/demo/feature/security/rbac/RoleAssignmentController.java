package com.demo.feature.security.rbac;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the Department:Role grants of a user.
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/role-assignments")
@Tag(name = "Role Assignments", description = "Department:Role grants held by a user")
public class RoleAssignmentController {

    private final RoleAssignmentService roleAssignmentService;

    public RoleAssignmentController(RoleAssignmentService roleAssignmentService) {
        this.roleAssignmentService = roleAssignmentService;
    }

    @GetMapping
    @Operation(summary = "List a user's role assignments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Assignments listed"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<RoleAssignmentDto>> list(@PathVariable Long userId) {
        return ResponseEntity.ok(roleAssignmentService.list(userId));
    }

    @PostMapping
    @Operation(summary = "Grant a role to a user",
        description = "Grants roleId to the user within departmentId, or globally when departmentId is omitted")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Role granted"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "User, role or department not found"),
        @ApiResponse(responseCode = "409", description = "The user already holds this grant")
    })
    public ResponseEntity<RoleAssignmentDto> grant(@PathVariable Long userId,
                                                   @Valid @RequestBody RoleAssignmentDto dto) {
        return new ResponseEntity<>(roleAssignmentService.grant(userId, dto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{assignmentId}")
    @Operation(summary = "Revoke a role assignment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Assignment revoked"),
        @ApiResponse(responseCode = "404", description = "User or assignment not found")
    })
    public ResponseEntity<Void> revoke(@PathVariable Long userId, @PathVariable Long assignmentId) {
        roleAssignmentService.revoke(userId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}

package com.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Computer System Data Transfer Object")
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

    @NotBlank(message = "User is required")
    @Schema(description = "Computer user", example = "john.doe")
    private String user;

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

    // Constructors
    public ComputerSystemDto() {
    }

    public ComputerSystemDto(Long id, String hostname, String manufacturer, String model, String user,
                            String department, String macAddress, String ipAddress, String networkName) {
        this.id = id;
        this.hostname = hostname;
        this.manufacturer = manufacturer;
        this.model = model;
        this.user = user;
        this.department = department;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.networkName = networkName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String hostname;
        private String manufacturer;
        private String model;
        private String user;
        private String department;
        private String macAddress;
        private String ipAddress;
        private String networkName;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder manufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder department(String department) {
            this.department = department;
            return this;
        }

        public Builder macAddress(String macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder networkName(String networkName) {
            this.networkName = networkName;
            return this;
        }

        public ComputerSystemDto build() {
            return new ComputerSystemDto(id, hostname, manufacturer, model, user, department,
                    macAddress, ipAddress, networkName);
        }
    }
}

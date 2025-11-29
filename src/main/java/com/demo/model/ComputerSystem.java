package com.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "computer_systems")
public class ComputerSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String hostname;

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private String model;

    @Column(name = "assigned_user", nullable = false)
    private String systemUser;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false, unique = true)
    private String macAddress;

    @Column(nullable = false, unique = true)
    private String ipAddress;

    @Column(nullable = false)
    private String networkName;

    // Constructors
    public ComputerSystem() {
    }

    public ComputerSystem(Long id, String hostname, String manufacturer, String model, String systemUser,
                         String department, String macAddress, String ipAddress, String networkName) {
        this.id = id;
        this.hostname = hostname;
        this.manufacturer = manufacturer;
        this.model = model;
        this.systemUser = systemUser;
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

    public String getSystemUser() {
        return systemUser;
    }

    public void setSystemUser(String systemUser) {
        this.systemUser = systemUser;
    }

    public String getUser() {
        return getSystemUser();
    }

    public void setUser(String user) {
        setSystemUser(user);
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
        private String systemUser;
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
            this.systemUser = user;
            return this;
        }

        public Builder systemUser(String systemUser) {
            this.systemUser = systemUser;
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

        public ComputerSystem build() {
            return new ComputerSystem(id, hostname, manufacturer, model, systemUser, department,
                    macAddress, ipAddress, networkName);
        }
    }
}

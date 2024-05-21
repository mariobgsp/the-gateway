package com.example.gatewayservice.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "\"user\"", schema = "\"user\"")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String email;
    private Long userRoleId;
    private String userSessionStatus;
    private LocalDateTime userLastLogin;
    private LocalDateTime userLastChangePassword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getUserRoleId() {
        return userRoleId;
    }

    public void setUserRoleId(Long userRoleId) {
        this.userRoleId = userRoleId;
    }

    public String getUserSessionStatus() {
        return userSessionStatus;
    }

    public void setUserSessionStatus(String userSessionStatus) {
        this.userSessionStatus = userSessionStatus;
    }

    public LocalDateTime getUserLastLogin() {
        return userLastLogin;
    }

    public void setUserLastLogin(LocalDateTime userLastLogin) {
        this.userLastLogin = userLastLogin;
    }

    public LocalDateTime getUserLastChangePassword() {
        return userLastChangePassword;
    }

    public void setUserLastChangePassword(LocalDateTime userLastChangePassword) {
        this.userLastChangePassword = userLastChangePassword;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

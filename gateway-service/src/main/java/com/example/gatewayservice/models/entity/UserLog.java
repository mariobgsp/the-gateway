package com.example.gatewayservice.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_log", schema = "\"user\"")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    private String userActivityName; // LOGIN, UPDATE-{operationName}, LOGOUT
    private String userSessionStatus; // ACTIVE, INACTIVE
    private String userSession;
    private String userToken;
    private String errorMessage;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public UserLog(User user , String userActivityName , String userSessionStatus , String userSession , String userToken , String errorMessage) {
        this.user = user;
        this.userActivityName = userActivityName;
        this.userSessionStatus = userSessionStatus;
        this.userSession = userSession;
        this.userToken = userToken;
        this.errorMessage = errorMessage;
    }
}

package com.example.gatewayservice.service;

import com.example.gatewayservice.models.entity.TokenAccessLog;
import com.example.gatewayservice.models.entity.TokenLog;
import com.example.gatewayservice.repository.TokenAccessLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class SchedulerServices {

    @Autowired
    private TokenAccessLogRepository tokenAccessLogRepository;
    @Autowired
    private SystemPropertiesServices systemPropertiesServices;

    public void checkToken() {
        String statusActive = systemPropertiesServices.getProps("TOKEN_ACCESS_ACTIVE_STATUS");
        String statusInactive = systemPropertiesServices.getProps("TOKEN_ACCESS_INACTIVE_STATUS");

        // in minutes
        long expiringTime = Long.parseLong(systemPropertiesServices.getProps("API_ACCESS_TIME"));

        // deactivate if time now exceeding
        List<TokenAccessLog> tokenLogList = tokenAccessLogRepository.findByStatus(statusActive);
        for (TokenAccessLog tl : tokenLogList) {
            LocalDateTime created = tl.getCreatedAt();
            LocalDateTime now = LocalDateTime.now();

            long hoursDifference = Duration.between(created, now).toMinutes();
            if (hoursDifference > expiringTime) {
                tl.setStatus(statusInactive);
                tokenAccessLogRepository.save(tl);
            } else {
                log.info("token {} still active", tl.getId());
            }
        }
    }
}

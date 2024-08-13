package com.example.gatewayservice.service;

import com.example.gatewayservice.models.entity.TokenLog;
import com.example.gatewayservice.repository.TokenLogRepository;
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
    private TokenLogRepository tokenLogRepository;
    @Autowired
    private SystemPropertiesServices systemPropertiesServices;

    public void checkToken() {
        String statusActive = systemPropertiesServices.getProps("token_active_status");
        String statusDeactive = systemPropertiesServices.getProps("token_deactive_status");

        // in minutes
        long expiringTime = Long.parseLong(systemPropertiesServices.getProps("api_access_time"));

        // deactivate if time now exceeding
        List<TokenLog> tokenLogList = tokenLogRepository.findByStatus(statusActive);
        for (TokenLog tl : tokenLogList) {
            LocalDateTime created = tl.getCreatedAt();
            LocalDateTime now = LocalDateTime.now();

            long hoursDifference = Duration.between(created, now).toMinutes();
            if (hoursDifference > expiringTime) {
                tl.setStatus(statusDeactive);
                tokenLogRepository.save(tl);
            } else {
                log.info("token still active");
            }
        }
    }
}

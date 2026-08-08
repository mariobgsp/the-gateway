package com.example.gatewayservice.service.security;

import com.example.gatewayservice.models.entity.TokenLog;
import com.example.gatewayservice.repository.security.TokenLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class TokenBlacklistService {

    @Autowired
    private TokenLogRepository tokenLogRepository;

    public void saveActiveToken(String token) {
        TokenLog tokenLog = new TokenLog();
        tokenLog.setToken(token);
        tokenLog.setStatus("ENABLED");
        tokenLog.setCreatedAt(LocalDateTime.now());
        tokenLog.setUpdatedAt(LocalDateTime.now());
        tokenLogRepository.save(tokenLog);
    }

    public Boolean blacklistToken(String token) {
        Optional<TokenLog> tokenLog = tokenLogRepository.findFirstByTokenOrderByIdDesc(token);
        tokenLog.ifPresent(log -> {
            log.setStatus("DISABLED");
            log.setUpdatedAt(LocalDateTime.now());
            tokenLogRepository.save(log);
        });
        return true;
    }

    public boolean isTokenBlacklisted(String token) {
        Optional<TokenLog> tokenLog = tokenLogRepository.findFirstByTokenOrderByIdDesc(token);
        return tokenLog.isEmpty() || tokenLog.get().getStatus().equals("DISABLED");
    }
}

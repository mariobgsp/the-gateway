package com.example.gatewayservice.service.security;

import com.example.gatewayservice.models.entity.TokenLog;
import com.example.gatewayservice.repository.security.TokenLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TokenBlacklistService {

    @Autowired
    private TokenLogRepository tokenLogRepository;

    public void saveActiveToken(String token) {
        TokenLog tokenLog = new TokenLog();
        tokenLog.setToken(token);
        tokenLog.setStatus("ENABLED");
        tokenLogRepository.save(tokenLog);
    }

    public Boolean blacklistToken(String token) {
        boolean blacklisted = false;
        TokenLog tokenLog = tokenLogRepository.findByToken(token);
        if (tokenLog != null) {
            tokenLog.setStatus("DISABLED");
            tokenLogRepository.save(tokenLog);
        }
        blacklisted = true;
        return blacklisted;
    }

    public boolean isTokenBlacklisted(String token) {
        TokenLog tokenLog = tokenLogRepository.findByToken(token);
        if (tokenLog == null) {
            return true;
        }

        return tokenLog.getStatus().equals("DISABLED");
    }
}

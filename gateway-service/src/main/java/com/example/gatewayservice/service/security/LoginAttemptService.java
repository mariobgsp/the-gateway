package com.example.gatewayservice.service.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 600;

    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String username) {
        Deque<Long> timestamps = attempts.getOrDefault(username, new ArrayDeque<>());
        prune(timestamps);
        return timestamps.size() >= MAX_ATTEMPTS;
    }

    public void registerFailure(String username) {
        attempts.computeIfAbsent(username, k -> new ArrayDeque<>()).addLast(Instant.now().getEpochSecond());
    }

    public void registerSuccess(String username) {
        attempts.remove(username);
    }

    private void prune(Deque<Long> timestamps) {
        long cutoff = Instant.now().getEpochSecond() - WINDOW_SECONDS;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }
    }
}

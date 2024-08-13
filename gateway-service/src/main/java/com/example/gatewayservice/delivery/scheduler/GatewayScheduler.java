package com.example.gatewayservice.delivery.scheduler;

import com.example.gatewayservice.service.SchedulerServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class GatewayScheduler {

    @Autowired
    private SchedulerServices schedulerServices;

    @Scheduled(cron = "${app.scheduler.cron.check.expiring.token}")
    public void checkExpiredToken() {
        log.info("start check expired token");
        CompletableFuture.runAsync(() -> schedulerServices.checkToken());
        log.info("end check expired token");
    }

}

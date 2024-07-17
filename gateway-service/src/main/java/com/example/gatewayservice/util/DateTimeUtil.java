package com.example.gatewayservice.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return dateTime.format(formatter);
    }
}

package com.railse.hiring.workforcemgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
@AllArgsConstructor
public class ActivityLogDto {
    private String message;
    private String timestamp;

    public ActivityLogDto(String message, long timestamp) {
        this.message = message;
        this.timestamp = formatTimestamp(timestamp);
    }
    private String formatTimestamp(long millis) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Kolkata"))
                .format(Instant.ofEpochMilli(millis));
    }
}

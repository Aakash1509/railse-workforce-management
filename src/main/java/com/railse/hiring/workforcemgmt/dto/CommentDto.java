package com.railse.hiring.workforcemgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
public class CommentDto {
    private String comment;
    private String timestamp;

    public CommentDto(String comment, long timestamp) {
        this.comment = comment;
        this.timestamp = formatTimestamp(timestamp);
    }

    private String formatTimestamp(long millis) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Kolkata"))
                .format(Instant.ofEpochMilli(millis));
    }
}

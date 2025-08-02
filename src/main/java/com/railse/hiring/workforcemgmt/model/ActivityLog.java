package com.railse.hiring.workforcemgmt.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityLog {
    private Long taskId;
    private String message;
    private Long timestamp;
}

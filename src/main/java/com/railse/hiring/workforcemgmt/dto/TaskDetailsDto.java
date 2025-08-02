package com.railse.hiring.workforcemgmt.dto;

import com.railse.hiring.workforcemgmt.model.ActivityLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDetailsDto {
    private TaskManagementDto task;
    private List<CommentDto> comments;
    private List<ActivityLogDto> activityHistory;
}

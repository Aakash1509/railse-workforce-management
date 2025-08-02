package com.railse.hiring.workforcemgmt.dto;

import lombok.Data;

@Data
public class AddCommentRequest {
    private Long taskId;
    private String commentText;
}

package com.household.todoservice.dto;

import com.household.todoservice.entity.TaskPriority;
import com.household.todoservice.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for task responses in API endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private UUID id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDateTime dueDate;
    private Integer estimatedDurationMinutes;
    private String recurringPattern;
    
    private CategoryResponse category;
    private UserResponse assignedUser;
    private UserResponse createdByUser;
    private UserResponse completedByUser;
    
    private List<String> tags;
    private String attachments;
    private String customFields;
    
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private boolean overdue;
    private boolean completed;
}

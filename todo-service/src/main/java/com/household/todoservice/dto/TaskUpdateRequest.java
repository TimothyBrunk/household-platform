package com.household.todoservice.dto;

import com.household.todoservice.entity.TaskPriority;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for updating an existing task.
 * All fields are optional to allow partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {

    @Size(max = 255, message = "Task title must be less than 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    private UUID categoryId;

    private TaskPriority priority;

    private LocalDateTime dueDate;

    private Integer estimatedDurationMinutes;

    private UUID assignedUserId;

    private List<String> tags;

    private String recurringPattern;

    private String customFields;
}

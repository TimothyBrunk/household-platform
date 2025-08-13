package com.household.todoservice.controller;

import com.household.todoservice.dto.TaskCreateRequest;
import com.household.todoservice.dto.TaskResponse;
import com.household.todoservice.dto.TaskUpdateRequest;
import com.household.todoservice.entity.TaskPriority;
import com.household.todoservice.entity.TaskStatus;
import com.household.todoservice.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for task-related operations.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tasks", description = "Task management operations")
public class TaskController {

    private final TaskService taskService;

    /**
     * Create a new task.
     */
    @PostMapping
    @Operation(summary = "Create a new task", description = "Creates a new task in the specified household")
    public ResponseEntity<TaskResponse> createTask(
            @RequestAttribute("householdId") UUID householdId,
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody TaskCreateRequest request) {
        
        log.info("Creating task for household: {}, created by user: {}", householdId, userId);
        TaskResponse task = taskService.createTask(householdId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    /**
     * Get a task by ID.
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID", description = "Retrieves a specific task by its ID")
    public ResponseEntity<TaskResponse> getTask(
            @RequestAttribute("householdId") UUID householdId,
            @PathVariable UUID taskId) {
        
        log.debug("Getting task: {} for household: {}", taskId, householdId);
        TaskResponse task = taskService.getTask(householdId, taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * Update a task.
     */
    @PutMapping("/{taskId}")
    @Operation(summary = "Update task", description = "Updates an existing task")
    public ResponseEntity<TaskResponse> updateTask(
            @RequestAttribute("householdId") UUID householdId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskUpdateRequest request) {
        
        log.info("Updating task: {} for household: {}", taskId, householdId);
        TaskResponse task = taskService.updateTask(householdId, taskId, request);
        return ResponseEntity.ok(task);
    }

    /**
     * Delete a task.
     */
    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete task", description = "Soft deletes a task")
    public ResponseEntity<Void> deleteTask(
            @RequestAttribute("householdId") UUID householdId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "false") boolean permanent) {
        
        log.info("Deleting task: {} for household: {}", taskId, householdId);
        taskService.deleteTask(householdId, taskId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get tasks with filtering and pagination.
     */
    @GetMapping
    @Operation(summary = "Get tasks", description = "Retrieves tasks with optional filtering and pagination")
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @RequestAttribute("householdId") UUID householdId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID assignedUserId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDateFrom,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        
        log.debug("Getting tasks for household: {} with filters", householdId);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<TaskResponse> tasks = taskService.getTasks(
                householdId, status, priority, categoryId, assignedUserId, 
                dueDateFrom, dueDateTo, pageable);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * Search tasks by text.
     */
    @GetMapping("/search")
    @Operation(summary = "Search tasks", description = "Searches tasks by text in title and description")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
            @RequestAttribute("householdId") UUID householdId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Searching tasks for household: {} with query: {}", householdId, q);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TaskResponse> tasks = taskService.searchTasks(householdId, q, pageable);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks assigned to a specific user.
     */
    @GetMapping("/assigned/{userId}")
    @Operation(summary = "Get tasks by user", description = "Retrieves tasks assigned to a specific user")
    public ResponseEntity<Page<TaskResponse>> getTasksByUser(
            @RequestAttribute("householdId") UUID householdId,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Getting tasks assigned to user: {} in household: {}", userId, householdId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending());
        Page<TaskResponse> tasks = taskService.getTasksByUser(householdId, userId, pageable);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * Update task status.
     */
    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Update task status", description = "Updates the status of a task")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @RequestAttribute("householdId") UUID householdId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID taskId,
            @RequestBody TaskStatusUpdateRequest request) {
        
        log.info("Updating task status: {} to {} for household: {}", taskId, request.status(), householdId);
        TaskResponse task = taskService.updateTaskStatus(householdId, taskId, request.status(), userId);
        return ResponseEntity.ok(task);
    }

    /**
     * Assign task to a user.
     */
    @PatchMapping("/{taskId}/assign")
    @Operation(summary = "Assign task", description = "Assigns a task to a specific user")
    public ResponseEntity<TaskResponse> assignTask(
            @RequestAttribute("householdId") UUID householdId,
            @PathVariable UUID taskId,
            @RequestBody TaskAssignmentRequest request) {
        
        log.info("Assigning task: {} to user: {} in household: {}", taskId, request.assignedUserId(), householdId);
        TaskResponse task = taskService.assignTask(householdId, taskId, request.assignedUserId());
        return ResponseEntity.ok(task);
    }

    /**
     * Get overdue tasks.
     */
    @GetMapping("/overdue")
    @Operation(summary = "Get overdue tasks", description = "Retrieves all overdue tasks for the household")
    public ResponseEntity<List<TaskResponse>> getOverdueTasks(
            @RequestAttribute("householdId") UUID householdId) {
        
        log.debug("Getting overdue tasks for household: {}", householdId);
        List<TaskResponse> tasks = taskService.getOverdueTasks(householdId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get task statistics.
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get task statistics", description = "Retrieves task statistics for the household")
    public ResponseEntity<TaskService.TaskStatistics> getTaskStatistics(
            @RequestAttribute("householdId") UUID householdId) {
        
        log.debug("Getting task statistics for household: {}", householdId);
        TaskService.TaskStatistics statistics = taskService.getTaskStatistics(householdId);
        return ResponseEntity.ok(statistics);
    }

    // Request DTOs for specific operations
    public record TaskStatusUpdateRequest(TaskStatus status) {}
    public record TaskAssignmentRequest(UUID assignedUserId) {}
}

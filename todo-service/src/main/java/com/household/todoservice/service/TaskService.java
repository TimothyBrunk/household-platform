package com.household.todoservice.service;

import com.household.todoservice.dto.TaskCreateRequest;
import com.household.todoservice.dto.TaskResponse;
import com.household.todoservice.dto.TaskUpdateRequest;
import com.household.todoservice.entity.Category;
import com.household.todoservice.entity.Task;
import com.household.todoservice.entity.TaskPriority;
import com.household.todoservice.entity.TaskStatus;
import com.household.todoservice.exception.ResourceNotFoundException;
import com.household.todoservice.repository.CategoryRepository;
import com.household.todoservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service class for task-related business operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final TaskMapper taskMapper;

    /**
     * Create a new task.
     */
    public TaskResponse createTask(UUID householdId, TaskCreateRequest request, UUID createdByUserId) {
        log.info("Creating task for household: {}, created by user: {}", householdId, createdByUserId);

        // Validate category if provided
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndHouseholdIdAndIsActiveTrue(request.getCategoryId(), householdId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        // Create task entity
        Task task = Task.builder()
                .householdId(householdId)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .dueDate(request.getDueDate())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .assignedUserId(request.getAssignedUserId())
                .createdByUserId(createdByUserId)
                .tags(request.getTags())
                .recurringPattern(request.getRecurringPattern())
                .customFields(request.getCustomFields())
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with ID: {}", savedTask.getId());

        return taskMapper.toResponse(savedTask);
    }

    /**
     * Get a task by ID within a household.
     */
    @Cacheable(value = "tasks", key = "#taskId")
    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID householdId, UUID taskId) {
        log.debug("Getting task: {} for household: {}", taskId, householdId);

        Task task = taskRepository.findByIdAndHouseholdIdAndIsDeletedFalse(taskId, householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        return taskMapper.toResponse(task);
    }

    /**
     * Update an existing task.
     */
    @CacheEvict(value = "tasks", key = "#taskId")
    public TaskResponse updateTask(UUID householdId, UUID taskId, TaskUpdateRequest request) {
        log.info("Updating task: {} for household: {}", taskId, householdId);

        Task task = taskRepository.findByIdAndHouseholdIdAndIsDeletedFalse(taskId, householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Update fields if provided
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getEstimatedDurationMinutes() != null) {
            task.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        }
        if (request.getAssignedUserId() != null) {
            task.setAssignedUserId(request.getAssignedUserId());
        }
        if (request.getTags() != null) {
            task.setTags(request.getTags());
        }
        if (request.getRecurringPattern() != null) {
            task.setRecurringPattern(request.getRecurringPattern());
        }
        if (request.getCustomFields() != null) {
            task.setCustomFields(request.getCustomFields());
        }

        // Update category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndHouseholdIdAndIsActiveTrue(request.getCategoryId(), householdId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            task.setCategory(category);
        }

        Task updatedTask = taskRepository.save(task);
        log.info("Task updated successfully: {}", taskId);

        return taskMapper.toResponse(updatedTask);
    }

    /**
     * Delete a task (soft delete).
     */
    @CacheEvict(value = "tasks", key = "#taskId")
    public void deleteTask(UUID householdId, UUID taskId, UUID deletedByUserId) {
        log.info("Deleting task: {} for household: {}", taskId, householdId);

        Task task = taskRepository.findByIdAndHouseholdIdAndIsDeletedFalse(taskId, householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        task.softDelete(deletedByUserId);
        taskRepository.save(task);
        log.info("Task deleted successfully: {}", taskId);
    }

    /**
     * Get tasks with pagination and filtering.
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(UUID householdId, TaskStatus status, TaskPriority priority,
                                     UUID categoryId, UUID assignedUserId, LocalDateTime dueDateFrom,
                                     LocalDateTime dueDateTo, Pageable pageable) {
        log.debug("Getting tasks for household: {} with filters", householdId);

        Page<Task> tasks = taskRepository.findTasksWithFilters(
                householdId, status, priority, categoryId, assignedUserId, dueDateFrom, dueDateTo, pageable);

        return tasks.map(taskMapper::toResponse);
    }

    /**
     * Search tasks by text in title and description.
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> searchTasks(UUID householdId, String searchTerm, Pageable pageable) {
        log.debug("Searching tasks for household: {} with term: {}", householdId, searchTerm);

        Page<Task> tasks = taskRepository.searchTasks(householdId, searchTerm, pageable);
        return tasks.map(taskMapper::toResponse);
    }

    /**
     * Get tasks assigned to a specific user.
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByUser(UUID householdId, UUID assignedUserId, Pageable pageable) {
        log.debug("Getting tasks assigned to user: {} in household: {}", assignedUserId, householdId);

        Page<Task> tasks = taskRepository.findByHouseholdIdAndAssignedUserIdAndIsDeletedFalse(householdId, assignedUserId, pageable);
        return tasks.map(taskMapper::toResponse);
    }

    /**
     * Update task status.
     */
    @CacheEvict(value = "tasks", key = "#taskId")
    public TaskResponse updateTaskStatus(UUID householdId, UUID taskId, TaskStatus status, UUID updatedByUserId) {
        log.info("Updating task status: {} to {} for household: {}", taskId, status, householdId);

        Task task = taskRepository.findByIdAndHouseholdIdAndIsDeletedFalse(taskId, householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        task.setStatus(status);

        // Set completion details if marking as completed
        if (status == TaskStatus.COMPLETED) {
            task.markAsCompleted(updatedByUserId);
        }

        Task updatedTask = taskRepository.save(task);
        log.info("Task status updated successfully: {} to {}", taskId, status);

        return taskMapper.toResponse(updatedTask);
    }

    /**
     * Assign task to a user.
     */
    @CacheEvict(value = "tasks", key = "#taskId")
    public TaskResponse assignTask(UUID householdId, UUID taskId, UUID assignedUserId) {
        log.info("Assigning task: {} to user: {} in household: {}", taskId, assignedUserId, householdId);

        Task task = taskRepository.findByIdAndHouseholdIdAndIsDeletedFalse(taskId, householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        task.setAssignedUserId(assignedUserId);
        Task updatedTask = taskRepository.save(task);
        log.info("Task assigned successfully: {} to user: {}", taskId, assignedUserId);

        return taskMapper.toResponse(updatedTask);
    }

    /**
     * Get overdue tasks.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getOverdueTasks(UUID householdId) {
        log.debug("Getting overdue tasks for household: {}", householdId);

        List<Task> overdueTasks = taskRepository.findOverdueTasks(householdId, LocalDateTime.now());
        return overdueTasks.stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    /**
     * Get task statistics for a household.
     */
    @Transactional(readOnly = true)
    public TaskStatistics getTaskStatistics(UUID householdId) {
        log.debug("Getting task statistics for household: {}", householdId);

        long totalTasks = taskRepository.countByHouseholdIdAndIsDeletedFalse(householdId);
        long pendingTasks = taskRepository.countByHouseholdIdAndStatusAndIsDeletedFalse(householdId, TaskStatus.PENDING);
        long inProgressTasks = taskRepository.countByHouseholdIdAndStatusAndIsDeletedFalse(householdId, TaskStatus.IN_PROGRESS);
        long completedTasks = taskRepository.countByHouseholdIdAndStatusAndIsDeletedFalse(householdId, TaskStatus.COMPLETED);

        List<Task> overdueTasks = taskRepository.findOverdueTasks(householdId, LocalDateTime.now());
        long overdueTasksCount = overdueTasks.size();

        return TaskStatistics.builder()
                .totalTasks(totalTasks)
                .pendingTasks(pendingTasks)
                .inProgressTasks(inProgressTasks)
                .completedTasks(completedTasks)
                .overdueTasks(overdueTasksCount)
                .completionRate(totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0.0)
                .build();
    }

    /**
     * Check if task exists and belongs to household.
     */
    @Transactional(readOnly = true)
    public boolean taskExists(UUID householdId, UUID taskId) {
        return taskRepository.existsByIdAndHouseholdIdAndIsDeletedFalse(taskId, householdId);
    }

    /**
     * Task statistics DTO.
     */
    public record TaskStatistics(
            long totalTasks,
            long pendingTasks,
            long inProgressTasks,
            long completedTasks,
            long overdueTasks,
            double completionRate
    ) {
        public static TaskStatisticsBuilder builder() {
            return new TaskStatisticsBuilder();
        }

        public static class TaskStatisticsBuilder {
            private long totalTasks;
            private long pendingTasks;
            private long inProgressTasks;
            private long completedTasks;
            private long overdueTasks;
            private double completionRate;

            public TaskStatisticsBuilder totalTasks(long totalTasks) {
                this.totalTasks = totalTasks;
                return this;
            }

            public TaskStatisticsBuilder pendingTasks(long pendingTasks) {
                this.pendingTasks = pendingTasks;
                return this;
            }

            public TaskStatisticsBuilder inProgressTasks(long inProgressTasks) {
                this.inProgressTasks = inProgressTasks;
                return this;
            }

            public TaskStatisticsBuilder completedTasks(long completedTasks) {
                this.completedTasks = completedTasks;
                return this;
            }

            public TaskStatisticsBuilder overdueTasks(long overdueTasks) {
                this.overdueTasks = overdueTasks;
                return this;
            }

            public TaskStatisticsBuilder completionRate(double completionRate) {
                this.completionRate = completionRate;
                return this;
            }

            public TaskStatistics build() {
                return new TaskStatistics(totalTasks, pendingTasks, inProgressTasks, completedTasks, overdueTasks, completionRate);
            }
        }
    }
}

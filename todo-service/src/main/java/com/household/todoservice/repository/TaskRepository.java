package com.household.todoservice.repository;

import com.household.todoservice.entity.Task;
import com.household.todoservice.entity.TaskPriority;
import com.household.todoservice.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Task entity operations.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    // Basic household queries
    Page<Task> findByHouseholdIdAndIsDeletedFalse(UUID householdId, Pageable pageable);
    
    List<Task> findByHouseholdIdAndIsDeletedFalse(UUID householdId);
    
    // Status-based queries
    List<Task> findByHouseholdIdAndStatusAndIsDeletedFalse(UUID householdId, TaskStatus status);
    
    Page<Task> findByHouseholdIdAndStatusAndIsDeletedFalse(UUID householdId, TaskStatus status, Pageable pageable);
    
    // Priority-based queries
    List<Task> findByHouseholdIdAndPriorityAndIsDeletedFalse(UUID householdId, TaskPriority priority);
    
    // User assignment queries
    List<Task> findByHouseholdIdAndAssignedUserIdAndIsDeletedFalse(UUID householdId, UUID assignedUserId);
    
    Page<Task> findByHouseholdIdAndAssignedUserIdAndIsDeletedFalse(UUID householdId, UUID assignedUserId, Pageable pageable);
    
    // Category-based queries
    List<Task> findByHouseholdIdAndCategoryIdAndIsDeletedFalse(UUID householdId, UUID categoryId);
    
    // Due date queries
    List<Task> findByHouseholdIdAndDueDateBeforeAndStatusInAndIsDeletedFalse(
        UUID householdId, 
        LocalDateTime date, 
        List<TaskStatus> statuses
    );
    
    // Overdue tasks
    @Query("SELECT t FROM Task t WHERE t.householdId = :householdId " +
           "AND t.dueDate < :now " +
           "AND t.status IN ('PENDING', 'IN_PROGRESS') " +
           "AND t.isDeleted = false")
    List<Task> findOverdueTasks(@Param("householdId") UUID householdId, @Param("now") LocalDateTime now);
    
    // Search by title and description
    @Query("SELECT t FROM Task t WHERE t.householdId = :householdId " +
           "AND t.isDeleted = false " +
           "AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Task> searchTasks(@Param("householdId") UUID householdId, 
                          @Param("searchTerm") String searchTerm, 
                          Pageable pageable);
    
    // Complex search with multiple filters
    @Query("SELECT t FROM Task t WHERE t.householdId = :householdId " +
           "AND t.isDeleted = false " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
           "AND (:assignedUserId IS NULL OR t.assignedUserId = :assignedUserId) " +
           "AND (:dueDateFrom IS NULL OR t.dueDate >= :dueDateFrom) " +
           "AND (:dueDateTo IS NULL OR t.dueDate <= :dueDateTo)")
    Page<Task> findTasksWithFilters(
        @Param("householdId") UUID householdId,
        @Param("status") TaskStatus status,
        @Param("priority") TaskPriority priority,
        @Param("categoryId") UUID categoryId,
        @Param("assignedUserId") UUID assignedUserId,
        @Param("dueDateFrom") LocalDateTime dueDateFrom,
        @Param("dueDateTo") LocalDateTime dueDateTo,
        Pageable pageable
    );
    
    // Count queries for statistics
    long countByHouseholdIdAndIsDeletedFalse(UUID householdId);
    
    long countByHouseholdIdAndStatusAndIsDeletedFalse(UUID householdId, TaskStatus status);
    
    long countByHouseholdIdAndAssignedUserIdAndIsDeletedFalse(UUID householdId, UUID assignedUserId);
    
    // Check if task exists and belongs to household
    boolean existsByIdAndHouseholdIdAndIsDeletedFalse(UUID taskId, UUID householdId);
}

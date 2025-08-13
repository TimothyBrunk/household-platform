package com.household.todoservice.repository;

import com.household.todoservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Category entity operations.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Find categories by household
    List<Category> findByHouseholdIdAndIsActiveTrueOrderBySortOrderAsc(UUID householdId);
    
    List<Category> findByHouseholdIdOrderBySortOrderAsc(UUID householdId);
    
    // Find by name within household (for uniqueness validation)
    Optional<Category> findByHouseholdIdAndNameAndIsActiveTrue(UUID householdId, String name);
    
    // Check if name exists in household
    boolean existsByHouseholdIdAndNameAndIsActiveTrue(UUID householdId, String name);
    
    // Count categories in household
    long countByHouseholdIdAndIsActiveTrue(UUID householdId);
    
    // Find categories with task counts
    @Query("SELECT c, COUNT(t) as taskCount FROM Category c " +
           "LEFT JOIN c.tasks t ON t.isDeleted = false " +
           "WHERE c.householdId = :householdId AND c.isActive = true " +
           "GROUP BY c ORDER BY c.sortOrder ASC")
    List<Object[]> findCategoriesWithTaskCounts(@Param("householdId") UUID householdId);
    
    // Check if category exists and belongs to household
    boolean existsByIdAndHouseholdIdAndIsActiveTrue(UUID categoryId, UUID householdId);
}

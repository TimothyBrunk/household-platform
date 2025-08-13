package com.household.todoservice.service;

import com.household.todoservice.dto.CategoryResponse;
import com.household.todoservice.entity.Category;
import com.household.todoservice.exception.ResourceNotFoundException;
import com.household.todoservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service class for category-related business operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Create a new category.
     */
    public CategoryResponse createCategory(UUID householdId, String name, String description, 
                                         String color, String icon, Integer sortOrder) {
        log.info("Creating category for household: {} with name: {}", householdId, name);

        // Check if category name already exists in household
        if (categoryRepository.existsByHouseholdIdAndNameAndIsActiveTrue(householdId, name)) {
            throw new IllegalArgumentException("Category with name '" + name + "' already exists in this household");
        }

        Category category = Category.builder()
                .householdId(householdId)
                .name(name)
                .description(description)
                .color(color)
                .icon(icon)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isActive(true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getId());

        return categoryMapper.toResponse(savedCategory);
    }

    /**
     * Get a category by ID within a household.
     */
    @Cacheable(value = "categories", key = "#categoryId")
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID householdId, UUID categoryId) {
        log.debug("Getting category: {} for household: {}", categoryId, householdId);

        Category category = categoryRepository.findByIdAndHouseholdIdAndIsActiveTrue(categoryId, householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        return categoryMapper.toResponse(category);
    }

    /**
     * Update an existing category.
     */
    @CacheEvict(value = "categories", key = "#categoryId")
    public CategoryResponse updateCategory(UUID householdId, UUID categoryId, String name, 
                                         String description, String color, String icon, Integer sortOrder) {
        log.info("Updating category: {} for household: {}", categoryId, householdId);

        Category category = categoryRepository.findByIdAndHouseholdIdAndIsActiveTrue(categoryId, householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // Check if new name conflicts with existing category (excluding current category)
        if (name != null && !name.equals(category.getName())) {
            if (categoryRepository.existsByHouseholdIdAndNameAndIsActiveTrue(householdId, name)) {
                throw new IllegalArgumentException("Category with name '" + name + "' already exists in this household");
            }
            category.setName(name);
        }

        if (description != null) {
            category.setDescription(description);
        }
        if (color != null) {
            category.setColor(color);
        }
        if (icon != null) {
            category.setIcon(icon);
        }
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully: {}", categoryId);

        return categoryMapper.toResponse(updatedCategory);
    }

    /**
     * Delete a category (soft delete).
     */
    @CacheEvict(value = "categories", key = "#categoryId")
    public void deleteCategory(UUID householdId, UUID categoryId) {
        log.info("Deleting category: {} for household: {}", categoryId, householdId);

        Category category = categoryRepository.findByIdAndHouseholdIdAndIsActiveTrue(categoryId, householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // Check if category has tasks
        if (!category.getTasks().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with existing tasks. Please reassign or delete tasks first.");
        }

        category.setIsActive(false);
        categoryRepository.save(category);
        log.info("Category deleted successfully: {}", categoryId);
    }

    /**
     * Get all categories for a household.
     */
    @Cacheable(value = "categories", key = "'household:' + #householdId")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID householdId) {
        log.debug("Getting categories for household: {}", householdId);

        List<Category> categories = categoryRepository.findByHouseholdIdAndIsActiveTrueOrderBySortOrderAsc(householdId);
        return categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    /**
     * Get categories with task counts.
     */
    @Transactional(readOnly = true)
    public List<CategoryWithTaskCount> getCategoriesWithTaskCounts(UUID householdId) {
        log.debug("Getting categories with task counts for household: {}", householdId);

        List<Object[]> results = categoryRepository.findCategoriesWithTaskCounts(householdId);
        return results.stream()
                .map(result -> {
                    Category category = (Category) result[0];
                    Long taskCount = (Long) result[1];
                    return new CategoryWithTaskCount(categoryMapper.toResponse(category), taskCount);
                })
                .toList();
    }

    /**
     * Check if category exists and belongs to household.
     */
    @Transactional(readOnly = true)
    public boolean categoryExists(UUID householdId, UUID categoryId) {
        return categoryRepository.existsByIdAndHouseholdIdAndIsActiveTrue(categoryId, householdId);
    }

    /**
     * Get category count for a household.
     */
    @Transactional(readOnly = true)
    public long getCategoryCount(UUID householdId) {
        return categoryRepository.countByHouseholdIdAndIsActiveTrue(householdId);
    }

    /**
     * Category with task count DTO.
     */
    public record CategoryWithTaskCount(CategoryResponse category, Long taskCount) {}
}

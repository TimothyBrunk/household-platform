package com.household.todoservice.controller;

import com.household.todoservice.dto.CategoryResponse;
import com.household.todoservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for category-related operations.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categories", description = "Category management operations")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Create a new category.
     */
    @PostMapping
    @Operation(summary = "Create a new category", description = "Creates a new category in the specified household")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestAttribute("householdId") UUID householdId,
            @Valid @RequestBody CategoryCreateRequest request) {
        
        log.info("Creating category for household: {} with name: {}", householdId, request.name());
        
        CategoryResponse category = categoryService.createCategory(
                householdId, 
                request.name(), 
                request.description(), 
                request.color(), 
                request.icon(), 
                request.sortOrder()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    /**
     * Get a category by ID.
     */
    @GetMapping("/{categoryId}")
    @Operation(summary = "Get category by ID", description = "Retrieves a specific category by its ID")
    public ResponseEntity<CategoryResponse> getCategory(
            @RequestAttribute("householdId") UUID householdId,
            @PathVariable UUID categoryId) {
        
        log.debug("Getting category: {} for household: {}", categoryId, householdId);
        CategoryResponse category = categoryService.getCategory(householdId, categoryId);
        return ResponseEntity.ok(category);
    }

    /**
     * Update a category.
     */
    @PutMapping("/{categoryId}")
    @Operation(summary = "Update category", description = "Updates an existing category")
    public ResponseEntity<CategoryResponse> updateCategory(
            @RequestAttribute("householdId") UUID householdId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest request) {
        
        log.info("Updating category: {} for household: {}", categoryId, householdId);
        
        CategoryResponse category = categoryService.updateCategory(
                householdId, 
                categoryId, 
                request.name(), 
                request.description(), 
                request.color(), 
                request.icon(), 
                request.sortOrder()
        );
        
        return ResponseEntity.ok(category);
    }

    /**
     * Delete a category.
     */
    @DeleteMapping("/{categoryId}")
    @Operation(summary = "Delete category", description = "Soft deletes a category")
    public ResponseEntity<Void> deleteCategory(
            @RequestAttribute("householdId") UUID householdId,
            @PathVariable UUID categoryId) {
        
        log.info("Deleting category: {} for household: {}", categoryId, householdId);
        categoryService.deleteCategory(householdId, categoryId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all categories for a household.
     */
    @GetMapping
    @Operation(summary = "Get categories", description = "Retrieves all categories for the household")
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @RequestAttribute("householdId") UUID householdId) {
        
        log.debug("Getting categories for household: {}", householdId);
        List<CategoryResponse> categories = categoryService.getCategories(householdId);
        return ResponseEntity.ok(categories);
    }

    /**
     * Get categories with task counts.
     */
    @GetMapping("/with-task-counts")
    @Operation(summary = "Get categories with task counts", description = "Retrieves categories with associated task counts")
    public ResponseEntity<List<CategoryService.CategoryWithTaskCount>> getCategoriesWithTaskCounts(
            @RequestAttribute("householdId") UUID householdId) {
        
        log.debug("Getting categories with task counts for household: {}", householdId);
        List<CategoryService.CategoryWithTaskCount> categories = categoryService.getCategoriesWithTaskCounts(householdId);
        return ResponseEntity.ok(categories);
    }

    /**
     * Get category count for a household.
     */
    @GetMapping("/count")
    @Operation(summary = "Get category count", description = "Retrieves the total number of categories for the household")
    public ResponseEntity<CategoryCountResponse> getCategoryCount(
            @RequestAttribute("householdId") UUID householdId) {
        
        log.debug("Getting category count for household: {}", householdId);
        long count = categoryService.getCategoryCount(householdId);
        return ResponseEntity.ok(new CategoryCountResponse(count));
    }

    // Request DTOs
    public record CategoryCreateRequest(
            @NotBlank(message = "Category name is required")
            String name,
            String description,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color code")
            String color,
            String icon,
            Integer sortOrder
    ) {}

    public record CategoryUpdateRequest(
            String name,
            String description,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color code")
            String color,
            String icon,
            Integer sortOrder
    ) {}

    public record CategoryCountResponse(long count) {}
}

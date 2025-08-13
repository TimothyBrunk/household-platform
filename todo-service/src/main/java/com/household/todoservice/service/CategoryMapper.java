package com.household.todoservice.service;

import com.household.todoservice.dto.CategoryResponse;
import com.household.todoservice.entity.Category;
import org.mapstruct.Mapper;

/**
 * Mapper interface for Category entity and DTO conversions.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);
}

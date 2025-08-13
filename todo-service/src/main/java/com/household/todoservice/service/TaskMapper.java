package com.household.todoservice.service;

import com.household.todoservice.dto.CategoryResponse;
import com.household.todoservice.dto.TaskResponse;
import com.household.todoservice.dto.UserResponse;
import com.household.todoservice.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

/**
 * Mapper interface for Task entity and DTO conversions.
 */
@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class})
public interface TaskMapper {

    @Mapping(target = "category", source = "category")
    @Mapping(target = "assignedUser", source = "assignedUserId", qualifiedByName = "userIdToUserResponse")
    @Mapping(target = "createdByUser", source = "createdByUserId", qualifiedByName = "userIdToUserResponse")
    @Mapping(target = "completedByUser", source = "completedByUserId", qualifiedByName = "userIdToUserResponse")
    @Mapping(target = "overdue", expression = "java(task.isOverdue())")
    @Mapping(target = "completed", expression = "java(task.isCompleted())")
    TaskResponse toResponse(Task task);

    @Named("userIdToUserResponse")
    default UserResponse userIdToUserResponse(UUID userId) {
        if (userId == null) {
            return null;
        }
        // In a real implementation, this would fetch user details from user service
        // For now, return a basic user response
        return UserResponse.builder()
                .id(userId)
                .name("User " + userId.toString().substring(0, 8))
                .email("user@" + userId.toString().substring(0, 8) + ".com")
                .build();
    }
}

package com.household.todoservice.entity;

/**
 * Enum representing the possible statuses of a task.
 */
public enum TaskStatus {
    PENDING("pending", "Task is waiting to be started"),
    IN_PROGRESS("in_progress", "Task is currently being worked on"),
    COMPLETED("completed", "Task has been finished"),
    CANCELLED("cancelled", "Task was cancelled");

    private final String value;
    private final String description;

    TaskStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return value;
    }
}

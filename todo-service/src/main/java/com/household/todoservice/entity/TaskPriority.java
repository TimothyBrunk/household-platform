package com.household.todoservice.entity;

/**
 * Enum representing the possible priority levels of a task.
 */
public enum TaskPriority {
    LOW("low", "Low priority", 1),
    MEDIUM("medium", "Medium priority", 2),
    HIGH("high", "High priority", 3),
    URGENT("urgent", "Urgent priority", 4);

    private final String value;
    private final String description;
    private final int level;

    TaskPriority(String value, String description, int level) {
        this.value = value;
        this.description = description;
        this.level = level;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return value;
    }
}

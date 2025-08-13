# Database Schema - To-Do Service

## Overview

This document defines the complete database schema for the To-Do Service, including table structures, relationships, indexes, and data constraints. The schema is designed for PostgreSQL and follows best practices for microservices architecture.

## Database Design Principles

- **Multi-tenancy**: Household-based data isolation
- **Audit Trail**: Comprehensive tracking of data changes
- **Soft Deletes**: Data preservation with logical deletion
- **Performance**: Optimized indexes for common queries
- **Scalability**: Support for future horizontal scaling

## Schema Definition

### 1. Households Table

```sql
CREATE TABLE households (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    settings JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_households_active ON households(is_active);
CREATE INDEX idx_households_created_at ON households(created_at);

-- Triggers
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_households_updated_at 
    BEFORE UPDATE ON households 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 2. Categories Table

```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    color VARCHAR(7), -- Hex color code
    icon VARCHAR(50),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT unique_category_name_per_household UNIQUE(household_id, name),
    CONSTRAINT valid_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

-- Indexes
CREATE INDEX idx_categories_household_id ON categories(household_id);
CREATE INDEX idx_categories_active ON categories(is_active);
CREATE INDEX idx_categories_sort_order ON categories(sort_order);
CREATE INDEX idx_categories_created_at ON categories(created_at);

-- Triggers
CREATE TRIGGER update_categories_updated_at 
    BEFORE UPDATE ON categories 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 3. Tasks Table

```sql
CREATE TYPE task_status AS ENUM ('pending', 'in_progress', 'completed', 'cancelled');
CREATE TYPE task_priority AS ENUM ('low', 'medium', 'high', 'urgent');

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    
    -- Task Details
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status task_status DEFAULT 'pending',
    priority task_priority DEFAULT 'medium',
    
    -- Scheduling
    due_date TIMESTAMP WITH TIME ZONE,
    estimated_duration_minutes INTEGER,
    recurring_pattern JSONB, -- For recurring tasks
    
    -- Assignment
    assigned_user_id UUID, -- References external user service
    created_by_user_id UUID NOT NULL, -- References external user service
    
    -- Completion Tracking
    completed_at TIMESTAMP WITH TIME ZONE,
    completed_by_user_id UUID, -- References external user service
    
    -- Metadata
    tags TEXT[], -- Array of tags
    attachments JSONB, -- Array of attachment metadata
    custom_fields JSONB, -- Flexible custom data
    
    -- Audit
    is_deleted BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_due_date CHECK (due_date IS NULL OR due_date > created_at),
    CONSTRAINT valid_completion_date CHECK (completed_at IS NULL OR completed_at >= created_at),
    CONSTRAINT valid_estimated_duration CHECK (estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0)
);

-- Indexes
CREATE INDEX idx_tasks_household_id ON tasks(household_id);
CREATE INDEX idx_tasks_category_id ON tasks(category_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_assigned_user_id ON tasks(assigned_user_id);
CREATE INDEX idx_tasks_created_by_user_id ON tasks(created_by_user_id);
CREATE INDEX idx_tasks_completed_at ON tasks(completed_at);
CREATE INDEX idx_tasks_created_at ON tasks(created_at);
CREATE INDEX idx_tasks_updated_at ON tasks(updated_at);
CREATE INDEX idx_tasks_deleted ON tasks(is_deleted);

-- Composite indexes for common queries
CREATE INDEX idx_tasks_household_status ON tasks(household_id, status);
CREATE INDEX idx_tasks_household_due_date ON tasks(household_id, due_date);
CREATE INDEX idx_tasks_household_assigned_user ON tasks(household_id, assigned_user_id);
CREATE INDEX idx_tasks_household_priority ON tasks(household_id, priority);

-- Full-text search index
CREATE INDEX idx_tasks_search ON tasks USING gin(to_tsvector('english', title || ' ' || COALESCE(description, '')));

-- Triggers
CREATE TRIGGER update_tasks_updated_at 
    BEFORE UPDATE ON tasks 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 4. Task Comments Table

```sql
CREATE TABLE task_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL, -- References external user service
    content TEXT NOT NULL,
    is_internal BOOLEAN DEFAULT false, -- Internal notes vs user comments
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_task_comments_task_id ON task_comments(task_id);
CREATE INDEX idx_task_comments_user_id ON task_comments(user_id);
CREATE INDEX idx_task_comments_created_at ON task_comments(created_at);

-- Triggers
CREATE TRIGGER update_task_comments_updated_at 
    BEFORE UPDATE ON task_comments 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 5. Task History Table

```sql
CREATE TABLE task_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL, -- References external user service
    action VARCHAR(50) NOT NULL, -- created, updated, completed, assigned, etc.
    field_name VARCHAR(50), -- For field-specific changes
    old_value TEXT,
    new_value TEXT,
    metadata JSONB, -- Additional context
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_task_history_task_id ON task_history(task_id);
CREATE INDEX idx_task_history_user_id ON task_history(user_id);
CREATE INDEX idx_task_history_action ON task_history(action);
CREATE INDEX idx_task_history_created_at ON task_history(created_at);
```

### 6. Task Dependencies Table

```sql
CREATE TABLE task_dependencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dependent_task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    prerequisite_task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    dependency_type VARCHAR(20) DEFAULT 'blocks', -- blocks, requires, suggests
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT unique_task_dependency UNIQUE(dependent_task_id, prerequisite_task_id),
    CONSTRAINT no_self_dependency CHECK (dependent_task_id != prerequisite_task_id)
);

-- Indexes
CREATE INDEX idx_task_dependencies_dependent ON task_dependencies(dependent_task_id);
CREATE INDEX idx_task_dependencies_prerequisite ON task_dependencies(prerequisite_task_id);
```

## Views

### 1. Active Tasks View

```sql
CREATE VIEW active_tasks AS
SELECT 
    t.*,
    c.name as category_name,
    c.color as category_color
FROM tasks t
LEFT JOIN categories c ON t.category_id = c.id
WHERE t.is_deleted = false
    AND t.status IN ('pending', 'in_progress');
```

### 2. Overdue Tasks View

```sql
CREATE VIEW overdue_tasks AS
SELECT 
    t.*,
    c.name as category_name,
    c.color as category_color,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - t.due_date))/86400 as days_overdue
FROM tasks t
LEFT JOIN categories c ON t.category_id = c.id
WHERE t.is_deleted = false
    AND t.status IN ('pending', 'in_progress')
    AND t.due_date < CURRENT_TIMESTAMP;
```

### 3. Task Statistics View

```sql
CREATE VIEW task_statistics AS
SELECT 
    household_id,
    COUNT(*) as total_tasks,
    COUNT(*) FILTER (WHERE status = 'pending') as pending_tasks,
    COUNT(*) FILTER (WHERE status = 'in_progress') as in_progress_tasks,
    COUNT(*) FILTER (WHERE status = 'completed') as completed_tasks,
    COUNT(*) FILTER (WHERE due_date < CURRENT_TIMESTAMP AND status IN ('pending', 'in_progress')) as overdue_tasks,
    AVG(EXTRACT(EPOCH FROM (completed_at - created_at))/86400) FILTER (WHERE status = 'completed') as avg_completion_days
FROM tasks
WHERE is_deleted = false
GROUP BY household_id;
```

## Functions

### 1. Soft Delete Function

```sql
CREATE OR REPLACE FUNCTION soft_delete_task(
    task_uuid UUID,
    user_uuid UUID
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE tasks 
    SET is_deleted = true,
        deleted_at = CURRENT_TIMESTAMP,
        deleted_by_user_id = user_uuid
    WHERE id = task_uuid AND is_deleted = false;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;
```

### 2. Complete Task Function

```sql
CREATE OR REPLACE FUNCTION complete_task(
    task_uuid UUID,
    user_uuid UUID
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE tasks 
    SET status = 'completed',
        completed_at = CURRENT_TIMESTAMP,
        completed_by_user_id = user_uuid
    WHERE id = task_uuid 
        AND status IN ('pending', 'in_progress')
        AND is_deleted = false;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;
```

### 3. Search Tasks Function

```sql
CREATE OR REPLACE FUNCTION search_tasks(
    household_uuid UUID,
    search_term TEXT,
    status_filter task_status[] DEFAULT NULL,
    priority_filter task_priority[] DEFAULT NULL,
    category_filter UUID[] DEFAULT NULL,
    assigned_user_filter UUID[] DEFAULT NULL,
    limit_count INTEGER DEFAULT 50,
    offset_count INTEGER DEFAULT 0
)
RETURNS TABLE (
    id UUID,
    title VARCHAR,
    description TEXT,
    status task_status,
    priority task_priority,
    due_date TIMESTAMP WITH TIME ZONE,
    category_name VARCHAR,
    category_color VARCHAR,
    created_at TIMESTAMP WITH TIME ZONE,
    rank FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        t.id,
        t.title,
        t.description,
        t.status,
        t.priority,
        t.due_date,
        c.name as category_name,
        c.color as category_color,
        t.created_at,
        ts_rank(to_tsvector('english', t.title || ' ' || COALESCE(t.description, '')), plainto_tsquery('english', search_term)) as rank
    FROM tasks t
    LEFT JOIN categories c ON t.category_id = c.id
    WHERE t.household_id = household_uuid
        AND t.is_deleted = false
        AND to_tsvector('english', t.title || ' ' || COALESCE(t.description, '')) @@ plainto_tsquery('english', search_term)
        AND (status_filter IS NULL OR t.status = ANY(status_filter))
        AND (priority_filter IS NULL OR t.priority = ANY(priority_filter))
        AND (category_filter IS NULL OR t.category_id = ANY(category_filter))
        AND (assigned_user_filter IS NULL OR t.assigned_user_id = ANY(assigned_user_filter))
    ORDER BY rank DESC, t.created_at DESC
    LIMIT limit_count OFFSET offset_count;
END;
$$ LANGUAGE plpgsql;
```

## Migration Strategy

### Version 1.0 - Initial Schema

```sql
-- Migration: 001_initial_schema.sql
BEGIN;

-- Create households table
CREATE TABLE households (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    settings JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    color VARCHAR(7),
    icon VARCHAR(50),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_category_name_per_household UNIQUE(household_id, name),
    CONSTRAINT valid_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

-- Create tasks table
CREATE TYPE task_status AS ENUM ('pending', 'in_progress', 'completed', 'cancelled');
CREATE TYPE task_priority AS ENUM ('low', 'medium', 'high', 'urgent');

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status task_status DEFAULT 'pending',
    priority task_priority DEFAULT 'medium',
    due_date TIMESTAMP WITH TIME ZONE,
    estimated_duration_minutes INTEGER,
    recurring_pattern JSONB,
    assigned_user_id UUID,
    created_by_user_id UUID NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    completed_by_user_id UUID,
    tags TEXT[],
    attachments JSONB,
    custom_fields JSONB,
    is_deleted BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_due_date CHECK (due_date IS NULL OR due_date > created_at),
    CONSTRAINT valid_completion_date CHECK (completed_at IS NULL OR completed_at >= created_at),
    CONSTRAINT valid_estimated_duration CHECK (estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0)
);

-- Create indexes
CREATE INDEX idx_households_active ON households(is_active);
CREATE INDEX idx_categories_household_id ON categories(household_id);
CREATE INDEX idx_categories_active ON categories(is_active);
CREATE INDEX idx_tasks_household_id ON tasks(household_id);
CREATE INDEX idx_tasks_category_id ON tasks(category_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_assigned_user_id ON tasks(assigned_user_id);
CREATE INDEX idx_tasks_household_status ON tasks(household_id, status);
CREATE INDEX idx_tasks_household_due_date ON tasks(household_id, due_date);

COMMIT;
```

## Data Seeding

### Default Categories

```sql
-- Insert default categories for new households
INSERT INTO categories (household_id, name, description, color, sort_order) VALUES
('00000000-0000-0000-0000-000000000001', 'Kitchen', 'Kitchen-related tasks', '#FF6B6B', 1),
('00000000-0000-0000-0000-000000000001', 'Bathroom', 'Bathroom cleaning and maintenance', '#4ECDC4', 2),
('00000000-0000-0000-0000-000000000001', 'Living Room', 'Living room tasks', '#45B7D1', 3),
('00000000-0000-0000-0000-000000000001', 'Bedroom', 'Bedroom organization and cleaning', '#96CEB4', 4),
('00000000-0000-0000-0000-000000000001', 'Outdoor', 'Outdoor maintenance and yard work', '#FFEAA7', 5),
('00000000-0000-0000-0000-000000000001', 'Laundry', 'Laundry and clothing care', '#DDA0DD', 6),
('00000000-0000-0000-0000-000000000001', 'Shopping', 'Grocery and household shopping', '#98D8C8', 7),
('00000000-0000-0000-0000-000000000001', 'Maintenance', 'Home maintenance and repairs', '#F7DC6F', 8);
```

## Performance Considerations

### Query Optimization

1. **Use appropriate indexes** for common query patterns
2. **Implement pagination** for large result sets
3. **Use materialized views** for complex aggregations
4. **Optimize JSONB queries** with GIN indexes
5. **Consider partitioning** for large tables by household_id

### Monitoring

1. **Track query performance** with pg_stat_statements
2. **Monitor index usage** with pg_stat_user_indexes
3. **Set up alerts** for slow queries
4. **Regular VACUUM and ANALYZE** operations

### Backup Strategy

1. **Daily full backups** with point-in-time recovery
2. **WAL archiving** for continuous backup
3. **Cross-region replication** for disaster recovery
4. **Regular backup testing** and restoration drills

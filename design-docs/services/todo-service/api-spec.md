# API Specification - To-Do Service

## Overview

This document defines the RESTful API specification for the To-Do Service. The API follows REST principles and provides comprehensive task and category management functionality for household applications.

## Base Information

- **Base URL**: `https://api.household.com/todo-service`
- **API Version**: `v1`
- **Content Type**: `application/json`
- **Authentication**: Bearer Token (JWT)
- **Rate Limiting**: 1000 requests per hour per user

## Authentication

All API endpoints require authentication via JWT Bearer token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

The JWT token must contain:
- `user_id`: User identifier
- `household_id`: Household identifier
- `exp`: Expiration timestamp
- `iat`: Issued at timestamp

## Common Response Format

### Success Response
```json
{
  "success": true,
  "data": {
    // Response data
  },
  "meta": {
    "timestamp": "2024-01-10T10:00:00Z",
    "request_id": "req_123456789"
  }
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": [
      {
        "field": "title",
        "message": "Title is required"
      }
    ]
  },
  "meta": {
    "timestamp": "2024-01-10T10:00:00Z",
    "request_id": "req_123456789"
  }
}
```

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `UNAUTHORIZED` | 401 | Invalid or missing authentication |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `VALIDATION_ERROR` | 422 | Invalid input data |
| `CONFLICT` | 409 | Resource conflict |
| `RATE_LIMIT_EXCEEDED` | 429 | Rate limit exceeded |
| `INTERNAL_ERROR` | 500 | Internal server error |

## Tasks API

### 1. List Tasks

**Endpoint**: `GET /api/v1/tasks`

**Query Parameters**:
- `page` (integer, optional): Page number (default: 1)
- `limit` (integer, optional): Items per page (default: 20, max: 100)
- `status` (string, optional): Filter by status (`pending`, `in_progress`, `completed`, `cancelled`)
- `priority` (string, optional): Filter by priority (`low`, `medium`, `high`, `urgent`)
- `category_id` (UUID, optional): Filter by category
- `assigned_user_id` (UUID, optional): Filter by assigned user
- `due_date_from` (ISO 8601, optional): Filter tasks due after this date
- `due_date_to` (ISO 8601, optional): Filter tasks due before this date
- `search` (string, optional): Search in title and description
- `sort_by` (string, optional): Sort field (`created_at`, `due_date`, `priority`, `title`)
- `sort_order` (string, optional): Sort order (`asc`, `desc`, default: `desc`)

**Response**:
```json
{
  "success": true,
  "data": {
    "tasks": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "title": "Clean kitchen",
        "description": "Wash dishes and wipe counters",
        "status": "pending",
        "priority": "medium",
        "due_date": "2024-01-15T18:00:00Z",
        "estimated_duration_minutes": 30,
        "category": {
          "id": "550e8400-e29b-41d4-a716-446655440001",
          "name": "Kitchen",
          "color": "#FF6B6B"
        },
        "assigned_user": {
          "id": "550e8400-e29b-41d4-a716-446655440002",
          "name": "John Doe"
        },
        "tags": ["cleaning", "kitchen"],
        "created_at": "2024-01-10T10:00:00Z",
        "updated_at": "2024-01-10T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 45,
      "total_pages": 3,
      "has_next": true,
      "has_prev": false
    }
  }
}
```

### 2. Create Task

**Endpoint**: `POST /api/v1/tasks`

**Request Body**:
```json
{
  "title": "Clean kitchen",
  "description": "Wash dishes and wipe counters",
  "category_id": "550e8400-e29b-41d4-a716-446655440001",
  "priority": "medium",
  "due_date": "2024-01-15T18:00:00Z",
  "estimated_duration_minutes": 30,
  "assigned_user_id": "550e8400-e29b-41d4-a716-446655440002",
  "tags": ["cleaning", "kitchen"],
  "recurring_pattern": {
    "type": "weekly",
    "interval": 1,
    "days_of_week": [1, 3, 5]
  }
}
```

**Validation Rules**:
- `title`: Required, max 255 characters
- `description`: Optional, max 1000 characters
- `category_id`: Optional, must exist in household
- `priority`: Optional, enum values
- `due_date`: Optional, must be future date
- `estimated_duration_minutes`: Optional, positive integer
- `assigned_user_id`: Optional, must be household member
- `tags`: Optional, array of strings
- `recurring_pattern`: Optional, valid recurring pattern

**Response**:
```json
{
  "success": true,
  "data": {
    "task": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Clean kitchen",
      "description": "Wash dishes and wipe counters",
      "status": "pending",
      "priority": "medium",
      "due_date": "2024-01-15T18:00:00Z",
      "estimated_duration_minutes": 30,
      "category": {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "name": "Kitchen",
        "color": "#FF6B6B"
      },
      "assigned_user": {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "name": "John Doe"
      },
      "tags": ["cleaning", "kitchen"],
      "created_at": "2024-01-10T10:00:00Z",
      "updated_at": "2024-01-10T10:00:00Z"
    }
  }
}
```

### 3. Get Task

**Endpoint**: `GET /api/v1/tasks/{task_id}`

**Response**:
```json
{
  "success": true,
  "data": {
    "task": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Clean kitchen",
      "description": "Wash dishes and wipe counters",
      "status": "pending",
      "priority": "medium",
      "due_date": "2024-01-15T18:00:00Z",
      "estimated_duration_minutes": 30,
      "category": {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "name": "Kitchen",
        "color": "#FF6B6B"
      },
      "assigned_user": {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "name": "John Doe"
      },
      "tags": ["cleaning", "kitchen"],
      "attachments": [],
      "custom_fields": {},
      "created_at": "2024-01-10T10:00:00Z",
      "updated_at": "2024-01-10T10:00:00Z"
    }
  }
}
```

### 4. Update Task

**Endpoint**: `PUT /api/v1/tasks/{task_id}`

**Request Body**: Same as create task (all fields optional)

**Response**: Same as get task

### 5. Delete Task

**Endpoint**: `DELETE /api/v1/tasks/{task_id}`

**Query Parameters**:
- `permanent` (boolean, optional): Permanent deletion (default: false)

**Response**:
```json
{
  "success": true,
  "data": {
    "message": "Task deleted successfully"
  }
}
```

### 6. Update Task Status

**Endpoint**: `PATCH /api/v1/tasks/{task_id}/status`

**Request Body**:
```json
{
  "status": "completed"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "task": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "status": "completed",
      "completed_at": "2024-01-10T15:30:00Z",
      "updated_at": "2024-01-10T15:30:00Z"
    }
  }
}
```

### 7. Assign Task

**Endpoint**: `PATCH /api/v1/tasks/{task_id}/assign`

**Request Body**:
```json
{
  "assigned_user_id": "550e8400-e29b-41d4-a716-446655440002"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "task": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "assigned_user": {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "name": "John Doe"
      },
      "updated_at": "2024-01-10T15:30:00Z"
    }
  }
}
```

## Categories API

### 1. List Categories

**Endpoint**: `GET /api/v1/categories`

**Query Parameters**:
- `active` (boolean, optional): Filter by active status (default: true)

**Response**:
```json
{
  "success": true,
  "data": {
    "categories": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "name": "Kitchen",
        "description": "Kitchen-related tasks",
        "color": "#FF6B6B",
        "icon": "kitchen",
        "sort_order": 1,
        "is_active": true,
        "task_count": 5,
        "created_at": "2024-01-10T10:00:00Z",
        "updated_at": "2024-01-10T10:00:00Z"
      }
    ]
  }
}
```

### 2. Create Category

**Endpoint**: `POST /api/v1/categories`

**Request Body**:
```json
{
  "name": "Kitchen",
  "description": "Kitchen-related tasks",
  "color": "#FF6B6B",
  "icon": "kitchen",
  "sort_order": 1
}
```

**Validation Rules**:
- `name`: Required, max 100 characters, unique per household
- `description`: Optional, max 500 characters
- `color`: Optional, valid hex color code
- `icon`: Optional, max 50 characters
- `sort_order`: Optional, integer

**Response**: Same as get category

### 3. Get Category

**Endpoint**: `GET /api/v1/categories/{category_id}`

**Response**:
```json
{
  "success": true,
  "data": {
    "category": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Kitchen",
      "description": "Kitchen-related tasks",
      "color": "#FF6B6B",
      "icon": "kitchen",
      "sort_order": 1,
      "is_active": true,
      "task_count": 5,
      "created_at": "2024-01-10T10:00:00Z",
      "updated_at": "2024-01-10T10:00:00Z"
    }
  }
}
```

### 4. Update Category

**Endpoint**: `PUT /api/v1/categories/{category_id}`

**Request Body**: Same as create category (all fields optional)

**Response**: Same as get category

### 5. Delete Category

**Endpoint**: `DELETE /api/v1/categories/{category_id}`

**Response**:
```json
{
  "success": true,
  "data": {
    "message": "Category deleted successfully"
  }
}
```

## Statistics API

### 1. Get Task Statistics

**Endpoint**: `GET /api/v1/statistics/tasks`

**Query Parameters**:
- `period` (string, optional): Time period (`day`, `week`, `month`, `year`, default: `month`)

**Response**:
```json
{
  "success": true,
  "data": {
    "statistics": {
      "total_tasks": 45,
      "pending_tasks": 12,
      "in_progress_tasks": 8,
      "completed_tasks": 25,
      "overdue_tasks": 3,
      "completion_rate": 55.6,
      "avg_completion_days": 2.3,
      "tasks_by_priority": {
        "low": 10,
        "medium": 20,
        "high": 12,
        "urgent": 3
      },
      "tasks_by_category": [
        {
          "category_id": "550e8400-e29b-41d4-a716-446655440001",
          "category_name": "Kitchen",
          "count": 15
        }
      ]
    }
  }
}
```

## System API

### 1. Health Check

**Endpoint**: `GET /health`

**Response**:
```json
{
  "status": "healthy",
  "timestamp": "2024-01-10T10:00:00Z",
  "version": "1.0.0",
  "services": {
    "database": "healthy",
    "cache": "healthy",
    "external_auth": "healthy"
  }
}
```

### 2. API Version

**Endpoint**: `GET /api/v1/version`

**Response**:
```json
{
  "success": true,
  "data": {
    "version": "1.0.0",
    "build_date": "2024-01-10T10:00:00Z",
    "git_commit": "abc123def456",
    "environment": "production"
  }
}
```

## Webhooks

### Webhook Events

The service can send webhook notifications for the following events:

- `task.created`
- `task.updated`
- `task.completed`
- `task.deleted`
- `task.assigned`
- `category.created`
- `category.updated`
- `category.deleted`

### Webhook Payload

```json
{
  "event": "task.created",
  "timestamp": "2024-01-10T10:00:00Z",
  "data": {
    "task": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Clean kitchen",
      "status": "pending"
    },
    "household_id": "550e8400-e29b-41d4-a716-446655440003"
  }
}
```

## Rate Limiting

Rate limits are applied per user and per household:

- **Per User**: 1000 requests per hour
- **Per Household**: 5000 requests per hour
- **Burst Limit**: 100 requests per minute

Rate limit headers are included in responses:

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1641816000
```

## Pagination

All list endpoints support pagination with the following parameters:

- `page`: Page number (1-based)
- `limit`: Items per page (max 100)

Pagination metadata is included in responses:

```json
{
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 45,
    "total_pages": 3,
    "has_next": true,
    "has_prev": false
  }
}
```

## Filtering and Sorting

### Filtering

Most list endpoints support filtering by common fields:

- `status`: Task status filter
- `priority`: Task priority filter
- `category_id`: Category filter
- `assigned_user_id`: User assignment filter
- `due_date_from` / `due_date_to`: Date range filter
- `search`: Text search in title and description

### Sorting

List endpoints support sorting by:

- `created_at`: Creation date
- `updated_at`: Last update date
- `due_date`: Due date
- `priority`: Priority level
- `title`: Task title

Sort order can be `asc` or `desc`.

## Search

Text search is available on task endpoints and searches both title and description fields using PostgreSQL full-text search capabilities.

Search queries are automatically tokenized and support:
- Partial word matching
- Case-insensitive search
- Ranking by relevance
- Stop word filtering

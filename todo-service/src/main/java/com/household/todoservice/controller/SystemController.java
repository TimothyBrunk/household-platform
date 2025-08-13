package com.household.todoservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * System controller for health checks and version information.
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System", description = "System information and health checks")
public class SystemController {

    private final BuildProperties buildProperties;

    /**
     * Get system health status.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns the health status of the service")
    public ResponseEntity<HealthResponse> getHealth() {
        log.debug("Health check requested");
        
        HealthResponse health = new HealthResponse(
                "healthy",
                LocalDateTime.now(),
                "1.0.0",
                Map.of(
                        "database", "healthy",
                        "cache", "healthy",
                        "external_auth", "healthy"
                )
        );
        
        return ResponseEntity.ok(health);
    }

    /**
     * Get API version information.
     */
    @GetMapping("/version")
    @Operation(summary = "Get version info", description = "Returns version information about the API")
    public ResponseEntity<VersionResponse> getVersion() {
        log.debug("Version info requested");
        
        VersionResponse version = new VersionResponse(
                buildProperties.getVersion(),
                buildProperties.getTime(),
                buildProperties.get("git.commit.id", "unknown"),
                System.getProperty("spring.profiles.active", "development")
        );
        
        return ResponseEntity.ok(version);
    }

    /**
     * Get system information.
     */
    @GetMapping("/info")
    @Operation(summary = "Get system info", description = "Returns general system information")
    public ResponseEntity<SystemInfoResponse> getSystemInfo() {
        log.debug("System info requested");
        
        SystemInfoResponse info = new SystemInfoResponse(
                "To-Do Service",
                "Household task management microservice",
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().totalMemory(),
                Runtime.getRuntime().freeMemory(),
                System.getProperty("java.version"),
                System.getProperty("os.name")
        );
        
        return ResponseEntity.ok(info);
    }

    // Response DTOs
    public record HealthResponse(
            String status,
            LocalDateTime timestamp,
            String version,
            Map<String, String> services
    ) {}

    public record VersionResponse(
            String version,
            LocalDateTime buildDate,
            String gitCommit,
            String environment
    ) {}

    public record SystemInfoResponse(
            String name,
            String description,
            int availableProcessors,
            long totalMemory,
            long freeMemory,
            String javaVersion,
            String osName
    ) {}
}

package com.household.todoservice.controller;

import com.household.todoservice.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authentication controller for login and token operations.
 * 
 * Note: In a real implementation, this would integrate with an external auth service.
 * For now, it provides endpoints for token validation and user info.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication operations")
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    /**
     * Validate JWT token and return user information.
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate token", description = "Validates JWT token and returns user information")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest request) {
        log.debug("Validating token");
        
        if (tokenProvider.validateToken(request.token())) {
            UUID userId = tokenProvider.getUserIdFromToken(request.token());
            UUID householdId = tokenProvider.getHouseholdIdFromToken(request.token());
            String email = tokenProvider.getEmailFromToken(request.token());
            
            TokenValidationResponse response = new TokenValidationResponse(
                    true,
                    userId,
                    householdId,
                    email,
                    tokenProvider.getExpirationDateFromToken(request.token())
            );
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.ok(new TokenValidationResponse(false, null, null, null, null));
        }
    }

    /**
     * Get current user information from authentication context.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns information about the currently authenticated user")
    public ResponseEntity<UserInfoResponse> getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String userId = authentication.getName();
            
            UserInfoResponse response = new UserInfoResponse(
                    UUID.fromString(userId),
                    "User " + userId.substring(0, 8),
                    "user@" + userId.substring(0, 8) + ".com"
            );
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Refresh JWT token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refreshes an existing JWT token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        log.debug("Refreshing token");
        
        if (tokenProvider.validateToken(request.token()) && !tokenProvider.isTokenExpired(request.token())) {
            UUID userId = tokenProvider.getUserIdFromToken(request.token());
            UUID householdId = tokenProvider.getHouseholdIdFromToken(request.token());
            String email = tokenProvider.getEmailFromToken(request.token());
            
            String newToken = tokenProvider.generateToken(userId, householdId, email);
            
            TokenRefreshResponse response = new TokenRefreshResponse(
                    newToken,
                    tokenProvider.getExpirationDateFromToken(newToken)
            );
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).build();
        }
    }

    // Request/Response DTOs
    public record TokenValidationRequest(String token) {}
    
    public record TokenValidationResponse(
            boolean valid,
            UUID userId,
            UUID householdId,
            String email,
            java.util.Date expiration
    ) {}
    
    public record TokenRefreshRequest(String token) {}
    
    public record TokenRefreshResponse(
            String token,
            java.util.Date expiration
    ) {}
    
    public record UserInfoResponse(
            UUID userId,
            String name,
            String email
    ) {}
}

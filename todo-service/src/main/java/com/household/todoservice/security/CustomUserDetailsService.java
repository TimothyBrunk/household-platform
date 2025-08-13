package com.household.todoservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

/**
 * Custom UserDetailsService for JWT authentication.
 * 
 * Note: In a real implementation, this would integrate with an external user service
 * to fetch actual user details. For now, it creates a basic user representation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // This method is not used in JWT authentication, but required by interface
        throw new UsernameNotFoundException("Username authentication not supported");
    }

    /**
     * Load user details by user ID.
     * 
     * In a real implementation, this would make a call to the user service
     * to fetch actual user details including roles and permissions.
     */
    public UserDetails loadUserById(UUID userId) {
        log.debug("Loading user details for user ID: {}", userId);
        
        // In a real implementation, you would:
        // 1. Make HTTP call to user service
        // 2. Fetch user details including roles and permissions
        // 3. Return UserDetails with proper authorities
        
        // For now, create a basic user representation
        return User.builder()
                .username(userId.toString())
                .password("") // No password needed for JWT
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}

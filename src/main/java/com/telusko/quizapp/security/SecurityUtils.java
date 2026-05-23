package com.telusko.quizapp.security;

import com.telusko.quizapp.entity.User;
import com.telusko.quizapp.exception.ResourceNotFoundException;
import com.telusko.quizapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper to get the currently authenticated User entity.
 *
 * Spring Security stores the email (username) in the context after JWT validation.
 * This class fetches the full User entity from DB using that email.
 *
 * Usage in any service:
 *   User currentUser = securityUtils.getCurrentUser();
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        String email = authentication.getName(); // getName() returns the email (subject)
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

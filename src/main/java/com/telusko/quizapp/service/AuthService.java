package com.telusko.quizapp.service;

import com.telusko.quizapp.dto.request.LoginRequest;
import com.telusko.quizapp.dto.request.RegisterRequest;
import com.telusko.quizapp.dto.response.AuthResponse;
import com.telusko.quizapp.entity.User;
import com.telusko.quizapp.exception.DuplicateResourceException;
import com.telusko.quizapp.repository.UserRepository;
import com.telusko.quizapp.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email is already taken
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "An account with email '" + request.getEmail() + "' already exists.");
        }

        // Build user — role defaults to STUDENT for self-registration
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt hash
                .role(User.Role.ROLE_STUDENT)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", request.getEmail());

        // Load as UserDetails to generate JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresInMs(jwtUtil.getExpirationMs())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // AuthenticationManager handles credential verification — throws BadCredentialsException if wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        log.info("User logged in: {}", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresInMs(jwtUtil.getExpirationMs())
                .build();
    }
}

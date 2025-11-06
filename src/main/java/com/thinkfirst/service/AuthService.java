package com.thinkfirst.service;

import com.thinkfirst.dto.AuthResponse;
import com.thinkfirst.dto.LoginRequest;
import com.thinkfirst.dto.RegisterRequest;
import com.thinkfirst.model.User;
import com.thinkfirst.repository.UserRepository;
import com.thinkfirst.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFullName().split(" ")[0])
                .lastName(request.getFullName().length() > request.getFullName().split(" ")[0].length() 
                        ? request.getFullName().substring(request.getFullName().split(" ")[0].length() + 1) 
                        : "")
                .role(User.UserRole.valueOf(request.getRole() != null ? request.getRole() : "PARENT"))
                .active(true)
                .emailVerified(false)
                .build();
        
        user = userRepository.save(user);
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .build();
    }
    
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        User user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}


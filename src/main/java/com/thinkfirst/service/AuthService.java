package com.thinkfirst.service;

import com.thinkfirst.dto.AuthResponse;
import com.thinkfirst.dto.ChildLoginRequest;
import com.thinkfirst.dto.LoginRequest;
import com.thinkfirst.dto.RegisterRequest;
import com.thinkfirst.model.Child;
import com.thinkfirst.model.User;
import com.thinkfirst.repository.ChildRepository;
import com.thinkfirst.repository.UserRepository;
import com.thinkfirst.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
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
                .fullName(request.getFullName())
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
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
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
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Child login with username/password
     */
    public AuthResponse childLogin(ChildLoginRequest request) {
        Child child = childRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Child not found"));

        if (!passwordEncoder.matches(request.getPassword(), child.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!child.getActive()) {
            throw new RuntimeException("Child account is inactive");
        }

        // Create UserDetails for child
        UserDetails childUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("child_" + child.getId()) // Prefix to distinguish from parent users
                .password(child.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_CHILD")))
                .build();

        String token = jwtTokenProvider.generateToken(childUserDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(childUserDetails);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(child.getId())
                .email(null) // Children don't have email
                .fullName(child.getUsername())
                .role("CHILD")
                .build();
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtTokenProvider.extractUsername(refreshToken);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        if (!jwtTokenProvider.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtTokenProvider.generateToken(userDetails);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}


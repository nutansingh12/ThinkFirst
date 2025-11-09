package com.thinkfirst.security;

import com.thinkfirst.model.Child;
import com.thinkfirst.model.User;
import com.thinkfirst.repository.ChildRepository;
import com.thinkfirst.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check if this is a child user (username starts with "child_")
        if (username.startsWith("child_")) {
            Long childId = Long.parseLong(username.substring(6)); // Remove "child_" prefix
            Child child = childRepository.findById(childId)
                    .orElseThrow(() -> new UsernameNotFoundException("Child not found with ID: " + childId));

            return new org.springframework.security.core.userdetails.User(
                    username, // Use "child_X" as username
                    child.getPassword(),
                    child.getActive(),
                    true,
                    true,
                    true,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_CHILD"))
            );
        }

        // Otherwise, it's a parent user (lookup by email)
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getActive(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}


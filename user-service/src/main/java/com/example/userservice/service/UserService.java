package com.example.userservice.service;

import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Loading user by username: " + username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.err.println("User not found: " + username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
        System.out.println("User found: " + user.getUsername() + ", roles: " + user.getRoles());
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRoles()))
        );
    }

    public User saveUser(User user) {
        System.out.println("Saving user: " + user.getUsername());
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            System.err.println("User already exists: " + user.getUsername());
            throw new IllegalArgumentException("User with username " + user.getUsername() + " already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles("ROLE_USER");
        User savedUser = userRepository.save(user);
        System.out.println("User saved: " + savedUser.getUsername());
        return savedUser;
    }

    public List<User> getAllUsers() {
        System.out.println("Fetching all users");
        return userRepository.findAll();
    }

    public User getUserByUsername(String username) {
        System.out.println("Fetching user by username: " + username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.err.println("User not found: " + username);
                    return new IllegalArgumentException("User with username " + username + " not found");
                });
    }

    public void deleteUser(Long id) {
        System.out.println("Deleting user with id: " + id);
        if (!userRepository.existsById(id)) {
            System.err.println("User not found with id: " + id);
            throw new IllegalArgumentException("User with id " + id + " not found");
        }
        userRepository.deleteById(id);
        System.out.println("User deleted with id: " + id);
    }
}
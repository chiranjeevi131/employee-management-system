package com.ems.service;

import java.util.Optional;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ems.entity.User;
import com.ems.repository.UserRepository;

@Service
public class UserService {


private final UserRepository userRepository;
private final PasswordEncoder passwordEncoder;

public UserService(UserRepository userRepository,
                   PasswordEncoder passwordEncoder) {

    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
}

// Register a new Admin
public User registerUser(User user) {

    return createUser(user, "ADMIN");
}

// Create an HR account
public User registerHR(User user) {

    return createUser(user, "HR");
}

// Common user creation method
private User createUser(User user, String role) {

    if (userRepository.existsByUsername(user.getUsername())) {
        throw new RuntimeException("Username already exists");
    }

    if (userRepository.findByEmail(user.getEmail()).isPresent()) {
        throw new RuntimeException("Email already exists");
    }

    // Encrypt password using BCrypt
    user.setPassword(passwordEncoder.encode(user.getPassword()));

    // Assign role securely on the server
    user.setRole(role);

    return userRepository.save(user);
}

// Find user by username
public Optional<User> findByUsername(String username) {

    return userRepository.findByUsername(username);
}

// Change admin password
public void changePassword(String username,
                           String currentPassword,
                           String newPassword) {

    User user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                new RuntimeException("User not found"));

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
        throw new RuntimeException("Current password is incorrect");
    }

    user.setPassword(passwordEncoder.encode(newPassword));

    userRepository.save(user);
}

//Update user profile
public User updateProfile(Long id, String username, String email) {

 User user = userRepository.findById(id)
         .orElseThrow(() ->
             new RuntimeException("User not found"));

 // Check duplicate username
 Optional<User> existingUsername =
         userRepository.findByUsername(username);

 if (existingUsername.isPresent()
         && !existingUsername.get().getId().equals(id)) {
     throw new RuntimeException("Username already exists");
 }

 // Check duplicate email
 Optional<User> existingEmail =
         userRepository.findByEmail(email);

 if (existingEmail.isPresent()
         && !existingEmail.get().getId().equals(id)) {
     throw new RuntimeException("Email already exists");
 }

 user.setUsername(username);
 user.setEmail(email);

 return userRepository.save(user);
}

//Delete HR account
public void deleteHRAccount(Long id) {

 User user = userRepository.findById(id)
         .orElseThrow(() ->
             new RuntimeException("User not found"));

 if (!"HR".equalsIgnoreCase(user.getRole())) {
     throw new RuntimeException(
         "Only HR accounts can be deleted");
 }

 userRepository.delete(user);
}

//Get all HR accounts
public List<User> getAllHRAccounts() {

 return userRepository.findAll()
         .stream()
         .filter(user -> "HR".equalsIgnoreCase(user.getRole()))
         .toList();
}

}

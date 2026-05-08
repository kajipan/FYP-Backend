package com.mechinow.service;

import com.mechinow.model.User;
import com.mechinow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }
        return userRepository.save(user);
    }

    public Optional<User> login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword() != null && 
                user.getPassword().equals(password)) {
                return userOpt;
            }
        }
        return Optional.empty();
    }

    public List<User> getAllMechanics() {
        return userRepository.findAll().stream()
                .filter(u -> "mechanic".equals(u.getRole()))
                .toList();
    }
}
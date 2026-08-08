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
            if (user.getPassword() != null && user.getPassword().equals(password)) {
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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public void updatePassword(String email, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        userOpt.ifPresent(user -> {
            user.setPassword(newPassword);
            userRepository.save(user);
        });
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setName(updatedUser.getName());
            user.setPhone(updatedUser.getPhone());
            user.setVehicleType(updatedUser.getVehicleType());
            user.setVehicleModel(updatedUser.getVehicleModel());
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found!"));
    }
}
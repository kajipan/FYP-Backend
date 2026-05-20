package com.mechinow.controller;

import com.mechinow.model.User;
import com.mechinow.service.UserService;
import com.mechinow.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (userService.emailExists(email)) {
            return ResponseEntity.badRequest().body("Email already registered!");
        }
        String otp = emailService.generateOTP(email);
        emailService.sendOTP(email, otp);
        return ResponseEntity.ok("OTP sent!");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        if (emailService.verifyOTP(email, otp)) {
            return ResponseEntity.ok("OTP verified!");
        }
        return ResponseEntity.badRequest().body("Invalid OTP!");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (!userService.emailExists(email)) {
            return ResponseEntity.badRequest().body("Email not found!");
        }
        String otp = emailService.generateOTP(email);
        emailService.sendPasswordReset(email, otp);
        return ResponseEntity.ok("Reset OTP sent!");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");
        if (emailService.verifyOTP(email, otp)) {
            userService.updatePassword(email, newPassword);
            return ResponseEntity.ok("Password reset successful!");
        }
        return ResponseEntity.badRequest().body("Invalid OTP!");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User saved = userService.register(user);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        Optional<User> user = userService.login(
            loginRequest.getEmail(),
            loginRequest.getPassword()
        );
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.badRequest().body("Invalid credentials!");
    }

    @GetMapping("/mechanics")
    public ResponseEntity<?> getMechanics() {
        return ResponseEntity.ok(userService.getAllMechanics());
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted!");
    }
}
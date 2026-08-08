package com.mechinow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private Map<String, String> otpStorage = new HashMap<>();

    public String generateOTP(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStorage.put(email, otp);
        return otp;
    }

    public boolean verifyOTP(String email, String otp) {
        String stored = otpStorage.get(email);
        if (stored != null && stored.equals(otp)) {
            otpStorage.remove(email);
            return true;
        }
        return false;
    }

    public void sendOTP(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("MechNow - Email Verification OTP");
        message.setText(
            "Welcome to MechNow!\n\n" +
            "Your OTP verification code is: " + otp + "\n\n" +
            "This code is valid for 10 minutes.\n\n" +
            "Do not share this code with anyone.\n\n" +
            "Team MechNow"
        );
        mailSender.send(message);
    }

    public void sendPasswordReset(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("MechNow - Password Reset OTP");
        message.setText(
            "MechNow Password Reset\n\n" +
            "Your password reset OTP is: " + otp + "\n\n" +
            "This code is valid for 10 minutes.\n\n" +
            "If you did not request this, ignore this email.\n\n" +
            "Team MechNow"
        );
        mailSender.send(message);
    }
}
 
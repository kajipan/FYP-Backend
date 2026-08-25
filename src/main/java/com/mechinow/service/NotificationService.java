package com.mechinow.service;

import com.mechinow.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.mechinow.repository.UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    public void sendPaymentEmail(Long userId, com.mechinow.model.ServiceRequest req) {
        String mechanicName = userRepository.findById(req.getMechanicId())
            .map(com.mechinow.model.User::getName)
            .orElse("Mechanic");
        userRepository.findById(userId).ifPresent(user -> {
            emailService.sendPaymentReceipt(
                user.getEmail(),
                user.getName(),
                mechanicName,
                req.getIssue(),
                req.getAmount(),
                req.getPaymentMethod()
            );
        });
    }

    public void notifyMechanic(Long mechanicId, ServiceRequest request) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "NEW_REQUEST");
        notification.put("requestId", request.getRequestId());
        notification.put("userId", request.getUserId());    
        notification.put("userName", request.getUserName());
        notification.put("issue", request.getIssue());
        notification.put("vehicleModel", request.getVehicleModel());
        notification.put("userLat", request.getUserLat());
        notification.put("userLng", request.getUserLng());
        messagingTemplate.convertAndSend("/topic/mechanic/" + mechanicId, notification);
    }

    public void notifyUser(Long userId, String status, Long mechanicId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", status.equals("ACCEPTED") ? "REQUEST_ACCEPTED" : "REQUEST_REJECTED");
        notification.put("status", status);
        notification.put("mechanicId", mechanicId);
        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
    }

    public void updateMechanicLocation(Long userId, double lat, double lng, String mechanicName) {
        Map<String, Object> location = new HashMap<>();
        location.put("type", "MECHANIC_LOCATION");
        location.put("lat", lat);
        location.put("lng", lng);
        location.put("mechanicName", mechanicName);
        messagingTemplate.convertAndSend("/topic/tracking/" + userId, location);
    }
    public void updateUserLocation(Long mechanicId, double lat, double lng, String userName) {
        Map<String, Object> location = new HashMap<>();
        location.put("type", "USER_LOCATION");
        location.put("lat", lat);
        location.put("lng", lng);
        location.put("userName", userName);
        messagingTemplate.convertAndSend("/topic/mechanic-tracking/" + mechanicId, location);
    }

    public void relayCallSignal(String topic, Object payload) {
    messagingTemplate.convertAndSend(topic, payload);
    }

    public void notifyCancellation(Long userId, Long mechanicId, String cancelledBy, String reason) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "REQUEST_CANCELLED");
        notification.put("cancelledBy", cancelledBy);
        notification.put("reason", reason);

        if (cancelledBy.equals("user")) {
            messagingTemplate.convertAndSend("/topic/mechanic/" + mechanicId, notification);
        } else {
            messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
        }
    }

    public void notifyBillSent(Long userId, ServiceRequest req) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "BILL_RECEIVED");
        notification.put("requestId", req.getRequestId());
        notification.put("amount", req.getAmount());
        notification.put("issue", req.getIssue());
        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
    }
    public void notifyArrived(Long userId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "MECHANIC_ARRIVED");
        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
    }

    public void notifyWorkStarted(Long userId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "WORK_STARTED");
        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
    }
    public void notifyCashSelected(Long mechanicId, ServiceRequest req) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "CASH_PAYMENT_SELECTED");
        notification.put("requestId", req.getRequestId());
        notification.put("amount", req.getAmount());
        messagingTemplate.convertAndSend("/topic/mechanic/" + mechanicId, notification);
    }

    public void notifyPaymentConfirmed(Long userId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PAYMENT_CONFIRMED");
        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
    }

    public void notifyMechanicPaymentReceived(Long mechanicId, Double amount, String method) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PAYMENT_RECEIVED");
        notification.put("amount", amount);
        notification.put("method", method);
        messagingTemplate.convertAndSend("/topic/mechanic/" + mechanicId, notification);
    }
}
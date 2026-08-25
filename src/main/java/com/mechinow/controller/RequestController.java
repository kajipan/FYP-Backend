package com.mechinow.controller;

import com.mechinow.model.ServiceRequest;
import com.mechinow.repository.ServiceRequestRepository;
import com.mechinow.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@CrossOrigin(origins = "*")
public class RequestController {

    @Autowired
    private ServiceRequestRepository requestRepository;

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private com.mechinow.repository.UserRepository userRepository;
    // Send mechanic request
    @MessageMapping("/request.send")
    public void sendRequest(@Payload Map<String, Object> request) {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setRequestId(request.get("requestId").toString()); 
        serviceRequest.setUserId(Long.valueOf(request.get("userId").toString()));
        serviceRequest.setMechanicId(Long.valueOf(request.get("mechanicId").toString()));
        serviceRequest.setUserName(request.get("userName").toString());
        serviceRequest.setIssue(request.get("issue").toString());
        serviceRequest.setVehicleModel(request.get("vehicleModel") != null ? request.get("vehicleModel").toString() : "");
        serviceRequest.setUserLat(Double.parseDouble(request.get("userLat").toString()));
        serviceRequest.setUserLng(Double.parseDouble(request.get("userLng").toString()));
        serviceRequest.setStatus("PENDING");
        serviceRequest.setCreatedAt(LocalDateTime.now());
        ServiceRequest saved = requestRepository.save(serviceRequest);
        notificationService.notifyMechanic(serviceRequest.getMechanicId(), saved);
    }

    // Accept or Reject request
    @MessageMapping("/request.respond")
    public void respondToRequest(@Payload Map<String, Object> response) {
        String requestId = response.get("requestId").toString();    // ✅ String
        String status = response.get("status").toString();
        Long mechanicId = Long.valueOf(response.get("mechanicId").toString());
        Long userId = Long.valueOf(response.get("userId").toString());  // ✅ from Flutter

        requestRepository.findByRequestId(requestId).ifPresent(req -> {
            req.setStatus(status);
            requestRepository.save(req);
            notificationService.notifyUser(userId, status, mechanicId); // ✅ use userId
        });
    }

    // Update mechanic location
    @MessageMapping("/location.update")
    public void updateLocation(@Payload Map<String, Object> location) {
        Long userId = Long.valueOf(location.get("userId").toString());
        double lat = Double.parseDouble(location.get("lat").toString());
        double lng = Double.parseDouble(location.get("lng").toString());
        String mechanicName = location.get("mechanicName").toString();
        notificationService.updateMechanicLocation(userId, lat, lng, mechanicName);
    }

    @MessageMapping("/user.location.update")
    public void updateUserLocation(@Payload Map<String, Object> location) {
        Long mechanicId = Long.valueOf(location.get("mechanicId").toString());
        double lat = Double.parseDouble(location.get("lat").toString());
        double lng = Double.parseDouble(location.get("lng").toString());
        String userName = location.get("userName").toString();
        notificationService.updateUserLocation(mechanicId, lat, lng, userName);
    }

    // REST endpoints
    @GetMapping("/api/requests/mechanic/{mechanicId}")
    @ResponseBody
    public ResponseEntity<?> getMechanicRequests(@PathVariable Long mechanicId) {
        List<ServiceRequest> requests = requestRepository.findByMechanicIdAndStatus(mechanicId, "PENDING");
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/api/requests/user/{userId}")
    @ResponseBody
    public ResponseEntity<?> getUserRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(requestRepository.findByUserId(userId));
    }

    @MessageMapping("/call.signal")
    public void relayCallSignal(@Payload Map<String, Object> signal) {
        String targetType = signal.get("targetType").toString(); // "user" or "mechanic"
        String targetId = signal.get("targetId").toString();
        notificationService.relayCallSignal("/topic/call/" + targetType + "/" + targetId, signal);
    }

    @MessageMapping("/request.cancel")
    public void cancelRequest(@Payload Map<String, Object> payload) {
        String requestId = payload.get("requestId").toString();
        String cancelledBy = payload.get("cancelledBy").toString(); // "user" or "mechanic"
        String reason = payload.get("reason").toString();

        requestRepository.findByRequestId(requestId).ifPresent(req -> {
            req.setStatus("CANCELLED");
            requestRepository.save(req);
            notificationService.notifyCancellation(req.getUserId(), req.getMechanicId(), cancelledBy, reason);
        });
    }

    @MessageMapping("/bill.send")
    public void sendBill(@Payload Map<String, Object> payload) {
        String requestId = payload.get("requestId").toString();
        Double amount = Double.parseDouble(payload.get("amount").toString());

        requestRepository.findByRequestId(requestId).ifPresent(req -> {
            req.setAmount(amount);
            req.setPaymentStatus("PENDING");
            requestRepository.save(req);
            notificationService.notifyBillSent(req.getUserId(), req);
        });
    }
    @MessageMapping("/request.arrived")
    public void mechanicArrived(@Payload Map<String, Object> payload) {
        String requestId = payload.get("requestId").toString();
        requestRepository.findByRequestId(requestId).ifPresent(req -> {
            req.setStatus("ARRIVED");
            requestRepository.save(req);
            notificationService.notifyArrived(req.getUserId());
        });
    }

    @MessageMapping("/request.startwork")
    public void startWork(@Payload Map<String, Object> payload) {
        String requestId = payload.get("requestId").toString();
        requestRepository.findByRequestId(requestId).ifPresent(req -> {
            req.setStatus("IN_PROGRESS");
            requestRepository.save(req);
            notificationService.notifyWorkStarted(req.getUserId());
        });
    }

    @MessageMapping("/payment.cash.select")
    public void cashPaymentSelected(@Payload Map<String, Object> payload) {
        String requestId = payload.get("requestId").toString();
        requestRepository.findByRequestId(requestId).ifPresent(req -> {
            req.setPaymentMethod("CASH");
            requestRepository.save(req);
            notificationService.notifyCashSelected(req.getMechanicId(), req);
        });
    }

    @MessageMapping("/payment.cash.confirm")
    public void cashPaymentConfirmed(@Payload Map<String, Object> payload) {
        String requestId = payload.get("requestId").toString();
        requestRepository.findByRequestId(requestId).ifPresent(req -> {
            req.setPaymentStatus("PAID");
            req.setStatus("COMPLETED");
            req.setCompletedAt(LocalDateTime.now());
            requestRepository.save(req);

            userRepository.findById(req.getMechanicId()).ifPresent(mech -> {
                mech.setJobsDone(mech.getJobsDone() + 1);
                mech.setTotalEarnings(mech.getTotalEarnings() + (req.getAmount() != null ? req.getAmount() : 0));
                userRepository.save(mech);
            });

            notificationService.sendPaymentEmail(req.getUserId(), req);
            notificationService.notifyPaymentConfirmed(req.getUserId());
            notificationService.notifyMechanicPaymentReceived(req.getMechanicId(), req.getAmount(), "CASH");
        });
    }

    @MessageMapping("/payment.card.complete")
    public void cardPaymentComplete(@Payload Map<String, Object> payload) {
        String requestId = payload.get("requestId").toString();
        requestRepository.findByRequestId(requestId).ifPresent(req -> {
            req.setPaymentMethod("CARD");
            req.setPaymentStatus("PAID");
            req.setStatus("COMPLETED");
            req.setCompletedAt(LocalDateTime.now());
            requestRepository.save(req);

            userRepository.findById(req.getMechanicId()).ifPresent(mech -> {
                mech.setJobsDone(mech.getJobsDone() + 1);
                mech.setTotalEarnings(mech.getTotalEarnings() + (req.getAmount() != null ? req.getAmount() : 0));
                userRepository.save(mech);
            });

            notificationService.sendPaymentEmail(req.getUserId(), req);
            notificationService.notifyMechanicPaymentReceived(req.getMechanicId(), req.getAmount(), "CARD");
        });
    }

    @GetMapping("/api/requests/user/{userId}/history")
    @ResponseBody
    public ResponseEntity<?> getUserHistory(@PathVariable Long userId) {
        List<ServiceRequest> completed = requestRepository.findByUserId(userId).stream()
            .filter(r -> "COMPLETED".equals(r.getStatus()))
            .sorted((a, b) -> {
                if (a.getCompletedAt() == null || b.getCompletedAt() == null) return 0;
                return b.getCompletedAt().compareTo(a.getCompletedAt());
            })
            .toList();
        return ResponseEntity.ok(completed);
    }

    @GetMapping("/api/requests/mechanic/{mechanicId}/history")
    @ResponseBody
    public ResponseEntity<?> getMechanicHistory(@PathVariable Long mechanicId) {
        List<ServiceRequest> completed = requestRepository.findByMechanicIdAndStatus(mechanicId, "COMPLETED").stream()
            .sorted((a, b) -> {
                if (a.getCompletedAt() == null || b.getCompletedAt() == null) return 0;
                return b.getCompletedAt().compareTo(a.getCompletedAt());
            })
            .toList();
        return ResponseEntity.ok(completed);
    }

}
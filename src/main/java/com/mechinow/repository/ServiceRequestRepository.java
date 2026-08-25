package com.mechinow.repository;

import com.mechinow.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;  

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByMechanicIdAndStatus(Long mechanicId, String status);
    List<ServiceRequest> findByUserId(Long userId);

    Optional<ServiceRequest> findByRequestId(String requestId);  
}
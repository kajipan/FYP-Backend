package com.mechinow.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    private String password;
    private String phone;
    private String role; // user or mechanic
    private String vehicleType;
    private String vehicleModel;
    private boolean isOnline;
}
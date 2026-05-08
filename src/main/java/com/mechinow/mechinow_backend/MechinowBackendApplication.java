package com.mechinow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.mechinow.model")
@EnableJpaRepositories("com.mechinow.repository")
public class MechinowBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MechinowBackendApplication.class, args);
    }
}

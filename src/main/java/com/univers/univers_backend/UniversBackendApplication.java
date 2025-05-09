/* (C)2025 */
package com.univers.univers_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UniversBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniversBackendApplication.class, args);

        System.out.println("Hello World Testing");
    }
}

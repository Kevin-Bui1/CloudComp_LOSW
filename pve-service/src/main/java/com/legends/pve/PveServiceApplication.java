package com.legends.pve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** PvE Service entry point — runs on port 5002. */
@SpringBootApplication
public class PveServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PveServiceApplication.class, args);
    }
}

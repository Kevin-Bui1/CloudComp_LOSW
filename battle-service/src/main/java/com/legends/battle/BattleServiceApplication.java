package com.legends.battle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Battle Service entry point — runs on port 5001. */
@SpringBootApplication
public class BattleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BattleServiceApplication.class, args);
    }
}

package com.lvatong.lft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LvatongApplication {

    public static void main(String[] args) {
        SpringApplication.run(LvatongApplication.class, args);
    }

}

package com.api.idealhome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class IdealhomeApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(IdealhomeApplication.class, args);
    }
}

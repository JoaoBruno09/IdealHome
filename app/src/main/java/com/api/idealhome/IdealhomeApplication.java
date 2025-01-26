package com.api.idealhome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class IdealhomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdealhomeApplication.class, args);
	}

}

package com.srm.creditengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CreditEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(CreditEngineApplication.class, args);
	}

}

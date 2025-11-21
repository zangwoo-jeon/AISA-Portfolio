package com.AISA.AISA;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class AisaApplication {

	public static void main(String[] args) {
		SpringApplication.run(AisaApplication.class, args);
	}

}

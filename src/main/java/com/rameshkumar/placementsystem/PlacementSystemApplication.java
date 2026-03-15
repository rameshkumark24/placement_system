package com.rameshkumar.placementsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class PlacementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlacementSystemApplication.class, args);
	}

}

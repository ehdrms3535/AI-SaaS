package com.example.saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class SaasApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaasApplication.class, args);
	}

	@Bean
	CommandLineRunner printHash() {
		return args -> {
			System.out.println(new BCryptPasswordEncoder().encode("1234"));
		};
	}

}

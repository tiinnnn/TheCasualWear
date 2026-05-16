package com.datn.TheCasualWear;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TheCasualWearApplication {

	public static void main(String[] args) {
		loadEnvironmentVariables();
		SpringApplication.run(TheCasualWearApplication.class, args);
	}

	private static void loadEnvironmentVariables() {
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();
		
		dotenv.entries().forEach(entry -> 
			System.setProperty(entry.getKey(), entry.getValue())
		);
	}

}

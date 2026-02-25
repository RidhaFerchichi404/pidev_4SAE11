package com.esprit.portfolio;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableDiscoveryClient
public class PortfolioApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication.run(PortfolioApplication.class, args);
	}

	private static void loadEnv() {
		String[] paths = {"backEnd/.env", ".env", "../../.env", "../../../.env"};
		for (String path : paths) {
			File file = new File(path);
			if (file.exists()) {
				Dotenv dotenv = Dotenv.configure()
						.directory(file.getParent())
						.filename(file.getName())
						.ignoreIfMissing()
						.load();
				dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
				System.out.println("Loaded .env from: " + file.getAbsolutePath());
				break;
			}
		}
	}

}

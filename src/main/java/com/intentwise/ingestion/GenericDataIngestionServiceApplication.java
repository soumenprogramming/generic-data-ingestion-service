package com.intentwise.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GenericDataIngestionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GenericDataIngestionServiceApplication.class, args);
	}
}

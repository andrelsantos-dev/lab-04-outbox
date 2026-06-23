package com.alssant.asclepio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AsclepioApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsclepioApplication.class, args);
	}

}

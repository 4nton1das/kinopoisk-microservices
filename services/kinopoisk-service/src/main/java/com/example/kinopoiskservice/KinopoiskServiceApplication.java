package com.example.kinopoiskservice;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.hateoas.config.EnableHypermediaSupport;

@SpringBootApplication(
		scanBasePackages = {
				"com.example.kinopoiskservice",
				"com.example.kinopoiskapicontract",
				"com.example.kinopoiskeventscontract"},
		exclude = {DataSourceAutoConfiguration.class}
)
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
public class KinopoiskServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(KinopoiskServiceApplication.class, args);
	}

}

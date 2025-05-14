package com.dappstp.dappstp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.dappstp.dappstp.service.getapifootball.FootballApiService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableAspectJAutoProxy
public class DappstpApplication {

	private static final Logger logger = LoggerFactory.getLogger(DappstpApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DappstpApplication.class, args);
		logger.info("holamundo");
	}
 
}

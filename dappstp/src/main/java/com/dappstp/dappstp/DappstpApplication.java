package com.dappstp.dappstp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling
@EnableCaching
public class DappstpApplication {

	private static final Logger logger = LoggerFactory.getLogger(DappstpApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DappstpApplication.class, args);
		logger.info("holamundo");
	}
 
}

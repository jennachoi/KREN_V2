package com.kren;

import org.springframework.boot.SpringApplication;

public class TestKrenApplication {

	public static void main(String[] args) {
		SpringApplication.from(KrenApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

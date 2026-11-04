package com.example.jolijob;

import org.springframework.boot.SpringApplication;

public class TestJoliJobApplication {

    public static void main(String[] args) {
        SpringApplication.from(JoliJobApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

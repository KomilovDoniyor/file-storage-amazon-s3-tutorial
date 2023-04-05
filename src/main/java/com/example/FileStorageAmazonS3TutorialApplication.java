package com.example;

import org.hashids.Hashids;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FileStorageAmazonS3TutorialApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileStorageAmazonS3TutorialApplication.class, args);
    }

    @Bean
    Hashids hashids() {
        return new Hashids();
    }

}

package com.MWS.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CloudStorageServer {

    public static void main(String[] args) {
        System.out.println(String.format("Hello and welcome!"));

        for (int i = 1; i <= 5; i++) {
            System.out.println("i = " + i);
        }

        SpringApplication.run(CloudStorageServer.class, args);
    }
}

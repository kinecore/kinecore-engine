package org.kinecore.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the KineCore Enterprise Server.
 */
@SpringBootApplication
@EnableAsync
public class KineCoreServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KineCoreServerApplication.class, args);
    }
}

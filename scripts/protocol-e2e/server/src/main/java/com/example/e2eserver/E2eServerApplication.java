package com.example.e2eserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Multi-protocol end-to-end test server. Started by run-protocol-e2e.sh before
 * the Codename One client app runs its protocol tests against it.
 */
@SpringBootApplication
public class E2eServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(E2eServerApplication.class, args);
    }
}

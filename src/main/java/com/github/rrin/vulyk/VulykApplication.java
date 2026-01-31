package com.github.rrin.vulyk;

import com.github.rrin.vulyk.config.FileStorageProperties;
import com.github.rrin.vulyk.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, FileStorageProperties.class})
public class VulykApplication {
    public static void main(String[] args) {
        SpringApplication.run(VulykApplication.class, args);
    }
}
package net.filemaid.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FileMaidServer {
    public static void main(String[] args) {
        SpringApplication.run(FileMaidServer.class, args);
    }
}

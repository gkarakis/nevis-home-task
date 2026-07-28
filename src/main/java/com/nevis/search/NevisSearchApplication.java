package com.nevis.search;

import com.nevis.search.config.SearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SearchProperties.class)
public class NevisSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(NevisSearchApplication.class, args);
    }
}

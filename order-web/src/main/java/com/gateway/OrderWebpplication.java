package com.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
                                   DataSourceTransactionManagerAutoConfiguration.class,
                                   FreeMarkerAutoConfiguration.class })
public class OrderWebpplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderWebpplication.class, args);
    }
}

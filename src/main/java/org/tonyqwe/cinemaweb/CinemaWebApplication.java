package org.tonyqwe.cinemaweb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan(basePackages = "org.tonyqwe.cinemaweb.mapper")
public class CinemaWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(CinemaWebApplication.class, args);
    }

}

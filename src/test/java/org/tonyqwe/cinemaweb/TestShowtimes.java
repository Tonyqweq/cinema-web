package org.tonyqwe.cinemaweb;

import org.tonyqwe.cinemaweb.domain.entity.Showtimes;
import org.tonyqwe.cinemaweb.service.ShowtimesService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestShowtimes {
    public static void main(String[] args) {
        // 测试Showtimes实体类
        Showtimes showtimes = new Showtimes();
        showtimes.setCinemaId(1L);
        showtimes.setHallId(1L);
        showtimes.setMovieId(1L);
        showtimes.setPrice(new java.math.BigDecimal(50));
        showtimes.setStatus(1);
        
        System.out.println("Showtimes entity created successfully: " + showtimes);
        System.out.println("Test completed successfully!");
    }
}
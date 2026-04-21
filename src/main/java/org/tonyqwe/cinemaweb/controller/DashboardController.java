package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tonyqwe.cinemaweb.service.MovieService;
import org.tonyqwe.cinemaweb.service.OrderService;
import org.tonyqwe.cinemaweb.service.UserRoleService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource
    private UserService userService;

    @Resource
    private MovieService movieService;

    @Resource
    private OrderService orderService;

    @Resource
    private UserRoleService userRoleService;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/stats")
    public ResponseResult<?> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 总用户数
        long totalUsers = userService.count();
        stats.put("totalUsers", totalUsers);

        // 电影数量
        long totalMovies = movieService.count();
        stats.put("totalMovies", totalMovies);

        // 订单总数
        long totalOrders = orderService.count();
        stats.put("totalOrders", totalOrders);

        // 总票房
        double totalRevenue = orderService.getTotalRevenue();
        stats.put("totalRevenue", totalRevenue);

        return ResponseResult.success(stats);
    }

    /**
     * 获取票房趋势数据
     */
    @GetMapping("/revenue-trend")
    public ResponseResult<?> getRevenueTrend() {
        List<Map<String, Object>> monthlyData = orderService.getMonthlyRevenue(6);

        List<String> months = new ArrayList<>();
        List<Double> revenue = new ArrayList<>();

        for (Map<String, Object> data : monthlyData) {
            months.add((String) data.get("month"));
            revenue.add((Double) data.get("revenue"));
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("months", months);
        trend.put("revenue", revenue);

        return ResponseResult.success(trend);
    }

    /**
     * 获取用户分布数据
     */
    @GetMapping("/user-distribution")
    public ResponseResult<?> getUserDistribution() {
        // 获取用户角色分布
        List<Map<String, Object>> distribution = userRoleService.getUserDistribution();

        return ResponseResult.success(distribution);
    }

    /**
     * 获取订单状态分布数据
     */
    @GetMapping("/order-status")
    public ResponseResult<?> getOrderStatus() {
        List<Map<String, Object>> status = orderService.getOrderStatusStats();

        return ResponseResult.success(status);
    }

    /**
     * 获取热门电影数据
     */
    @GetMapping("/popular-movies")
    public ResponseResult<?> getPopularMovies() {
        List<Map<String, Object>> movies = orderService.getPopularMovies(5);

        return ResponseResult.success(movies);
    }
}
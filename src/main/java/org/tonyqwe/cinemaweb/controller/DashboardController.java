package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
     * @param cinemaId 影院ID（可选）
     */
    @GetMapping("/stats")
    public ResponseResult<?> getDashboardStats(@RequestParam(required = false) Long cinemaId) {
        Map<String, Object> stats = new HashMap<>();

        // 总用户数（不受影院影响）
        long totalUsers = userService.count();
        stats.put("totalUsers", totalUsers);

        // 电影数量（不受影院影响）
        long totalMovies = movieService.count();
        stats.put("totalMovies", totalMovies);

        // 订单总数
        long totalOrders = orderService.count(cinemaId);
        stats.put("totalOrders", totalOrders);

        // 总票房
        double totalRevenue = orderService.getTotalRevenue(cinemaId);
        stats.put("totalRevenue", totalRevenue);

        return ResponseResult.success(stats);
    }

    /**
     * 获取票房趋势数据
     * @param cinemaId 影院ID（可选）
     */
    @GetMapping("/revenue-trend")
    public ResponseResult<?> getRevenueTrend(@RequestParam(required = false) Long cinemaId) {
        List<Map<String, Object>> monthlyData = orderService.getMonthlyRevenue(6, cinemaId);

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
        // 获取用户角色分布（不受影院影响）
        List<Map<String, Object>> distribution = userRoleService.getUserDistribution();

        return ResponseResult.success(distribution);
    }

    /**
     * 获取订单状态分布数据
     * @param cinemaId 影院ID（可选）
     */
    @GetMapping("/order-status")
    public ResponseResult<?> getOrderStatus(@RequestParam(required = false) Long cinemaId) {
        List<Map<String, Object>> status = orderService.getOrderStatusStats(cinemaId);

        return ResponseResult.success(status);
    }

    /**
     * 获取热门电影数据
     * @param cinemaId 影院ID（可选）
     */
    @GetMapping("/popular-movies")
    public ResponseResult<?> getPopularMovies(@RequestParam(required = false) Long cinemaId) {
        List<Map<String, Object>> movies = orderService.getPopularMovies(5, cinemaId);

        return ResponseResult.success(movies);
    }
}
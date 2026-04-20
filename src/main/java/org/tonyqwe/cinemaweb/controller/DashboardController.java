package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tonyqwe.cinemaweb.domain.entity.Orders;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.domain.entity.SysUserRole;
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
        // 模拟6个月的票房数据
        List<Integer> revenueData = new ArrayList<>();
        revenueData.add(120000);
        revenueData.add(190000);
        revenueData.add(300000);
        revenueData.add(278000);
        revenueData.add(480000);
        revenueData.add(590000);

        Map<String, Object> trend = new HashMap<>();
        trend.put("months", List.of("1月", "2月", "3月", "4月", "5月", "6月"));
        trend.put("revenue", revenueData);

        return ResponseResult.success(trend);
    }

    /**
     * 获取用户分布数据
     */
    @GetMapping("/user-distribution")
    public ResponseResult<?> getUserDistribution() {
        // 模拟用户类型分布
        List<Map<String, Object>> distribution = new ArrayList<>();
        distribution.add(Map.of("name", "普通用户", "value", 8000));
        distribution.add(Map.of("name", "VIP用户", "value", 3000));
        distribution.add(Map.of("name", "管理员", "value", 1000));
        distribution.add(Map.of("name", "员工", "value", 345));

        return ResponseResult.success(distribution);
    }

    /**
     * 获取订单状态分布数据
     */
    @GetMapping("/order-status")
    public ResponseResult<?> getOrderStatus() {
        // 模拟订单状态分布
        List<Map<String, Object>> status = new ArrayList<>();
        status.add(Map.of("name", "已完成", "value", 4000));
        status.add(Map.of("name", "待支付", "value", 1000));
        status.add(Map.of("name", "已取消", "value", 500));
        status.add(Map.of("name", "退款中", "value", 178));

        return ResponseResult.success(status);
    }

    /**
     * 获取热门电影数据
     */
    @GetMapping("/popular-movies")
    public ResponseResult<?> getPopularMovies() {
        // 模拟热门电影数据
        List<Map<String, Object>> movies = new ArrayList<>();
        movies.add(Map.of("name", "电影A", "revenue", 350000));
        movies.add(Map.of("name", "电影B", "revenue", 280000));
        movies.add(Map.of("name", "电影C", "revenue", 220000));
        movies.add(Map.of("name", "电影D", "revenue", 180000));
        movies.add(Map.of("name", "电影E", "revenue", 150000));

        return ResponseResult.success(movies);
    }
}
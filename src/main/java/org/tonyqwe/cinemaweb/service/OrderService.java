package org.tonyqwe.cinemaweb.service;

import org.tonyqwe.cinemaweb.domain.dto.OrderRequest;
import org.tonyqwe.cinemaweb.domain.entity.Orders;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     * @param request 订单请求DTO
     * @param userId 用户ID
     * @return 订单信息
     */
    Orders createOrder(OrderRequest request, Long userId);

    /**
     * 获取用户的订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    List<Orders> getOrdersByUserId(Long userId);

    /**
     * 获取订单详情
     * @param orderId 订单ID
     * @return 订单信息
     */
    Orders getOrderById(Long orderId);

    /**
     * 取消订单
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean cancelOrder(Long orderId, Long userId);

    /**
     * 支付订单
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param paymentMethod 支付方式
     * @return 是否成功
     */
    boolean payOrder(Long orderId, Long userId, String paymentMethod);

    /**
     * 获取订单总数
     */
    long count();

    /**
     * 获取总票房
     */
    double getTotalRevenue();

    /**
     * 按月获取票房统计
     * @param months 月份数
     * @return 每月票房列表
     */
    java.util.List<java.util.Map<String, Object>> getMonthlyRevenue(int months);

    /**
     * 按状态获取订单统计
     * @return 各状态订单数量
     */
    java.util.List<java.util.Map<String, Object>> getOrderStatusStats();

    /**
     * 获取热门电影
     * @param limit 返回数量
     * @return 热门电影列表
     */
    java.util.List<java.util.Map<String, Object>> getPopularMovies(int limit);

    /**
     * 获取所有订单
     * @return 订单列表
     */
    List<Orders> getAllOrders();

    /**
     * 根据影院ID获取订单
     * @param cinemaId 影院ID
     * @return 订单列表
     */
    List<Orders> getOrdersByCinemaId(Long cinemaId);

    /**
     * 获取订单总数（可按影院筛选）
     * @param cinemaId 影院ID（可选）
     * @return 订单数量
     */
    long count(Long cinemaId);

    /**
     * 获取总票房（可按影院筛选）
     * @param cinemaId 影院ID（可选）
     * @return 总票房
     */
    double getTotalRevenue(Long cinemaId);

    /**
     * 按月获取票房统计（可按影院筛选）
     * @param months 月份数
     * @param cinemaId 影院ID（可选）
     * @return 每月票房列表
     */
    java.util.List<java.util.Map<String, Object>> getMonthlyRevenue(int months, Long cinemaId);

    /**
     * 按状态获取订单统计（可按影院筛选）
     * @param cinemaId 影院ID（可选）
     * @return 各状态订单数量
     */
    java.util.List<java.util.Map<String, Object>> getOrderStatusStats(Long cinemaId);

    /**
     * 获取热门电影（可按影院筛选）
     * @param limit 返回数量
     * @param cinemaId 影院ID（可选）
     * @return 热门电影列表
     */
    java.util.List<java.util.Map<String, Object>> getPopularMovies(int limit, Long cinemaId);
}

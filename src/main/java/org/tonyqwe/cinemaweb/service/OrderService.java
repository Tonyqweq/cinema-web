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
}

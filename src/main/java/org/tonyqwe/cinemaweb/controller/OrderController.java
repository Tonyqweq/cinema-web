package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.OrderRequest;
import org.tonyqwe.cinemaweb.domain.entity.Orders;
import org.tonyqwe.cinemaweb.service.OrderService;
import org.tonyqwe.cinemaweb.service.TokenService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private TokenService tokenService;

    /**
     * 创建订单
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<ResponseResult<Orders>> createOrder(@RequestBody OrderRequest request, HttpServletRequest httpRequest) {
        try {
            // 从请求头中获取token
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 解析token获取用户ID
            Long userId = tokenService.getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.ok(ResponseResult.error(401, "未登录或登录已过期"));
            }

            // 创建订单
            Orders order = orderService.createOrder(request, userId);
            return ResponseEntity.ok(ResponseResult.success(order));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseResult.error(500, e.getMessage()));
        }
    }

    /**
     * 获取用户的订单列表
     * GET /api/orders
     */
    @GetMapping
    public ResponseEntity<ResponseResult<List<Orders>>> getOrders(HttpServletRequest httpRequest) {
        try {
            // 从请求头中获取token
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 解析token获取用户ID
            Long userId = tokenService.getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.ok(ResponseResult.error(401, "未登录或登录已过期"));
            }

            // 获取订单列表
            List<Orders> orders = orderService.getOrdersByUserId(userId);
            return ResponseEntity.ok(ResponseResult.success(orders));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseResult.error(500, e.getMessage()));
        }
    }

    /**
     * 获取订单详情
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseResult<Orders>> getOrderById(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            // 从请求头中获取token
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 解析token获取用户ID
            Long userId = tokenService.getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.ok(ResponseResult.error(401, "未登录或登录已过期"));
            }

            // 获取订单详情
            Orders order = orderService.getOrderById(id);
            if (order == null) {
                return ResponseEntity.ok(ResponseResult.error(404, "订单不存在"));
            }

            // 验证订单是否属于当前用户
            if (!order.getUserId().equals(userId)) {
                return ResponseEntity.ok(ResponseResult.error(403, "无权访问该订单"));
            }

            return ResponseEntity.ok(ResponseResult.success(order));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseResult.error(500, e.getMessage()));
        }
    }

    /**
     * 取消订单
     * PUT /api/orders/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ResponseResult<Boolean>> cancelOrder(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            // 从请求头中获取token
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 解析token获取用户ID
            Long userId = tokenService.getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.ok(ResponseResult.error(401, "未登录或登录已过期"));
            }

            // 取消订单
            boolean result = orderService.cancelOrder(id, userId);
            return ResponseEntity.ok(ResponseResult.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseResult.error(500, e.getMessage()));
        }
    }

    /**
     * 支付订单
     * PUT /api/orders/{id}/pay
     */
    @PutMapping("/{id}/pay")
    public ResponseEntity<ResponseResult<Boolean>> payOrder(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            // 从请求头中获取token
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 解析token获取用户ID
            Long userId = tokenService.getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.ok(ResponseResult.error(401, "未登录或登录已过期"));
            }

            // 支付订单
            boolean result = orderService.payOrder(id, userId);
            return ResponseEntity.ok(ResponseResult.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseResult.error(500, e.getMessage()));
        }
    }
}

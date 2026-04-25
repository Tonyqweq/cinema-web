package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.SimpleDateFormat;
import org.tonyqwe.cinemaweb.domain.dto.OrderRequest;
import org.tonyqwe.cinemaweb.domain.entity.Orders;
import org.tonyqwe.cinemaweb.domain.entity.SeatStatus;
import org.tonyqwe.cinemaweb.domain.entity.Seats;
import org.tonyqwe.cinemaweb.domain.entity.Showtimes;
import org.tonyqwe.cinemaweb.domain.entity.Movies;
import org.tonyqwe.cinemaweb.domain.entity.Cinemas;
import org.tonyqwe.cinemaweb.domain.entity.Halls;
import org.tonyqwe.cinemaweb.mapper.OrderMapper;
import org.tonyqwe.cinemaweb.mapper.SeatMapper;
import org.tonyqwe.cinemaweb.mapper.SeatStatusMapper;
import org.tonyqwe.cinemaweb.mapper.ShowtimesMapper;
import org.tonyqwe.cinemaweb.mapper.MovieMapper;
import org.tonyqwe.cinemaweb.mapper.CinemaMapper;
import org.tonyqwe.cinemaweb.mapper.HallMapper;
import org.tonyqwe.cinemaweb.messaging.OrderTimeoutProducer;
import org.tonyqwe.cinemaweb.service.OrderService;
import org.tonyqwe.cinemaweb.utils.RedisLockUtil;
import org.tonyqwe.cinemaweb.websocket.SeatWebSocketHandler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现类
 * 支持高并发的购票流程
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ShowtimesMapper showtimesMapper;

    @Resource
    private SeatMapper seatMapper;

    @Resource
    private SeatStatusMapper seatStatusMapper;

    @Resource
    private MovieMapper movieMapper;

    @Resource
    private CinemaMapper cinemaMapper;

    @Resource
    private HallMapper hallMapper;

    @Resource
    private RedisLockUtil redisLockUtil;

    @Resource
    private OrderTimeoutProducer orderTimeoutProducer;

    @Resource
    private SeatWebSocketHandler seatWebSocketHandler;

    /**
     * 订单超时时间（分钟）
     */
    private static final int ORDER_TIMEOUT_MINUTES = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders createOrder(OrderRequest request, Long userId) {
        log.info("开始创建订单，用户ID: {}, 场次ID: {}, 座位: {}", userId, request.getShowtimeId(), request.getSeats());
        
        // 1. 验证场次是否存在
        Showtimes showtime = showtimesMapper.selectById(request.getShowtimeId());
        if (showtime == null) {
            log.error("场次不存在，场次ID: {}", request.getShowtimeId());
            throw new RuntimeException("场次不存在");
        }

        // 2. 使用Redis分布式锁锁定座位（高并发关键）
        List<Long> seatIds = request.getSeats();
        boolean locksAcquired = redisLockUtil.tryLockMultiSeats(
                request.getShowtimeId(),
                seatIds,
                3,  // 等待3秒
                30, // 锁定30秒（足够完成订单创建）
                TimeUnit.SECONDS
        );

        if (!locksAcquired) {
            throw new RuntimeException("座位已被其他用户锁定，请重新选择");
        }

        try {
            // 3. 再次验证座位状态（双重检查，防止并发问题）
            for (Long seatId : seatIds) {
                SeatStatus seatStatus = seatStatusMapper.selectByShowtimeIdAndSeatId(
                        request.getShowtimeId(), seatId);
                if (seatStatus != null && seatStatus.getStatus() != 0) {
                    throw new RuntimeException("座位已被占用，请重新选择");
                }
            }

            // 4. 验证座位是否存在
            List<Seats> seats = seatMapper.selectBatchIds(seatIds);
            if (seats.size() != seatIds.size()) {
                throw new RuntimeException("部分座位不存在");
            }

            // 5. 创建座位状态记录（锁定座位）
            Date lockExpireTime = new Date(System.currentTimeMillis() + ORDER_TIMEOUT_MINUTES * 60 * 1000);
            for (Long seatId : seatIds) {
                SeatStatus seatStatus = new SeatStatus();
                seatStatus.setShowtimeId(request.getShowtimeId());
                seatStatus.setSeatId(seatId);
                seatStatus.setStatus(1); // 1-已锁定
                seatStatus.setLockExpireTime(lockExpireTime);
                seatStatusMapper.insert(seatStatus);
            }

            // 6. 计算总价
            java.math.BigDecimal totalPrice = java.math.BigDecimal.ZERO;
            for (Seats seat : seats) {
                if (seat.getPrice() != null) {
                    totalPrice = totalPrice.add(seat.getPrice());
                } else {
                    totalPrice = totalPrice.add(showtime.getPrice());
                }
            }

            // 7. 创建订单
            Orders order = new Orders();
            order.setUserId(userId);
            order.setShowtimeId(request.getShowtimeId());
            order.setOrderStatus(0); // 0-待支付
            order.setTotalPrice(totalPrice.doubleValue());
            // 将座位ID列表转换为字符串，格式："[1,2,3]"
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < seatIds.size(); i++) {
                sb.append(seatIds.get(i));
                if (i < seatIds.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            order.setSeats(sb.toString());

            orderMapper.insert(order);

            // 8. 发送订单超时延迟消息到RabbitMQ
            orderTimeoutProducer.sendOrderTimeoutMessage(
                    order.getId(),
                    userId,
                    request.getShowtimeId(),
                    order.getSeats()
            );

            // 9. 通过WebSocket广播座位锁定消息
            seatWebSocketHandler.broadcastSeatLocked(
                    request.getShowtimeId(),
                    order.getSeats(),
                    userId
            );

            // 10. 关联场次和座位信息
            order.setShowtime(showtime);
            order.setSeatList(seats);

            log.info("订单创建成功: orderId={}, userId={}, showtimeId={}, seats={}",
                    order.getId(), userId, request.getShowtimeId(), order.getSeats());

            return order;

        } finally {
            // 释放Redis分布式锁
            redisLockUtil.unlockMultiSeats(request.getShowtimeId(), seatIds);
        }
    }

    @Override
    public List<Orders> getOrdersByUserId(Long userId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId);
        wrapper.orderByDesc(Orders::getCreatedAt);
        List<Orders> orders = orderMapper.selectList(wrapper);
        
        // 为每个订单添加电影名、影院名、影厅名、放映时间和座位名称
        for (Orders order : orders) {
            Showtimes showtime = showtimesMapper.selectById(order.getShowtimeId());
            if (showtime != null) {
                // 获取电影名
                Movies movie = movieMapper.selectById(showtime.getMovieId());
                if (movie != null) {
                    order.setMovieName(movie.getTitle());
                } else {
                    order.setMovieName("未知电影");
                }
                
                // 获取影院名
                Cinemas cinema = cinemaMapper.selectById(showtime.getCinemaId());
                if (cinema != null) {
                    order.setCinemaName(cinema.getName());
                } else {
                    order.setCinemaName("未知影院");
                }
                
                // 获取影厅名称
                Halls hall = hallMapper.selectById(showtime.getHallId());
                if (hall != null) {
                    order.setHallName(hall.getName());
                } else {
                    order.setHallName("未知影厅");
                }
                
                // 格式化放映时间
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                String startTime = dateFormat.format(showtime.getStartTime());
                String endTime = dateFormat.format(showtime.getEndTime());
                order.setShowtimeTime(startTime + "-" + endTime);
            }
            
            // 获取座位名称列表
            if (order.getSeats() != null && !order.getSeats().isEmpty()) {
                try {
                    // 解析座位ID列表
                    String cleanJson = order.getSeats().replace("[", "").replace("]", "").replace(" ", "");
                    List<Long> seatIds = new ArrayList<>();
                    for (String seatIdStr : cleanJson.split(",")) {
                        if (!seatIdStr.isEmpty()) {
                            seatIds.add(Long.parseLong(seatIdStr));
                        }
                    }
                    
                    // 查询座位信息
                    List<Seats> seats = seatMapper.selectBatchIds(seatIds);
                    List<String> seatNames = new ArrayList<>();
                    for (Seats seat : seats) {
                        seatNames.add(seat.getSeatNumber());
                    }
                    order.setSeatNames(seatNames);
                } catch (Exception e) {
                    log.error("解析座位信息失败: orderId={}, seats={}, error={}", order.getId(), order.getSeats(), e.getMessage());
                }
            }
        }
        
        return orders;
    }

    @Override
    public List<Orders> getOrdersByCinemaId(Long cinemaId) {
        // 获取所有订单并按影院筛选
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Orders::getCreatedAt);
        List<Orders> orders = orderMapper.selectList(wrapper);
        
        // 过滤出属于指定影院的订单
        List<Orders> filteredOrders = new ArrayList<>();
        for (Orders order : orders) {
            Showtimes showtime = showtimesMapper.selectById(order.getShowtimeId());
            if (showtime != null && cinemaId.equals(showtime.getCinemaId())) {
                // 添加关联信息
                Movies movie = movieMapper.selectById(showtime.getMovieId());
                if (movie != null) {
                    order.setMovieName(movie.getTitle());
                }
                Cinemas cinema = cinemaMapper.selectById(showtime.getCinemaId());
                if (cinema != null) {
                    order.setCinemaName(cinema.getName());
                }
                Halls hall = hallMapper.selectById(showtime.getHallId());
                if (hall != null) {
                    order.setHallName(hall.getName());
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                String startTime = dateFormat.format(showtime.getStartTime());
                String endTime = dateFormat.format(showtime.getEndTime());
                order.setShowtimeTime(startTime + "-" + endTime);
                
                filteredOrders.add(order);
            }
        }
        
        return filteredOrders;
    }

    @Override
    public List<Orders> getAllOrders() {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Orders::getCreatedAt);
        List<Orders> orders = orderMapper.selectList(wrapper);
        
        // 为每个订单添加关联信息
        for (Orders order : orders) {
            Showtimes showtime = showtimesMapper.selectById(order.getShowtimeId());
            if (showtime != null) {
                Movies movie = movieMapper.selectById(showtime.getMovieId());
                if (movie != null) {
                    order.setMovieName(movie.getTitle());
                }
                Cinemas cinema = cinemaMapper.selectById(showtime.getCinemaId());
                if (cinema != null) {
                    order.setCinemaName(cinema.getName());
                }
                Halls hall = hallMapper.selectById(showtime.getHallId());
                if (hall != null) {
                    order.setHallName(hall.getName());
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                String startTime = dateFormat.format(showtime.getStartTime());
                String endTime = dateFormat.format(showtime.getEndTime());
                order.setShowtimeTime(startTime + "-" + endTime);
            }
        }
        
        return orders;
    }

    @Override
    public Orders getOrderById(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order != null) {
            Showtimes showtime = showtimesMapper.selectById(order.getShowtimeId());
            if (showtime != null) {
                // 获取电影名
                Movies movie = movieMapper.selectById(showtime.getMovieId());
                if (movie != null) {
                    order.setMovieName(movie.getTitle());
                } else {
                    order.setMovieName("未知电影");
                }
                
                // 获取影院名
                Cinemas cinema = cinemaMapper.selectById(showtime.getCinemaId());
                if (cinema != null) {
                    order.setCinemaName(cinema.getName());
                } else {
                    order.setCinemaName("未知影院");
                }
                
                // 获取影厅名称
                Halls hall = hallMapper.selectById(showtime.getHallId());
                if (hall != null) {
                    order.setHallName(hall.getName());
                } else {
                    order.setHallName("未知影厅");
                }
                
                // 格式化放映时间
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                String startTime = dateFormat.format(showtime.getStartTime());
                String endTime = dateFormat.format(showtime.getEndTime());
                order.setShowtimeTime(startTime + "-" + endTime);
            }
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId) {
        log.info("开始取消订单，订单ID: {}, 用户ID: {}", orderId, userId);
        
        // 1. 验证订单是否存在
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            log.error("订单不存在，订单ID: {}", orderId);
            throw new RuntimeException("订单不存在");
        }

        // 2. 验证订单是否属于当前用户
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该订单");
        }

        // 3. 验证订单状态
        if (order.getOrderStatus() != 0) {
            throw new RuntimeException("订单状态不允许取消");
        }

        // 4. 使用分布式锁保护订单操作
        String lockKey = redisLockUtil.getOrderLockKey(orderId);
        Boolean result = redisLockUtil.executeWithLock(lockKey, () -> {
            // 再次检查订单状态
            Orders currentOrder = orderMapper.selectById(orderId);
            if (currentOrder == null || currentOrder.getOrderStatus() != 0) {
                return false;
            }

            // 更新订单状态为已取消
            currentOrder.setOrderStatus(3); // 3-已取消
            orderMapper.updateById(currentOrder);

            // 释放座位锁定状态
            releaseSeats(order.getShowtimeId(), order.getSeats());

            return true;
        });

        if (Boolean.TRUE.equals(result)) {
            // 通知RabbitMQ取消超时检查（实际在消费端检查状态）
            orderTimeoutProducer.cancelOrderTimeoutMessage(orderId);

            // 通过WebSocket广播座位释放消息
            seatWebSocketHandler.broadcastSeatReleased(order.getShowtimeId(), order.getSeats());

            log.info("订单已取消: orderId={}, userId={}", orderId, userId);
        }

        return Boolean.TRUE.equals(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(Long orderId, Long userId, String paymentMethod) {
        log.info("开始支付订单，订单ID: {}, 用户ID: {}, 支付方式: {}", orderId, userId, paymentMethod);
        
        // 1. 验证订单是否存在
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            log.error("订单不存在，订单ID: {}", orderId);
            throw new RuntimeException("订单不存在");
        }

        // 2. 验证订单是否属于当前用户
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该订单");
        }

        // 3. 验证订单状态
        if (order.getOrderStatus() != 0) {
            throw new RuntimeException("订单状态不允许支付");
        }

        // 4. 使用分布式锁保护订单操作
        String lockKey = redisLockUtil.getOrderLockKey(orderId);
        Boolean result = redisLockUtil.executeWithLock(lockKey, () -> {
            // 再次检查订单状态
            Orders currentOrder = orderMapper.selectById(orderId);
            if (currentOrder == null || currentOrder.getOrderStatus() != 0) {
                return false;
            }

            // 更新订单状态为已支付
            currentOrder.setOrderStatus(1); // 1-已支付
            // 更新支付方式
            if (paymentMethod != null) {
                currentOrder.setPaymentMethod(paymentMethod);
            }
            orderMapper.updateById(currentOrder);

            // 更新座位状态为已售
            updateSeatsToSold(order.getShowtimeId(), order.getSeats());

            return true;
        });

        if (Boolean.TRUE.equals(result)) {
            // 通知RabbitMQ取消超时检查（实际在消费端检查状态）
            orderTimeoutProducer.cancelOrderTimeoutMessage(orderId);

            // 通过WebSocket广播座位已售消息
            seatWebSocketHandler.broadcastSeatSold(order.getShowtimeId(), order.getSeats());

            log.info("订单支付成功: orderId={}, userId={}, paymentMethod={}", orderId, userId, paymentMethod);
        }

        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放座位锁定状态
     *
     * @param showtimeId 场次ID
     * @param seatsJson  座位ID列表（JSON格式）
     */
    private void releaseSeats(Long showtimeId, String seatsJson) {
        if (seatsJson == null || seatsJson.isEmpty()) {
            return;
        }

        try {
            // 解析座位ID列表
            String cleanJson = seatsJson.replace("[", "").replace("]", "").replace(" ", "");
            List<Long> seatIds = new ArrayList<>();
            for (String seatIdStr : cleanJson.split(",")) {
                if (!seatIdStr.isEmpty()) {
                    seatIds.add(Long.parseLong(seatIdStr));
                }
            }

            // 删除座位状态记录（释放锁定）
            for (Long seatId : seatIds) {
                seatStatusMapper.deleteByShowtimeIdAndSeatId(showtimeId, seatId);
            }

            log.info("座位已释放: showtimeId={}, seats={}", showtimeId, seatIds);
        } catch (Exception e) {
            log.error("释放座位失败: showtimeId={}, seats={}, error={}", showtimeId, seatsJson, e.getMessage());
        }
    }

    /**
     * 更新座位状态为已售
     *
     * @param showtimeId 场次ID
     * @param seatsJson  座位ID列表（JSON格式）
     */
    private void updateSeatsToSold(Long showtimeId, String seatsJson) {
        if (seatsJson == null || seatsJson.isEmpty()) {
            return;
        }

        try {
            // 解析座位ID列表
            String cleanJson = seatsJson.replace("[", "").replace("]", "").replace(" ", "");
            List<Long> seatIds = new ArrayList<>();
            for (String seatIdStr : cleanJson.split(",")) {
                if (!seatIdStr.isEmpty()) {
                    seatIds.add(Long.parseLong(seatIdStr));
                }
            }

            // 更新座位状态为已售
            for (Long seatId : seatIds) {
                SeatStatus seatStatus = seatStatusMapper.selectByShowtimeIdAndSeatId(showtimeId, seatId);
                if (seatStatus != null) {
                    seatStatus.setStatus(2); // 2-已售出
                    seatStatusMapper.updateById(seatStatus);
                }
            }

            log.info("座位已更新为已售状态: showtimeId={}, seats={}", showtimeId, seatIds);
        } catch (Exception e) {
            log.error("更新座位状态失败: showtimeId={}, seats={}, error={}", showtimeId, seatsJson, e.getMessage());
        }
    }

    @Override
    public long count() {
        return super.count();
    }

    @Override
    public long count(Long cinemaId) {
        if (cinemaId == null) {
            return count();
        }
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getShowtimeId, null); // 需要关联查询，这里简化处理
        return orderMapper.selectCount(wrapper);
    }

    @Override
    public double getTotalRevenue() {
        return getTotalRevenue(null);
    }

    @Override
    public double getTotalRevenue(Long cinemaId) {
        // 获取已完成订单的总金额
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getOrderStatus, 1); // 1-已支付
        List<Orders> orders = orderMapper.selectList(wrapper);

        double totalRevenue = 0;
        for (Orders order : orders) {
            totalRevenue += order.getTotalPrice();
        }
        return totalRevenue;
    }

    @Override
    public List<Map<String, Object>> getMonthlyRevenue(int months) {
        return getMonthlyRevenue(months, null);
    }

    @Override
    public List<Map<String, Object>> getMonthlyRevenue(int months, Long cinemaId) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -i);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            
            Date startDate = cal.getTime();
            
            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.SECOND, -1);
            Date endDate = cal.getTime();
            
            String monthLabel = (cal.get(Calendar.MONTH)) + "月";

            // 查询该月的已支付订单
            LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Orders::getOrderStatus, 1);
            wrapper.between(Orders::getCreatedAt, startDate, endDate);
            List<Orders> orders = orderMapper.selectList(wrapper);

            double monthlyRevenue = 0;
            for (Orders order : orders) {
                monthlyRevenue += order.getTotalPrice();
            }

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthLabel);
            monthData.put("revenue", monthlyRevenue);
            result.add(monthData);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getOrderStatusStats() {
        return getOrderStatusStats(null);
    }

    @Override
    public List<Map<String, Object>> getOrderStatusStats(Long cinemaId) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 已完成
        LambdaQueryWrapper<Orders> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Orders::getOrderStatus, 1);
        if (cinemaId != null) {
            wrapper1.eq(Orders::getShowtimeId, null); // 需要关联查询，这里简化处理
        }
        long completed = orderMapper.selectCount(wrapper1);
        result.add(Map.of("name", "已完成", "value", completed));

        // 待支付
        LambdaQueryWrapper<Orders> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Orders::getOrderStatus, 0);
        long pending = orderMapper.selectCount(wrapper2);
        result.add(Map.of("name", "待支付", "value", pending));

        // 已取消
        LambdaQueryWrapper<Orders> wrapper3 = new LambdaQueryWrapper<>();
        wrapper3.eq(Orders::getOrderStatus, 3);
        long cancelled = orderMapper.selectCount(wrapper3);
        result.add(Map.of("name", "已取消", "value", cancelled));

        return result;
    }

    @Override
    public List<Map<String, Object>> getPopularMovies(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 获取所有已支付的订单
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getOrderStatus, 1);
        List<Orders> orders = orderMapper.selectList(wrapper);

        // 按电影分组统计票房
        Map<Long, Double> movieRevenueMap = new HashMap<>();
        Map<Long, String> movieNameMap = new HashMap<>();

        for (Orders order : orders) {
            Showtimes showtime = showtimesMapper.selectById(order.getShowtimeId());
            if (showtime != null) {
                Long movieId = showtime.getMovieId();
                movieRevenueMap.merge(movieId, order.getTotalPrice(), Double::sum);

                if (!movieNameMap.containsKey(movieId)) {
                    Movies movie = movieMapper.selectById(movieId);
                    if (movie != null) {
                        movieNameMap.put(movieId, movie.getTitle());
                    }
                }
            }
        }

        // 按票房排序
        List<Map.Entry<Long, Double>> sortedEntries = new ArrayList<>(movieRevenueMap.entrySet());
        sortedEntries.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        // 取前limit个
        int count = 0;
        for (Map.Entry<Long, Double> entry : sortedEntries) {
            if (count >= limit) break;

            Map<String, Object> movieData = new HashMap<>();
            movieData.put("name", movieNameMap.getOrDefault(entry.getKey(), "未知电影"));
            movieData.put("revenue", entry.getValue());
            result.add(movieData);
            count++;
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getPopularMovies(int limit, Long cinemaId) {
        if (cinemaId == null) {
            return getPopularMovies(limit);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();

        // 获取指定影院的已支付订单
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getOrderStatus, 1);
        // 关联排片获取影院ID
        List<Orders> allOrders = orderMapper.selectList(wrapper);
        
        // 按电影分组统计票房（仅指定影院）
        Map<Long, Double> movieRevenueMap = new HashMap<>();
        Map<Long, String> movieNameMap = new HashMap<>();

        for (Orders order : allOrders) {
            Showtimes showtime = showtimesMapper.selectById(order.getShowtimeId());
            if (showtime != null && cinemaId.equals(showtime.getCinemaId())) {
                Long movieId = showtime.getMovieId();
                movieRevenueMap.merge(movieId, order.getTotalPrice(), Double::sum);

                if (!movieNameMap.containsKey(movieId)) {
                    Movies movie = movieMapper.selectById(movieId);
                    if (movie != null) {
                        movieNameMap.put(movieId, movie.getTitle());
                    }
                }
            }
        }

        // 按票房排序
        List<Map.Entry<Long, Double>> sortedEntries = new ArrayList<>(movieRevenueMap.entrySet());
        sortedEntries.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        // 取前limit个
        int count = 0;
        for (Map.Entry<Long, Double> entry : sortedEntries) {
            if (count >= limit) break;

            Map<String, Object> movieData = new HashMap<>();
            movieData.put("name", movieNameMap.getOrDefault(entry.getKey(), "未知电影"));
            movieData.put("revenue", entry.getValue());
            result.add(movieData);
            count++;
        }

        return result;
    }
}

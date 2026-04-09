package org.tonyqwe.cinemaweb.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 用于订单超时处理的延迟队列配置
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 订单超时处理交换机
     */
    public static final String ORDER_TIMEOUT_EXCHANGE = "order.timeout.exchange";

    /**
     * 订单超时处理队列
     */
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";

    /**
     * 订单延迟队列（用于延迟发送超时消息）
     */
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";

    /**
     * 订单超时路由键
     */
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout.routing.key";

    /**
     * 订单延迟路由键
     */
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay.routing.key";

    /**
     * 订单超时时间（毫秒）- 5分钟
     */
    public static final long ORDER_TIMEOUT_MILLIS = 5 * 60 * 1000;

    /**
     * 配置直连交换机
     */
    @Bean
    public DirectExchange orderTimeoutExchange() {
        return new DirectExchange(ORDER_TIMEOUT_EXCHANGE, true, false);
    }

    /**
     * 配置订单超时处理队列
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", ORDER_DELAY_QUEUE)
                .build();
    }

    /**
     * 配置订单延迟队列（死信队列）
     */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .withArgument("x-message-ttl", ORDER_TIMEOUT_MILLIS)
                .withArgument("x-dead-letter-exchange", ORDER_TIMEOUT_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_TIMEOUT_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定超时队列到交换机
     */
    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue())
                .to(orderTimeoutExchange())
                .with(ORDER_TIMEOUT_ROUTING_KEY);
    }

    /**
     * 配置RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

    /**
     * 配置Rabbit事务管理器
     */
    @Bean
    public RabbitTransactionManager rabbitTransactionManager(ConnectionFactory connectionFactory) {
        return new RabbitTransactionManager(connectionFactory);
    }
}

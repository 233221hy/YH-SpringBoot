package cn.xfywz.guozespring.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "exam.score.exchange";
    public static final String QUEUE_NAME = "exam.score.queue";
    public static final String ROUTING_KEY = "exam.score";

    public static final String WORK_EXCHANGE = "work.score.exchange";
    public static final String WORK_QUEUE_NAME = "work.score.queue";
    public static final String WORK_ROUTING_KEY = "work.score";

    // ==================== Exchange ====================
    @Bean
    public DirectExchange examScoreExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    // ==================== 主队列（持久化） ====================
    @Bean
    public Queue examScoreQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .deadLetterExchange("exam.score.dlx.exchange")
                .deadLetterRoutingKey("exam.score.dlx")
                .build();
    }

    // ==================== 死信队列 ====================
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("exam.score.dlx.exchange", true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("exam.score.dlx.queue").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange()).with("exam.score.dlx");
    }

    // ==================== 绑定 ====================
    @Bean
    public Binding examScoreBinding() {
        return BindingBuilder.bind(examScoreQueue())
                .to(examScoreExchange()).with(ROUTING_KEY);
    }

    // ==================== 作业算分 Exchange ====================
    @Bean
    public DirectExchange workScoreExchange() {
        return new DirectExchange(WORK_EXCHANGE, true, false);
    }

    // ==================== 作业算分主队列 ====================
    @Bean
    public Queue workScoreQueue() {
        return QueueBuilder.durable(WORK_QUEUE_NAME)
                .deadLetterExchange("work.score.dlx.exchange")
                .deadLetterRoutingKey("work.score.dlx")
                .build();
    }

    // ==================== 作业算分死信队列 ====================
    @Bean
    public DirectExchange workDeadLetterExchange() {
        return new DirectExchange("work.score.dlx.exchange", true, false);
    }

    @Bean
    public Queue workDeadLetterQueue() {
        return QueueBuilder.durable("work.score.dlx.queue").build();
    }

    @Bean
    public Binding workDeadLetterBinding() {
        return BindingBuilder.bind(workDeadLetterQueue())
                .to(workDeadLetterExchange()).with("work.score.dlx");
    }

    @Bean
    public Binding workScoreBinding() {
        return BindingBuilder.bind(workScoreQueue())
                .to(workScoreExchange()).with(WORK_ROUTING_KEY);
    }

    // ==================== RabbitTemplate（JSON 序列化） ====================
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }

    // ==================== 批量消费容器工厂 ====================
    @Bean("batchContainerFactory")
    public SimpleRabbitListenerContainerFactory batchContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setBatchListener(true);
        factory.setBatchSize(50);
        factory.setReceiveTimeout(500L);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConsumerBatchEnabled(true);
        return factory;
    }
}

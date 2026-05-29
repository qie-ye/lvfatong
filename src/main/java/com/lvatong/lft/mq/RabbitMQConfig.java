package com.lvatong.lft.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "lvatong.exchange";
    public static final String QUEUE_CONTRACT_ANALYSIS = "contract.analysis";
    public static final String QUEUE_CONTRACT_ANALYSIS_RESULT = "contract.analysis.result";
    public static final String ROUTING_KEY_CONTRACT_ANALYSIS = "contract.analysis.start";
    public static final String ROUTING_KEY_CONTRACT_RESULT = "contract.analysis.done";

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue contractAnalysisQueue() {
        return QueueBuilder.durable(QUEUE_CONTRACT_ANALYSIS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", "contract.analysis.dlq")
                .build();
    }

    @Bean
    public Queue contractResultQueue() {
        return QueueBuilder.durable(QUEUE_CONTRACT_ANALYSIS_RESULT).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("contract.analysis.dlq").build();
    }

    @Bean
    public Binding contractAnalysisBinding(Queue contractAnalysisQueue, DirectExchange exchange) {
        return BindingBuilder.bind(contractAnalysisQueue).to(exchange).with(ROUTING_KEY_CONTRACT_ANALYSIS);
    }

    @Bean
    public Binding contractResultBinding(Queue contractResultQueue, DirectExchange exchange) {
        return BindingBuilder.bind(contractResultQueue).to(exchange).with(ROUTING_KEY_CONTRACT_RESULT);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange exchange) {
        return BindingBuilder.bind(deadLetterQueue).to(exchange).with("contract.analysis.dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }
}

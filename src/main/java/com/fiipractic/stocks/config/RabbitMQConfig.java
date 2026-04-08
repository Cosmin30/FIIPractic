package com.fiipractic.stocks.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PRICE_REFRESH_QUEUE = "stock.price.refresh";
    public static final String PRICE_EXCHANGE      = "stock.price.exchange";
    public static final String ROUTING_KEY         = "stock.refresh";

    public static final String DLQ_NAME        = "stock.price.refresh.dlq";
    public static final String DLX_NAME        = "stock.price.dlx";
    public static final String DLX_ROUTING_KEY = "stock.refresh.dead";

    @Bean
    public Queue priceRefreshQueue() {
        return QueueBuilder.durable(PRICE_REFRESH_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange priceRefreshExchange() {
        return new DirectExchange(PRICE_EXCHANGE);
    }

    @Bean
    public Binding priceBinding() {
        return BindingBuilder
                .bind(priceRefreshQueue())
                .to(priceRefreshExchange())
                .with(ROUTING_KEY);
    }
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLX_ROUTING_KEY);
    }


}

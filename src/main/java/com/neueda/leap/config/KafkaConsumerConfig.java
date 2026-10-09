package com.neueda.leap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@Configuration
public class KafkaConsumerConfig {

    // Spring Boot attaches this to the default listener container factory, so every @KafkaListener gets the
    // JSON message converted to the type of its parameter (TradeValidatedDTO, TradeRecordedDTO, ...).
    // A message that can't be converted is logged and skipped rather than retried.
    @Bean
    public RecordMessageConverter messageConverter() {
        return new JsonMessageConverter();
    }
}

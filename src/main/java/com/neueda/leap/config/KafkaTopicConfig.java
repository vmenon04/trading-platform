package com.neueda.leap.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic tradeSubmittedTopic() {
        return new NewTopic("trade.submitted", 1, (short) 1);
    }

    @Bean
    public NewTopic tradeValidatedTopic() {
        return new NewTopic("trade.validated", 1, (short) 1);
    }

    @Bean
    public NewTopic tradeRecordedTopic() {
        return new NewTopic("trade.recorded", 1, (short) 1);
    }

    @Bean
    public NewTopic tradeFinishedTopic() {
        return new NewTopic("trade.finished", 1, (short) 1);
    }
}

package com.neueda.leap.config;

import com.neueda.leap.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic tradeSubmittedTopic() {
        return new NewTopic(KafkaTopics.TRADE_SUBMITTED, 1, (short) 1);
    }

    @Bean
    public NewTopic tradeValidatedTopic() {
        return new NewTopic(KafkaTopics.TRADE_VALIDATED, 1, (short) 1);
    }

    @Bean
    public NewTopic tradeRecordedTopic() {
        return new NewTopic(KafkaTopics.TRADE_RECORDED, 1, (short) 1);
    }

    @Bean
    public NewTopic tradeFinishedTopic() {
        return new NewTopic(KafkaTopics.TRADE_FINISHED, 1, (short) 1);
    }
}

package a.gleb.consumer.configuration;

import a.gleb.common.models.UserMessage;
import a.gleb.consumer.service.UserMessageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class KafkaConfiguration {

    @Bean
    public Consumer<Message<UserMessage>> receiveUserMessage(UserMessageService userMessageService) {
        return userMessageService::receiveUserMessage;
    }
}

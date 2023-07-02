package a.gleb.consumer.configuration;

import a.gleb.common.models.MessageModel;
import a.gleb.consumer.service.MessageHandlerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class KafkaConfiguration {

    @Bean
    public Consumer<MessageModel> receiveMessage(MessageHandlerService messageHandlerService) {
        return messageHandlerService::receiveMessage;
    }
}

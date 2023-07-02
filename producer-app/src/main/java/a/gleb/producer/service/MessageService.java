package a.gleb.producer.service;

import a.gleb.common.models.MessageModel;
import a.gleb.producer.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private static final String BINDING_NAME = "proceedMessage-out-0";

    private final StreamBridge streamBridge;

    public void sendMessage(Message message) {
        streamBridge.send(
                BINDING_NAME,
                MessageModel.builder().login("producer-app").userMessage(message.getMessage()).build()
        );
    }
}

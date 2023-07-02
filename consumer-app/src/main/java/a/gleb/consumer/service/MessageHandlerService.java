package a.gleb.consumer.service;

import a.gleb.common.models.MessageModel;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
public class MessageHandlerService {

    public void receiveMessage(@Valid MessageModel messageModel) {
        log.info("received message from topic: {}", messageModel);
    }
}

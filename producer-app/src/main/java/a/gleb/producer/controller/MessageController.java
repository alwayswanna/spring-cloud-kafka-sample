package a.gleb.producer.controller;

import a.gleb.producer.model.Message;
import a.gleb.producer.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MessageController {
    private static final String RESPONSE_MESSAGE_CONST = "Message was send";

    private final MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody Message message){
        messageService.sendMessage(message);
        return ResponseEntity.ok(RESPONSE_MESSAGE_CONST);
    }
}

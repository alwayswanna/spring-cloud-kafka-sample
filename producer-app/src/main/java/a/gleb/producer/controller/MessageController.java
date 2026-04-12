package a.gleb.producer.controller;

import a.gleb.producer.controller.swagger.MessageControllerSwagger;
import a.gleb.producer.model.request.MessageRequest;
import a.gleb.producer.model.response.MessageResponse;
import a.gleb.producer.service.UserMessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling message producer endpoints.
 * <p>
 * This controller provides an API to publish messages to a Kafka topic via the
 * {@link UserMessageService}. It validates incoming requests and returns a
 * correlation identifier for each successfully sent message.
 * </p>
 *
 * @author [a.gleb]
 * @version 1.0
 * @see UserMessageService
 * @since 2026-04-12
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/message")
@Tag(name = "message-producer-controller")
public class MessageController implements MessageControllerSwagger {

    private final UserMessageService userMessageService;

    @PostMapping("/send")
    public MessageResponse sendMessage(
            @RequestHeader String login,
            @Valid @RequestBody MessageRequest messageRequest
    ) {
        return userMessageService.sendMessage(login, messageRequest);
    }
}
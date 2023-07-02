package a.gleb.producer.controller;

import a.gleb.producer.model.MessageRequest;
import a.gleb.producer.model.MessageResponse;
import a.gleb.producer.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static a.gleb.producer.controller.MessageController.PRODUCER_CONTROLLER_TAG;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = PRODUCER_CONTROLLER_TAG)
public class MessageController {

    static final String PRODUCER_CONTROLLER_TAG = "message-producer-controller";

    private final MessageService messageService;

    @Operation(summary = "Method which send messages to Kafka")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500")
    })
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(@RequestBody MessageRequest messageRequest) {
        return ResponseEntity.ok(messageService.sendMessage(messageRequest));
    }
}

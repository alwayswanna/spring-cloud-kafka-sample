package a.gleb.producer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User response after send message to Kafka.")
public class MessageResponse {

    private static final String RESPONSE_MESSAGE_CONST = "Message was successfully send";

    private String message;

    public static MessageResponse successResponse() {
        return MessageResponse.builder().message(RESPONSE_MESSAGE_CONST).build();
    }
}

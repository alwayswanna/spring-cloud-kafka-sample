package a.gleb.producer.service;

import a.gleb.common.models.UserMessage;
import a.gleb.producer.constant.BindingNameConstant;
import a.gleb.producer.exception.ProducerAppException;
import a.gleb.producer.model.request.MessageRequest;
import a.gleb.producer.model.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service responsible for publishing user messages to a Kafka topic.
 * <p>
 * This service uses {@link StreamBridge} from Spring Cloud Stream to send
 * messages to a configured binding (topic). It enriches the incoming message
 * with a server-side timestamp and a unique correlation ID, then forwards
 * it as a {@link UserMessage} object.
 * </p>
 *
 * <p><b>Processing flow:</b></p>
 * <ol>
 *   <li>Accepts user login and message request.</li>
 *   <li>Builds a {@link UserMessage} with current timestamp.</li>
 *   <li>Generates a random {@link UUID} as correlation ID.</li>
 *   <li>Constructs a Spring Integration {@code Message} with the payload
 *       and the correlation ID as a header.</li>
 *   <li>Sends the message to the Kafka topic via {@link StreamBridge}.</li>
 *   <li>On success, returns a {@link MessageResponse} with a confirmation
 *       string and the correlation ID.</li>
 *   <li>On failure (send result not {@code true}), throws a custom
 *       {@link ProducerAppException}.</li>
 * </ol>
 *
 * <p><b>Error handling:</b> If the Kafka broker is unavailable or the send
 * operation fails, the service throws {@link ProducerAppException} with
 * HTTP status {@code 503 SERVICE_UNAVAILABLE}.</p>
 *
 * @author [a.gleb]
 * @see StreamBridge
 * @see UserMessage
 * @see MessageResponse
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMessageService {

    private final StreamBridge streamBridge;

    /**
     * Sends a message to the topic {@code user-message}.
     *
     * @param login   the login from the request header {@code login}
     * @param request the value of the request body
     * @return {@link MessageResponse} with a correlation ID
     */
    @Transactional
    public MessageResponse sendMessage(String login, MessageRequest request) {
        var userMessage = new UserMessage(login, request.message(), LocalDateTime.now());
        var correlationId = UUID.randomUUID();

        var message = MessageBuilder.withPayload(userMessage)
                .setHeader("correlationId", correlationId)
                .build();

        var result = streamBridge.send(BindingNameConstant.USER_MESSAGE, message);

        if (result) {
            log.info("Message was successfully sent to topic, [binding_name={}, correlation_id={}]",
                    BindingNameConstant.USER_MESSAGE,
                    correlationId
            );

            return new MessageResponse(
                    "Message was sent: %s".formatted(userMessage.toString()),
                    correlationId
            );
        }

        throw new ProducerAppException(HttpStatus.SERVICE_UNAVAILABLE, "Kafka cluster is not available");
    }
}

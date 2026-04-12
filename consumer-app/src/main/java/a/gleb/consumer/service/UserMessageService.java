package a.gleb.consumer.service;

import a.gleb.common.models.UserMessage;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Service responsible for handling incoming Kafka messages containing {@link UserMessage} payloads.
 *
 * <p>Validates the payload using Bean Validation (Jakarta). Any constraint violation throws a
 * {@link ConstraintViolationException}, which causes Spring Cloud Stream to route the message
 * to the Dead Letter Queue (DLQ) without retrying.</p>
 *
 * <p>A custom header {@code correlationId} is required for tracing. If the header is absent
 * or blank, the message is also routed to DLQ.</p>
 *
 * <p>Offset acknowledgment is handled automatically by the framework ({@code ack-mode: record}).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMessageService {

    private final Validator validator;

    public void receiveUserMessage(Message<UserMessage> message) {
        var payload = message.getPayload();
        var headers = message.getHeaders();

        /* validate payload */
        var errors = new BeanPropertyBindingResult(payload, "userMessage");
        validator.validate(payload, errors);
        if (errors.hasErrors()) {
            var errorDetails = errors.getAllErrors().stream()
                    .map(ObjectError::toString)
                    .collect(Collectors.joining(", "));
            log.error("Validation failed: {}", errorDetails);
            throw new ConstraintViolationException("Invalid UserMessage: " + errorDetails, null);
        }

        /* check mandatory correlationId header */
        var correlationIdBytes = (byte[]) headers.get("correlationId");
        if (correlationIdBytes == null || correlationIdBytes.length == 0) {
            throw new IllegalArgumentException("Required header `correlationId` is missing or blank");
        }
        var correlationId = new String(correlationIdBytes, StandardCharsets.UTF_8);

        log.info("Received message: correlationId={}, payload={}", correlationId, payload);
    }
}
package a.gleb.common.models;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;

/**
 * Represents a user message that is sent to or retrieved from a Kafka topic.
 * <p>
 * This model is used by both Kafka producers and consumers to exchange messages
 * in a JSON format. The {@code toString()} method produces a JSON string matching
 * the expected structure for serialization/deserialization.
 * </p>
 *
 * <p><b>Validation constraints:</b>
 * <ul>
 *   <li>{@code login} must not be blank</li>
 *   <li>{@code message} must not be blank</li>
 *   <li>{@code dtCreated} is recommended to be not null (you may add {@code @NotNull} if required)</li>
 * </ul>
 * </p>
 *
 * <p><b>JSON representation (example):</b>
 * <pre>
 * {
 *   "login": "john_doe",¡
 *   "message": "Hello, Kafka!",
 *   "dtCreated": "2025-03-15T10:30:00"
 * }
 * </pre>
 * </p>
 *
 * @param login     the user's login name – must not be blank
 * @param message   the message content – must not be blank
 * @param dtCreated the timestamp when the message was created (typically set by the producer)
 */
public record UserMessage(
        @NotBlank(message = "Login must not be blank")
        String login,

        @NotBlank(message = "Message must not be blank")
        String message,

        @NotNull(message = "Creation timestamp must not be null")
        LocalDateTime dtCreated
) {

    /**
     * Returns the JSON representation of this message.
     * <p>
     * The format matches the structure expected by Kafka consumers and producers.
     * The timestamp is formatted using {@link LocalDateTime#toString()}, which
     * produces an ISO-8601 string (e.g., "2025-03-15T10:30:00").
     * </p>
     *
     * @return a JSON string containing login, message, and dtCreated fields
     */
    @Override
    public @NonNull String toString() {
        return "{\"login\": \"%s\",\"message\": \"%s\",\"dtCreated\": \"%s\"}"
                .formatted(this.login, this.message, this.dtCreated.toString());
    }
}

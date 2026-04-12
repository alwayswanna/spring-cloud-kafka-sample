package a.gleb.producer.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        description = "Response returned after successfully sending a user message to Kafka.",
        example = """
        {
          "message": "Message was sent: UserMessage[login=john.doe, message=Hello Kafka!, timestamp=2026-04-12T10:30:00]",
          "correlationId": "550e8400-e29b-41d4-a716-446655440000"
        }
        """
)
public record MessageResponse(
        @Schema(
                description = "Human-readable confirmation message containing details of the sent message (e.g., login, content, timestamp).",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Message was sent: UserMessage[login=john.doe, message=Hello Kafka!, timestamp=2026-04-12T10:30:00]"
        )
        String message,
        @Schema(
                description = "Unique identifier (UUID) generated for the message to enable tracking and correlation across systems.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                format = "uuid",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID correlationId
) {}

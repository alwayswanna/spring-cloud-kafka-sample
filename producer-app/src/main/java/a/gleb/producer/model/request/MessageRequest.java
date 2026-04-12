package a.gleb.producer.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
        description = "Represents a user message to be sent to the Kafka topic.",
        example = """
                {
                  "message": "Hello, Kafka!"
                }
                """
)
public record MessageRequest(

        @NotBlank(message = "Message must not be blank")
        @Schema(
                description = "The textual content of the user message. Must contain at least one non-whitespace character.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                example = "Hello, Kafka!",
                defaultValue = "N/A"
        )
        String message
) {
}

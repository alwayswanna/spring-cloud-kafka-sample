package a.gleb.producer.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Standard error response")
public record ErrorResponse(
        @Schema(description = "Error message", example = "Invalid request format")
        String message,
        @Schema(description = "Additional error attributes (e.g., timestamp, request path). May be absent or empty",
                nullable = true,
                additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> attributes
) {

    @Schema(description = "Field-specific error details")
    public record FieldErrorResponse(
            @Schema(description = "Name of the field that caused the error", example = "email")
            String field,
            @Schema(description = "Validation error description", example = "must be a valid email address")
            String message
    ) {}
}



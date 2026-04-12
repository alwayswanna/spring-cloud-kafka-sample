package a.gleb.producer.controller.swagger;

import a.gleb.producer.model.request.MessageRequest;
import a.gleb.producer.model.response.ErrorResponse;
import a.gleb.producer.model.response.MessageResponse;
import a.gleb.producer.service.UserMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

public interface MessageControllerSwagger {

    /**
     * Sends a user message to the Kafka topic {@code user-message}.
     * <p>
     * This method accepts a login header and a validated message request body,
     * delegates the sending logic to {@link UserMessageService}, and returns a
     * {@link MessageResponse} containing the correlation ID of the produced record.
     * </p>
     *
     * @param login           the user login extracted from the {@code login} request header,
     *                        used to identify the message sender
     * @param messageRequest  the request body containing the actual message content,
     *                        validated via Jakarta Bean Validation annotations
     * @return a {@link MessageResponse} object that includes a confirmation message
     *         and the generated correlation ID for the sent message
     * @throws a.gleb.producer.exception.ProducerAppException if the message
     *         fails to be sent to the Kafka topic (e.g., due to broker unavailability
     *         or misconfiguration)
     * @see UserMessageService#sendMessage(String, MessageRequest)
     * @see MessageResponse
     */
    @Operation(
            summary = "Send a user message to Kafka",
            parameters = @Parameter(
                    name = "login",
                    description = "User login",
                    required = true,
                    in = ParameterIn.HEADER
            ),
            description = """
                    Accepts a user login header and a message payload, validates the input,
                    publishes the message to the 'user-message' Kafka topic, and returns a
                    unique correlation ID for tracking. The message is enriched with a server-side timestamp.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public MessageResponse sendMessage(
            String login,
            MessageRequest messageRequest
    );
}

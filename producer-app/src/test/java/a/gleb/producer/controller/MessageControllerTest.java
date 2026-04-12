package a.gleb.producer.controller;

import a.gleb.common.models.UserMessage;
import a.gleb.producer.AbstractProducerAppTest;
import a.gleb.producer.model.response.MessageResponse;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static a.gleb.producer.constant.TestConstant.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Tests for API - `/api/v1/message/send`")
class MessageControllerTest extends AbstractProducerAppTest {

    @Test
    @DisplayName("Send message with null body → 400 Bad Request")
    void testOnSendUserMessage_nullBody_returns400() throws Exception {
        mockMvc.perform(post(SEND_ENDPOINT)
                        .content(NULL_MESSAGE_JSON)
                        .header("login", "user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Send message with empty body → 400 Bad Request")
    void testOnSendUserMessage_emptyBody_returns400() throws Exception {
        mockMvc.perform(post(SEND_ENDPOINT)
                        .content(EMPTY_MESSAGE_JSON)
                        .header("login", "user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Send message without login header → 400 Bad Request")
    void testOnSendUserMessage_noLoginHeader_returns400() throws Exception {
        mockMvc.perform(post(SEND_ENDPOINT)
                        .content(EMPTY_MESSAGE_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Send valid message → 200 OK and message consumed with correlationId")
    void testOnSendUserMessage_validRequest_returns200() throws Exception {
        String userLogin = "userLoginForMessage";

        var result = mockMvc.perform(post(SEND_ENDPOINT)
                        .content(VALID_JSON)
                        .header("login", userLogin)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        MessageResponse response = jsonMapper.readValue(result.getResponse().getContentAsByteArray(), MessageResponse.class);
        Assertions.assertNotNull(response.correlationId(), "Correlation ID must not be null");

        var received = consumeMessageFromTopic();
        Headers headers = received.getKey();
        String value = received.getValue();

        // Check correlationId header
        String correlationIdFromHeader = getHeaderValue(headers, "correlationId")
                .orElseThrow(() -> new AssertionError("Header correlationId not found"));
        UUID uuidFromHeader = UUID.fromString(correlationIdFromHeader);
        Assertions.assertEquals(response.correlationId(), uuidFromHeader);

        // Check message body
        UserMessage userMessage = jsonMapper.readValue(value, UserMessage.class);
        Assertions.assertEquals(userLogin, userMessage.login());
        Assertions.assertEquals(response.correlationId().toString(), uuidFromHeader.toString());
    }
}

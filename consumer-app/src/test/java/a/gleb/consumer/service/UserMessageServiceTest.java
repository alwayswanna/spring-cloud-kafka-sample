package a.gleb.consumer.service;

import a.gleb.common.models.UserMessage;
import a.gleb.consumer.AbstractConsumerAppTest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests on consume messages from `user-message` Kafka topic")
public class UserMessageServiceTest extends AbstractConsumerAppTest {

    private static final String TOPIC = "user-message";
    private static final String VALID_LOGIN = "userLogin";
    private static final String VALID_MESSAGE = "Hi, Kafka!";
    public static final LocalDateTime TEST_TIME = LocalDateTime.now();

    @MockitoSpyBean
    private UserMessageService userMessageService;

    @Test
    @DisplayName("Valid message with correlationId → processed successfully by the service")
    void testOnConsumeUserMessage_success() throws Exception {
        var latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return invocation.callRealMethod();
        }).when(userMessageService).receiveUserMessage(any());

        var correlationId = UUID.randomUUID().toString();
        var userMessage = new UserMessage(VALID_LOGIN, VALID_MESSAGE, TEST_TIME);
        sendToUserMessageTopic(userMessage, correlationId);

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        verify(userMessageService, times(1))
                .receiveUserMessage(argThat(msg ->
                        msg != null
                                && VALID_LOGIN.equals(msg.getPayload().login())
                                && VALID_MESSAGE.equals(msg.getPayload().message())
                ));
    }

    @Test
    @DisplayName("Null login field → validation fails → message routed to DLQ with correlationId preserved")
    void testOnConsumeUserMessage_nullLogin_goesToDlq() throws Exception {
        var correlationId = UUID.randomUUID().toString();
        var userMessage = new UserMessage(null, VALID_MESSAGE, TEST_TIME);
        sendToUserMessageTopic(userMessage, correlationId);

        var dlqMessage = consumeMessageFromTopic();
        assertThat(dlqMessage).isNotNull();
        assertThat(dlqMessage.payload().get("login")).isNull();
        assertThat(dlqMessage.payload().get("message")).isEqualTo(userMessage.message());
        assertThat(dlqMessage.correlationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Null message field → validation fails → message routed to DLQ with correlationId preserved")
    void testOnConsumeUserMessage_nullMessage_goesToDlq() throws Exception {
        var correlationId = UUID.randomUUID().toString();
        var userMessage = new UserMessage(VALID_LOGIN, null, TEST_TIME);
        sendToUserMessageTopic(userMessage, correlationId);

        var dlqMessage = consumeMessageFromTopic();
        assertThat(dlqMessage).isNotNull();
        assertThat(dlqMessage.payload().get("login")).isEqualTo(userMessage.login());
        assertThat(dlqMessage.payload().get("message")).isNull();
        assertThat(dlqMessage.correlationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Null dtCreated field → validation fails → message routed to DLQ with correlationId preserved")
    void testOnConsumeUserMessage_nullDtCreated_goesToDlq() throws Exception {
        var correlationId = UUID.randomUUID().toString();
        var userMessage = new UserMessage(VALID_LOGIN, VALID_MESSAGE, null);
        sendToUserMessageTopic(userMessage, correlationId);

        var dlqMessage = consumeMessageFromTopic();
        assertThat(dlqMessage).isNotNull();
        assertThat(dlqMessage.payload().get("login")).isEqualTo(userMessage.login());
        assertThat(dlqMessage.payload().get("message")).isEqualTo(userMessage.message());
        assertThat(dlqMessage.payload().get("dtCreated")).isNull();
        assertThat(dlqMessage.correlationId()).isEqualTo(correlationId);
    }

    private void sendToUserMessageTopic(UserMessage userMessage, String correlationId) {
        var record = new ProducerRecord<String, Object>(TOPIC, userMessage);
        record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }
}
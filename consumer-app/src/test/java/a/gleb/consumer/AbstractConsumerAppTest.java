package a.gleb.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static a.gleb.consumer.constant.TestConstant.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@Testcontainers
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = MOCK)
public abstract class AbstractConsumerAppTest {

    @Container
    static ConfluentKafkaContainer confluentKafkaContainer =
            new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    protected JsonMapper jsonMapper = new JsonMapper();

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Represents a message consumed from the DLQ topic.
     *
     * @param payload       the deserialized payload fields from the original message
     * @param correlationId the {@code correlationId} header value from the original message
     */
    public record DlqMessage(Map<String, Object> payload, String correlationId) {
    }

    @AfterEach
    void cleanDlqTopic() throws Exception {
        recreateTopic(); /* recreate dlq topic after each test */
    }

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", confluentKafkaContainer::getBootstrapServers);
        registry.add("spring.cloud.stream.kafka.binder.brokers", confluentKafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer", ByteArraySerializer.class::getName);
        registry.add("spring.kafka.producer.value-serializer", JacksonJsonSerializer.class::getName);
    }

    /**
     * Polls the DLQ topic and returns the first message as a {@link DlqMessage}.
     *
     * <p>The DLQ message value is expected to be a Spring Integration message envelope:
     * {@code {"payload": {...}, "headers": {"correlationId": "...", ...}}}.
     * This method extracts both the payload map and the {@code correlationId} header.</p>
     *
     * @throws AssertionError if no message arrives within {@link a.gleb.consumer.constant.TestConstant#CONSUME_TIMEOUT}
     */
    protected DlqMessage consumeMessageFromTopic() throws Exception {
        var consumerProps = createConsumerProperties();
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(TOPIC_NAME));
            var future = new CompletableFuture<ConsumerRecord<String, byte[]>>();

            var pollingThread = startPollingThread(consumer, future);

            try {
                var record = future.get(CONSUME_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                return parseDlqRecord(record);
            } catch (TimeoutException e) {
                throw new AssertionError("Message not received [topic=%s, timeout=%s]".formatted(TOPIC_NAME, CONSUME_TIMEOUT));
            } finally {
                consumer.wakeup();
                pollingThread.join(Duration.ofSeconds(1).toMillis());
            }
        }
    }

    /**
     * Parses a raw DLQ {@link ConsumerRecord} into a {@link DlqMessage}.
     *
     * <p><b>Value:</b> tests send the {@link a.gleb.common.models.UserMessage} directly
     * (not wrapped in a Spring Integration envelope), so the Kafka record value is the
     * raw JSON of {@code UserMessage}. Spring Cloud Stream's DLQ producer inherits the
     * application-level {@code JacksonJsonSerializer}, which re-encodes {@code byte[]}
     * values as base64 JSON strings. This method detects that case and decodes it first.</p>
     *
     * <p><b>correlationId:</b> sent as a Kafka record header (byte array), copied verbatim
     * to the DLQ record by Spring Cloud Stream's {@code DeadLetterPublishingRecoverer}.</p>
     */
    private DlqMessage parseDlqRecord(ConsumerRecord<String, byte[]> record) {
        var rootNode = jsonMapper.readTree(record.value());
        if (rootNode.isString()) {
            var originalBytes = Base64.getDecoder().decode(rootNode.stringValue());
            rootNode = jsonMapper.readTree(originalBytes);
        }

        var payload = jsonMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
        });

        var correlationIdHeader = record.headers().lastHeader("correlationId");
        return new DlqMessage(payload,
                correlationIdHeader != null
                        ? new String(correlationIdHeader.value(), StandardCharsets.UTF_8)
                        : null
        );
    }

    private Properties createConsumerProperties() {
        var props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, confluentKafkaContainer.getBootstrapServers());
        props.put(GROUP_ID_CONFIG, CONSUMER_GROUP_PREFIX + UUID.randomUUID());
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        return props;
    }

    private Thread startPollingThread(
            KafkaConsumer<String, byte[]> consumer,
            CompletableFuture<ConsumerRecord<String, byte[]>> future
    ) {
        var thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    var records = consumer.poll(POLL_DURATION);
                    for (var record : records) {
                        future.complete(record);
                        return;
                    }
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        thread.start();
        return thread;
    }

    private void recreateTopic() throws Exception {
        var adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, confluentKafkaContainer.getBootstrapServers());
        try (var admin = AdminClient.create(adminProps)) {
            var deleteResult = admin.deleteTopics(List.of(TOPIC_NAME, "user-message"));
            deleteResult.all().get(30, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        try (var admin = AdminClient.create(adminProps)) {
            var newTopic = new NewTopic(TOPIC_NAME, 1, (short) 1);
            admin.createTopics(List.of(newTopic)).all().get(10, TimeUnit.SECONDS);
        }

        Thread.sleep(500);
    }
}
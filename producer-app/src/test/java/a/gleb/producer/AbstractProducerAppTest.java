package a.gleb.producer;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static a.gleb.producer.constant.TestConstant.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = MOCK)
public abstract class AbstractProducerAppTest {

    @Container
    static ConfluentKafkaContainer confluentKafkaContainer =
            new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    public JsonMapper jsonMapper;

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", confluentKafkaContainer::getBootstrapServers);
        registry.add("spring.cloud.stream.kafka.binder.brokers", confluentKafkaContainer::getBootstrapServers);
    }

    protected Pair<Headers, String> consumeMessageFromTopic() throws Exception {
        var consumerProps = createConsumerProperties();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(TOPIC_NAME));
            CompletableFuture<ConsumerRecord<String, String>> future = new CompletableFuture<>();

            // Start polling in a separate thread, but with the ability to shut down correctly
            Thread pollingThread = startPollingThread(consumer, future);

            try {
                ConsumerRecord<String, String> record = future.get(CONSUME_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                return Pair.of(record.headers(), record.value());
            } catch (TimeoutException e) {
                throw new AssertionError("Message not received [topic=%s, timeout=%s]".formatted(TOPIC_NAME, CONSUME_TIMEOUT));
            } finally {
                consumer.wakeup();   // interrupt poll waiting
                pollingThread.join(Duration.ofSeconds(1).toMillis());
            }
        }
    }

    private Properties createConsumerProperties() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, confluentKafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_PREFIX + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }

    private Thread startPollingThread(KafkaConsumer<String, String> consumer,
                                      CompletableFuture<ConsumerRecord<String, String>> future) {
        Thread thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    var records = consumer.poll(POLL_DURATION);
                    for (ConsumerRecord<String, String> record : records) {
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

    // Helper method to extract a header
    protected Optional<String> getHeaderValue(Headers headers, String key) {
        return Optional.ofNullable(headers.lastHeader(key))
                .map(header -> new String(header.value(), java.nio.charset.StandardCharsets.UTF_8))
                .map(value -> value.replace("\"", ""));  // remove quotes if present
    }
}

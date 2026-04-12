package a.gleb.producer.constant;

import java.time.Duration;

public final class TestConstant {

    public static final String SEND_ENDPOINT = "/api/v1/message/send";
    public static final String VALID_JSON = """
            {
                "message": "Hi, Kafka!"
            }
            """;
    public static final String NULL_MESSAGE_JSON = """
            {
                "message": null
            }
            """;
    public static final String EMPTY_MESSAGE_JSON = """
            {
                "message": ""
            }
            """;
    public static final Duration CONSUME_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration POLL_DURATION = Duration.ofMillis(100);
    public static final String TOPIC_NAME = "user-message";
    public static final String CONSUMER_GROUP_PREFIX = "user-message-group-";

    private TestConstant () {}
}

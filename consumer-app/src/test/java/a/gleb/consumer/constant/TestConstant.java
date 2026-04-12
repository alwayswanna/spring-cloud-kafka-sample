package a.gleb.consumer.constant;

import java.time.Duration;

public final class TestConstant {

    public static final Duration CONSUME_TIMEOUT = Duration.ofSeconds(60);
    public static final Duration POLL_DURATION = Duration.ofMillis(300);
    public static final String TOPIC_NAME = "user-message.dlq";
    public static final String CONSUMER_GROUP_PREFIX = "um-dlq-group-";

    private TestConstant() {}
}

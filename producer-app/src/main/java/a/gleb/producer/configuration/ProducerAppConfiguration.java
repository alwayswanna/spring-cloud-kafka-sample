package a.gleb.producer.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "producer-app",
                description = "API for sending user`s message to Kafka.",
                version = "1"
        )
)
@Configuration
public class ProducerAppConfiguration {
}

package krivokapic.djordjije.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import krivokapic.djordjije.producer.event.MetricEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;


public class KafkaConsumer {
    private static final Logger logger = LogManager.getLogger(KafkaConsumer.class);

    private final ObjectMapper objectMapper;
    private final long pollingInterval;
    private final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer;


    private KafkaConsumer(Properties properties, long pollingInterval) {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        this.pollingInterval = pollingInterval;
        this.consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(properties);
    }


    public void subscribe(String topic) {
        this.consumer.subscribe(Collections.singleton(topic));
    }


    public void consume() {
        try {
            while (true) {
                ConsumerRecords<String, String> records = this.consumer.poll(Duration.ofMillis(this.pollingInterval));
                for (ConsumerRecord<String, String> record : records) {
                    MetricEvent event = this.objectMapper.readValue(record.value(), MetricEvent.class);
                    logger.info("Event ::: Key: {} ::: Value: {} ::: Partition: {} ::: Offset: {}", record.key(), event, record.partition(), record.offset());
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize event", e);
        }
    }


    public static KafkaConsumer.KafkaConsumerBuilder builder() {
        return new KafkaConsumer.KafkaConsumerBuilder();
    }


    public static class KafkaConsumerBuilder {
        private long builderPollingInterval;
        private Properties builderProperties;


        public KafkaConsumer.KafkaConsumerBuilder properties(Properties properties) {
            if (properties == null) {
                throw new IllegalStateException("Properties cannot be null or empty");
            }

            builderProperties = properties;
            return this;
        }


        public KafkaConsumer.KafkaConsumerBuilder pollingInterval(long pollingInterval) {
            if (pollingInterval <= 0) {
                throw new IllegalStateException("Polling interval cannot be <= 0");
            }

            builderPollingInterval = pollingInterval;
            return this;
        }


        public KafkaConsumer build() {
            if (builderProperties == null || builderPollingInterval <= 0) {
                throw new IllegalStateException("Kafka consumer configuration validation failed");
            }

            return new KafkaConsumer(builderProperties, builderPollingInterval);
        }
    }
}

package krivokapic.djordjije.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import krivokapic.djordjije.producer.event.EventFactory;
import krivokapic.djordjije.producer.event.MetricEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public final class KafkaProducer extends BaseProducer<MetricEvent> {
    private static final Logger logger = LogManager.getLogger(KafkaProducer.class);

    private final ObjectMapper objectMapper;

    private final String topic;
    private final org.apache.kafka.clients.producer.KafkaProducer<String, String> producer;


    private KafkaProducer(Properties properties, String topic, EventFactory<MetricEvent> eventFactory, int threads, int maxJitter, TimeUnit timeUnit) {
        super(eventFactory, threads, maxJitter, timeUnit);

        this.topic = topic;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.producer = new org.apache.kafka.clients.producer.KafkaProducer<>(properties);
    }


    @Override
    public void send(MetricEvent event) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(this.topic, event.location(), this.objectMapper.writeValueAsString(event));
            RecordMetadata metadata = producer.send(record).get();
            logger.info("Event ::: {} ::: Sent to topic {} in partition {} with offset {}", event, metadata.topic(), metadata.partition(), metadata.offset());
        } catch (JsonProcessingException e) {
            logger.error("JSON processing exception", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


    @Override
    public void close() {
        super.close();
        this.producer.close();
    }


    public static KafkaProducer.KafkaProducerBuilder builder() {
        return new KafkaProducer.KafkaProducerBuilder();
    }


    public static class KafkaProducerBuilder {
        private String builderTopic;
        private Properties builderProperties;
        private EventFactory<MetricEvent> builderEventFactory;
        private int builderThreads;
        private int builderMaxJitter;
        private TimeUnit builderTimeUnit;


        public KafkaProducer.KafkaProducerBuilder properties(Properties properties) {
            if (properties == null) {
                throw new IllegalStateException("Properties cannot be null or empty");
            }

            builderProperties = properties;
            return this;
        }


        public KafkaProducer.KafkaProducerBuilder topic(String topic) {
            if (topic == null || topic.isBlank()) {
                throw new IllegalStateException("Topic cannot be null or empty");
            }

            builderTopic = topic;
            return this;
        }


        public KafkaProducer.KafkaProducerBuilder eventFactory(EventFactory<MetricEvent> eventFactory) {
            if (eventFactory == null) {
                throw new IllegalStateException("EventFactory cannot be null");
            }

            builderEventFactory = eventFactory;
            return this;
        }


        public KafkaProducer.KafkaProducerBuilder threads(int threads) {
            builderThreads = threads;
            return this;
        }


        public KafkaProducer.KafkaProducerBuilder maxJitter(int maxJitter) {
            builderMaxJitter = maxJitter;
            return this;
        }


        public KafkaProducer.KafkaProducerBuilder timeUnit(TimeUnit timeUnit) {
            if (timeUnit == null) {
                throw new IllegalStateException("Time unit cannot be null");
            }

            builderTimeUnit = timeUnit;
            return this;
        }


        public KafkaProducer build() {
            if (builderProperties == null || builderEventFactory == null || builderThreads == 0 || builderMaxJitter < 0 || builderTimeUnit == null) {
                throw new IllegalStateException("Kafka producer configuration validation failed");
            }

            return new KafkaProducer(
                    builderProperties,
                    builderTopic,
                    builderEventFactory,
                    builderThreads,
                    builderMaxJitter,
                    builderTimeUnit
            );
        }
    }
}

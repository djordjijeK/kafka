package krivokapic.djordjije.producer;

import krivokapic.djordjije.producer.event.MetricEvent;
import krivokapic.djordjije.producer.event.MetricEventFactory;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ProducerApplication {

    public static void main(String[] args) {
        String topic = "metrics";
        String bootstrapServers = "localhost:8881,localhost:8882,localhost:8883";

        Properties producerConfiguration = new Properties();
        producerConfiguration.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfiguration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerConfiguration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerConfiguration.put(ProducerConfig.ACKS_CONFIG, "all");
        producerConfiguration.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerConfiguration.put(ProducerConfig.LINGER_MS_CONFIG, 100);

        Producer<MetricEvent> producer = KafkaProducer.builder()
                .properties(producerConfiguration)
                .topic(topic)
                .eventFactory(MetricEventFactory.create())
                .threads(3)
                .maxJitter(2000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();

        producer.start();

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(producer::getStatistics, 0, 30, TimeUnit.SECONDS);
    }

}
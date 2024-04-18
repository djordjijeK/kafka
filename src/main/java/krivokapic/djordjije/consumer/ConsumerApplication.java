package krivokapic.djordjije.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;


public class ConsumerApplication {

    public static void main(String[] args) {
        String topic = "metrics";
        String bootstrapServers = "localhost:8881,localhost:8882,localhost:8883";

        Properties consumerConfiguration = new Properties();
        consumerConfiguration.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfiguration.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerConfiguration.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerConfiguration.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "KafkaConsumerGroup");
        consumerConfiguration.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        consumerConfiguration.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer kafkaConsumer = KafkaConsumer.builder()
                .properties(consumerConfiguration)
                .pollingInterval(1000)
                .build();

        kafkaConsumer.subscribe(topic);
        kafkaConsumer.consume();
    }

}

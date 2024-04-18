package krivokapic.djordjije;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class KafkaAdminApplication {

    public static void main(String[] args) {
        String topic = "metrics";
        String bootstrapServers = "localhost:8881,localhost:8882,localhost:8883";

        KafkaAdminClient kafkaAdminClient = new KafkaAdminClient(bootstrapServers);
        kafkaAdminClient.createTopic(topic, 6, 3);
        // kafkaAdminClient.increaseTopicPartitions(topic, 10);


        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(kafkaAdminClient::describeTopics, 0, 30, TimeUnit.SECONDS);
    }

}

## Apache ZooKeeper

- ZooKeeper operates in a client-server architecture, where clients are the nodes (applications) that use ZooKeeper to coordinate with each other, and servers are the ZooKeeper
  instances running in a cluster.


- In a ZooKeeper ensemble (cluster), one server acts as the leader, while the others act as followers.
  The leader handles all write requests, ensuring consistent updates across the cluster.
  Followers handle read requests and participate in leader election if the current leader fails.
  The leader makes the update and replicates it across the followers.
  Only after the majority of the servers have acknowledged the update does the leader confirm the write operation to the initiating client.


- ZooKeeper maintains a hierarchical namespace similar to a filesystem (think of it as a tree data structure), with nodes referred to as znodes.
  Znodes can be persistent, remaining in the tree until explicitly deleted, or ephemeral, which automatically disappear when the session that created them ends (e.g., when the
  client disconnects).


- This in-memory data structure is replicated across all servers in the cluster, ensuring high availability and fault tolerance.


- Clients use znodes to store configuration information or to coordinate distributed processes.
  Ephemeral znodes are particularly useful for managing presence and group membership, as they automatically clean up after a client disconnects.
  Clients maintain a session with ZooKeeper, with each session assigned a unique ID.
  Ephemeral nodes are tied to the session that created them, ensuring they exist as long as the session is active.


- When a Kafka broker starts, it registers itself with ZooKeeper, typically in a specific ZooKeeper path (`/brokers/ids/[id]` <- znode).
  This registration includes metadata about the broker, such as its IP address and port, which allows clients and other brokers to discover and communicate with it.


- ZooKeeper stores metadata about Kafka topics, including the number of partitions for each topic, their replication factor, and where the partitions are located (i.e., which
  brokers hold which partitions).


- Kafka uses ZooKeeper to monitor the health and presence of brokers in the cluster.
  If a broker fails, ZooKeeper notifies the other brokers, triggering the leader election process for partitions that were led by the failed broker and reassigning replicas if
  necessary.

------

## Apache Kafka

- Kafka operates on a distributed architecture and is designed primarily for handling high-throughput, real-time data streaming and processing.
  Kafka ensures fault tolerance and scalability by distributing data across a cluster of servers.


- A Kafka cluster is composed of multiple brokers. Each broker is an independent Kafka server instance that can handle read and write operations. Brokers also handle tasks such as
  data replication, request serving, and participating in leader elections for partitions.
  Brokers can be added to a cluster without downtime, allowing the cluster to scale
  horizontally.


- Data in Kafka is organized into topics. Each topic is divided into partitions, which are the basic unit of parallelism in Kafka.
  Each partition is an ordered, immutable sequence
  of records that is continually appended to—a commit log. Partitions allow topics to be parallelized by splitting the data across multiple brokers.


- Kafka replicates partitions across multiple brokers. This replication ensures high availability and durability.
  Each partition has one leader and zero or more followers. All
  write and read operations for a partition go through the leader broker, and followers replicate the leader’s data, staying in sync.


- Producers publish data to topics. Producers are responsible for choosing which record to assign to which partition within the topic, either through a round-robin selection or by
  a partitioning strategy, which can be customized based on the message key.


- Consumers read data from topics.
  Kafka consumers are organized into consumer groups.
  Each consumer within a group reads from exclusive partitions of the topic, ensuring that each
  record is delivered to one consumer in the group.


- Kafka stores the offsets at which a consumer group has been reading.
  The offsets are committed to a Kafka topic named `__consumer_offsets`.
  Storing offsets in Kafka itself allows consumers to pick up where they left off in their data processing after any rebalancing or failures, providing fault tolerance for consumer
  positions.


- Kafka Connect is a framework for connecting Kafka with external systems such as databases, key-value stores, search indexes, and file systems.
  Kafka Streams is a client library for stateful and stateless processing and real-time data processing.

-----------------------

ZooKeeper access:

```bash
$ docker compose --profile cluster up
$ docker container exec --interactive --tty zookeeper-1 bash
$ sh /bin/zookeeper-shell zookeeper-1
$ ls /brokers
[ids, seqid, topics]
$ ls /brokers/ids
[1, 2, 3]
$ get /brokers/ids/1
{
  "features": {},
  "listener_security_protocol_map": {
    "PLAINTEXT": "PLAINTEXT"
  },
  "endpoints": [
    "PLAINTEXT://kafka-1:8881"
  ],
  "jmx_port": -1,
  "port": 8881,
  "host": "kafka-1",
  "version": 5,
  "timestamp": "1712480716931"
}
```

Admin client:

```java
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
```

Producer:

```java
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
    }

}
```

Consumer:

```java
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
```

Scale up cluster:

```bash
$ docker compose --profile scale up
```

Scale down cluster:

```bash
$ docker compose down kafka-4 kafka-5
```

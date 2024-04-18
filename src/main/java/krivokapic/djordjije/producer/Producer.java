package krivokapic.djordjije.producer;

import java.time.Instant;


public interface Producer<T> {
    void send(T event);

    void start();

    Statistics getStatistics();

    record Statistics(long eventsProduced, Instant start, Instant end) {}
}

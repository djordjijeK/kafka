package krivokapic.djordjije.producer.event;


public interface EventFactory<T> {
    T generateNextEvent();
}

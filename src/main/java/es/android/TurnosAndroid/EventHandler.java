package es.android.TurnosAndroid;

/**
 * User: Jesús
 * Date: 2/12/13
 */
public interface EventHandler {
    long getSupportedEventTypes();

    void handleEvent(EventInfo event);

    /**
     * This notifies the handler that the database has changed and it should
     * update its view.
     */
    void eventsChanged();
}

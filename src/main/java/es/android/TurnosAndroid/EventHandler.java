package es.android.TurnosAndroid;

import es.android.TurnosAndroid.model.EventInfo;

/**
 * User: Jesús
 * Date: 2/12/13
 */
public interface EventHandler {
  public long getSupportedEventTypes();

  public void handleEvent(EventInfo event);

  /**
   * This notifies the handler that the database has changed and it should update its view.
   */
  public void eventsChanged();
}

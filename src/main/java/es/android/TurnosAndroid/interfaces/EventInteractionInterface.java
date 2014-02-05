package es.android.TurnosAndroid.interfaces;

import es.android.TurnosAndroid.model.Event;

/**
 * Controls the interaction of the dialogs managing events
 */
public interface EventInteractionInterface {

  /**
   * Called when a new event is saved
   *
   * @param event The event saved
   */
  void onSaveEventClicked(Event event);

  /**
   * Called when the data of a event is updated
   *
   * @param event The event updated
   */
  void onUpdateEventClicked(Event event);

  /**
   * Called when a event is deleted
   *
   * @param event The event deleted
   */
  void onDeleteEventClicked(Event event);
}

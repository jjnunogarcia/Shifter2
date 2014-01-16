package es.android.TurnosAndroid.model;

import android.content.ComponentName;
import android.text.format.Time;
import es.android.TurnosAndroid.views.ViewType;

/**
 * User: Jesús
 * Date: 2/12/13
 */
public class EventInfo {
  public long          eventType; // one of the EventType
  public ViewType      viewType; // one of the ViewType
  public long          id; // event id
  public Time          selectedTime; // the selected time in focus
  public Time          startTime; // start of a range of time.
  public Time          endTime; // end of a range of time.
  public int           x; // x coordinate in the activity space
  public int           y; // y coordinate in the activity space
  public String        query; // query for a user search
  public ComponentName componentName;  // used in combination with query
}

package es.android.TurnosAndroid.model;

import android.text.format.Time;

/**
 * User: Jesús
 * Date: 28/12/13
 */
public class CalendarEvent {
  private int   id;
  private int   day;
  // TODO Insert for every day a list of events instead?
  private Event event;

  public CalendarEvent() {
    String timeZone = Time.getCurrentTimezone();
    Time time = new Time(timeZone);
    time.setToNow();
    time.normalize(true);
    day = Time.getJulianDay(time.toMillis(true), time.gmtoff);
    event = new Event();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getDay() {
    return day;
  }

  public void setDay(int day) {
    this.day = day;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }
}

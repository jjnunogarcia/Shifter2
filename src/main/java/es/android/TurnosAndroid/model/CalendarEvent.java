package es.android.TurnosAndroid.model;

/**
 * User: Jesús
 * Date: 28/12/13
 */
public class CalendarEvent {
  private int   id;
  private long  day;
  private Event event;

  public CalendarEvent() {
    day = System.currentTimeMillis();
    event = new Event();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public long getDay() {
    return day;
  }

  public void setDay(long day) {
    this.day = day;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }
}

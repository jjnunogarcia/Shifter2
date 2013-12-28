package es.android.TurnosAndroid.model;

import java.sql.Date;

/**
 * User: Jesús
 * Date: 28/12/13
 */
public class CalendarEvent {
  private int   id;
  private Date  day;
  private Event event;

  public CalendarEvent() {
    day = new Date(System.currentTimeMillis());
    event = new Event();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Date getDay() {
    return day;
  }

  public void setDay(Date day) {
    this.day = day;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }
}

package es.android.TurnosAndroid.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

/**
 * User: Jesús
 * Date: 28/12/13
 */
public class CalendarEvent implements Parcelable {
  public static final Parcelable.Creator<CalendarEvent> CREATOR = new Parcelable.Creator<CalendarEvent>() {
    @Override
    public CalendarEvent createFromParcel(Parcel in) {
      return new CalendarEvent(in);
    }

    @Override
    public CalendarEvent[] newArray(int size) {
      return new CalendarEvent[size];
    }
  };

  private int   id;
  private int   day;
  private Event event;
  private long  creationTime;

  public CalendarEvent() {
    id = 0;
    String timeZone = Time.getCurrentTimezone();
    Time time = new Time(timeZone);
    time.setToNow();
    time.normalize(true);
    day = Time.getJulianDay(time.toMillis(true), time.gmtoff);
    event = new Event();
    creationTime = System.currentTimeMillis();
  }

  private CalendarEvent(Parcel in) {
    id = in.readInt();
    day = in.readInt();
    event = in.readParcelable(Event.class.getClassLoader());
    creationTime = in.readLong();
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

  public long getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(day);
    dest.writeParcelable(event, flags);
    dest.writeLong(creationTime);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CalendarEvent)) {
      return false;
    }

    CalendarEvent that = (CalendarEvent) o;

    return id == that.id;
  }

  @Override
  public int hashCode() {
    return id;
  }
}

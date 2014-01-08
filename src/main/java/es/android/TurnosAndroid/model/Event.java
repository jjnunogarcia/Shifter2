/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.android.TurnosAndroid.model;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.database.DBConstants;
import es.android.TurnosAndroid.helpers.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: should Event be Parcelable so it can be passed via Intents?
public class Event {
  public static final String SORT_EVENTS_BY = DBConstants.NAME + " ASC";
  // The coordinates of the event rectangle drawn on the screen.
  public  float  left;
  public  float  right;
  public  float  top;
  public  float  bottom;
  // These 4 fields are used for navigating among events within the selected hour in the Day and Week view.
  public  Event  nextRight;
  public  Event  nextLeft;
  public  Event  nextUp;
  public  Event  nextDown;
  private long   id;
  private String name;
  private String description;
  private long   startTime;      // Start and end time are in minutes since midnight
  private long   duration;
  private long   startDay;       // start Julian day
  private long   endDay;         // end Julian day
  private String location;
  private int    color;
  private int    column;
  private int    maxColumns;

  public Event() {
    id = 0;
    name = "";
    description = "";
    startTime = 0;
    duration = 0;
    startDay = 0;
    endDay = 0;
    location = null;
    color = Color.WHITE;
  }

  public Event(Event event) {
    id = event.getId();
    name = event.getName();
    description = event.getDescription();
    startTime = event.getStartTime();
    duration = event.getDuration();
    startDay = event.getStartDay();
    endDay = event.getEndDay();
    location = event.getLocation();
    color = event.getColor();
  }

  /**
   * Loads <i>days</i> days worth of instances starting at <i>startDay</i>.
   */
  public static ArrayList<Event> loadEvents(Context context, int startDay, int days, int requestId, AtomicInteger sequenceNumber) {
    Cursor eventsCursor = null;
    ArrayList<Event> events = new ArrayList<Event>();

    try {
      int endDay = startDay + days - 1;
      Uri.Builder builder = CalendarProvider.EVENTS_URI.buildUpon();
      // TODO are the two following lines really necessary?
      ContentUris.appendId(builder, startDay);
      ContentUris.appendId(builder, endDay);

      eventsCursor = context.getContentResolver().query(builder.build(), DBConstants.EVENTS_PROJECTION, null, null, SORT_EVENTS_BY);

      // Check if we should return early because there are more recent load requests waiting.
      if (requestId != sequenceNumber.get()) {
        return events;
      }

      events = buildEventsFromCursor(eventsCursor, startDay, endDay);
    } finally {
      if (eventsCursor != null) {
        eventsCursor.close();
      }
    }
    return events;
  }

  /**
   * Adds all the events from the cursors to the events list.
   */
  public static ArrayList<Event> buildEventsFromCursor(Cursor cursor, int startDay, int endDay) {
    ArrayList<Event> events = new ArrayList<Event>();

    if (cursor != null) {
      int count = cursor.getCount();

      if (count == 0) {
        return events;
      }

      // Sort events in two passes so we ensure the allday and standard events get sorted in the correct order
      cursor.moveToPosition(-1);
      while (cursor.moveToNext()) {
        Event e = Utils.createEventFromCursor(cursor);
        if (e.startDay <= endDay && e.endDay >= startDay) {
          events.add(e);
        }
      }
    }

    return events;
  }

  public static ArrayList<CalendarEvent> getCalendarEvents(Cursor cursor) {
    ArrayList<CalendarEvent> calendarEvents = new ArrayList<CalendarEvent>();

    if (cursor != null && cursor.getCount() > 0) {
      while (cursor.moveToNext()) {
        calendarEvents.add(createCalendarEventFromCursor(cursor));
      }
    }

    return calendarEvents;
  }

  private static CalendarEvent createCalendarEventFromCursor(Cursor cursor) {
    CalendarEvent calendarEvent = new CalendarEvent();
    calendarEvent.setId(cursor.getInt(cursor.getColumnIndex(DBConstants.ID)));
    long date = cursor.getLong(cursor.getColumnIndex(DBConstants.DATE));
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTime(new Date(date));
    calendarEvent.setEvent(Utils.createEventFromCursor(cursor));

    return calendarEvent;
  }

  /**
   * Computes a position for each event.  Each event is displayed as a non-overlapping rectangle.  For normal events, these rectangles
   * are displayed in separate columns in the week view and day view. For all-day events, these rectangles are displayed in separate rows along
   * the top.  In both cases, each event is assigned two numbers: N, and Max, that specify that this event is the Nth event of Max number of
   * events that are displayed in a group. The width and position of each rectangle depend on the maximum number of rectangles that occur at the same time.
   *
   * @param eventsList the list of events, sorted into increasing time order
   */
  public static void computePositions(ArrayList<Event> eventsList) {
    if (eventsList != null) {
      // Compute the column positions separately for the all-day events
      doComputePositions(eventsList);
      doComputePositions(eventsList);
    }
  }

  private static void doComputePositions(ArrayList<Event> eventsList) {
    final ArrayList<Event> activeList = new ArrayList<Event>();
    final ArrayList<Event> groupList = new ArrayList<Event>();

    long colMask = 0;
    int maxCols = 0;

    for (Event event : eventsList) {
      // If the active list is empty, then reset the max columns, clear the column bit mask, and empty the groupList.
      if (activeList.isEmpty()) {
        for (Event ev : groupList) {
          ev.setMaxColumns(maxCols);
        }
        maxCols = 0;
        colMask = 0;
        groupList.clear();
      }

      // Find the first empty column.  Empty columns are represented by zero bits in the column mask "colMask".
      int col = findFirstZeroBit(colMask);
      if (col == 64) {
        col = 63;
      }
      colMask |= (1L << col);
      event.setColumn(col);
      activeList.add(event);
      groupList.add(event);
      int len = activeList.size();
      if (maxCols < len) {
        maxCols = len;
      }
    }
    for (Event ev : groupList) {
      ev.setMaxColumns(maxCols);
    }
  }

  public static int findFirstZeroBit(long val) {
    for (int i = 0; i < 64; ++i) {
      if ((val & (1L << i)) == 0) {
        return i;
      }
    }
    return 64;
  }

  public int getColumn() {
    return column;
  }

  public void setColumn(int column) {
    this.column = column;
  }

  public int getMaxColumns() {
    return maxColumns;
  }

  public void setMaxColumns(int maxColumns) {
    this.maxColumns = maxColumns;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public long getStartDay() {
    return startDay;
  }

  public void setStartDay(long startDay) {
    this.startDay = startDay;
  }

  public long getEndDay() {
    return endDay;
  }

  public void setEndDay(long endDay) {
    this.endDay = endDay;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }
}

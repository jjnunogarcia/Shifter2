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
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.database.DBConstants;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: should Event be Parcelable so it can be passed via Intents?
public class Event implements Cloneable {
  public static final String[] EVENT_PROJECTION = new String[]{
      DBConstants.ID,
      DBConstants.NAME,
      DBConstants.DESCRIPTION,
      DBConstants.START_TIME,
      DBConstants.DURATION,
      DBConstants.START_DAY,
      DBConstants.END_DAY,
      DBConstants.LOCATION,
      DBConstants.DISPLAY_COLOR
  };
  public static final String   SORT_EVENTS_BY   = DBConstants.NAME + " ASC";
  private static String mNoTitleString;
  private          long   id;
  private         String name;
  private         String description;
  private         long   startTime;      // Start and end time are in minutes since midnight
  private         long   duration;
  private         long   startDay;       // start Julian day
  private         long   endDay;         // end Julian day
  private         String location;
  private         String color;
  // The coordinates of the event rectangle drawn on the screen.
  public         float  left;
  public         float  right;
  public         float  top;
  public         float  bottom;
  // These 4 fields are used for navigating among events within the selected hour in the Day and Week view.
  public         Event  nextRight;
  public         Event  nextLeft;
  public         Event  nextUp;
  public         Event  nextDown;
  private        int    column;
  private        int    maxColumns;

  public Event() {
    id = 0;
    name = "";
    description = "";
    startTime = 0;
    duration = 0;
    startDay = 0;
    endDay = 0;
    location = null;
    color = "";
  }

  /**
   * Loads <i>days</i> days worth of instances starting at <i>startDay</i>.
   */
  public static ArrayList<Event> loadEvents(Context context, int startDay, int days, int requestId, AtomicInteger sequenceNumber) {
    Cursor eventsCursor = null;
    ArrayList<Event> events = new ArrayList<Event>();

    try {
      int endDay = startDay + days - 1;
      Uri.Builder builder = CalendarProvider.CONTENT_URI.buildUpon();
      // TODO are the two following lines really necessary?
      ContentUris.appendId(builder, startDay);
      ContentUris.appendId(builder, endDay);

      eventsCursor = context.getContentResolver().query(builder.build(), EVENT_PROJECTION, null, null, SORT_EVENTS_BY);

      // Check if we should return early because there are more recent load requests waiting.
      if (requestId != sequenceNumber.get()) {
        return events;
      }

      events = buildEventsFromCursor(eventsCursor, context, startDay, endDay);
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
  public static ArrayList<Event> buildEventsFromCursor(Cursor cEvents, Context context, int startDay, int endDay) {
    ArrayList<Event> events = new ArrayList<Event>();

    if (cEvents != null) {
      int count = cEvents.getCount();

      if (count == 0) {
        return events;
      }

      Resources res = context.getResources();
      mNoTitleString = res.getString(R.string.no_title_label);
      // Sort events in two passes so we ensure the allday and standard events get sorted in the correct order
      cEvents.moveToPosition(-1);
      while (cEvents.moveToNext()) {
        Event e = generateEventFromCursor(cEvents);
        if (e.startDay <= endDay && e.endDay >= startDay) {
          events.add(e);
        }
      }
    }

    return events;
  }

  public static ArrayList<Event> getMyEvents(Cursor cursor) {
    ArrayList<Event> events = new ArrayList<Event>();

    if (cursor != null && cursor.getCount() > 0) {
      while (cursor.moveToNext()) {
        events.add(createEventFromCursor(cursor));
      }
    }

    return events;
  }

  private static Event createEventFromCursor(Cursor cursor) {
    Event event = new Event();
    event.id = cursor.getInt(0);
    event.name = cursor.getString(1);
    event.description = cursor.getString(2);
    event.startTime = cursor.getLong(3);
    event.duration = cursor.getLong(4);
    event.startDay = cursor.getLong(5);
    event.endDay = cursor.getLong(6);
    event.location = cursor.getString(7);
    event.color = cursor.getString(8);

    return event;
  }

  /**
   * @param cEvents Cursor pointing at event
   * @return An event created from the cursor
   */
  private static Event generateEventFromCursor(Cursor cEvents) {
    Event event = new Event();

    event.id = cEvents.getLong(7);
    event.name = cEvents.getString(1);
    event.location = cEvents.getString(2);

//        event.id = cEvents.getLong(PROJECTION_EVENT_ID_INDEX);
//        event.title = cEvents.getString(PROJECTION_TITLE_INDEX);
//        event.location = cEvents.getString(PROJECTION_LOCATION_INDEX);
//        event.allDay = cEvents.getInt(PROJECTION_ALL_DAY_INDEX) != 0;
//        event.organizer = cEvents.getString(PROJECTION_ORGANIZER_INDEX);
//        event.guestsCanModify = cEvents.getInt(PROJECTION_GUESTS_CAN_INVITE_OTHERS_INDEX) != 0;

    if (event.name == null || event.name.length() == 0) {
      event.name = mNoTitleString;
    }

//    if (!cEvents.isNull(PROJECTION_COLOR_INDEX)) {
    // Read the color from the database
//      event.color = Utils.getDisplayColorFromColor(cEvents.getInt(PROJECTION_COLOR_INDEX));
//    } else {
//      event.color = mNoColorColor;
//    }

    event.startTime = cEvents.getInt(10);
    event.startDay = cEvents.getInt(8);
    event.endDay = cEvents.getInt(9);

    // Check if this is a repeating event
//        String rrule = cEvents.getString(PROJECTION_RRULE_INDEX);
//        String rdate = cEvents.getString(PROJECTION_RDATE_INDEX);
//        if (!TextUtils.isEmpty(rrule) || !TextUtils.isEmpty(rdate)) {
//            event.isRepeating = true;
//        } else {
//            event.isRepeating = false;
//        }
//
//        event.selfAttendeeStatus = cEvents.getInt(PROJECTION_SELF_ATTENDEE_STATUS_INDEX);
    return event;
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

  @Override
  public final Object clone() throws CloneNotSupportedException {
    super.clone();
    Event event = new Event();
    event.id = id;
    event.name = name;
    event.description = description;
    event.startTime = startTime;
    event.duration = duration;
    event.startDay = startDay;
    event.endDay = endDay;
    event.location = location;
    event.color = color;

    return event;
  }

  public final void copyTo(Event dest) {
    dest.id = id;
    dest.name = name;
    dest.description = description;
    dest.startTime = startTime;
    dest.duration = duration;
    dest.startDay = startDay;
    dest.endDay = endDay;
    dest.location = location;
    dest.color = color;
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

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }
}

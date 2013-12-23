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
import android.provider.CalendarContract.Attendees;
import android.text.format.DateUtils;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.helpers.Utils;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.database.DBConstants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: should Event be Parcelable so it can be passed via Intents?
public class Event implements Cloneable {
  // The projection to use when querying instances to build a list of events
  public static final  String[] EVENT_PROJECTION       = new String[] {
      DBConstants.NAME,
      DBConstants.DISPLAY_COLOR,
      DBConstants.ID,
      DBConstants.START_DAY,
      DBConstants.END_DAY,
      DBConstants.START_TIME,
      DBConstants.DURATION,
      DBConstants.LOCATION,
      DBConstants.DESCRIPTION,
  };
  private static final String   TAG                    = Event.class.getSimpleName();
  /**
   * The sort order is:
   * 1) events with an earlier start (begin for normal events, startday for allday)
   * 2) events with a later end (end for normal events, endday for allday)
   * 3) the title (unnecessary, but nice)
   * <p/>
   * The start and end day is sorted first so that all day events are
   * sorted correctly with respect to events that are >24 hours (and
   * therefore show up in the allday area).
   */
  private static final String   SORT_EVENTS_BY         = "start ASC, end DESC, event ASC";
  // The indices for the projection array above.
  private static final int      PROJECTION_COLOR_INDEX = 3;
  private static String mNoTitleString;
  private static int    mNoColorColor;

  public  long         id;
  public  int          color;
  public  CharSequence title;
  public  CharSequence location;
  public  boolean      allDay;
  public  String       organizer;
  public  boolean      guestsCanModify;
  public  int          startDay;       // start Julian day
  public  int          endDay;         // end Julian day
  public  int          startTime;      // Start and end time are in minutes since midnight
  public  int          endTime;
  public  long         startMillis;   // UTC milliseconds since the epoch
  public  long         endMillis;     // UTC milliseconds since the epoch
  public  boolean      hasAlarm;
  public  boolean      isRepeating;
  public  int          selfAttendeeStatus;
  // The coordinates of the event rectangle drawn on the screen.
  public  float        left;
  public  float        right;
  public  float        top;
  public  float        bottom;
  // These 4 fields are used for navigating among events within the selected hour in the Day and Week view.
  public  Event        nextRight;
  public  Event        nextLeft;
  public  Event        nextUp;
  public  Event        nextDown;
  private int          column;
  private int          maxColumns;

  public Event() {
    id = 0;
    title = null;
    color = 0;
    location = null;
    allDay = false;
    startDay = 0;
    endDay = 0;
    startTime = 0;
    endTime = 0;
    startMillis = 0;
    endMillis = 0;
    hasAlarm = false;
    isRepeating = false;
    selfAttendeeStatus = Attendees.ATTENDEE_STATUS_NONE;
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
      mNoColorColor = res.getColor(R.color.event_center);
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

  /**
   * @param cEvents Cursor pointing at event
   * @return An event created from the cursor
   */
  private static Event generateEventFromCursor(Cursor cEvents) {
    Event event = new Event();

    event.id = cEvents.getLong(7);
    event.title = cEvents.getString(1);
    event.location = cEvents.getString(2);

//        event.id = cEvents.getLong(PROJECTION_EVENT_ID_INDEX);
//        event.title = cEvents.getString(PROJECTION_TITLE_INDEX);
//        event.location = cEvents.getString(PROJECTION_LOCATION_INDEX);
//        event.allDay = cEvents.getInt(PROJECTION_ALL_DAY_INDEX) != 0;
//        event.organizer = cEvents.getString(PROJECTION_ORGANIZER_INDEX);
//        event.guestsCanModify = cEvents.getInt(PROJECTION_GUESTS_CAN_INVITE_OTHERS_INDEX) != 0;

    if (event.title == null || event.title.length() == 0) {
      event.title = mNoTitleString;
    }

    if (!cEvents.isNull(PROJECTION_COLOR_INDEX)) {
      // Read the color from the database
      event.color = Utils.getDisplayColorFromColor(cEvents.getInt(PROJECTION_COLOR_INDEX));
    } else {
      event.color = mNoColorColor;
    }

    long eStart = cEvents.getLong(4);
    long eEnd = cEvents.getLong(5);

//        long eStart = cEvents.getLong(PROJECTION_BEGIN_INDEX);
//        long eEnd = cEvents.getLong(PROJECTION_END_INDEX);

    event.startMillis = eStart;
    event.startTime = cEvents.getInt(10);
//        event.startTime = cEvents.getInt(PROJECTION_START_MINUTE_INDEX);
//        event.startDay = cEvents.getInt(PROJECTION_START_DAY_INDEX);
    event.startDay = cEvents.getInt(8);

    event.endMillis = eEnd;
    event.endTime = cEvents.getInt(11);
//        event.endTime = cEvents.getInt(PROJECTION_END_MINUTE_INDEX);
//        event.endDay = cEvents.getInt(PROJECTION_END_DAY_INDEX);
    event.endDay = cEvents.getInt(9);

//        event.hasAlarm = cEvents.getInt(PROJECTION_HAS_ALARM_INDEX) != 0;

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
   * @param eventsList            the list of events, sorted into increasing time order
   * @param minimumDurationMillis minimum duration acceptable as cell height of each event
   *                              rectangle in millisecond. Should be 0 when it is not determined.
   */
  public static void computePositions(ArrayList<Event> eventsList, long minimumDurationMillis) {
    if (eventsList != null) {
      // Compute the column positions separately for the all-day events
      doComputePositions(eventsList, minimumDurationMillis, false);
      doComputePositions(eventsList, minimumDurationMillis, true);
    }
  }

  private static void doComputePositions(ArrayList<Event> eventsList, long minimumDurationMillis, boolean doAlldayEvents) {
    final ArrayList<Event> activeList = new ArrayList<Event>();
    final ArrayList<Event> groupList = new ArrayList<Event>();

    if (minimumDurationMillis < 0) {
      minimumDurationMillis = 0;
    }

    long colMask = 0;
    int maxCols = 0;

    for (Event event : eventsList) {
      // Process all-day events separately
      if (event.drawAsAllday() != doAlldayEvents) {
        continue;
      }

      if (!doAlldayEvents) {
        colMask = removeNonAlldayActiveEvents(event, activeList.iterator(), minimumDurationMillis, colMask);
      } else {
        colMask = removeAlldayActiveEvents(event, activeList.iterator(), colMask);
      }

      // If the active list is empty, then reset the max columns, clear
      // the column bit mask, and empty the groupList.
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

  private static long removeAlldayActiveEvents(Event event, Iterator<Event> iter, long colMask) {
    // Remove the inactive allday events. An event on the active list
    // becomes inactive when the end day is less than the current event's
    // start day.
    while (iter.hasNext()) {
      final Event active = iter.next();
      if (active.endDay < event.startDay) {
        colMask &= ~(1L << active.getColumn());
        iter.remove();
      }
    }
    return colMask;
  }

  private static long removeNonAlldayActiveEvents(Event event, Iterator<Event> iter, long minDurationMillis, long colMask) {
    long start = event.getStartMillis();
    // Remove the inactive events. An event on the active list
    // becomes inactive when its end time is less than or equal to
    // the current event's start time.
    while (iter.hasNext()) {
      final Event active = iter.next();

      final long duration = Math.max(active.getEndMillis() - active.getStartMillis(), minDurationMillis);
      if ((active.getStartMillis() + duration) <= start) {
        colMask &= ~(1L << active.getColumn());
        iter.remove();
      }
    }
    return colMask;
  }

  public static int findFirstZeroBit(long val) {
    for (int ii = 0; ii < 64; ++ii) {
      if ((val & (1L << ii)) == 0) {
        return ii;
      }
    }
    return 64;
  }

  @Override
  public final Object clone() throws CloneNotSupportedException {
    super.clone();
    Event e = new Event();

    e.title = title;
    e.color = color;
    e.location = location;
    e.allDay = allDay;
    e.startDay = startDay;
    e.endDay = endDay;
    e.startTime = startTime;
    e.endTime = endTime;
    e.startMillis = startMillis;
    e.endMillis = endMillis;
    e.hasAlarm = hasAlarm;
    e.isRepeating = isRepeating;
    e.selfAttendeeStatus = selfAttendeeStatus;
    e.organizer = organizer;
    e.guestsCanModify = guestsCanModify;

    return e;
  }

  public final void copyTo(Event dest) {
    dest.id = id;
    dest.title = title;
    dest.color = color;
    dest.location = location;
    dest.allDay = allDay;
    dest.startDay = startDay;
    dest.endDay = endDay;
    dest.startTime = startTime;
    dest.endTime = endTime;
    dest.startMillis = startMillis;
    dest.endMillis = endMillis;
    dest.hasAlarm = hasAlarm;
    dest.isRepeating = isRepeating;
    dest.selfAttendeeStatus = selfAttendeeStatus;
    dest.organizer = organizer;
    dest.guestsCanModify = guestsCanModify;
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

  public long getStartMillis() {
    return startMillis;
  }

  public long getEndMillis() {
    return endMillis;
  }

  public boolean drawAsAllday() {
    // Use >= so we'll pick up Exchange allday events
    return allDay || endMillis - startMillis >= DateUtils.DAY_IN_MILLIS;
  }
}

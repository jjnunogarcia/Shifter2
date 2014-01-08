/*
 * Copyright (C) 2006 The Android Open Source Project
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

package es.android.TurnosAndroid.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.text.format.Time;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.database.DBConstants;
import es.android.TurnosAndroid.model.CalendarEvent;
import es.android.TurnosAndroid.model.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class Utils {
  // Defines used by the DNA generation code
  static final         int           DAY_IN_MINUTES         = 60 * 24;
  static final         int           WEEK_IN_MINUTES        = DAY_IN_MINUTES * 7;
  // The name of the shared preferences file. This name must be maintained for historical
  // reasons, as it's what PreferenceManager assigned the first time the file was created.
  static final         String        SHARED_PREFS_NAME      = "calendar_preferences";
  private static final String        TAG                    = Utils.class.getSimpleName();
  private static final TimeZoneUtils timeZoneUtils          = new TimeZoneUtils(SHARED_PREFS_NAME);
  // The work day is being counted as 6am to 8pm
  static               int           WORK_DAY_MINUTES       = 14 * 60;
  static               int           WORK_DAY_START_MINUTES = 6 * 60;
  static               int           WORK_DAY_END_MINUTES   = 20 * 60;
  static               int           WORK_DAY_END_LENGTH    = (24 * 60) - WORK_DAY_END_MINUTES;
  static               int           CONFLICT_COLOR         = Color.parseColor("#FF000000");
  static               boolean       minutesLoaded          = false;

  /**
   * Returns whether the SDK is the Jellybean release or later.
   */
  public static boolean isJellybeanOrLater() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
  }

  /**
   * Gets the time zone that Calendar should be displayed in. This is a helper
   * method to get the appropriate time zone for Calendar. If this is the
   * first time this method has been called it will initiate an asynchronous
   * query to verify that the data in preferences is correct. The callback
   * supplied will only be called if this query returns a value other than
   * what is stored in preferences and should cause the calling activity to
   * refresh anything that depends on calling this method.
   *
   * @param context  The calling activity
   * @param callback The runnable that should execute if a query returns new
   *                 values
   * @return The string value representing the time zone Calendar should
   * display
   */
  public static String getTimeZone(Context context, Runnable callback) {
    return timeZoneUtils.getTimeZone(context, callback);
  }

  /**
   * Formats a date or a time range according to the local conventions.
   *
   * @param context     the context is required only if the time is shown
   * @param startMillis the start time in UTC milliseconds
   * @param endMillis   the end time in UTC milliseconds
   * @param flags       a bit mask of options See {@link android.text.format.DateUtils#formatDateRange(android.content.Context, java.util.Formatter,
   *                    long, long, int, String) formatDateRange}
   * @return a string containing the formatted date/time range.
   */
  public static String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
    return timeZoneUtils.formatDateRange(context, startMillis, endMillis, flags);
  }

  /**
   * Returns the week since {@link android.text.format.Time#EPOCH_JULIAN_DAY} (Jan 1, 1970)
   * adjusted for first day of week.
   * <p/>
   * This takes a julian day and the week start day and calculates which
   * week since {@link android.text.format.Time#EPOCH_JULIAN_DAY} that day occurs in, starting
   * at 0. *Do not* use this to compute the ISO week number for the year.
   *
   * @param julianDay      The julian day to calculate the week number for
   * @param firstDayOfWeek Which week day is the first day of the week,
   *                       see {@link android.text.format.Time#SUNDAY}
   * @return Weeks since the epoch
   */
  public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
    int diff = Time.THURSDAY - firstDayOfWeek;
    if (diff < 0) {
      diff += 7;
    }
    int refDay = Time.EPOCH_JULIAN_DAY - diff;
    return (julianDay - refDay) / 7;
  }

  /**
   * Get first day of week as android.text.format.Time constant.
   *
   * @return the first day of week in android.text.format.Time
   */
  public static int getFirstDayOfWeek(Context context) {
//        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
//        String pref = prefs.getString(
//                GeneralPreferences.KEY_WEEK_START_DAY, GeneralPreferences.WEEK_START_DEFAULT);
//
//        int startDay;
//        if (GeneralPreferences.WEEK_START_DEFAULT.equals(pref)) {
//            startDay = Calendar.getInstance().getFirstDayOfWeek();
//        } else {
//            startDay = Integer.parseInt(pref);
//        }

//        if (startDay == Calendar.SATURDAY) {
//            return Time.SATURDAY;
//        } else if (startDay == Calendar.MONDAY) {
    return Time.MONDAY;
//        } else {
//    return Time.SUNDAY;
//        }
  }

  /**
   * @return true when week number should be shown.
   */
  public static boolean getShowWeekNumber(Context context) {
//        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
//        return prefs.getBoolean(
//                GeneralPreferences.KEY_SHOW_WEEK_NUM, GeneralPreferences.DEFAULT_SHOW_WEEK_NUM);
    return false;
  }

  /**
   * Determine whether the column position is Saturday or not.
   *
   * @param column         the column position
   * @param firstDayOfWeek the first day of week in android.text.format.Time
   * @return true if the column is Saturday position
   */
  public static boolean isSaturday(int column, int firstDayOfWeek) {
    return (firstDayOfWeek == Time.SUNDAY && column == 6)
           || (firstDayOfWeek == Time.MONDAY && column == 5)
           || (firstDayOfWeek == Time.SATURDAY && column == 0);
  }

  /**
   * Determine whether the column position is Sunday or not.
   *
   * @param column         the column position
   * @param firstDayOfWeek the first day of week in android.text.format.Time
   * @return true if the column is Sunday position
   */
  public static boolean isSunday(int column, int firstDayOfWeek) {
    return (firstDayOfWeek == Time.SUNDAY && column == 0)
           || (firstDayOfWeek == Time.MONDAY && column == 6)
           || (firstDayOfWeek == Time.SATURDAY && column == 1);
  }

  /**
   * Converts a list of calendarEvents to a list of segments to draw. Assumes list is ordered by start time of the calendarEvents. The function processes calendarEvents for a
   * range of days from firstJulianDay to firstJulianDay + dayXs.length - 1. The algorithm goes over all the calendarEvents and creates a set of segments
   * ordered by start time. This list of segments is then converted into a HashMap of strands which contain the draw points and are organized by color. The strands can
   * then be drawn by setting the paint color to each strand's color and calling drawLines on its set of points. The points are set up using the following parameters.
   * <ul>
   * <li>Events between midnight and WORK_DAY_START_MINUTES are compressed into the first 1/8th of the space between top and bottom.</li>
   * <li>Events between WORK_DAY_END_MINUTES and the following midnight are compressed into the last 1/8th of the space between top and bottom</li>
   * <li>Events between WORK_DAY_START_MINUTES and WORK_DAY_END_MINUTES use the remaining 3/4ths of the space</li>
   * <li>All segments drawn will maintain at least minPixels height, except for conflicts in the first or last 1/8th, which may be smaller</li>
   * </ul>
   *
   * @param firstJulianDay The julian day of the first day of calendarEvents
   * @param calendarEvents A list of calendarEvents sorted by start time
   * @param top            The lowest y value the dna should be drawn at
   * @param bottom         The highest y value the dna should be drawn at
   * @param dayXs          An array of x values to draw the dna at, one for each day
   * @return
   */
  public static HashMap<Integer, EventStrand> createDNAStrands(int firstJulianDay, ArrayList<CalendarEvent> calendarEvents, int top, int bottom, int minPixels, int[] dayXs,
                                                               Context context) {

    if (!minutesLoaded) {
      if (context == null) {
        return null;
      }
      Resources res = context.getResources();
      CONFLICT_COLOR = res.getColor(R.color.month_dna_conflict_time_color);
      WORK_DAY_START_MINUTES = res.getInteger(R.integer.work_start_minutes);
      WORK_DAY_END_MINUTES = res.getInteger(R.integer.work_end_minutes);
      WORK_DAY_END_LENGTH = DAY_IN_MINUTES - WORK_DAY_END_MINUTES;
      WORK_DAY_MINUTES = WORK_DAY_END_MINUTES - WORK_DAY_START_MINUTES;
      minutesLoaded = true;
    }

    if (calendarEvents == null || calendarEvents.isEmpty() || dayXs == null || dayXs.length < 1 || bottom - top < 8 || minPixels < 0) {
      return null;
    }

    LinkedList<EventSegment> segments = new LinkedList<EventSegment>();
    HashMap<Integer, EventStrand> strands = new HashMap<Integer, EventStrand>();
    // add a black strand by default, other colors will get added in the loop
    EventStrand blackStrand = new EventStrand();
    blackStrand.color = CONFLICT_COLOR;
    strands.put(CONFLICT_COLOR, blackStrand);

    // the min length is the number of minutes that will occupy MIN_SEGMENT_PIXELS in the 'work day' time slot. This computes the
    // minutes/pixel * minpx where the number of pixels are 3/4 the total dna height: 4*(mins/(px * 3/4))
    int minMinutes = minPixels * 4 * WORK_DAY_MINUTES / (3 * (bottom - top));

    // There are slightly fewer than half as many pixels in 1/6 the space, so round to 2.5x for the min minutes in the non-work area
    int minOtherMinutes = minMinutes * 5 / 2;
    int lastJulianDay = firstJulianDay + dayXs.length - 1;

    Event event;
    // Go through all the calendarEvents for the week
    for (CalendarEvent currEvent : calendarEvents) {
      // if this event is outside the weeks range skip it
//      if (currEvent.getEndDay() < firstJulianDay || currEvent.getStartDay() > lastJulianDay) {
//        continue;
//      }
//      if (currEvent.drawAsAllday()) {
//        addAllDayToStrands(currEvent, strands, firstJulianDay, dayXs.length);
//        continue;
//      }
      // Copy the event over so we can clip its start and end to our range
      event = new Event(currEvent.getEvent());
//      if (event.getStartDay() < firstJulianDay) {
//        event.setStartDay(firstJulianDay);
//        event.setStartTime(0);
//      }
      // If it starts after the work day make sure the start is at least minPixels from midnight
//      if (event.getStartTime() > DAY_IN_MINUTES - minOtherMinutes) {
//        event.setStartTime(DAY_IN_MINUTES - minOtherMinutes);
//      }
//      if (event.getEndDay() > lastJulianDay) {
//        event.setEndDay(lastJulianDay);
//        event.endTime = DAY_IN_MINUTES - 1;
//      }
      // If the end time is before the work day make sure it ends at least minPixels after midnight
//      if (event.endTime < minOtherMinutes) {
//        event.endTime = minOtherMinutes;
//      }
      // If the start and end are on the same day make sure they are at least minPixels apart. This only needs to be done for times
      // outside the work day as the min distance for within the work day is enforced in the segment code.
//      if (event.startDay == event.endDay && event.endTime - event.startTime < minOtherMinutes) {
      // If it's less than minPixels in an area before the work day
//        if (event.startTime < WORK_DAY_START_MINUTES) {
      // extend the end to the first easy guarantee that it's
      // minPixels
//          event.endTime = Math.min(event.startTime + minOtherMinutes, WORK_DAY_START_MINUTES + minMinutes);
      // if it's in the area after the work day
//        } else if (event.endTime > WORK_DAY_END_MINUTES) {
      // First try shifting the end but not past midnight
//          event.endTime = Math.min(event.endTime + minOtherMinutes, DAY_IN_MINUTES - 1);
      // if it's still too small move the start back
//          if (event.endTime - event.startTime < minOtherMinutes) {
//            event.startTime = event.endTime - minOtherMinutes;
//          }
//        }
//      }

      // This handles adding the first segment
      if (segments.size() == 0) {
        addNewSegment(segments, currEvent, strands);
        continue;
      }
      // Now compare our current start time to the end time of the last
      // segment in the list
      EventSegment lastSegment = segments.getLast();
      long startMinute = (event.getStartDay() - firstJulianDay) * DAY_IN_MINUTES + event.getStartTime();
//      int endMinute = Math.max((event.endDay - firstJulianDay) * DAY_IN_MINUTES + event.endTime, startMinute + minMinutes);

      if (startMinute < 0) {
        startMinute = 0;
      }
//      if (endMinute >= WEEK_IN_MINUTES) {
//        endMinute = WEEK_IN_MINUTES - 1;
//      }
      // If we start before the last segment in the list ends we need to start going through the list as this may conflict with other calendarEvents
      if (startMinute < lastSegment.endMinute) {
        int i = segments.size();
        // find the last segment this event intersects with
//        while (--i >= 0 && endMinute < segments.get(i).startMinute) {
//        }

        EventSegment currSegment;
        // for each segment this event intersects with
        for (; i >= 0 && startMinute <= (currSegment = segments.get(i)).endMinute; i--) {
          // if the segment is already a conflict ignore it
          if (currSegment.color == CONFLICT_COLOR) {
            continue;
          }
          // if the event ends before the segment and wouldn't create a segment that is too small split off the right side
//          if (endMinute < currSegment.endMinute - minMinutes) {
//            EventSegment rhs = new EventSegment();
//            rhs.endMinute = currSegment.endMinute;
//            rhs.color = currSegment.color;
//            rhs.startMinute = endMinute + 1;
//            rhs.day = currSegment.day;
//            currSegment.endMinute = endMinute;
//            segments.add(i + 1, rhs);
//            strands.get(rhs.color).count++;
//          }
          // if the event starts after the segment and wouldn't create a segment that is too small split off the left side
          if (startMinute > currSegment.startMinute + minMinutes) {
            EventSegment lhs = new EventSegment();
            lhs.startMinute = currSegment.startMinute;
            lhs.color = currSegment.color;
//            lhs.endMinute = startMinute - 1;
            lhs.day = currSegment.day;
//            currSegment.startMinute = startMinute;
            // increment i so that we are at the right position when
            // referencing the segments to the right and left of the
            // current segment.
            segments.add(i++, lhs);
            strands.get(lhs.color).count++;
          }
          // if the right side is black merge this with the segment to
          // the right if they're on the same day and overlap
          if (i + 1 < segments.size()) {
            EventSegment rhs = segments.get(i + 1);
            if (rhs.color == CONFLICT_COLOR && currSegment.day == rhs.day && rhs.startMinute <= currSegment.endMinute + 1) {
              rhs.startMinute = Math.min(currSegment.startMinute, rhs.startMinute);
              segments.remove(currSegment);
              strands.get(currSegment.color).count--;
              // point at the new current segment
              currSegment = rhs;
            }
          }
          // if the left side is black merge this with the segment to the left if they're on the same day and overlap
          if (i - 1 >= 0) {
            EventSegment lhs = segments.get(i - 1);
            if (lhs.color == CONFLICT_COLOR && currSegment.day == lhs.day && lhs.endMinute >= currSegment.startMinute - 1) {
              lhs.endMinute = Math.max(currSegment.endMinute, lhs.endMinute);
              segments.remove(currSegment);
              strands.get(currSegment.color).count--;
              // point at the new current segment
              currSegment = lhs;
              // point i at the new current segment in case new code is added
              i--;
            }
          }
          // if we're still not black, decrement the count for the color being removed, change this to black, and increment the black count
          if (currSegment.color != CONFLICT_COLOR) {
            strands.get(currSegment.color).count--;
            currSegment.color = CONFLICT_COLOR;
            strands.get(CONFLICT_COLOR).count++;
          }
        }

      }
      // If this event extends beyond the last segment add a new segment
//      if (endMinute > lastSegment.endMinute) {
//        addNewSegment(segments, event, strands, firstJulianDay, lastSegment.endMinute, minMinutes);
//      }
    }
    weaveDNAStrands(segments, firstJulianDay, strands, top, bottom, dayXs);
    return strands;
  }

  // This figures out allDay colors as allDay events are found
  private static void addAllDayToStrands(Event event, HashMap<Integer, EventStrand> strands, int firstJulianDay, int numDays) {
    EventStrand strand = getOrCreateStrand(strands, CONFLICT_COLOR);
    // if we haven't initialized the allDay portion create it now
    if (strand.allDays == null) {
      strand.allDays = new int[numDays];
    }

    // For each day this event is on update the color
    long end = Math.min(event.getEndDay() - firstJulianDay, numDays - 1);
    for (long i = Math.max(event.getStartDay() - firstJulianDay, 0); i <= end; i++) {
      if (strand.allDays[((int) i)] != 0) {
        // if this day already had a color, it is now a conflict
        strand.allDays[((int) i)] = CONFLICT_COLOR;
      } else {
        // else it's just the color of the event
        strand.allDays[((int) i)] = event.getColor();
      }
    }
  }

  // This processes all the segments, sorts them by color, and generates a list of points to draw
  private static void weaveDNAStrands(LinkedList<EventSegment> segments, int firstJulianDay, HashMap<Integer, EventStrand> strands, int top, int bottom, int[] dayXs) {
    // First, get rid of any colors that ended up with no segments
    Iterator<EventStrand> strandIterator = strands.values().iterator();
    while (strandIterator.hasNext()) {
      EventStrand strand = strandIterator.next();
      if (strand.count < 1 && strand.allDays == null) {
        strandIterator.remove();
        continue;
      }
      strand.points = new float[strand.count * 4];
      strand.position = 0;
    }
    // Go through each segment and compute its points
    for (EventSegment segment : segments) {
      // Add the points to the strand of that color
      EventStrand strand = strands.get(segment.color);
      long dayIndex = segment.day - firstJulianDay;
      long dayStartMinute = segment.startMinute % DAY_IN_MINUTES;
      long dayEndMinute = segment.endMinute % DAY_IN_MINUTES;
      int height = bottom - top;
      int workDayHeight = height * 3 / 4;
      int remainderHeight = (height - workDayHeight) / 2;

      int x = dayXs[((int) dayIndex)];
      long y0 = top + getPixelOffsetFromMinutes(dayStartMinute, workDayHeight, remainderHeight);
      long y1 = top + getPixelOffsetFromMinutes(dayEndMinute, workDayHeight, remainderHeight);
      strand.points[strand.position++] = x;
      strand.points[strand.position++] = y0;
      strand.points[strand.position++] = x;
      strand.points[strand.position++] = y1;
    }
  }

  /**
   * Compute a pixel offset from the top for a given minute from the work day height and the height of the top area.
   */
  private static long getPixelOffsetFromMinutes(long minute, int workDayHeight, int remainderHeight) {
    long y;
    if (minute < WORK_DAY_START_MINUTES) {
      y = minute * remainderHeight / WORK_DAY_START_MINUTES;
    } else if (minute < WORK_DAY_END_MINUTES) {
      y = remainderHeight + (minute - WORK_DAY_START_MINUTES) * workDayHeight / WORK_DAY_MINUTES;
    } else {
      y = remainderHeight + workDayHeight + (minute - WORK_DAY_END_MINUTES) * remainderHeight / WORK_DAY_END_LENGTH;
    }
    return y;
  }

  /**
   * Add a new segment based on the event provided. This will handle splitting segments across day boundaries and ensures a minimum size for segments.
   */
  private static void addNewSegment(LinkedList<EventSegment> segments, CalendarEvent calendarEvent, HashMap<Integer, EventStrand> strands) {
    // If this is a multiday event, split it up by day
    Event event = calendarEvent.getEvent();

    if (eventEndsInOtherDay(calendarEvent)) {
      CalendarEvent _calendarEvent = new CalendarEvent();
      Event _event = new Event();
      _event.setColor(event.getColor());
      _calendarEvent.setDay(event.getStartDay() + TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS));
      // the first day we want the start time to be the actual start time
      _event.setStartTime(_calendarEvent.getDay());
      long millisecondsFromStartToEndOfDay = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS) - event.getStartTime();
      _event.setDuration(event.getDuration() - millisecondsFromStartToEndOfDay);
      _calendarEvent.setEvent(_event);
//      _event.endTime = DAY_IN_MINUTES - 1;
//      while (_event.getStartDay() != event.getEndDay()) {
      addNewSegment(segments, _calendarEvent, strands);
      // The days in between are all day, even though that shouldn't actually happen due to the allday filtering
//      }
      // The last day we want the end time to be the actual end time
//      _event.endTime = event.endTime;
//      event = _event;
    } else {
      EventSegment segment = new EventSegment();
//      long dayOffset = (event.getStartDay() - firstJulianDay) * DAY_IN_MINUTES;
//      long endOfDay = dayOffset + DAY_IN_MINUTES - 1;
      // clip the start if needed
//      segment.startMinute = Math.max(dayOffset + event.getStartTime(), minStart);
      segment.startMinute = event.getStartTime();
      // and extend the end if it's too small, but not beyond the end of the day
//      long minEnd = Math.min(segment.startMinute + minMinutes, endOfDay);
//    segment.endMinute = Math.max(dayOffset + event.endTime, minEnd);
//      if (segment.endMinute > endOfDay) {
//        segment.endMinute = endOfDay;
//      }
      segment.endMinute = segment.startMinute + event.getDuration();

      segment.color = event.getColor();
      segment.day = calendarEvent.getDay();
      segments.add(segment);
      // increment the count for the correct color or add a new strand if we don't have that color yet
      EventStrand strand = getOrCreateStrand(strands, segment.color);
//      strand.count++;
    }
  }

  private static boolean eventEndsInOtherDay(CalendarEvent calendarEvent) {
    long initDay = calendarEvent.getDay();
    long endDay = initDay + TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);
    Event event = calendarEvent.getEvent();
    long duration = TimeUnit.MILLISECONDS.convert(event.getStartTime(), TimeUnit.HOURS);

    return initDay + duration <= endDay;
  }

  /**
   * Try to get a strand of the given color. Create it if it doesn't exist.
   */
  private static EventStrand getOrCreateStrand(HashMap<Integer, EventStrand> strands, int color) {
    EventStrand strand = strands.get(color);
    if (strand == null) {
      strand = new EventStrand();
      strand.color = color;
      strand.count = 0;
      strands.put(strand.color, strand);
    }
    return strand;
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

  public static Event createEventFromCursor(Cursor cursor) {
    Event event = new Event();
    event.setId(cursor.getInt(cursor.getColumnIndex(DBConstants.ID)));
    event.setName(cursor.getString(cursor.getColumnIndex(DBConstants.NAME)));
    event.setDescription(cursor.getString(cursor.getColumnIndex(DBConstants.DESCRIPTION)));
    event.setStartTime(cursor.getLong(cursor.getColumnIndex(DBConstants.START)));
    event.setDuration(cursor.getLong(cursor.getColumnIndex(DBConstants.DURATION)));
    event.setLocation(cursor.getString(cursor.getColumnIndex(DBConstants.LOCATION)));
    event.setColor(cursor.getInt(cursor.getColumnIndex(DBConstants.COLOR)));

    return event;
  }

  public static String convertToRGB(int color) {
    String red = Integer.toHexString(Color.red(color));
    String green = Integer.toHexString(Color.green(color));
    String blue = Integer.toHexString(Color.blue(color));

    if (red.length() == 1) {
      red = "0" + red;
    }

    if (green.length() == 1) {
      green = "0" + green;
    }

    if (blue.length() == 1) {
      blue = "0" + blue;
    }

    return "#" + red + green + blue;
  }

  // A single strand represents one color of events. Events are divided up by color to make them convenient to draw. The black strand is special in
  // that it holds conflicting events as well as color settings for allday on each day.
  public static class EventStrand {
    public float[] points;
    public int[]   allDays; // color for the allday, 0 means no event
    public int     color;
    int position;
    int count;
  }

  // A segment is a single continuous length of time occupied by a single color. Segments should never span multiple days.
  private static class EventSegment {
    long startMinute; // in minutes since the start of the week
    long endMinute;
    int  color; // Calendar color or black for conflicts
    long day; // quick reference to the day this segment is on
  }
}

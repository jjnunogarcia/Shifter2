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

package es.android.TurnosAndroid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.text.format.Time;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class Utils {
  private static final String TAG = Utils.class.getSimpleName();

  public static final int DECLINED_EVENT_ALPHA      = 0x66;
  public static final int DECLINED_EVENT_TEXT_ALPHA = 0xC0;

  private static final float SATURATION_ADJUST = 1.3f;
  private static final float INTENSITY_ADJUST  = 0.8f;

  // Defines used by the DNA generation code
  static final int     DAY_IN_MINUTES         = 60 * 24;
  static final int     WEEK_IN_MINUTES        = DAY_IN_MINUTES * 7;
  // The work day is being counted as 6am to 8pm
  static       int     WORK_DAY_MINUTES       = 14 * 60;
  static       int     WORK_DAY_START_MINUTES = 6 * 60;
  static       int     WORK_DAY_END_MINUTES   = 20 * 60;
  static       int     WORK_DAY_END_LENGTH    = (24 * 60) - WORK_DAY_END_MINUTES;
  static       int     CONFLICT_COLOR         = 0xFF000000;
  static       boolean mMinutesLoaded         = false;

  // The name of the shared preferences file. This name must be maintained for historical
  // reasons, as it's what PreferenceManager assigned the first time the file was created.
  static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";

  private static final TimeZoneUtils timeZoneUtils = new TimeZoneUtils(SHARED_PREFS_NAME);

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
//            return Time.MONDAY;
//        } else {
    return Time.SUNDAY;
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
   * @return true when declined events should be hidden.
   */
  public static boolean getHideDeclinedEvents(Context context) {
//        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
//        return prefs.getBoolean(GeneralPreferences.KEY_HIDE_DECLINED, false);
    return false;
  }

  public static int getDaysPerWeek(Context context) {
//        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
//        return prefs.getInt(GeneralPreferences.KEY_DAYS_PER_WEEK, 7);
    return 7;
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

  public static boolean getConfigBool(Context c, int key) {
    return c.getResources().getBoolean(key);
  }

  public static int getDisplayColorFromColor(int color) {
    if (!isJellybeanOrLater()) {
      return color;
    }

    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    hsv[1] = Math.min(hsv[1] * SATURATION_ADJUST, 1.0f);
    hsv[2] = hsv[2] * INTENSITY_ADJUST;
    return Color.HSVToColor(hsv);
  }

  // This takes a color and computes what it would look like blended with
  // white. The result is the color that should be used for declined events.
  public static int getDeclinedColorFromColor(int color) {
    int bg = 0xffffffff;
    int a = DECLINED_EVENT_ALPHA;
    int r = (((color & 0x00ff0000) * a) + ((bg & 0x00ff0000) * (0xff - a))) & 0xff000000;
    int g = (((color & 0x0000ff00) * a) + ((bg & 0x0000ff00) * (0xff - a))) & 0x00ff0000;
    int b = (((color & 0x000000ff) * a) + ((bg & 0x000000ff) * (0xff - a))) & 0x0000ff00;
    return (0xff000000) | ((r | g | b) >> 8);
  }

  // A single strand represents one color of events. Events are divided up by
  // color to make them convenient to draw. The black strand is special in
  // that it holds conflicting events as well as color settings for allday on
  // each day.
  public static class DNAStrand {
    public float[] points;
    public int[]   allDays; // color for the allday, 0 means no event
    int position;
    public int color;
    int count;
  }

  // A segment is a single continuous length of time occupied by a single
  // color. Segments should never span multiple days.
  private static class DNASegment {
    int startMinute; // in minutes since the start of the week
    int endMinute;
    int color; // Calendar color or black for conflicts
    int day; // quick reference to the day this segment is on
  }

  /**
   * Converts a list of events to a list of segments to draw. Assumes list is
   * ordered by start time of the events. The function processes events for a
   * range of days from firstJulianDay to firstJulianDay + dayXs.length - 1.
   * The algorithm goes over all the events and creates a set of segments
   * ordered by start time. This list of segments is then converted into a
   * HashMap of strands which contain the draw points and are organized by
   * color. The strands can then be drawn by setting the paint color to each
   * strand's color and calling drawLines on its set of points. The points are
   * set up using the following parameters.
   * <ul>
   * <li>Events between midnight and WORK_DAY_START_MINUTES are compressed
   * into the first 1/8th of the space between top and bottom.</li>
   * <li>Events between WORK_DAY_END_MINUTES and the following midnight are
   * compressed into the last 1/8th of the space between top and bottom</li>
   * <li>Events between WORK_DAY_START_MINUTES and WORK_DAY_END_MINUTES use
   * the remaining 3/4ths of the space</li>
   * <li>All segments drawn will maintain at least minPixels height, except
   * for conflicts in the first or last 1/8th, which may be smaller</li>
   * </ul>
   *
   * @param firstJulianDay The julian day of the first day of events
   * @param events         A list of events sorted by start time
   * @param top            The lowest y value the dna should be drawn at
   * @param bottom         The highest y value the dna should be drawn at
   * @param dayXs          An array of x values to draw the dna at, one for each day
   * @param conflictColor  the color to use for conflicts
   * @return
   */
  public static HashMap<Integer, DNAStrand> createDNAStrands(int firstJulianDay, ArrayList<Event> events, int top, int bottom, int minPixels, int[] dayXs, Context context) {

    if (!mMinutesLoaded) {
      if (context == null) {
        return null;
      }
      Resources res = context.getResources();
      CONFLICT_COLOR = res.getColor(R.color.month_dna_conflict_time_color);
      WORK_DAY_START_MINUTES = res.getInteger(R.integer.work_start_minutes);
      WORK_DAY_END_MINUTES = res.getInteger(R.integer.work_end_minutes);
      WORK_DAY_END_LENGTH = DAY_IN_MINUTES - WORK_DAY_END_MINUTES;
      WORK_DAY_MINUTES = WORK_DAY_END_MINUTES - WORK_DAY_START_MINUTES;
      mMinutesLoaded = true;
    }

    if (events == null || events.isEmpty() || dayXs == null || dayXs.length < 1 || bottom - top < 8 || minPixels < 0) {
      return null;
    }

    LinkedList<DNASegment> segments = new LinkedList<DNASegment>();
    HashMap<Integer, DNAStrand> strands = new HashMap<Integer, DNAStrand>();
    // add a black strand by default, other colors will get added in
    // the loop
    DNAStrand blackStrand = new DNAStrand();
    blackStrand.color = CONFLICT_COLOR;
    strands.put(CONFLICT_COLOR, blackStrand);
    // the min length is the number of minutes that will occupy
    // MIN_SEGMENT_PIXELS in the 'work day' time slot. This computes the
    // minutes/pixel * minpx where the number of pixels are 3/4 the total
    // dna height: 4*(mins/(px * 3/4))
    int minMinutes = minPixels * 4 * WORK_DAY_MINUTES / (3 * (bottom - top));

    // There are slightly fewer than half as many pixels in 1/6 the space,
    // so round to 2.5x for the min minutes in the non-work area
    int minOtherMinutes = minMinutes * 5 / 2;
    int lastJulianDay = firstJulianDay + dayXs.length - 1;

    Event event = new Event();
    // Go through all the events for the week
    for (Event currEvent : events) {
      // if this event is outside the weeks range skip it
      if (currEvent.endDay < firstJulianDay || currEvent.startDay > lastJulianDay) {
        continue;
      }
      if (currEvent.drawAsAllday()) {
        addAllDayToStrands(currEvent, strands, firstJulianDay, dayXs.length);
        continue;
      }
      // Copy the event over so we can clip its start and end to our range
      currEvent.copyTo(event);
      if (event.startDay < firstJulianDay) {
        event.startDay = firstJulianDay;
        event.startTime = 0;
      }
      // If it starts after the work day make sure the start is at least
      // minPixels from midnight
      if (event.startTime > DAY_IN_MINUTES - minOtherMinutes) {
        event.startTime = DAY_IN_MINUTES - minOtherMinutes;
      }
      if (event.endDay > lastJulianDay) {
        event.endDay = lastJulianDay;
        event.endTime = DAY_IN_MINUTES - 1;
      }
      // If the end time is before the work day make sure it ends at least
      // minPixels after midnight
      if (event.endTime < minOtherMinutes) {
        event.endTime = minOtherMinutes;
      }
      // If the start and end are on the same day make sure they are at
      // least minPixels apart. This only needs to be done for times
      // outside the work day as the min distance for within the work day
      // is enforced in the segment code.
      if (event.startDay == event.endDay &&
          event.endTime - event.startTime < minOtherMinutes) {
        // If it's less than minPixels in an area before the work
        // day
        if (event.startTime < WORK_DAY_START_MINUTES) {
          // extend the end to the first easy guarantee that it's
          // minPixels
          event.endTime = Math.min(event.startTime + minOtherMinutes,
                                   WORK_DAY_START_MINUTES + minMinutes);
          // if it's in the area after the work day
        } else if (event.endTime > WORK_DAY_END_MINUTES) {
          // First try shifting the end but not past midnight
          event.endTime = Math.min(event.endTime + minOtherMinutes, DAY_IN_MINUTES - 1);
          // if it's still too small move the start back
          if (event.endTime - event.startTime < minOtherMinutes) {
            event.startTime = event.endTime - minOtherMinutes;
          }
        }
      }

      // This handles adding the first segment
      if (segments.size() == 0) {
        addNewSegment(segments, event, strands, firstJulianDay, 0, minMinutes);
        continue;
      }
      // Now compare our current start time to the end time of the last
      // segment in the list
      DNASegment lastSegment = segments.getLast();
      int startMinute = (event.startDay - firstJulianDay) * DAY_IN_MINUTES + event.startTime;
      int endMinute = Math.max((event.endDay - firstJulianDay) * DAY_IN_MINUTES + event.endTime, startMinute + minMinutes);

      if (startMinute < 0) {
        startMinute = 0;
      }
      if (endMinute >= WEEK_IN_MINUTES) {
        endMinute = WEEK_IN_MINUTES - 1;
      }
      // If we start before the last segment in the list ends we need to
      // start going through the list as this may conflict with other
      // events
      if (startMinute < lastSegment.endMinute) {
        int i = segments.size();
        // find the last segment this event intersects with
        while (--i >= 0 && endMinute < segments.get(i).startMinute) {
        }

        DNASegment currSegment;
        // for each segment this event intersects with
        for (; i >= 0 && startMinute <= (currSegment = segments.get(i)).endMinute; i--) {
          // if the segment is already a conflict ignore it
          if (currSegment.color == CONFLICT_COLOR) {
            continue;
          }
          // if the event ends before the segment and wouldn't create
          // a segment that is too small split off the right side
          if (endMinute < currSegment.endMinute - minMinutes) {
            DNASegment rhs = new DNASegment();
            rhs.endMinute = currSegment.endMinute;
            rhs.color = currSegment.color;
            rhs.startMinute = endMinute + 1;
            rhs.day = currSegment.day;
            currSegment.endMinute = endMinute;
            segments.add(i + 1, rhs);
            strands.get(rhs.color).count++;
          }
          // if the event starts after the segment and wouldn't create
          // a segment that is too small split off the left side
          if (startMinute > currSegment.startMinute + minMinutes) {
            DNASegment lhs = new DNASegment();
            lhs.startMinute = currSegment.startMinute;
            lhs.color = currSegment.color;
            lhs.endMinute = startMinute - 1;
            lhs.day = currSegment.day;
            currSegment.startMinute = startMinute;
            // increment i so that we are at the right position when
            // referencing the segments to the right and left of the
            // current segment.
            segments.add(i++, lhs);
            strands.get(lhs.color).count++;
          }
          // if the right side is black merge this with the segment to
          // the right if they're on the same day and overlap
          if (i + 1 < segments.size()) {
            DNASegment rhs = segments.get(i + 1);
            if (rhs.color == CONFLICT_COLOR && currSegment.day == rhs.day
                && rhs.startMinute <= currSegment.endMinute + 1) {
              rhs.startMinute = Math.min(currSegment.startMinute, rhs.startMinute);
              segments.remove(currSegment);
              strands.get(currSegment.color).count--;
              // point at the new current segment
              currSegment = rhs;
            }
          }
          // if the left side is black merge this with the segment to
          // the left if they're on the same day and overlap
          if (i - 1 >= 0) {
            DNASegment lhs = segments.get(i - 1);
            if (lhs.color == CONFLICT_COLOR && currSegment.day == lhs.day
                && lhs.endMinute >= currSegment.startMinute - 1) {
              lhs.endMinute = Math.max(currSegment.endMinute, lhs.endMinute);
              segments.remove(currSegment);
              strands.get(currSegment.color).count--;
              // point at the new current segment
              currSegment = lhs;
              // point i at the new current segment in case new
              // code is added
              i--;
            }
          }
          // if we're still not black, decrement the count for the
          // color being removed, change this to black, and increment
          // the black count
          if (currSegment.color != CONFLICT_COLOR) {
            strands.get(currSegment.color).count--;
            currSegment.color = CONFLICT_COLOR;
            strands.get(CONFLICT_COLOR).count++;
          }
        }

      }
      // If this event extends beyond the last segment add a new segment
      if (endMinute > lastSegment.endMinute) {
        addNewSegment(segments, event, strands, firstJulianDay, lastSegment.endMinute,
                      minMinutes);
      }
    }
    weaveDNAStrands(segments, firstJulianDay, strands, top, bottom, dayXs);
    return strands;
  }

  // This figures out allDay colors as allDay events are found
  private static void addAllDayToStrands(Event event, HashMap<Integer, DNAStrand> strands, int firstJulianDay, int numDays) {
    DNAStrand strand = getOrCreateStrand(strands, CONFLICT_COLOR);
    // if we haven't initialized the allDay portion create it now
    if (strand.allDays == null) {
      strand.allDays = new int[numDays];
    }

    // For each day this event is on update the color
    int end = Math.min(event.endDay - firstJulianDay, numDays - 1);
    for (int i = Math.max(event.startDay - firstJulianDay, 0); i <= end; i++) {
      if (strand.allDays[i] != 0) {
        // if this day already had a color, it is now a conflict
        strand.allDays[i] = CONFLICT_COLOR;
      } else {
        // else it's just the color of the event
        strand.allDays[i] = event.color;
      }
    }
  }

  // This processes all the segments, sorts them by color, and generates a
  // list of points to draw
  private static void weaveDNAStrands(LinkedList<DNASegment> segments, int firstJulianDay, HashMap<Integer, DNAStrand> strands, int top, int bottom, int[] dayXs) {
    // First, get rid of any colors that ended up with no segments
    Iterator<DNAStrand> strandIterator = strands.values().iterator();
    while (strandIterator.hasNext()) {
      DNAStrand strand = strandIterator.next();
      if (strand.count < 1 && strand.allDays == null) {
        strandIterator.remove();
        continue;
      }
      strand.points = new float[strand.count * 4];
      strand.position = 0;
    }
    // Go through each segment and compute its points
    for (DNASegment segment : segments) {
      // Add the points to the strand of that color
      DNAStrand strand = strands.get(segment.color);
      int dayIndex = segment.day - firstJulianDay;
      int dayStartMinute = segment.startMinute % DAY_IN_MINUTES;
      int dayEndMinute = segment.endMinute % DAY_IN_MINUTES;
      int height = bottom - top;
      int workDayHeight = height * 3 / 4;
      int remainderHeight = (height - workDayHeight) / 2;

      int x = dayXs[dayIndex];
      int y0;
      int y1;

      y0 = top + getPixelOffsetFromMinutes(dayStartMinute, workDayHeight, remainderHeight);
      y1 = top + getPixelOffsetFromMinutes(dayEndMinute, workDayHeight, remainderHeight);
      strand.points[strand.position++] = x;
      strand.points[strand.position++] = y0;
      strand.points[strand.position++] = x;
      strand.points[strand.position++] = y1;
    }
  }

  /**
   * Compute a pixel offset from the top for a given minute from the work day
   * height and the height of the top area.
   */
  private static int getPixelOffsetFromMinutes(int minute, int workDayHeight, int remainderHeight) {
    int y;
    if (minute < WORK_DAY_START_MINUTES) {
      y = minute * remainderHeight / WORK_DAY_START_MINUTES;
    } else if (minute < WORK_DAY_END_MINUTES) {
      y = remainderHeight + (minute - WORK_DAY_START_MINUTES) * workDayHeight / WORK_DAY_MINUTES;
    } else {
      y = remainderHeight + workDayHeight + (minute - WORK_DAY_END_MINUTES) * remainderHeight
                                            / WORK_DAY_END_LENGTH;
    }
    return y;
  }

  /**
   * Add a new segment based on the event provided. This will handle splitting segments across day boundaries and ensures a minimum size for segments.
   */
  private static void addNewSegment(LinkedList<DNASegment> segments, Event event, HashMap<Integer, DNAStrand> strands, int firstJulianDay, int minStart, int minMinutes) {
    if (event.startDay > event.endDay) {
      Log.wtf(TAG, "Event starts after it ends: " + event.toString());
    }
    // If this is a multiday event split it up by day
    if (event.startDay != event.endDay) {
      Event lhs = new Event();
      lhs.color = event.color;
      lhs.startDay = event.startDay;
      // the first day we want the start time to be the actual start time
      lhs.startTime = event.startTime;
      lhs.endDay = lhs.startDay;
      lhs.endTime = DAY_IN_MINUTES - 1;
      // Nearly recursive iteration!
      while (lhs.startDay != event.endDay) {
        addNewSegment(segments, lhs, strands, firstJulianDay, minStart, minMinutes);
        // The days in between are all day, even though that shouldn't
        // actually happen due to the allday filtering
        lhs.startDay++;
        lhs.endDay = lhs.startDay;
        lhs.startTime = 0;
        minStart = 0;
      }
      // The last day we want the end time to be the actual end time
      lhs.endTime = event.endTime;
      event = lhs;
    }
    // Create the new segment and compute its fields
    DNASegment segment = new DNASegment();
    int dayOffset = (event.startDay - firstJulianDay) * DAY_IN_MINUTES;
    int endOfDay = dayOffset + DAY_IN_MINUTES - 1;
    // clip the start if needed
    segment.startMinute = Math.max(dayOffset + event.startTime, minStart);
    // and extend the end if it's too small, but not beyond the end of the
    // day
    int minEnd = Math.min(segment.startMinute + minMinutes, endOfDay);
    segment.endMinute = Math.max(dayOffset + event.endTime, minEnd);
    if (segment.endMinute > endOfDay) {
      segment.endMinute = endOfDay;
    }

    segment.color = event.color;
    segment.day = event.startDay;
    segments.add(segment);
    // increment the count for the correct color or add a new strand if we
    // don't have that color yet
    DNAStrand strand = getOrCreateStrand(strands, segment.color);
    strand.count++;
  }

  /**
   * Try to get a strand of the given color. Create it if it doesn't exist.
   */
  private static DNAStrand getOrCreateStrand(HashMap<Integer, DNAStrand> strands, int color) {
    DNAStrand strand = strands.get(color);
    if (strand == null) {
      strand = new DNAStrand();
      strand.color = color;
      strand.count = 0;
      strands.put(strand.color, strand);
    }
    return strand;
  }

}

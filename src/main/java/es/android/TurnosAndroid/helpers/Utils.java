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
import android.database.Cursor;
import es.android.TurnosAndroid.database.DBConstants;
import es.android.TurnosAndroid.model.CalendarEvent;
import es.android.TurnosAndroid.model.Event;

import java.util.ArrayList;

public class Utils {
  static final int DAY_IN_MINUTES = 60 * 24;

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
  public static ArrayList<EventStrand> createDNAStrands(int firstJulianDay, ArrayList<CalendarEvent> calendarEvents, int top, int bottom, int[] dayXs, Context context, int mondayJulianDay) {
    if (calendarEvents == null || calendarEvents.isEmpty() || dayXs == null || dayXs.length < 1 || bottom - top < 8) {
      return null;
    }

    ArrayList<EventSegment> segments = new ArrayList<EventSegment>();
    ArrayList<EventStrand> strands = new ArrayList<EventStrand>();

    for (CalendarEvent calendarEvent : calendarEvents) {
      addNewSegment(segments, calendarEvent, mondayJulianDay);
    }

    weaveDnaStrands(segments, firstJulianDay, strands, top, bottom, dayXs);
    return strands;
  }

  /**
   * Add a new segment based on the event provided. This will handle splitting segments across day boundaries and ensures a minimum size for segments.
   */
  private static void addNewSegment(ArrayList<EventSegment> segments, CalendarEvent calendarEvent, int mondayJulianDay) {
    // If this is a multiday event, split it up by day
    Event event = calendarEvent.getEvent();

    if (eventEndsInOtherDay(calendarEvent)) {
      CalendarEvent nextDayCalendarEvent = new CalendarEvent();
      Event nextDayEvent = new Event(event);
      nextDayCalendarEvent.setDay(calendarEvent.getDay() + 1);
      nextDayEvent.setStartTime(0);
      computeNewDuration(event, nextDayEvent);
      nextDayCalendarEvent.setEvent(nextDayEvent);
      addNewSegment(segments, nextDayCalendarEvent, mondayJulianDay);
    } else {
      EventSegment segment = new EventSegment();
      segment.startMinute = event.getStartTime();
      segment.endMinute = segment.startMinute + event.getDuration();
      segment.color = event.getColor();
      segment.dayOfWeek = getDayOfWeek(calendarEvent.getDay(), mondayJulianDay);
      segments.add(segment);
    }
  }

  private static boolean eventEndsInOtherDay(CalendarEvent calendarEvent) {
    Event event = calendarEvent.getEvent();
    return event.getStartTime() + event.getDuration() > DAY_IN_MINUTES;
  }

  // TODO refactor this so variables are not modified but returned?
  private static void computeNewDuration(Event event, Event nextDayEvent) {
    int currentEventOldDuration = event.getDuration();
    int currentEventNewDuration = DAY_IN_MINUTES - event.getStartTime();
    event.setDuration(currentEventNewDuration);
    nextDayEvent.setDuration(currentEventOldDuration - currentEventNewDuration);
  }

  private static int getDayOfWeek(int julianDay, int mondayJulianDay) {
    return julianDay - mondayJulianDay;
  }

  // This processes all the segments, sorts them by color, and generates a list of points to draw
  // TODO replace "top" by a variable that indicates the month number height, so the lines are below
  private static void weaveDnaStrands(ArrayList<EventSegment> segments, int firstJulianDay, ArrayList<EventStrand> strands, int top, int bottom, int[] dayXs) {
    for (EventSegment segment : segments) {
      int x = dayXs[segment.dayOfWeek];
      int eventWidth = dayXs[1] - dayXs[0]; // TODO better put a event width
      EventStrand strand = new EventStrand();
      strand.points[strand.position++] = x;
      strand.points[strand.position++] = 55;
      strand.points[strand.position++] = x + eventWidth;
      strand.points[strand.position++] = 55;
      strand.color = segment.color;
      strands.add(strand);
    }
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
    event.setStartTime(cursor.getInt(cursor.getColumnIndex(DBConstants.START)));
    event.setDuration(cursor.getInt(cursor.getColumnIndex(DBConstants.DURATION)));
    event.setLocation(cursor.getString(cursor.getColumnIndex(DBConstants.LOCATION)));
    event.setColor(cursor.getInt(cursor.getColumnIndex(DBConstants.COLOR)));

    return event;
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
    calendarEvent.setDay(cursor.getInt(cursor.getColumnIndex(DBConstants.DAY)));
    calendarEvent.setEvent(createEventFromCursor(cursor));

    return calendarEvent;
  }

  // A single strand represents one color of events. Events are divided up by color to make them convenient to draw. The black strand is special in
  // that it holds conflicting events as well as color settings for allday on each day.
  public static class EventStrand {
    public float[] points;
    public int     color;
    int position;

    public EventStrand() {
      this.points = new float[4];
      this.position = 0;
    }
  }

  // A segment is a single continuous length of time occupied by a single color. Segments should never span multiple days.
  private static class EventSegment {
    int startMinute; // in minutes since the start of the week
    int endMinute;
    int color; // Calendar color or black for conflicts
    int dayOfWeek; // quick reference to the day this segment is on
  }
}

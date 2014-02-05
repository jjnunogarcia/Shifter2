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

import android.database.Cursor;
import es.android.TurnosAndroid.database.DBConstants;
import es.android.TurnosAndroid.model.CalendarEvent;
import es.android.TurnosAndroid.model.Event;

import java.util.ArrayList;

public class Utils {
  private static final int    DAY_IN_MINUTES      = 60 * 24;
  public static final  String KEY_EVENT_TO_MANAGE = "event_to_manage";

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
    event.setStartTime(cursor.getInt(cursor.getColumnIndex(DBConstants.START_TIME)));
    event.setDuration(cursor.getInt(cursor.getColumnIndex(DBConstants.DURATION)));
    event.setLocation(cursor.getString(cursor.getColumnIndex(DBConstants.LOCATION)));
    event.setColor(cursor.getInt(cursor.getColumnIndex(DBConstants.COLOR)));
    event.setCreationTime(cursor.getLong(cursor.getColumnIndex(DBConstants.CREATION_TIME)));

    return event;
  }

  public static ArrayList<CalendarEvent> getCalendarEvents(Cursor cursor) {
    ArrayList<CalendarEvent> calendarEvents = new ArrayList<CalendarEvent>();

    if (cursor != null && cursor.getCount() > 0) {
      while (cursor.moveToNext()) {
        calendarEvents.add(createCalendarEventFromCursor(cursor));
      }
    }

    return getNormalizedCalendarEvents(calendarEvents);
  }

  private static ArrayList<CalendarEvent> getNormalizedCalendarEvents(ArrayList<CalendarEvent> calendarEvents) {
    ArrayList<CalendarEvent> calendarEventsSplit = new ArrayList<CalendarEvent>();

    for (CalendarEvent calendarEvent : calendarEvents) {
      calendarEventsSplit.add(splitLongCalendarEvents(calendarEvent));
    }

    return calendarEventsSplit;
  }

  private static CalendarEvent splitLongCalendarEvents(CalendarEvent calendarEvent) {
    if (eventEndsInOtherDay(calendarEvent)) {
      Event event = calendarEvent.getEvent();
      CalendarEvent nextDayCalendarEvent = new CalendarEvent();
      Event nextDayEvent = new Event(event);
      nextDayCalendarEvent.setDay(calendarEvent.getDay() + 1);
      nextDayEvent.setStartTime(0);
      computeNewDuration(calendarEvent, nextDayCalendarEvent);
      splitLongCalendarEvents(nextDayCalendarEvent);
      return calendarEvent;
    }

    return calendarEvent;
  }

  private static CalendarEvent createCalendarEventFromCursor(Cursor cursor) {
    CalendarEvent calendarEvent = new CalendarEvent();
    calendarEvent.setId(cursor.getInt(cursor.getColumnIndex(DBConstants.ID)));
    calendarEvent.setDay(cursor.getInt(cursor.getColumnIndex(DBConstants.DAY)));
    calendarEvent.setEvent(createEventFromCursor(cursor));
    calendarEvent.setCreationTime(cursor.getLong(cursor.getColumnIndex(DBConstants.CREATION_TIME)));

    return calendarEvent;
  }

  private static boolean eventEndsInOtherDay(CalendarEvent calendarEvent) {
    Event event = calendarEvent.getEvent();
    return event.getStartTime() + event.getDuration() > DAY_IN_MINUTES;
  }

  // TODO refactor this so variables are not modified but returned?
  private static void computeNewDuration(CalendarEvent calendarEvent, CalendarEvent nextDayCalendarEvent) {
    Event event = calendarEvent.getEvent();
    Event nextDayEvent = nextDayCalendarEvent.getEvent();
    int currentEventOldDuration = event.getDuration();
    int currentEventNewDuration = DAY_IN_MINUTES - event.getStartTime();
    event.setDuration(currentEventNewDuration);
    nextDayEvent.setDuration(currentEventOldDuration - currentEventNewDuration);
    calendarEvent.setEvent(event);
    nextDayCalendarEvent.setEvent(nextDayEvent);
  }
}

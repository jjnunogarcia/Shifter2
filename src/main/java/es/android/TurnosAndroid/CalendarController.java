/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.text.format.Time;
import android.util.Log;

import java.util.ArrayList;

public class CalendarController {
  private static final String TAG                         = CalendarController.class.getSimpleName();
  public static final  long   EXTRA_CREATE_ALL_DAY        = 0x10;
  public static final  long   EXTRA_GOTO_DATE             = 1;
  public static final  long   EXTRA_GOTO_TIME             = 2;
  public static final  long   EXTRA_GOTO_BACK_TO_PREVIOUS = 4;
  public static final  long   EXTRA_GOTO_TODAY            = 8;

  private ArrayList<EventHandler> eventHandlers;
  private Context                 context;
  private Time                    time;
  private EventHandler            firstEventHandler;
  private ViewType                viewType;
  private ViewType                detailViewType;
  private final Runnable updateTimezone = new Runnable() {
    @Override
    public void run() {
      time.switchTimezone(Utils.getTimeZone(context, this));
    }
  };

  public CalendarController(Context context) {
    this.context = context;
    eventHandlers = new ArrayList<EventHandler>();
    time = new Time();
    updateTimezone.run();
    time.setToNow();
    viewType = ViewType.DETAIL;
    detailViewType = ViewType.DETAIL;
//        detailViewType = Utils.getSharedPreference(context, GeneralPreferences.KEY_DETAILED_VIEW, GeneralPreferences.DEFAULT_DETAILED_VIEW);
  }

  public void sendEventRelatedEvent(long eventType, long eventId, long startMillis, long endMillis, int x, int y, long selectedMillis) {
    // TODO: pass the real allDay status or at least a status that says we don't know the status and have the receiver query the data.
    // The current use of this method for VIEW_EVENT is by the day view to show an EventInfo so currently the missing allDay status has no effect.
    sendEventRelatedEventWithExtra(eventType, eventId, startMillis, endMillis, x, y, EventInfo.buildViewExtraLong(Attendees.ATTENDEE_STATUS_NONE, false), selectedMillis);
  }

  /**
   * Helper for sending New/View/Edit/Delete events
   *
   * @param eventType      one of {@link es.android.TurnosAndroid.EventType}
   * @param eventId        event id
   * @param startMillis    start time
   * @param endMillis      end time
   * @param x              x coordinate in the activity space
   * @param y              y coordinate in the activity space
   * @param extraLong      default response value for the "simple event view" and all day indication.
   *                       Use Attendees.ATTENDEE_STATUS_NONE for no response.
   * @param selectedMillis The time to specify as selected
   */
  public void sendEventRelatedEventWithExtra(long eventType, long eventId, long startMillis, long endMillis, int x, int y, long extraLong, long selectedMillis) {
    EventInfo info = new EventInfo();
    info.eventType = eventType;
    if (eventType == EventType.EDIT_EVENT || eventType == EventType.VIEW_EVENT_DETAILS) {
      info.viewType = ViewType.CURRENT;
    }
    info.id = eventId;
    info.startTime = new Time(Utils.getTimeZone(context, updateTimezone));
    info.startTime.set(startMillis);
    if (selectedMillis != -1) {
      info.selectedTime = new Time(Utils.getTimeZone(context, updateTimezone));
      info.selectedTime.set(selectedMillis);
    } else {
      info.selectedTime = info.startTime;
    }
    info.endTime = new Time(Utils.getTimeZone(context, updateTimezone));
    info.endTime.set(endMillis);
    info.x = x;
    info.y = y;
    info.extraLong = extraLong;

    sendEvent(info);
  }

  /**
   * Helper for sending non-calendar-event events
   *
   * @param eventType one of {@link es.android.TurnosAndroid.EventType}
   * @param start     start time
   * @param end       end time
   * @param eventId   event id
   * @param viewType  {@link es.android.TurnosAndroid.ViewType}
   */
  public void sendEvent(long eventType, Time start, Time end, long eventId, ViewType viewType) {
    sendEvent(eventType, start, end, start, eventId, viewType, EXTRA_GOTO_TIME, null, null);
  }

  /**
   * sendEvent() variant with extraLong, search query, and search component name.
   */
  public void sendEvent(long eventType, Time start, Time end, long eventId, ViewType viewType, long extraLong, String query, ComponentName componentName) {
    sendEvent(eventType, start, end, start, eventId, viewType, extraLong, query, componentName);
  }

  public void sendEvent(long eventType, Time start, Time end, Time selected, long eventId, ViewType viewType, long extraLong, String query, ComponentName componentName) {
    EventInfo info = new EventInfo();
    info.eventType = eventType;
    info.startTime = start;
    info.selectedTime = selected;
    info.endTime = end;
    info.id = eventId;
    info.viewType = viewType;
    info.query = query;
    info.componentName = componentName;
    info.extraLong = extraLong;
    sendEvent(info);
  }

  private void sendEvent(EventInfo event) {
    // Fix up view if not specified
    if (event.viewType == ViewType.DETAIL) {
      event.viewType = detailViewType;
      viewType = detailViewType;
    } else if (event.viewType == ViewType.CURRENT) {
      event.viewType = viewType;
    } else if (event.viewType != ViewType.EDIT) {
      viewType = event.viewType;

      if (event.viewType == ViewType.AGENDA || event.viewType == ViewType.DAY || (Utils.getAllowWeekForDetailView() && event.viewType == ViewType.WEEK)) {
        detailViewType = viewType;
      }
    }

    long startMillis = 0;
    if (event.startTime != null) {
      startMillis = event.startTime.toMillis(false);
    }

    // Set time if selectedTime is set
    if (event.selectedTime != null && event.selectedTime.toMillis(false) != 0) {
      time.set(event.selectedTime);
    } else {
      if (startMillis != 0) {
        // selectedTime is not set so set time to startTime iff it is not
        // within start and end times
        long mtimeMillis = time.toMillis(false);
        if (mtimeMillis < startMillis
            || (event.endTime != null && mtimeMillis > event.endTime.toMillis(false))) {
          time.set(event.startTime);
        }
      }
      event.selectedTime = time;
    }

    // Fix up start time if not specified
    if (startMillis == 0) {
      event.startTime = time;
    }

    boolean handled = false;

    // Dispatch to event handler(s)
    if (firstEventHandler != null) {
      // Handle the 'first' one before handling the others
      EventHandler handler = firstEventHandler;
      if ((handler.getSupportedEventTypes() & event.eventType) != 0) {
        handler.handleEvent(event);
        handled = true;
      }
    }
    for (EventHandler eventHandler : eventHandlers) {
      if (eventHandler != null && (eventHandler.getSupportedEventTypes() & event.eventType) != 0) {
        eventHandler.handleEvent(event);
        handled = true;
      }
    }

    if (!handled) {
      // Launch Calendar Visible Selector
      if (event.eventType == EventType.LAUNCH_SELECT_VISIBLE_CALENDARS) {
//                launchSelectVisibleCalendars();
        return;
      }

      // Create/View/Edit/Delete Event
      long endTime = (event.endTime == null) ? -1 : event.endTime.toMillis(false);
      if (event.eventType == EventType.CREATE_EVENT) {
//                launchCreateEvent(event.startTime.toMillis(false), endTime, event.extraLong == EXTRA_CREATE_ALL_DAY);
      } else if (event.eventType == EventType.VIEW_EVENT) {
//                launchViewEvent(event.id, event.startTime.toMillis(false), endTime, event.getResponse());
      } else if (event.eventType == EventType.EDIT_EVENT) {
//                launchEditEvent(event.id, event.startTime.toMillis(false), endTime, true);
      } else if (event.eventType == EventType.VIEW_EVENT_DETAILS) {
//                launchEditEvent(event.id, event.startTime.toMillis(false), endTime, false);
      } else if (event.eventType == EventType.DELETE_EVENT) {
//                launchDeleteEvent(event.id, event.startTime.toMillis(false), endTime);
      } else if (event.eventType == EventType.SEARCH) {
        launchSearch(event.query, event.componentName);
      }
    }
  }

  /**
   * Adds or updates an event handler. This uses a LinkedHashMap so that we can
   * replace fragments based on the view id they are being expanded into.
   *
   * @param eventHandler Typically a fragment or activity in the calendar app
   */
  public void registerEventHandler(EventHandler eventHandler) {
    eventHandlers.add(eventHandler);
    firstEventHandler = eventHandler;
  }

  /**
   * @return the time that this controller is currently pointed at
   */
  public long getTime() {
    return time.toMillis(false);
  }

  /**
   * Set the time this controller is currently pointed at
   *
   * @param millisTime Time since epoch in millis
   */
  public void setTime(long millisTime) {
    time.set(millisTime);
  }

//    private void launchSelectVisibleCalendars() {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setClass(context, SelectVisibleCalendarsActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        context.startActivity(intent);
//    }
//
//    private void launchCreateEvent(long startMillis, long endMillis, boolean allDayEvent) {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setClass(context, EditEventActivity.class);
//        intent.putExtra(EXTRA_EVENT_BEGIN_TIME, startMillis);
//        intent.putExtra(EXTRA_EVENT_END_TIME, endMillis);
//        intent.putExtra(EXTRA_EVENT_ALL_DAY, allDayEvent);
//        eventId = -1;
//        context.startActivity(intent);
//    }
//
//    public void launchViewEvent(long eventId, long startMillis, long endMillis, int response) {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
//        intent.setData(eventUri);
//        intent.setClass(context, AllInOneActivity.class);
//        intent.putExtra(EXTRA_EVENT_BEGIN_TIME, startMillis);
//        intent.putExtra(EXTRA_EVENT_END_TIME, endMillis);
//        intent.putExtra(ATTENDEE_STATUS, response);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        context.startActivity(intent);
//    }

//    private void launchDeleteEvent(long eventId, long startMillis, long endMillis) {
//        launchDeleteEventAndFinish(null, eventId, startMillis, endMillis, -1);
//    }

//    private void launchDeleteEventAndFinish(Activity parentActivity, long eventId, long startMillis,
//            long endMillis, int deleteWhich) {
//        DeleteEventHelper deleteEventHelper = new DeleteEventHelper(context, parentActivity,
//                parentActivity != null /* exit when done */);
//        deleteEventHelper.delete(startMillis, endMillis, eventId, deleteWhich);
//    }

  private void launchSearch(String query, ComponentName componentName) {
    final SearchManager searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
    final SearchableInfo searchableInfo = searchManager.getSearchableInfo(componentName);
    final Intent intent = new Intent(Intent.ACTION_SEARCH);
    intent.putExtra(SearchManager.QUERY, query);
    intent.setComponent(searchableInfo.getSearchActivity());
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    context.startActivity(intent);
  }

  /**
   * Performs a manual refresh of calendars in all known accounts.
   */
  public void refreshCalendars() {
    Account[] accounts = AccountManager.get(context).getAccounts();
    Log.d(TAG, "Refreshing " + accounts.length + " accounts");

    String authority = Calendars.CONTENT_URI.getAuthority();
    for (Account account : accounts) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Refreshing calendars for: " + account);
      }
      Bundle extras = new Bundle();
      extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
      ContentResolver.requestSync(account, authority, extras);
    }
  }

}

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
import android.util.Pair;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.WeakHashMap;

public class CalendarController {
  public static final  String                                   EVENT_EDIT_ON_LAUNCH        = "editMode";
  public static final  int                                      MIN_CALENDAR_YEAR           = 1970;
  public static final  int                                      MAX_CALENDAR_YEAR           = 2036;
  public static final  int                                      MIN_CALENDAR_WEEK           = 0;
  public static final  int                                      MAX_CALENDAR_WEEK           = 3497; // weeks between 1/1/1970 and 1/1/2037
  /**
   * Pass to the ExtraLong parameter for EventType.CREATE_EVENT to create an all-day event
   */
  public static final  long                                     EXTRA_CREATE_ALL_DAY        = 0x10;
  /**
   * Pass to the ExtraLong parameter for EventType.GO_TO to signal the time
   * can be ignored
   */
  public static final  long                                     EXTRA_GOTO_DATE             = 1;
  public static final  long                                     EXTRA_GOTO_TIME             = 2;
  public static final  long                                     EXTRA_GOTO_BACK_TO_PREVIOUS = 4;
  public static final  long                                     EXTRA_GOTO_TODAY            = 8;
  private static final boolean                                  DEBUG                       = false;
  private static final String                                   TAG                         = "CalendarController";
  private static final String                                   REFRESH_SELECTION           = Calendars.SYNC_EVENTS + "=?";
  private static final String[]                                 REFRESH_ARGS                = new String[] {"1"};
  private static final String                                   REFRESH_ORDER               = Calendars.ACCOUNT_NAME + "," + Calendars.ACCOUNT_TYPE;
  private static       WeakHashMap<Context, CalendarController> instances                   = new WeakHashMap<Context, CalendarController>();
  // This uses a LinkedHashMap so that we can replace fragments based on the view id they are being expanded into since we can't guarantee a reference to the handler will be findable
  private final        LinkedHashMap<Integer, EventHandler>     eventHandlers               = new LinkedHashMap<Integer, EventHandler>(5);
  private final        LinkedList<Integer>                      toBeRemovedEventHandlers    = new LinkedList<Integer>();
  private final        LinkedHashMap<Integer, EventHandler>     toBeAddedEventHandlers      = new LinkedHashMap<Integer, EventHandler>();
  private final        WeakHashMap<Object, Long>                filters                     = new WeakHashMap<Object, Long>(1);
  private final Context context;
  private final Time     time           = new Time();
  private final Runnable updateTimezone = new Runnable() {
    @Override
    public void run() {
      time.switchTimezone(Utils.getTimeZone(context, this));
    }
  };
  private Pair<Integer, EventHandler> firstEventHandler;
  private Pair<Integer, EventHandler> toBeAddedFirstEventHandler;
  private volatile int  dispatchInProgressCounter = 0;
  private          int  viewType                  = -1;
  private          int  detailViewType            = -1;
  private          int  previousViewType          = -1;
  private          long eventId                   = -1;
  private          long dateFlags                 = 0;

  private CalendarController(Context context) {
    this.context = context;
    updateTimezone.run();
    time.setToNow();
//        detailViewType = Utils.getSharedPreference(context, GeneralPreferences.KEY_DETAILED_VIEW, GeneralPreferences.DEFAULT_DETAILED_VIEW);
  }

  /**
   * Creates and/or returns an instance of CalendarController associated with
   * the supplied context. It is best to pass in the current Activity.
   *
   * @param context The activity if at all possible.
   */
  public static CalendarController getInstance(Context context) {
    synchronized (instances) {
      CalendarController controller = instances.get(context);
      if (controller == null) {
        controller = new CalendarController(context);
        instances.put(context, controller);
      }
      return controller;
    }
  }

  public void sendEventRelatedEvent(Object sender, long eventType, long eventId, long startMillis, long endMillis, int x, int y, long selectedMillis) {
    // TODO: pass the real allDay status or at least a status that says we don't know the
    // status and have the receiver query the data.
    // The current use of this method for VIEW_EVENT is by the day view to show an EventInfo so currently the missing allDay status has no effect.
    sendEventRelatedEventWithExtra(sender, eventType, eventId, startMillis, endMillis, x, y, EventInfo.buildViewExtraLong(Attendees.ATTENDEE_STATUS_NONE, false), selectedMillis);
    }

    /**
     * Helper for sending New/View/Edit/Delete events
     *
     * @param sender         object of the caller
     * @param eventType      one of {@link EventType}
     * @param eventId        event id
     * @param startMillis    start time
     * @param endMillis      end time
     * @param x              x coordinate in the activity space
     * @param y              y coordinate in the activity space
     * @param extraLong      default response value for the "simple event view" and all day indication.
     *                       Use Attendees.ATTENDEE_STATUS_NONE for no response.
     * @param selectedMillis The time to specify as selected
     */
    public void sendEventRelatedEventWithExtra(Object sender, long eventType, long eventId, long startMillis, long endMillis, int x, int y, long extraLong, long selectedMillis) {
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
        this.sendEvent(sender, info);
    }

    /**
     * Helper for sending non-calendar-event events
     *
     * @param sender    object of the caller
     * @param eventType one of {@link EventType}
     * @param start     start time
     * @param end       end time
     * @param eventId   event id
     * @param viewType  {@link ViewType}
     */
    public void sendEvent(Object sender, long eventType, Time start, Time end, long eventId, int viewType) {
        sendEvent(sender, eventType, start, end, start, eventId, viewType, EXTRA_GOTO_TIME, null, null);
    }

    /**
     * sendEvent() variant with extraLong, search query, and search component name.
     */
    public void sendEvent(Object sender, long eventType, Time start, Time end, long eventId, int viewType, long extraLong, String query, ComponentName componentName) {
        sendEvent(sender, eventType, start, end, start, eventId, viewType, extraLong, query, componentName);
    }

    public void sendEvent(Object sender, long eventType, Time start, Time end, Time selected, long eventId, int viewType, long extraLong, String query, ComponentName componentName) {
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
        sendEvent(sender, info);
    }

    private void sendEvent(Object sender, final EventInfo event) {
        // TODO Throw exception on invalid events

        if (DEBUG) {
            Log.d(TAG, eventInfoToString(event));
        }

        Long filteredTypes = filters.get(sender);
        if (filteredTypes != null && (filteredTypes & event.eventType) != 0) {
            // Suppress event per filter
            if (DEBUG) {
                Log.d(TAG, "Event suppressed");
            }
            return;
        }

        previousViewType = viewType;

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

        if (DEBUG) {
            Log.e(TAG, "vvvvvvvvvvvvvvv");
            Log.e(TAG, "Start  " + (event.startTime == null ? "null" : event.startTime.toString()));
            Log.e(TAG, "End    " + (event.endTime == null ? "null" : event.endTime.toString()));
            Log.e(TAG, "Select " + (event.selectedTime == null ? "null" : event.selectedTime.toString()));
            Log.e(TAG, "time  " + (time == null ? "null" : time.toString()));
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
        // Store the formatting flags if this is an update to the title
        if (event.eventType == EventType.UPDATE_TITLE) {
            dateFlags = event.extraLong;
        }

        // Fix up start time if not specified
        if (startMillis == 0) {
            event.startTime = time;
        }
        if (DEBUG) {
            Log.e(TAG, "Start  " + (event.startTime == null ? "null" : event.startTime.toString()));
            Log.e(TAG, "End    " + (event.endTime == null ? "null" : event.endTime.toString()));
            Log.e(TAG, "Select " + (event.selectedTime == null ? "null" : event.selectedTime.toString()));
            Log.e(TAG, "time  " + (time == null ? "null" : time.toString()));
            Log.e(TAG, "^^^^^^^^^^^^^^^");
        }

        // Store the eventId if we're entering edit event
        if ((event.eventType & (EventType.CREATE_EVENT | EventType.EDIT_EVENT | EventType.VIEW_EVENT_DETAILS)) != 0) {
            if (event.id > 0) {
                eventId = event.id;
            } else {
                eventId = -1;
            }
        }

        boolean handled = false;
        synchronized (this) {
            dispatchInProgressCounter++;

            if (DEBUG) {
                Log.d(TAG, "sendEvent: Dispatching to " + eventHandlers.size() + " handlers");
            }
            // Dispatch to event handler(s)
            if (firstEventHandler != null) {
                // Handle the 'first' one before handling the others
                EventHandler handler = firstEventHandler.second;
                if (handler != null && (handler.getSupportedEventTypes() & event.eventType) != 0 && !toBeRemovedEventHandlers.contains(firstEventHandler.first)) {
                    handler.handleEvent(event);
                    handled = true;
                }
            }
            for (Entry<Integer, EventHandler> entry : eventHandlers.entrySet()) {
                int key = entry.getKey();
                if (firstEventHandler != null && key == firstEventHandler.first) {
                    // If this was the 'first' handler it was already handled
                    continue;
                }
                EventHandler eventHandler = entry.getValue();
                if (eventHandler != null && (eventHandler.getSupportedEventTypes() & event.eventType) != 0) {
                  if (!toBeRemovedEventHandlers.contains(key)) {
                    eventHandler.handleEvent(event);
                    handled = true;
                  }
                }
            }

            dispatchInProgressCounter--;

            if (dispatchInProgressCounter == 0) {

                // Deregister removed handlers
                if (toBeRemovedEventHandlers.size() > 0) {
                    for (Integer zombie : toBeRemovedEventHandlers) {
                        eventHandlers.remove(zombie);
                        if (firstEventHandler != null && zombie.equals(firstEventHandler.first)) {
                            firstEventHandler = null;
                        }
                    }
                    toBeRemovedEventHandlers.clear();
                }
                // Add new handlers
                if (toBeAddedFirstEventHandler != null) {
                    firstEventHandler = toBeAddedFirstEventHandler;
                    toBeAddedFirstEventHandler = null;
                }
                if (toBeAddedEventHandlers.size() > 0) {
                    for (Entry<Integer, EventHandler> food : toBeAddedEventHandlers.entrySet()) {
                        eventHandlers.put(food.getKey(), food.getValue());
                    }
                }
            }
        }

        if (!handled) {
            // Launch Settings
            if (event.eventType == EventType.LAUNCH_SETTINGS) {
//                launchSettings();
                return;
            }

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
                launchSearch(event.id, event.query, event.componentName);
            }
        }
    }

    /**
     * Adds or updates an event handler. This uses a LinkedHashMap so that we can
     * replace fragments based on the view id they are being expanded into.
     *
     * @param key          The view id or placeholder for this handler
     * @param eventHandler Typically a fragment or activity in the calendar app
     */
    public void registerEventHandler(int key, EventHandler eventHandler) {
        synchronized (this) {
            if (dispatchInProgressCounter > 0) {
                toBeAddedEventHandlers.put(key, eventHandler);
            } else {
                eventHandlers.put(key, eventHandler);
            }
        }
    }

    public void registerFirstEventHandler(int key, EventHandler eventHandler) {
        synchronized (this) {
            registerEventHandler(key, eventHandler);
            if (dispatchInProgressCounter > 0) {
                toBeAddedFirstEventHandler = new Pair<Integer, EventHandler>(key, eventHandler);
            } else {
                firstEventHandler = new Pair<Integer, EventHandler>(key, eventHandler);
            }
        }
    }

    public void deregisterEventHandler(Integer key) {
        synchronized (this) {
            if (dispatchInProgressCounter > 0) {
                // To avoid ConcurrencyException, stash away the event handler for now.
                toBeRemovedEventHandlers.add(key);
            } else {
                eventHandlers.remove(key);
                if (firstEventHandler != null && firstEventHandler.first.equals(key)) {
                    firstEventHandler = null;
                }
            }
        }
    }

    public void deregisterAllEventHandlers() {
        synchronized (this) {
            if (dispatchInProgressCounter > 0) {
                // To avoid ConcurrencyException, stash away the event handler for now.
                toBeRemovedEventHandlers.addAll(eventHandlers.keySet());
            } else {
                eventHandlers.clear();
                firstEventHandler = null;
            }
        }
    }

    // FRAG_TODO doesn't work yet
    public void filterBroadcasts(Object sender, long eventTypes) {
        filters.put(sender, eventTypes);
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

    /**
     * @return the last set of date flags sent with {@link EventType#UPDATE_TITLE}
     */
    public long getDateFlags() {
        return dateFlags;
    }

    /**
     * @return the last event ID the edit view was launched with
     */
    public long getEventId() {
        return eventId;
    }

    // Sets the eventId. Should only be used for initialization.
    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public int getViewType() {
        return viewType;
    }

    // Forces the viewType. Should only be used for initialization.
    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public int getPreviousViewType() {
        return previousViewType;
    }

//    private void launchSelectVisibleCalendars() {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setClass(context, SelectVisibleCalendarsActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        context.startActivity(intent);
//    }
//
//    private void launchSettings() {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setClass(context, CalendarSettingsActivity.class);
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
//
//    private void launchEditEvent(long eventId, long startMillis, long endMillis, boolean edit) {
//        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
//        Intent intent = new Intent(Intent.ACTION_EDIT, uri);
//        intent.putExtra(EXTRA_EVENT_BEGIN_TIME, startMillis);
//        intent.putExtra(EXTRA_EVENT_END_TIME, endMillis);
//        intent.setClass(context, EditEventActivity.class);
//        intent.putExtra(EVENT_EDIT_ON_LAUNCH, edit);
//        eventId = eventId;
//        context.startActivity(intent);
//    }

//    private void launchAlerts() {
//        Intent intent = new Intent();
//        intent.setClass(context, AlertActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

    private void launchSearch(long eventId, String query, ComponentName componentName) {
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

    private String eventInfoToString(EventInfo eventInfo) {
        String tmp = "Unknown";
        StringBuilder builder = new StringBuilder();

        if ((eventInfo.eventType & EventType.GO_TO) != 0) {
            tmp = "Go to time/event";
        } else if ((eventInfo.eventType & EventType.CREATE_EVENT) != 0) {
            tmp = "New event";
        } else if ((eventInfo.eventType & EventType.VIEW_EVENT) != 0) {
            tmp = "View event";
        } else if ((eventInfo.eventType & EventType.VIEW_EVENT_DETAILS) != 0) {
            tmp = "View details";
        } else if ((eventInfo.eventType & EventType.EDIT_EVENT) != 0) {
            tmp = "Edit event";
        } else if ((eventInfo.eventType & EventType.DELETE_EVENT) != 0) {
            tmp = "Delete event";
        } else if ((eventInfo.eventType & EventType.LAUNCH_SELECT_VISIBLE_CALENDARS) != 0) {
            tmp = "Launch select visible calendars";
        } else if ((eventInfo.eventType & EventType.LAUNCH_SETTINGS) != 0) {
            tmp = "Launch settings";
        } else if ((eventInfo.eventType & EventType.EVENTS_CHANGED) != 0) {
            tmp = "Refresh events";
        } else if ((eventInfo.eventType & EventType.SEARCH) != 0) {
            tmp = "Search";
        } else if ((eventInfo.eventType & EventType.USER_HOME) != 0) {
            tmp = "Gone home";
        } else if ((eventInfo.eventType & EventType.UPDATE_TITLE) != 0) {
            tmp = "Update title";
        }

        builder.append(tmp);
        builder.append(": id=");
        builder.append(eventInfo.id);
        builder.append(", selected=");
        builder.append(eventInfo.selectedTime);
        builder.append(", start=");
        builder.append(eventInfo.startTime);
        builder.append(", end=");
        builder.append(eventInfo.endTime);
        builder.append(", viewType=");
        builder.append(eventInfo.viewType);
        builder.append(", x=");
        builder.append(eventInfo.x);
        builder.append(", y=");
        builder.append(eventInfo.y);

        return builder.toString();
    }
}

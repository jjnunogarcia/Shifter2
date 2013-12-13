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

import android.content.Context;
import android.content.res.Configuration;
import android.text.format.Time;
import android.util.Log;
import android.view.*;
import android.view.View.OnTouchListener;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

/**
 * <p>
 * This is a specialized adapter for creating a list of weeks with selectable days. It can be configured to display the week number, start the week on a
 * given day, show a reduced number of days, or display an arbitrary number of weeks at a time. See {@link MonthFragment} for usage.
 * </p>
 */
public class MonthAdapter extends BaseAdapter implements OnTouchListener {
  private static final String TAG                       = MonthAdapter.class.getSimpleName();
  /**
   * The number of weeks to display at a time.
   */
  public static final  String WEEK_PARAMS_NUM_WEEKS     = "num_weeks";
  /**
   * Which month should be in focus currently.
   */
  public static final  String WEEK_PARAMS_FOCUS_MONTH   = "focus_month";
  /**
   * Whether the week number should be shown. Non-zero to show them.
   */
  public static final  String WEEK_PARAMS_SHOW_WEEK     = "week_numbers";
  /**
   * Which day the week should start on. {@link android.text.format.Time#SUNDAY} through {@link android.text.format.Time#SATURDAY}.
   */
  public static final  String WEEK_PARAMS_WEEK_START    = "week_start";
  /**
   * The Julian day to highlight as selected.
   */
  public static final  String WEEK_PARAMS_JULIAN_DAY    = "selected_day";
  public static final  String WEEK_PARAMS_DAYS_PER_WEEK = "days_per_week";
  private static final int    WEEK_COUNT                = 3497;
  private static final int    DEFAULT_NUM_WEEKS         = 6;
  private static final int    DEFAULT_MONTH_FOCUS       = 0;
  private static final int    DEFAULT_DAYS_PER_WEEK     = 7;
  private static final long   ANIMATE_TODAY_TIMEOUT     = 1000;

  private Context                     context;
  private Time                        selectedDay;
  private int                         selectedWeek;
  private int                         firstDayOfWeek;
  private boolean                     showWeekNumber;
  private GestureDetector             gestureDetector;
  private int                         numWeeks;
  private int                         daysPerWeek;
  private int                         focusMonth;
  private ListView                    listView;
  private CalendarController          controller;
  private String                      homeTimeZone;
  private Time                        tempTime;
  private Time                        today;
  private int                         firstJulianDay;
  private int                         orientation;
  private int                         totalClickDelay;
  private int                         onDownDelay;
  private float                       movedPixelToCancel;
  private boolean                     showAgendaWithMonth;
  private ArrayList<ArrayList<Event>> eventDayList;
  private ArrayList<Event>            events;
  private boolean                     animateToday;
  private long                        animateTime;
  private MonthWeekEventsView         clickedView;
  private MonthWeekEventsView         singleTapUpView;
  private float                       clickedXLocation;                // Used to find which day was clicked
  private long                        clickTime;                        // Used to calculate minimum click animation time

  public MonthAdapter(Context context, HashMap<String, Integer> params) {
    this.context = context;

    // Get default week start based on locale, subtracting one for use with android Time.
    Calendar calendar = Calendar.getInstance(Locale.getDefault());
    firstDayOfWeek = calendar.getFirstDayOfWeek() - 1;
    gestureDetector = new GestureDetector(context, new CalendarGestureListener());
    selectedDay = new Time();
    selectedDay.setToNow();
    showWeekNumber = false;
    numWeeks = DEFAULT_NUM_WEEKS;
    daysPerWeek = DEFAULT_DAYS_PER_WEEK;
    focusMonth = DEFAULT_MONTH_FOCUS;
    controller = new CalendarController(context);
    homeTimeZone = Utils.getTimeZone(context, null);
    selectedDay.switchTimezone(homeTimeZone);
    today = new Time(homeTimeZone);
    today.setToNow();
    tempTime = new Time(homeTimeZone);
    updateParams(params);
    orientation = Configuration.ORIENTATION_LANDSCAPE;
    eventDayList = new ArrayList<ArrayList<Event>>();
    animateToday = false;
    animateTime = 0;
    int onTapDelay = 100;
    showAgendaWithMonth = Utils.getConfigBool(context, R.bool.show_agenda_with_month);
    ViewConfiguration vc = ViewConfiguration.get(context);
    onDownDelay = ViewConfiguration.getTapTimeout();
    movedPixelToCancel = vc.getScaledTouchSlop();
    totalClickDelay = onDownDelay + onTapDelay;

  }

  /**
   * Parse the parameters and set any necessary fields. See {@link #WEEK_PARAMS_NUM_WEEKS} for parameter details.
   *
   * @param params A list of parameters for this adapter
   */
  public void updateParams(HashMap<String, Integer> params) {
    if (params == null) {
      Log.e(TAG, "WeekParameters are null! Cannot update adapter.");
      return;
    }
    if (params.containsKey(WEEK_PARAMS_FOCUS_MONTH)) {
      focusMonth = params.get(WEEK_PARAMS_FOCUS_MONTH);
    }
    if (params.containsKey(WEEK_PARAMS_FOCUS_MONTH)) {
      numWeeks = params.get(WEEK_PARAMS_NUM_WEEKS);
    }
    if (params.containsKey(WEEK_PARAMS_SHOW_WEEK)) {
      showWeekNumber = params.get(WEEK_PARAMS_SHOW_WEEK) != 0;
    }
    if (params.containsKey(WEEK_PARAMS_WEEK_START)) {
      firstDayOfWeek = params.get(WEEK_PARAMS_WEEK_START);
    }
    if (params.containsKey(WEEK_PARAMS_JULIAN_DAY)) {
      int julianDay = params.get(WEEK_PARAMS_JULIAN_DAY);
      selectedDay.setJulianDay(julianDay);
      selectedWeek = getWeeksSinceEpochFromJulianDay(julianDay, firstDayOfWeek);
    }
    if (params.containsKey(WEEK_PARAMS_DAYS_PER_WEEK)) {
      daysPerWeek = params.get(WEEK_PARAMS_DAYS_PER_WEEK);
    }
    notifyDataSetChanged();
  }

  public void animateToday() {
    animateToday = true;
    animateTime = System.currentTimeMillis();
  }

  private void updateTimeZones() {
    selectedDay.timezone = homeTimeZone;
    selectedDay.normalize(true);
    today.timezone = homeTimeZone;
    today.setToNow();
    tempTime.switchTimezone(homeTimeZone);
  }

  /**
   * Updates the selected day and related parameters.
   *
   * @param selectedTime The time to highlight
   */
  public void setSelectedDay(Time selectedTime) {
    selectedDay.set(selectedTime);
    long millis = selectedDay.normalize(true);
    selectedWeek = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(millis, selectedDay.gmtoff), firstDayOfWeek);
    notifyDataSetChanged();
  }

  private int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
    int diff = Time.THURSDAY - firstDayOfWeek;
    if (diff < 0) {
      diff += 7;
    }
    int refDay = Time.EPOCH_JULIAN_DAY - diff;
    return (julianDay - refDay) / 7;
  }

  public Time getSelectedDay() {
    return selectedDay;
  }

  @Override
  public int getCount() {
    return WEEK_COUNT;
  }

  @Override
  public Object getItem(int position) {
    return null;
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  public void setEvents(int firstJulianDay, int numDays, ArrayList<Event> events) {
    this.events = events;
    this.firstJulianDay = firstJulianDay;
    // Create a new list, this is necessary since the weeks are referencing pieces of the old list
    ArrayList<ArrayList<Event>> eventDayList = new ArrayList<ArrayList<Event>>();
    for (int i = 0; i < numDays; i++) {
      eventDayList.add(new ArrayList<Event>());
    }

    if (events == null || events.size() == 0) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "No events. Returning early--go schedule something fun.");
      }
      this.eventDayList = eventDayList;
      refresh();
      return;
    }

    // Compute the new set of days with events
    for (Event event : events) {
      int startDay = event.startDay - this.firstJulianDay;
      int endDay = event.endDay - this.firstJulianDay + 1;
      if (startDay < numDays || endDay >= 0) {
        if (startDay < 0) {
          startDay = 0;
        }
        if (startDay > numDays) {
          continue;
        }
        if (endDay < 0) {
          continue;
        }
        if (endDay > numDays) {
          endDay = numDays;
        }
        for (int j = startDay; j < endDay; j++) {
          eventDayList.get(j).add(event);
        }
      }
    }
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "Processed " + events.size() + " events.");
    }
    this.eventDayList = eventDayList;
    refresh();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    MonthWeekEventsView v;
    LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    HashMap<String, Integer> drawingParams = null;
    boolean isAnimatingToday = false;

    if (convertView != null) {
      v = (MonthWeekEventsView) convertView;
      // Checking updateToday uses the current params instead of the new params, so this is assuming the view is relatively stable
      if (animateToday && v.updateToday(selectedDay.timezone)) {
        long currentTime = System.currentTimeMillis();
        // If it's been too long since we tried to start the animation don't show it. This can happen if the user stops a scroll before reaching today.
        if (currentTime - animateTime > ANIMATE_TODAY_TIMEOUT) {
          animateToday = false;
          animateTime = 0;
        } else {
          isAnimatingToday = true;
          // There is a bug that causes invalidates to not work some of the time unless we recreate the view.
          v = new MonthWeekEventsView(context);
        }
      } else {
        drawingParams = (HashMap<String, Integer>) v.getTag();
      }
    } else {
      v = new MonthWeekEventsView(context);
    }

    if (drawingParams == null) {
      drawingParams = new HashMap<String, Integer>();
    }

    drawingParams.clear();

    v.setLayoutParams(params);
    v.setClickable(true);
    v.setOnTouchListener(this);

    int selectedDay = -1;
    if (selectedWeek == position) {
      selectedDay = this.selectedDay.weekDay;
    }

    drawingParams.put(SimpleWeekView.VIEW_PARAMS_HEIGHT, (parent.getHeight() + parent.getTop()) / numWeeks);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_SELECTED_DAY, selectedDay);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_SHOW_WK_NUM, showWeekNumber ? 1 : 0);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_WEEK_START, firstDayOfWeek);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_NUM_DAYS, daysPerWeek);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_WEEK, position);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_FOCUS_MONTH, focusMonth);
    drawingParams.put(MonthWeekEventsView.VIEW_PARAMS_ORIENTATION, orientation);

    if (isAnimatingToday) {
      drawingParams.put(MonthWeekEventsView.VIEW_PARAMS_ANIMATE_TODAY, 1);
      animateToday = false;
    }

    v.setWeekParams(drawingParams, this.selectedDay.timezone);
    sendEventsToView(v);
    return v;
  }

  /**
   * Changes which month is in focus and updates the view.
   *
   * @param month The month to show as in focus [0-11]
   */
  public void updateFocusMonth(int month) {
    focusMonth = month;
    notifyDataSetChanged();
  }

  private void sendEventsToView(MonthWeekEventsView v) {
    if (eventDayList.size() == 0) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "No events loaded, did not pass any events to view.");
      }
      v.setEvents(null, null);
      return;
    }
    int viewJulianDay = v.getFirstJulianDay();
    int start = viewJulianDay - firstJulianDay;
    int end = start + v.mNumDays;
    if (start < 0 || end > eventDayList.size()) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Week is outside range of loaded events. viewStart: " + viewJulianDay + " eventsStart: " + firstJulianDay);
      }
      v.setEvents(null, null);
      return;
    }
    v.setEvents(eventDayList.subList(start, end), events);
  }

  protected void refresh() {
    notifyDataSetChanged();
    firstDayOfWeek = Utils.getFirstDayOfWeek(context);
    showWeekNumber = Utils.getShowWeekNumber(context);
    homeTimeZone = Utils.getTimeZone(context, null);
    orientation = context.getResources().getConfiguration().orientation;
    updateTimeZones();
    notifyDataSetChanged();
  }

  /**
   * Maintains the same hour/min/sec but moves the day to the tapped day.
   *
   * @param day The day that was tapped
   */
  private void onDayTapped(Time day) {
    day.timezone = homeTimeZone;
    Time currTime = new Time(homeTimeZone);
    currTime.set(controller.getTime());
    day.hour = currTime.hour;
    day.minute = currTime.minute;
    day.allDay = false;
    day.normalize(true);
    if (showAgendaWithMonth) {
      // If agenda view is visible with month view , refresh the views with the selected day's info
      controller.sendEvent(EventType.GO_TO, day, day, -1, ViewType.CURRENT, CalendarController.EXTRA_GOTO_DATE, null, null);
    } else {
      // Else , switch to the detailed view
      controller.sendEvent(EventType.GO_TO, day, day, -1, ViewType.DETAIL, CalendarController.EXTRA_GOTO_DATE | CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS, null, null);
    }
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    int action = event.getAction();

    // Event was tapped - switch to the detailed view making sure the click animation is done first.
    if (gestureDetector.onTouchEvent(event)) {
      singleTapUpView = (MonthWeekEventsView) v;
      long delay = System.currentTimeMillis() - clickTime;
      // Make sure the animation is visible for at least onTapDelay - onDownDelay ms
      listView.postDelayed(doSingleTapUp, delay > totalClickDelay ? 0 : totalClickDelay - delay);
      return true;
    } else {
      // Animate a click - on down: show the selected day in the "clicked" color.
      // On Up/scroll/move/cancel: hide the "clicked" color.
      switch (action) {
        case MotionEvent.ACTION_DOWN:
          clickedView = (MonthWeekEventsView) v;
          clickedXLocation = event.getX();
          clickTime = System.currentTimeMillis();
          listView.postDelayed(doClick, onDownDelay);
          break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_SCROLL:
        case MotionEvent.ACTION_CANCEL:
          clearClickedView((MonthWeekEventsView) v);
          break;
        case MotionEvent.ACTION_MOVE:
          // No need to cancel on vertical movement, ACTION_SCROLL will do that.
          if (Math.abs(event.getX() - clickedXLocation) > movedPixelToCancel) {
            clearClickedView((MonthWeekEventsView) v);
          }
          break;
        default:
          break;
      }
    }
    // Do not tell the frameworks we consumed the touch action so that fling actions can be processed by the fragment.
    return false;
  }

  // Clear the visual cues of the click animation and related running code.
  private void clearClickedView(MonthWeekEventsView v) {
    listView.removeCallbacks(doClick);
    v.clearClickedDay();
    clickedView = null;
  }

  // Perform the tap animation in a runnable to allow a delay before showing the tap color.
  // This is done to prevent a click animation when a fling is done.
  private final Runnable doClick = new Runnable() {
    @Override
    public void run() {
      if (clickedView != null) {
        synchronized (clickedView) {
          clickedView.setClickedDay(clickedXLocation);
        }
        clickedView = null;
        // This is a workaround , sometimes the top item on the listview doesn't refresh on invalidate, so this forces a re-draw.
        listView.invalidate();
      }
    }
  };

  // Performs the single tap operation: go to the tapped day.
  // This is done in a runnable to allow the click animation to finish before switching views
  private final Runnable doSingleTapUp = new Runnable() {
    @Override
    public void run() {
      if (singleTapUpView != null) {
        Time day = singleTapUpView.getDayFromLocation(clickedXLocation);
        if (day != null) {
          onDayTapped(day);
        }
        clearClickedView(singleTapUpView);
        singleTapUpView = null;
      }
    }
  };

  /**
   * This is here so we can identify single tap events and set the selected day correctly
   */
  private class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
      return true;
    }
  }

  public void setListView(ListView lv) {
    listView = lv;
  }
}

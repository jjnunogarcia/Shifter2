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

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Instances;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.*;
import android.view.View.OnTouchListener;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import java.util.*;

/**
 * <p>
 * This displays a titled list of weeks with selectable days. It can be configured to display the week number, start the week on a given day, show a reduced number of days, or display an
 * arbitrary number of weeks at a time. By overriding methods and changing variables this fragment can be customized to easily display a month selection component in a given style.
 * </p>
 */
public class MonthFragment extends ListFragment implements EventHandler, LoaderManager.LoaderCallbacks<Cursor>, OnScrollListener, OnTouchListener {
  public static final    String          TAG                     = MonthFragment.class.getSimpleName();
  // The number of days to display in each week
  public static final    int             DAYS_PER_WEEK           = 7;
  // The size of the month name displayed above the week list
  public static final    String          INITIAL_TIME            = "initial_time";
  // Affects when the month selection will change while scrolling up
  protected static final int             SCROLL_HYST_WEEKS       = 2;
  protected static final int             GOTO_SCROLL_DURATION    = 500;
  // How long to wait after receiving an onScrollStateChanged notification before acting on it
  protected static final int             SCROLL_CHANGE_DELAY     = 40;
  // Selection and selection args for adding event queries
  private static final   String          WHERE_CALENDARS_VISIBLE = "visible=1";
  private static final   int             WEEKS_BUFFER            = 1;
  // How long to wait after scroll stops before starting the loader
  // Using scroll duration because scroll state changes don't update correctly when a scroll is triggered programmatically.
  private static final   int             LOADER_DELAY            = 200;
  // The minimum time between requeries of the data if the db is changing
  private static final   int             LOADER_THROTTLE_DELAY   = 500;
  public static          int             LIST_TOP_OFFSET         = -1;  // so that the top line will be under the separator
  protected static       StringBuilder   mSB                     = new StringBuilder(50);
  protected static       Formatter       mF                      = new Formatter(mSB, Locale.getDefault());
  private final          Runnable        timeZoneUpdater         = new Runnable() {
    @Override
    public void run() {
      String tz = Utils.getTimeZone(context, this);
      selectedDay.timezone = tz;
      selectedDay.normalize(true);
      tempTime.timezone = tz;
      firstDayOfMonth.timezone = tz;
      firstDayOfMonth.normalize(true);
      firstVisibleDay.timezone = tz;
      firstVisibleDay.normalize(true);
      if (adapter != null) {
        adapter.notifyDataSetChanged();
      }
    }
  };
  private final          Runnable        updateLoader            = new Runnable() {
    @Override
    public void run() {
      if (shouldLoad && cursorLoader != null) {
        // Stop any previous loads while we update the uri
        stopLoader();

        // Start the loader again
        eventUri = updateUri();
        cursorLoader.setUri(eventUri);
        cursorLoader.startLoading();
        cursorLoader.onContentChanged();
      }
    }
  };
  // This causes an update of the view at midnight
  private final          Runnable        todayUpdater            = new Runnable() {
    @Override
    public void run() {
      Time midnight = new Time(firstVisibleDay.timezone);
      midnight.setToNow();
      long currentMillis = midnight.toMillis(true);

      midnight.hour = 0;
      midnight.minute = 0;
      midnight.second = 0;
      midnight.monthDay++;
      long millisToMidnight = midnight.normalize(true) - currentMillis;
      handler.postDelayed(this, millisToMidnight);

      if (adapter != null) {
        adapter.notifyDataSetChanged();
      }
    }
  };
  // This allows us to update our position when a day is tapped
  private final          DataSetObserver observer                = new DataSetObserver() {
    @Override
    public void onChanged() {
      Time day = adapter.getSelectedDay();
      if (day.year != selectedDay.year || day.yearDay != selectedDay.yearDay) {
        goTo(day.toMillis(true), true, true, false);
      }
    }
  };
  // Used to load the events when a delay is needed
  private final          Runnable        loadingRunnable         = new Runnable() {
    @Override
    public void run() {
      if (!isDetached()) {
        cursorLoader = (CursorLoader) getLoaderManager().initLoader(0, null, MonthFragment.this);
      }
    }
  };
  private Context             context;
  private int                 weekMinVisibleHeight;
  private int                 bottomBuffer;
  private Handler             handler;
  private CursorLoader        cursorLoader;
  private Uri                 eventUri;
  private Time                desiredDay;
  private boolean             shouldLoad;
  private boolean             userScrolled;
  private boolean             showDetailsInMonth;
  private boolean             hideDeclined;
  private int                 firstLoadedJulianDay;
  private int                 lastLoadedJulianDay;
  private Time                selectedDay;
  private MonthAdapter        adapter;
  private ListView            listView;
  private ViewGroup           dayNamesHeader;
  private String[]            dayLabels;
  private Time                tempTime;
  private int                 firstDayOfWeek;
  private Time                firstDayOfMonth;
  private Time                firstVisibleDay;
  private TextView            monthName;
  private int                 currentMonthDisplayed;
  private long                previousScrollPosition;
  private boolean             isScrollingUp;
  private int                 previousScrollState;
  private int                 currentScrollState;
  private int                 saturdayColor;
  private int                 sundayColor;
  private int                 dayNameColor;
  private int                 numWeeks;
  private boolean             showWeekNumber;
  private int                 daysPerWeek;
  private float               friction;
  private ScrollStateRunnable scrollStateChangedRunnable;
  private long                initialTime;
  private CalendarController  calendarController;
  private TextView            header;

  public MonthFragment() {
    weekMinVisibleHeight = 12;
    bottomBuffer = 20;
    selectedDay = new Time();
    desiredDay = new Time();
    tempTime = new Time();
    firstDayOfMonth = new Time();
    firstVisibleDay = new Time();
    isScrollingUp = false;
    previousScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    currentScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    saturdayColor = 0;
    sundayColor = 0;
    dayNameColor = 0;
    numWeeks = 6;
    showWeekNumber = false;
    daysPerWeek = 7;
    friction = 1.0f;
    shouldLoad = true;
    userScrolled = false;
    showDetailsInMonth = false;
    scrollStateChangedRunnable = new ScrollStateRunnable();
    handler = new Handler();
    initialTime = -1;

    String currentTimezone = Time.getCurrentTimezone();
    // Ensure we're in the correct time zone
    selectedDay.switchTimezone(currentTimezone);
    selectedDay.normalize(true);
    firstDayOfMonth.timezone = currentTimezone;
    firstDayOfMonth.normalize(true);
    firstVisibleDay.timezone = currentTimezone;
    firstVisibleDay.normalize(true);
    tempTime.timezone = currentTimezone;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Bundle arguments = getArguments();

    if (arguments != null) {
      initialTime = arguments.getLong(INITIAL_TIME, -1);
    }

    View view = inflater.inflate(R.layout.month_fragment, container, false);
    dayNamesHeader = (ViewGroup) view.findViewById(R.id.day_names);
    monthName = (TextView) view.findViewById(R.id.month_name);
    header = (TextView) dayNamesHeader.findViewById(R.id.wk_label);

    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    context = getActivity().getApplicationContext();
    calendarController = ((CustomApplication) getActivity().getApplication()).getCalendarController();

    Resources res = getResources();
    saturdayColor = res.getColor(R.color.month_saturday);
    sundayColor = res.getColor(R.color.month_sunday);
    dayNameColor = res.getColor(R.color.month_day_names_color);

    // Adjust sizes for screen density
    float scale = res.getDisplayMetrics().density;
    if (scale != 1) {
      weekMinVisibleHeight *= scale;
      bottomBuffer *= scale;
      LIST_TOP_OFFSET *= scale;
    }

    setUpHeader();
    setUpListView();
    setUpAdapter();
    setListAdapter(adapter);
    timeZoneUpdater.run();

    adapter.setSelectedDay(selectedDay);

    showDetailsInMonth = res.getBoolean(R.bool.show_details_in_month);

    listView.post(new Runnable() {
      @Override
      public void run() {
        MonthView child = (MonthView) listView.getChildAt(0);
        if (child == null) {
          return;
        }
        int julianDay = child.getFirstJulianDay();
        firstVisibleDay.setJulianDay(julianDay);
        // set the title to the month of the second week
        tempTime.setJulianDay(julianDay + DAYS_PER_WEEK);
        setMonthDisplayed(tempTime, true);

        cursorLoader = (CursorLoader) getLoaderManager().initLoader(0, null, MonthFragment.this);
        adapter.setListView(listView);

        goTo(initialTime, false, true, true);
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();
    setUpAdapter();
    doResumeUpdates();
  }

  @Override
  public void onPause() {
    super.onPause();
    handler.removeCallbacks(todayUpdater);
  }

  private void setUpHeader() {
    dayLabels = new String[7];
    for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
      dayLabels[i - Calendar.SUNDAY] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_MEDIUM).toUpperCase();
    }
  }

  private void setUpListView() {
    listView = getListView();
    listView.setCacheColorHint(0);
    listView.setDivider(null);
    listView.setItemsCanFocus(true);
    listView.setFastScrollEnabled(false);
    listView.setVerticalScrollBarEnabled(false);
    listView.setOnScrollListener(this);
    listView.setFadingEdgeLength(0);
    listView.setFriction(ViewConfiguration.getScrollFriction() * friction);
    listView.setOnTouchListener(this);
    listView.setBackgroundColor(getResources().getColor(R.color.month_bgcolor));
  }

  private void setUpAdapter() {
    firstDayOfWeek = Utils.getFirstDayOfWeek(context);
    showWeekNumber = Utils.getShowWeekNumber(context);

    HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
    weekParams.put(MonthAdapter.WEEK_PARAMS_NUM_WEEKS, numWeeks);
    weekParams.put(MonthAdapter.WEEK_PARAMS_SHOW_WEEK, showWeekNumber ? 1 : 0);
    weekParams.put(MonthAdapter.WEEK_PARAMS_WEEK_START, firstDayOfWeek);
    weekParams.put(MonthAdapter.WEEK_PARAMS_JULIAN_DAY, Time.getJulianDay(selectedDay.toMillis(true), selectedDay.gmtoff));
    weekParams.put(MonthAdapter.WEEK_PARAMS_DAYS_PER_WEEK, daysPerWeek);

    if (adapter == null) {
      adapter = new MonthAdapter(getActivity().getApplicationContext(), calendarController, weekParams);
      adapter.registerDataSetObserver(observer);
    } else {
      adapter.updateParams(weekParams);
    }
    adapter.notifyDataSetChanged();
  }

  private void updateHeader() {
    if (showWeekNumber) {
      header.setVisibility(View.VISIBLE);
    } else {
      header.setVisibility(View.GONE);
    }
    int offset = firstDayOfWeek - 1;
    for (int i = 1; i < 8; i++) {
      header = (TextView) dayNamesHeader.getChildAt(i);
      if (i < daysPerWeek + 1) {
        int position = (offset + i) % 7;
        header.setText(dayLabels[position]);
        header.setVisibility(View.VISIBLE);
        if (position == Time.SATURDAY) {
          header.setTextColor(saturdayColor);
        } else if (position == Time.SUNDAY) {
          header.setTextColor(sundayColor);
        } else {
          header.setTextColor(dayNameColor);
        }
      } else {
        header.setVisibility(View.GONE);
      }
    }
    dayNamesHeader.invalidate();
  }

  private void doResumeUpdates() {
    firstDayOfWeek = Utils.getFirstDayOfWeek(context);
    showWeekNumber = Utils.getShowWeekNumber(context);
    boolean prevHideDeclined = hideDeclined;
    hideDeclined = Utils.getHideDeclinedEvents(context);
    if (prevHideDeclined != hideDeclined && cursorLoader != null) {
      cursorLoader.setSelection(updateWhere());
    }
    daysPerWeek = Utils.getDaysPerWeek(context);
    updateHeader();
    adapter.setSelectedDay(selectedDay);
    timeZoneUpdater.run();
    todayUpdater.run();
    goTo(selectedDay.toMillis(true), false, true, false);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    CursorLoader loader;
    firstLoadedJulianDay = Time.getJulianDay(selectedDay.toMillis(true), selectedDay.gmtoff) - (numWeeks * 7 / 2);
    eventUri = updateUri();

    String where = updateWhere();
    loader = new CursorLoader(getActivity().getApplicationContext(), eventUri, new String[]{DBConstants.ID, DBConstants.EVENT, DBConstants.LOCATION, DBConstants.DESCRIPTION,
        DBConstants.START, DBConstants.END, DBConstants.CALENDAR_ID, DBConstants.EVENT_ID,
        DBConstants.START_DAY, DBConstants.END_DAY, DBConstants.START_TIME, DBConstants.END_TIME}
                                                                                                /*Event.EVENT_PROJECTION*/, /*where*/null,
                                                                                                null /* WHERE_CALENDARS_SELECTED_ARGS */, null/*INSTANCES_SORT_ORDER*/);
    loader.setUpdateThrottle(LOADER_THROTTLE_DELAY);
    return loader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    CursorLoader cLoader = (CursorLoader) loader;
    if (eventUri == null) {
      eventUri = cLoader.getUri();
      updateLoadedDays();
    }
    if (cLoader.getUri().compareTo(eventUri) != 0) {
      // We've started a new query since this loader ran so ignore the result
      return;
    }

    ArrayList<Event> events = Event.buildEventsFromCursor(data, context, firstLoadedJulianDay, lastLoadedJulianDay);
    adapter.setEvents(firstLoadedJulianDay, lastLoadedJulianDay - firstLoadedJulianDay + 1, events);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
  }

  private void stopLoader() {
    handler.removeCallbacks(updateLoader);
    if (cursorLoader != null) {
      cursorLoader.stopLoading();
    }
  }

  @Override
  public long getSupportedEventTypes() {
    return EventType.GO_TO | EventType.EVENTS_CHANGED;
  }

  @Override
  public void handleEvent(EventInfo event) {
    if (event.eventType == EventType.GO_TO) {
      boolean animate = true;
      if (daysPerWeek * numWeeks * 2 < Math.abs(Time.getJulianDay(event.selectedTime.toMillis(true), event.selectedTime.gmtoff)
                                                - Time.getJulianDay(firstVisibleDay.toMillis(true), firstVisibleDay.gmtoff)
                                                - daysPerWeek * numWeeks / 2)) {
        animate = false;
      }
      desiredDay.set(event.selectedTime);
      desiredDay.normalize(true);
      boolean animateToday = (event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0;
      boolean delayAnimation = goTo(event.selectedTime.toMillis(true), animate, true, false);

      if (animateToday) {
        // If we need to flash today start the animation after any movement from listView has ended.
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            adapter.animateToday();
            adapter.notifyDataSetChanged();
          }
        }, delayAnimation ? GOTO_SCROLL_DURATION : 0);
      }
    } else if (event.eventType == EventType.EVENTS_CHANGED) {
      eventsChanged();
    }
  }

  @Override
  public void eventsChanged() {
    // TODO remove this after b/3387924 is resolved
    if (cursorLoader != null) {
      cursorLoader.forceLoad();
    }
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    desiredDay.setToNow();
    return false;
    // TODO post a cleanup to push us back onto the grid if something went wrong in a scroll such as the user stopping the view but not scrolling
  }

  /**
   * This moves to the specified time in the view. If the time is not already in range it will move the list so that the first of the month containing the time is at the top of the view.
   * If the new time is already in view the list will not be scrolled unless forceScroll is true. This time may optionally be highlighted as selected as well.
   *
   * @param time        The time to move to
   * @param animate     Whether to scroll to the given time or just redraw at the new location
   * @param setSelected Whether to set the given time as selected
   * @param forceScroll Whether to recenter even if the time is already visible
   * @return Whether or not the view animated to the new location
   */
  private boolean goTo(long time, boolean animate, boolean setSelected, boolean forceScroll) {
    if (time == -1) {
      return false;
    }

    // Set the selected day
    if (setSelected) {
      selectedDay.set(time);
      selectedDay.normalize(true);
    }

    // If this view isn't returned yet we won't be able to load the lists current position, so return after setting the selected day.
//    if (!isResumed()) {
//      return false;
//    }

    tempTime.set(time);
    long millis = tempTime.normalize(true);
    // Get the week we're going to
    // TODO push Util function into Calendar public api.
    int position = getWeeksSinceEpochFromJulianDay(Time.getJulianDay(millis, tempTime.gmtoff), firstDayOfWeek);

    View child;
    int i = 0;
    int top = 0;
    // Find a child that's completely in the view
    do {
      child = listView.getChildAt(i++);
      if (child == null) {
        break;
      }
      top = child.getTop();
    } while (top < 0);

    // Compute the first and last position visible
    int firstPosition;
    if (child != null) {
      firstPosition = listView.getPositionForView(child);
    } else {
      firstPosition = 0;
    }
    int lastPosition = firstPosition + numWeeks - 1;
    if (top > bottomBuffer) {
      lastPosition--;
    }

    if (setSelected) {
      adapter.setSelectedDay(selectedDay);
    }

    // Check if the selected day is now outside of our visible range and, if so, scroll to the month that contains it
    if (position < firstPosition || position > lastPosition || forceScroll) {
      firstDayOfMonth.set(tempTime);
      firstDayOfMonth.monthDay = 1;
      millis = firstDayOfMonth.normalize(true);
      setMonthDisplayed(firstDayOfMonth, true);
      position = getWeeksSinceEpochFromJulianDay(Time.getJulianDay(millis, firstDayOfMonth.gmtoff), firstDayOfWeek);

      previousScrollState = OnScrollListener.SCROLL_STATE_FLING;
      if (animate) {
        listView.smoothScrollToPositionFromTop(position, LIST_TOP_OFFSET, GOTO_SCROLL_DURATION);
        return true;
      } else {
        listView.setSelectionFromTop(position, LIST_TOP_OFFSET);
        // Perform any after scroll operations that are needed
        onScrollStateChanged(listView, OnScrollListener.SCROLL_STATE_IDLE);
      }
    } else if (setSelected) {
      // Otherwise just set the selection
      setMonthDisplayed(selectedDay, true);
    }
    return false;
  }

  private int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
    int diff = Time.THURSDAY - firstDayOfWeek;
    if (diff < 0) {
      diff += 7;
    }
    int refDay = Time.EPOCH_JULIAN_DAY - diff;
    return (julianDay - refDay) / 7;
  }

  /**
   * Updates the title and selected month if the view has moved to a new month.
   */
  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    MonthView child = (MonthView) view.getChildAt(0);
    if (child == null) {
      return;
    }

    // Figure out where we are
    long currScroll = view.getFirstVisiblePosition() * child.getHeight() - child.getBottom();
    firstVisibleDay.setJulianDay(child.getFirstJulianDay());

    // If we have moved since our last call update the direction
    if (currScroll < previousScrollPosition) {
      isScrollingUp = true;
    } else if (currScroll > previousScrollPosition) {
      isScrollingUp = false;
    } else {
      return;
    }

    previousScrollPosition = currScroll;
    previousScrollState = currentScrollState;

    updateMonthHighlight(listView);
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {
    if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
      shouldLoad = false;
      stopLoader();
      desiredDay.setToNow();
    } else {
      handler.removeCallbacks(updateLoader);
      shouldLoad = true;
      handler.postDelayed(updateLoader, LOADER_DELAY);
    }
    if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
      userScrolled = true;
    }

    scrollStateChangedRunnable.doScrollStateChange(scrollState);
  }

  /**
   * Figures out if the month being shown has changed and updates the highlight if needed
   *
   * @param view The ListView containing the weeks
   */
  private void updateMonthHighlight(AbsListView view) {
    MonthView child = (MonthView) view.getChildAt(0);
    if (child == null) {
      return;
    }

    // Figure out where we are
    int offset = child.getBottom() < weekMinVisibleHeight ? 1 : 0;
    // Use some hysteresis for checking which month to highlight. This
    // causes the month to transition when two full weeks of a month are
    // visible.
    child = (MonthView) view.getChildAt(SCROLL_HYST_WEEKS + offset);

    if (child == null) {
      return;
    }

    // Find out which month we're moving into
    int month;
    if (isScrollingUp) {
      month = child.getFirstMonth();
    } else {
      month = child.getLastMonth();
    }

    // And how it relates to our current highlighted month
    int monthDiff;
    if (currentMonthDisplayed == 11 && month == 0) {
      monthDiff = 1;
    } else if (currentMonthDisplayed == 0 && month == 11) {
      monthDiff = -1;
    } else {
      monthDiff = month - currentMonthDisplayed;
    }

    // Only switch months if we're scrolling away from the currently
    // selected month
    if (monthDiff != 0) {
      int julianDay = child.getFirstJulianDay();
      if (isScrollingUp) {
        // Takes the start of the week
      } else {
        // Takes the start of the following week
        julianDay += DAYS_PER_WEEK;
      }
      tempTime.setJulianDay(julianDay);
      setMonthDisplayed(tempTime, false);
    }
  }

  // Extract range of julian days from URI
  private void updateLoadedDays() {
    List<String> pathSegments = eventUri.getPathSegments();
    int size = pathSegments.size();
    if (size <= 2) {
      return;
    }
    long first = Long.parseLong(pathSegments.get(size - 2));
    long last = Long.parseLong(pathSegments.get(size - 1));
    tempTime.set(first);
    firstLoadedJulianDay = Time.getJulianDay(first, tempTime.gmtoff);
    tempTime.set(last);
    lastLoadedJulianDay = Time.getJulianDay(last, tempTime.gmtoff);
  }

  /**
   * Updates the uri used by the loader according to the current position of the listview.
   *
   * @return The new Uri to use
   */
  private Uri updateUri() {
    MonthView child = (MonthView) listView.getChildAt(0);
    if (child != null) {
      firstLoadedJulianDay = child.getFirstJulianDay();
    }
    // -1 to ensure we get all day events from any time zone
    tempTime.setJulianDay(firstLoadedJulianDay - 1);
    long start = tempTime.toMillis(true);
    lastLoadedJulianDay = firstLoadedJulianDay + (numWeeks + 2 * WEEKS_BUFFER) * 7;
    // +1 to ensure we get all day events from any time zone
    tempTime.setJulianDay(lastLoadedJulianDay + 1);
    long end = tempTime.toMillis(true);

    // Create a new uri with the updated times
    Uri.Builder builder = CalendarProvider.CONTENT_URI.buildUpon();
    ContentUris.appendId(builder, start);
    ContentUris.appendId(builder, end);
    return builder.build();
  }

  /**
   * Sets the month displayed at the top of this view based on time. Override to add custom events when the title is changed.
   *
   * @param time            A day in the new focus month.
   * @param updateHighlight TODO(epastern):
   */
  private void setMonthDisplayed(Time time, boolean updateHighlight) {
    CharSequence oldMonth = monthName.getText();
    int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY | DateUtils.FORMAT_SHOW_YEAR;
    monthName.setText(formatDateRange(context, time.toMillis(true), time.toMillis(true), flags));
    monthName.invalidate();
    if (!TextUtils.equals(oldMonth, monthName.getText())) {
      monthName.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
    }
    currentMonthDisplayed = time.month;
    if (updateHighlight) {
      adapter.updateFocusMonth(currentMonthDisplayed);
    }

    boolean useSelected = false;
    if (time.year == desiredDay.year && time.month == desiredDay.month) {
      selectedDay.set(desiredDay);
      adapter.setSelectedDay(desiredDay);
      useSelected = true;
    } else {
      selectedDay.set(time);
      adapter.setSelectedDay(time);
    }

    if (selectedDay.minute >= 30) {
      selectedDay.minute = 30;
    } else {
      selectedDay.minute = 0;
    }
    long newTime = selectedDay.normalize(true);
    if (newTime != calendarController.getTime() && userScrolled) {
      long offset = useSelected ? 0 : DateUtils.WEEK_IN_MILLIS * numWeeks / 3;
      calendarController.setTime(newTime + offset);
    }
    calendarController.sendEvent(EventType.UPDATE_TITLE, time, time, time, -1, ViewType.CURRENT,
                                 DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY | DateUtils.FORMAT_SHOW_YEAR, null, null);
  }

  private String updateWhere() {
    // TODO fix selection/selection args after b/3206641 is fixed
    String where = WHERE_CALENDARS_VISIBLE;
    if (hideDeclined || !showDetailsInMonth) {
      where += " AND " + Instances.SELF_ATTENDEE_STATUS + "!=" + Attendees.ATTENDEE_STATUS_DECLINED;
    }
    return where;
  }

  private String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
    String date;
    String tz;
    if ((flags & DateUtils.FORMAT_UTC) != 0) {
      tz = Time.TIMEZONE_UTC;
    } else {
      tz = Time.getCurrentTimezone();
    }
    mSB.setLength(0);
    date = DateUtils.formatDateRange(context, mF, startMillis, endMillis, flags, tz).toString();
    return date;
  }

  private class ScrollStateRunnable implements Runnable {
    private int newState;

    /**
     * Sets up the runnable with a short delay in case the scroll state
     * immediately changes again.
     *
     * @param scrollState The new state it changed to
     */
    public void doScrollStateChange(int scrollState) {
      handler.removeCallbacks(this);
      newState = scrollState;
      handler.postDelayed(this, SCROLL_CHANGE_DELAY);
    }

    @Override
    public void run() {
      currentScrollState = newState;
      // Fix the position after a scroll or a fling ends
      if (newState == OnScrollListener.SCROLL_STATE_IDLE && previousScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
        previousScrollState = newState;
        // Uncomment the below to add snap to week back
//                int i = 0;
//                View child = mView.getChildAt(i);
//                while (child != null && child.getBottom() <= 0) {
//                    child = mView.getChildAt(++i);
//                }
//                if (child == null) {
//                    // The view is no longer visible, just return
//                    return;
//                }
//                int dist = child.getTop();
//                if (dist < LIST_TOP_OFFSET) {
//                    int firstPosition = mView.getFirstVisiblePosition();
//                    int lastPosition = mView.getLastVisiblePosition();
//                    boolean scroll = firstPosition != 0 && lastPosition != mView.getCount() - 1;
//                    if (isScrollingUp && scroll) {
//                        mView.smoothScrollBy(dist, 500);
//                    } else if (!isScrollingUp && scroll) {
//                        mView.smoothScrollBy(child.getHeight() + dist, 500);
//                    }
//                }
        adapter.updateFocusMonth(currentMonthDisplayed);
      } else {
        previousScrollState = newState;
      }
    }
  }
}

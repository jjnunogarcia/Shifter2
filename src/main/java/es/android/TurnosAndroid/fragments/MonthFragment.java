package es.android.TurnosAndroid.fragments;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.TextView;
import es.android.TurnosAndroid.*;
import es.android.TurnosAndroid.controllers.CalendarController;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.helpers.TimeZoneUtils;
import es.android.TurnosAndroid.helpers.Utils;
import es.android.TurnosAndroid.model.CalendarEvent;
import es.android.TurnosAndroid.model.Event;
import es.android.TurnosAndroid.model.EventInfo;
import es.android.TurnosAndroid.model.EventType;
import es.android.TurnosAndroid.requests.CalendarEventsLoader;
import es.android.TurnosAndroid.views.ViewType;
import es.android.TurnosAndroid.views.month.MonthAdapter;
import es.android.TurnosAndroid.views.month.MonthListView;
import es.android.TurnosAndroid.views.month.WeekView;

import java.text.DateFormatSymbols;
import java.util.*;

/**
 * This displays a titled list of weeks with selectable days. It can be configured to display the week number, start the week on a given day, show a reduced number of days, or display an
 * arbitrary number of weeks at a time. By overriding methods and changing variables this fragment can be customized to easily display a month selection component in a given style.
 */
public class MonthFragment extends ListFragment implements EventHandler, LoaderManager.LoaderCallbacks, OnScrollListener, OnTouchListener, MonthActionBarInterface {
  public static final  String          TAG                                 = MonthFragment.class.getSimpleName();
  public static final  int             DAYS_PER_WEEK                       = 7;
  public static final  String          KEY_INITIAL_TIME                    = "initial_time";
  // Affects when the month selection will change while scrolling up
  private static final int             SCROLL_HYST_WEEKS                   = 2;
  private static final int             GOTO_SCROLL_DURATION                = 500;
  // The minimum time between requeries of the data if the db is changing
  private static final int             LOADER_THROTTLE_DELAY               = 500;
  private static final int             NUM_WEEKS                           = 6;
  public static        int             LIST_TOP_OFFSET                     = -1;  // so that the top line will be under the separator
  private static       int             MIN_WEEK_HEIGHT_TO_CONSIDER_VISIBLE = 12;
  private static       StringBuilder   mSB                                 = new StringBuilder(50);
  private static       Formatter       mF                                  = new Formatter(mSB, Locale.getDefault());
  private final        Runnable        timeZoneUpdater                     = new Runnable() {
    @Override
    public void run() {
      String tz = TimeZoneUtils.getTimeZone(context, this);
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
  private final        Runnable        updateLoader                        = new Runnable() {
    @Override
    public void run() {
      if (shouldLoad && cursorLoader != null) {
        // Stop any previous loads while we update the uri
        stopLoader();

        // Start the loader again
        eventUri = updateUri();
//        cursorLoader.setUri(eventUri);
        cursorLoader.startLoading();
        cursorLoader.onContentChanged();
      }
    }
  };
  private final        Runnable        updateAtMidnight                    = new Runnable() {
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
  private final        DataSetObserver observer                            = new DataSetObserver() {
    @Override
    public void onChanged() {
      Time day = adapter.getSelectedDay();
      if (day.year != selectedDay.year || day.yearDay != selectedDay.yearDay) {
        goTo(day.toMillis(true), true, false);
      }
    }
  };
  private Context              context;
  private int                  bottomBuffer;
  private Handler              handler;
  private CalendarEventsLoader cursorLoader;
  private Uri                  eventUri;
  private Time                 desiredDay;
  private boolean              shouldLoad;
  private boolean              userScrolled;
  private int                  firstLoadedJulianDay;
  private int                  lastLoadedJulianDay;
  private Time                 selectedDay;
  private MonthAdapter         adapter;
  private MonthListView        listView;
  private ViewGroup            dayNamesHeader;
  private String[]             dayLabels;
  private Time                 tempTime;
  private int                  firstDayOfWeek;
  private Time                 firstDayOfMonth;
  private Time                 firstVisibleDay;
  private TextView             monthName;
  private int                  currentMonthDisplayed;
  private long                 previousScrollPosition;
  private boolean              isScrollingUp;
  private int                  previousScrollState;
  private int                  currentScrollState;
  private int                  saturdayColor;
  private int                  sundayColor;
  private int                  dayNameColor;
  private boolean              showWeekNumber;
  private ScrollStateRunnable  scrollStateChangedRunnable;
  private long                 initialTime;
  private CalendarController   calendarController;
  private TextView             header;

  public MonthFragment() {
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
    showWeekNumber = false;
    shouldLoad = true;
    userScrolled = false;
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
      initialTime = arguments.getLong(KEY_INITIAL_TIME, -1);
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
    ((MainActivity) getActivity()).getActionBarManager().setMonthActionBarInterface(this);

    Resources res = getResources();
    saturdayColor = res.getColor(R.color.month_saturday);
    sundayColor = res.getColor(R.color.month_sunday);
    dayNameColor = res.getColor(R.color.month_day_names_color);

    // Adjust sizes for screen density
    float scale = res.getDisplayMetrics().density;
    if (scale != 1) {
      MIN_WEEK_HEIGHT_TO_CONSIDER_VISIBLE *= scale;
      bottomBuffer *= scale;
      LIST_TOP_OFFSET *= scale;
    }

    setUpHeader();
    setUpListView();
    setUpAdapter();
    timeZoneUpdater.run();

    adapter.setSelectedDay(selectedDay);

    listView.post(new Runnable() {
      @Override
      public void run() {
        WeekView child = (WeekView) listView.getChildAt(0);
        if (child == null) {
          return;
        }
        int julianDay = child.getFirstJulianDay();
        firstVisibleDay.setJulianDay(julianDay);
        // set the title to the month of the second week
        tempTime.setJulianDay(julianDay + DAYS_PER_WEEK);
        setMonthDisplayed(tempTime, true);

        cursorLoader = (CalendarEventsLoader) getLoaderManager().initLoader(0, null, MonthFragment.this);
//        ContentValues contentValues = new ContentValues();
//        GregorianCalendar initialDay = new GregorianCalendar();
//        initialDay.set(2013, Calendar.JANUARY, 31);
//        contentValues.put(DBConstants.DATE, initialDay.getTimeInMillis());
//        contentValues.put(DBConstants.EVENT_ID, 1);
//        getActivity().getApplicationContext().getContentResolver().insert(CalendarProvider.CALENDAR_EVENTS_URI, contentValues);
        adapter.setListView(listView);

        goTo(initialTime, false, true);
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();
//    setUpAdapter();
    doResumeUpdates();
  }

  @Override
  public void onPause() {
    super.onPause();
    handler.removeCallbacks(updateAtMidnight);
  }

  private void setUpHeader() {
    dayLabels = new String[DAYS_PER_WEEK];
    String[] daysOfWeek = new DateFormatSymbols(Locale.getDefault()).getWeekdays();

    for (int i = 1; i <= DAYS_PER_WEEK; i++) {
      dayLabels[i - 1] = daysOfWeek[i].toUpperCase().substring(0, 3);
    }
  }

  private void setUpListView() {
    listView = (MonthListView) getListView();
    listView.setOnScrollListener(this);
    listView.setOnTouchListener(this);
  }

  private void setUpAdapter() {
    firstDayOfWeek = Utils.getFirstDayOfWeek(context);
    showWeekNumber = Utils.getShowWeekNumber(context);

    HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
    weekParams.put(MonthAdapter.WEEK_PARAMS_NUM_WEEKS, NUM_WEEKS);
    weekParams.put(MonthAdapter.WEEK_PARAMS_SHOW_WEEK, showWeekNumber ? 1 : 0);
    weekParams.put(MonthAdapter.WEEK_PARAMS_WEEK_START, firstDayOfWeek);
    weekParams.put(MonthAdapter.WEEK_PARAMS_JULIAN_DAY, Time.getJulianDay(selectedDay.toMillis(true), selectedDay.gmtoff));
    weekParams.put(MonthAdapter.WEEK_PARAMS_DAYS_PER_WEEK, DAYS_PER_WEEK);

    if (adapter == null) {
      adapter = new MonthAdapter(getActivity().getApplicationContext(), calendarController, weekParams);
      adapter.registerDataSetObserver(observer);
    } else {
      adapter.updateParams(weekParams);
    }

    setListAdapter(adapter);
  }

  private void updateHeader() {
    if (showWeekNumber) {
      header.setVisibility(View.VISIBLE);
    } else {
      header.setVisibility(View.GONE);
    }
    int offset = firstDayOfWeek - 1;

    for (int i = 1; i <= DAYS_PER_WEEK; i++) {
      header = (TextView) dayNamesHeader.getChildAt(i);
      int position = (offset + i) % DAYS_PER_WEEK;
      header.setText(dayLabels[position]);
      header.setVisibility(View.VISIBLE);

      if (position == Time.SATURDAY) {
        header.setTextColor(saturdayColor);
      } else if (position == Time.SUNDAY) {
        header.setTextColor(sundayColor);
      } else {
        header.setTextColor(dayNameColor);
      }
    }
    dayNamesHeader.invalidate();
  }

  private void doResumeUpdates() {
    firstDayOfWeek = Utils.getFirstDayOfWeek(context);
    showWeekNumber = Utils.getShowWeekNumber(context);
    updateHeader();
    adapter.setSelectedDay(selectedDay);
    timeZoneUpdater.run();
    updateAtMidnight.run();
    goTo(selectedDay.toMillis(true), false, false);
  }

  @Override
  public Loader onCreateLoader(int id, Bundle args) {
//    firstLoadedJulianDay = Time.getJulianDay(selectedDay.toMillis(true), selectedDay.gmtoff) - (NUM_WEEKS * 7 / 2);
//    eventUri = updateUri();

    GregorianCalendar initialDay = new GregorianCalendar();
    GregorianCalendar finalDay = new GregorianCalendar();
    initialDay.set(2013, Calendar.JANUARY, 1);
    finalDay.set(2013, Calendar.DECEMBER, 31);
    CalendarEventsLoader calendarEventsLoader = new CalendarEventsLoader(getActivity().getApplicationContext(), initialDay.getTimeInMillis(), finalDay.getTimeInMillis());
    calendarEventsLoader.setUpdateThrottle(LOADER_THROTTLE_DELAY);
    return calendarEventsLoader;
  }

  @Override
  public void onLoadFinished(Loader loader, Object data) {
//    CursorLoader cLoader = (CursorLoader) loader;
//    if (eventUri == null) {
//      eventUri = cLoader.getUri();
//      extractFirstAndLastDayToLoad();
//    }
//    if (cLoader.getUri().compareTo(eventUri) != 0) {
//      // We've started a new query since this loader ran so ignore the result
//      return;
//    }
    ArrayList<CalendarEvent> calendarEvents = Event.getCalendarEvents((Cursor) data);
//    ArrayList<Event> events = Event.buildEventsFromCursor(data, context, firstLoadedJulianDay, lastLoadedJulianDay);
    adapter.setEvents(firstLoadedJulianDay, lastLoadedJulianDay - firstLoadedJulianDay + 1, calendarEvents);
  }

  @Override
  public void onLoaderReset(Loader loader) {
  }

  private void stopLoader() {
    handler.removeCallbacks(updateLoader);
    if (cursorLoader != null) {
      cursorLoader.stopLoading();
    }
  }

  @Override
  public void onTodayClicked() {
    goTo(System.currentTimeMillis(), true, false);
  }

  @Override
  public long getSupportedEventTypes() {
    return EventType.GO_TO | EventType.EVENTS_CHANGED;
  }

  @Override
  public void handleEvent(EventInfo event) {
    if (event.eventType == EventType.GO_TO) {
      boolean animate = true;
      if (DAYS_PER_WEEK * NUM_WEEKS * 2 < Math.abs(Time.getJulianDay(event.selectedTime.toMillis(true), event.selectedTime.gmtoff)
                                                   - Time.getJulianDay(firstVisibleDay.toMillis(true), firstVisibleDay.gmtoff)
                                                   - DAYS_PER_WEEK * NUM_WEEKS / 2)) {
        animate = false;
      }
      desiredDay.set(event.selectedTime);
      desiredDay.normalize(true);
      boolean animateToday = true;
      boolean delayAnimation = goTo(event.selectedTime.toMillis(true), animate, false);

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
   * @param forceScroll Whether to recenter even if the time is already visible
   * @return Whether or not the view animated to the new location
   */
  public boolean goTo(long time, boolean animate, boolean forceScroll) {
    if (time == -1) {
      return false;
    }

    // Set the selected day
    selectedDay.set(time);
    selectedDay.normalize(true);

    tempTime.set(time);
    long millis = tempTime.normalize(true);
    // Get the week we're going to
    // TODO push Util function into Calendar public api.
    int position = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(millis, tempTime.gmtoff), firstDayOfWeek);

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
    int lastPosition = firstPosition + NUM_WEEKS - 1;
    if (top > bottomBuffer) {
      lastPosition--;
    }

    adapter.setSelectedDay(selectedDay);

    // Check if the selected day is now outside of our visible range and, if so, scroll to the month that contains it
    if (position < firstPosition || position > lastPosition || forceScroll) {
      firstDayOfMonth.set(tempTime);
      firstDayOfMonth.monthDay = 1;
      millis = firstDayOfMonth.normalize(true);
      setMonthDisplayed(firstDayOfMonth, true);
      position = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(millis, firstDayOfMonth.gmtoff), firstDayOfWeek);

      previousScrollState = OnScrollListener.SCROLL_STATE_FLING;
      if (animate) {
        listView.smoothScrollToPositionFromTop(position, LIST_TOP_OFFSET, GOTO_SCROLL_DURATION);
        return true;
      } else {
        listView.setSelectionFromTop(position, LIST_TOP_OFFSET);
        // Perform any after scroll operations that are needed
        onScrollStateChanged(listView, OnScrollListener.SCROLL_STATE_IDLE);
      }
    } else {
      // Otherwise just set the selection
      setMonthDisplayed(selectedDay, true);
    }
    return false;
  }

  /**
   * Updates the title and selected month if the view has moved to a new month.
   */
  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    WeekView child = (WeekView) view.getChildAt(0);
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
      handler.post(updateLoader);
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
  private void updateMonthHighlight(MonthListView view) {
    WeekView child = (WeekView) view.getChildAt(0);
    if (child == null) {
      return;
    }

    // Figure out where we are
    int offset = child.getBottom() < MIN_WEEK_HEIGHT_TO_CONSIDER_VISIBLE ? 1 : 0;
    // Use some hysteresis for checking which month to highlight. This causes the month to transition when two full weeks of a month are visible.
    child = (WeekView) view.getChildAt(SCROLL_HYST_WEEKS + offset);

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

    // Only switch months if we're scrolling away from the currently selected month
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

  private void extractFirstAndLastDayToLoad() {
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
    WeekView child = (WeekView) listView.getChildAt(0);
    if (child != null) {
      firstLoadedJulianDay = child.getFirstJulianDay();
    }
    // -1 to ensure we get all day events from any time zone
    tempTime.setJulianDay(firstLoadedJulianDay - 1);
    long start = tempTime.toMillis(true);
    lastLoadedJulianDay = firstLoadedJulianDay + (NUM_WEEKS + 2) * 7;
    // +1 to ensure we get all day events from any time zone
    tempTime.setJulianDay(lastLoadedJulianDay + 1);
    long end = tempTime.toMillis(true);

    // Create a new uri with the updated times
    Uri.Builder builder = CalendarProvider.EVENTS_URI.buildUpon();
    ContentUris.appendId(builder, start);
    ContentUris.appendId(builder, end);
    return builder.build();
  }

  /**
   * Sets the month displayed at the top of this view based on time. Override to add custom events when the title is changed.
   */
  private void setMonthDisplayed(Time time, boolean updateHighlight) {
    int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY | DateUtils.FORMAT_SHOW_YEAR;
    monthName.setText(formatDateRange(context, time.toMillis(true), time.toMillis(true), flags));
    monthName.invalidate();
    currentMonthDisplayed = time.month;
    boolean useSelected = false;

    if (updateHighlight) {
      adapter.updateFocusMonth(currentMonthDisplayed);
    }

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
      long offset = useSelected ? 0 : DateUtils.WEEK_IN_MILLIS * NUM_WEEKS / 3;
      calendarController.setTime(newTime + offset);
    }

    calendarController.sendEvent(EventType.UPDATE_TITLE, time, time, time, -1, ViewType.CURRENT, null, null);
  }

  private String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
    String tz;
    if ((flags & DateUtils.FORMAT_UTC) != 0) {
      tz = Time.TIMEZONE_UTC;
    } else {
      tz = Time.getCurrentTimezone();
    }
    mSB.setLength(0);
    return DateUtils.formatDateRange(context, mF, startMillis, endMillis, flags, tz).toString();
  }

  private class ScrollStateRunnable implements Runnable {
    private int newState;

    /**
     * Sets up the runnable in case the scroll state immediately changes again.
     *
     * @param scrollState The new state it changed to
     */
    public void doScrollStateChange(int scrollState) {
      handler.removeCallbacks(this);
      newState = scrollState;
      handler.post(this);
    }

    @Override
    public void run() {
      currentScrollState = newState;
      // Fix the position after a scroll or a fling ends
      if (newState == OnScrollListener.SCROLL_STATE_IDLE && previousScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
        previousScrollState = newState;
        adapter.updateFocusMonth(currentMonthDisplayed);
      } else {
        previousScrollState = newState;
      }
    }
  }
}

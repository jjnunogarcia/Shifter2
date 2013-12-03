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

import android.app.Activity;
import android.support.v4.app.LoaderManager;
import android.content.ContentUris;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.*;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MonthByWeekFragment extends SimpleDayPickerFragment implements EventHandler, LoaderManager.LoaderCallbacks<Cursor>, OnScrollListener, OnTouchListener {
    private static final String TAG = "MonthFragment";

    // Selection and selection args for adding event queries
    private static final String  WHERE_CALENDARS_VISIBLE = Calendars.VISIBLE + "=1";
    private static final String  INSTANCES_SORT_ORDER    = Instances.START_DAY + "," + Instances.START_MINUTE + "," + Instances.TITLE;
    protected static     boolean mShowDetailsInMonth     = false;

    protected float   mMinimumTwoMonthFlingVelocity;
    protected boolean mIsMiniMonth;
    protected boolean mHideDeclined;

    protected int mFirstLoadedJulianDay;
    protected int mLastLoadedJulianDay;

    private static final int WEEKS_BUFFER          = 1;
    // How long to wait after scroll stops before starting the loader
    // Using scroll duration because scroll state changes don't update correctly when a scroll is triggered programmatically.
    private static final int LOADER_DELAY          = 200;
    // The minimum time between requeries of the data if the db is
    // changing
    private static final int LOADER_THROTTLE_DELAY = 500;

    private CursorLoader mLoader;
    private Uri          mEventUri;
    private final Time mDesiredDay = new Time();

    private volatile boolean mShouldLoad   = true;
    private          boolean mUserScrolled = false;

    private int     mEventsLoadingDelay;
    private boolean mShowCalendarControls;
    private boolean mIsDetached;


    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            String tz = Utils.getTimeZone(context, mTZUpdater);
            selectedDay.timezone = tz;
            selectedDay.normalize(true);
            tempTime.timezone = tz;
            firstDayOfMonth.timezone = tz;
            firstDayOfMonth.normalize(true);
            firstVisibleDay.timezone = tz;
            firstVisibleDay.normalize(true);
            if (adapter != null) {
                adapter.refresh();
            }
        }
    };


    private final Runnable mUpdateLoader = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                if (!mShouldLoad || mLoader == null) {
                    return;
                }
                // Stop any previous loads while we update the uri
                stopLoader();

                // Start the loader again
                mEventUri = updateUri();

                mLoader.setUri(mEventUri);
                mLoader.startLoading();
                mLoader.onContentChanged();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Started loader with uri: " + mEventUri);
                }
            }
        }
    };
    // Used to load the events when a delay is needed
    Runnable mLoadingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsDetached) {
                mLoader = (CursorLoader) getLoaderManager().initLoader(0, null, MonthByWeekFragment.this);
            }
        }
    };


    /**
     * Updates the uri used by the loader according to the current position of
     * the listview.
     *
     * @return The new Uri to use
     */
    private Uri updateUri() {
        SimpleWeekView child = (SimpleWeekView) listView.getChildAt(0);
        if (child != null) {
            mFirstLoadedJulianDay = child.getFirstJulianDay();
        }
        // -1 to ensure we get all day events from any time zone
        tempTime.setJulianDay(mFirstLoadedJulianDay - 1);
        long start = tempTime.toMillis(true);
        mLastLoadedJulianDay = mFirstLoadedJulianDay + (numWeeks + 2 * WEEKS_BUFFER) * 7;
        // +1 to ensure we get all day events from any time zone
        tempTime.setJulianDay(mLastLoadedJulianDay + 1);
        long end = tempTime.toMillis(true);

        // Create a new uri with the updated times
        Uri.Builder builder = CalendarProvider.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);
        return builder.build();
    }

    // Extract range of julian days from URI
    private void updateLoadedDays() {
        List<String> pathSegments = mEventUri.getPathSegments();
        int size = pathSegments.size();
        if (size <= 2) {
            return;
        }
        long first = Long.parseLong(pathSegments.get(size - 2));
        long last = Long.parseLong(pathSegments.get(size - 1));
        tempTime.set(first);
        mFirstLoadedJulianDay = Time.getJulianDay(first, tempTime.gmtoff);
        tempTime.set(last);
        mLastLoadedJulianDay = Time.getJulianDay(last, tempTime.gmtoff);
    }

    protected String updateWhere() {
        // TODO fix selection/selection args after b/3206641 is fixed
        String where = WHERE_CALENDARS_VISIBLE;
    if (mHideDeclined || !mShowDetailsInMonth) {
      where += " AND " + Instances.SELF_ATTENDEE_STATUS + "!=" + Attendees.ATTENDEE_STATUS_DECLINED;
    }
    return where;
  }

  private void stopLoader() {
    synchronized (mUpdateLoader) {
      handler.removeCallbacks(mUpdateLoader);
      if (mLoader != null) {
        mLoader.stopLoading();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Stopped loader from loading");
        }
      }
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mTZUpdater.run();
    if (adapter != null) {
      adapter.setSelectedDay(selectedDay);
    }
    mIsDetached = false;

    ViewConfiguration viewConfig = ViewConfiguration.get(activity);
    mMinimumTwoMonthFlingVelocity = viewConfig.getScaledMaximumFlingVelocity() / 2;
    Resources res = activity.getResources();
    mShowCalendarControls = Utils.getConfigBool(activity, R.bool.show_calendar_controls);
    // Synchronized the loading time of the month's events with the animation of the
    // calendar controls.
    if (mShowCalendarControls) {
      mEventsLoadingDelay = res.getInteger(R.integer.calendar_controls_animation_time);
    }
    mShowDetailsInMonth = res.getBoolean(R.bool.show_details_in_month);
  }

  @Override
  public void onDetach() {
    mIsDetached = true;
    super.onDetach();
    if (mShowCalendarControls) {
      if (listView != null) {
        listView.removeCallbacks(mLoadingRunnable);
      }
    }
  }

  @Override
  protected void setUpAdapter() {
    firstDayOfWeek = Utils.getFirstDayOfWeek(context);
    showWeekNumber = Utils.getShowWeekNumber(context);

    HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
    weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_NUM_WEEKS, numWeeks);
    weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_SHOW_WEEK, showWeekNumber ? 1 : 0);
    weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_WEEK_START, firstDayOfWeek);
    weekParams.put(MonthByWeekAdapter.WEEK_PARAMS_IS_MINI, mIsMiniMonth ? 1 : 0);
    weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_JULIAN_DAY,
                   Time.getJulianDay(selectedDay.toMillis(true), selectedDay.gmtoff));
    weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_DAYS_PER_WEEK, daysPerWeek);
    if (adapter == null) {
      adapter = new MonthByWeekAdapter(getActivity(), weekParams);
      adapter.registerDataSetObserver(mObserver);
    } else {
      adapter.updateParams(weekParams);
    }
    adapter.notifyDataSetChanged();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v;
//        if (mIsMiniMonth) {
//            v = inflater.inflate(R.layout.month_by_week, container, false);
//        } else {
    v = inflater.inflate(R.layout.full_month_by_week, container, false);
//        }
    dayNamesHeader = (ViewGroup) v.findViewById(R.id.day_names);
    return v;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    listView.setOnTouchListener(this);
    if (!mIsMiniMonth) {
      listView.setBackgroundColor(getResources().getColor(R.color.month_bgcolor));
    }

    // To get a smoother transition when showing this fragment, delay loading of events until
    // the fragment is expended fully and the calendar controls are gone.
    if (mShowCalendarControls) {
      listView.postDelayed(mLoadingRunnable, mEventsLoadingDelay);
    } else {
      mLoader = (CursorLoader) getLoaderManager().initLoader(0, null, this);
    }
    adapter.setListView(listView);
  }

  public MonthByWeekFragment() {
    this(System.currentTimeMillis(), true);
  }

  public MonthByWeekFragment(long initialTime, boolean isMiniMonth) {
    super(initialTime);
    mIsMiniMonth = isMiniMonth;
  }

  @Override
  protected void setUpHeader() {
    if (mIsMiniMonth) {
      super.setUpHeader();
      return;
    }

    dayLabels = new String[7];
    for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
      dayLabels[i - Calendar.SUNDAY] = DateUtils.getDayOfWeekString(i,
                                                                     DateUtils.LENGTH_MEDIUM).toUpperCase();
    }
  }

  // TODO
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    CursorLoader loader;
    synchronized (mUpdateLoader) {
      mFirstLoadedJulianDay = Time.getJulianDay(selectedDay.toMillis(true), selectedDay.gmtoff) - (numWeeks * 7 / 2);
      mEventUri = updateUri();
      String where = updateWhere();

      loader = new CursorLoader(
          getActivity(), mEventUri, new String[] {DBConstants.ID, DBConstants.EVENT, DBConstants.LOCATION, DBConstants.DESCRIPTION,
                                                  DBConstants.START, DBConstants.END, DBConstants.CALENDAR_ID, DBConstants.EVENT_ID, DBConstants.START_DAY, DBConstants.END_DAY,
                                                  DBConstants.START_TIME, DBConstants.END_TIME}/*Event.EVENT_PROJECTION*/, /*where*/null,
          null /* WHERE_CALENDARS_SELECTED_ARGS */, null/*INSTANCES_SORT_ORDER*/);
      loader.setUpdateThrottle(LOADER_THROTTLE_DELAY);
    }
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "Returning new loader with uri: " + mEventUri);
    }
    return loader;
  }

  @Override
  public void doResumeUpdates() {
    firstDayOfWeek = Utils.getFirstDayOfWeek(context);
    showWeekNumber = Utils.getShowWeekNumber(context);
    boolean prevHideDeclined = mHideDeclined;
    mHideDeclined = Utils.getHideDeclinedEvents(context);
    if (prevHideDeclined != mHideDeclined && mLoader != null) {
      mLoader.setSelection(updateWhere());
    }
    daysPerWeek = Utils.getDaysPerWeek(context);
    updateHeader();
    adapter.setSelectedDay(selectedDay);
    mTZUpdater.run();
    mTodayUpdater.run();
    goTo(selectedDay.toMillis(true), false, true, false);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    synchronized (mUpdateLoader) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Found " + data.getCount() + " cursor entries for uri " + mEventUri);
      }
      CursorLoader cLoader = (CursorLoader) loader;
      if (mEventUri == null) {
        mEventUri = cLoader.getUri();
        updateLoadedDays();
      }
      if (cLoader.getUri().compareTo(mEventUri) != 0) {
        // We've started a new query since this loader ran so ignore the
        // result
        return;
      }
      ArrayList<Event> events = new ArrayList<Event>();
      Event.buildEventsFromCursor(events, data, context, mFirstLoadedJulianDay, mLastLoadedJulianDay);
      ((MonthByWeekAdapter) adapter).setEvents(mFirstLoadedJulianDay, mLastLoadedJulianDay - mFirstLoadedJulianDay + 1, events);
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
  }

  @Override
  public void eventsChanged() {
    // TODO remove this after b/3387924 is resolved
    if (mLoader != null) {
      mLoader.forceLoad();
    }
  }

  @Override
  public long getSupportedEventTypes() {
    return EventType.GO_TO | EventType.EVENTS_CHANGED;
  }

  @Override
  protected void setMonthDisplayed(Time time, boolean updateHighlight) {
    super.setMonthDisplayed(time, updateHighlight);
    if (!mIsMiniMonth) {
      boolean useSelected = false;
      if (time.year == mDesiredDay.year && time.month == mDesiredDay.month) {
        selectedDay.set(mDesiredDay);
        adapter.setSelectedDay(mDesiredDay);
        useSelected = true;
      } else {
        selectedDay.set(time);
        adapter.setSelectedDay(time);
      }
      CalendarController controller = CalendarController.getInstance(context);
      if (selectedDay.minute >= 30) {
        selectedDay.minute = 30;
      } else {
        selectedDay.minute = 0;
      }
      long newTime = selectedDay.normalize(true);
      if (newTime != controller.getTime() && mUserScrolled) {
        long offset = useSelected ? 0 : DateUtils.WEEK_IN_MILLIS * numWeeks / 3;
        controller.setTime(newTime + offset);
      }
      controller.sendEvent(this, EventType.UPDATE_TITLE, time, time, time, -1,
                           ViewType.CURRENT, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                                             | DateUtils.FORMAT_SHOW_YEAR, null, null);
    }
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {

    synchronized (mUpdateLoader) {
      if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
        mShouldLoad = false;
        stopLoader();
        mDesiredDay.setToNow();
      } else {
        handler.removeCallbacks(mUpdateLoader);
        mShouldLoad = true;
        handler.postDelayed(mUpdateLoader, LOADER_DELAY);
      }
    }
    if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
      mUserScrolled = true;
    }

    mScrollStateChangedRunnable.doScrollStateChange(view, scrollState);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    mDesiredDay.setToNow();
    return false;
    // TODO post a cleanup to push us back onto the grid if something went
    // wrong in a scroll such as the user stopping the view but not
    // scrolling
  }

  @Override
  public void handleEvent(EventInfo event) {
    if (event.eventType == EventType.GO_TO) {
      boolean animate = true;
      if (daysPerWeek * numWeeks * 2 < Math.abs(
          Time.getJulianDay(event.selectedTime.toMillis(true), event.selectedTime.gmtoff)
          - Time.getJulianDay(firstVisibleDay.toMillis(true), firstVisibleDay.gmtoff)
          - daysPerWeek * numWeeks / 2)) {
        animate = false;
      }
      mDesiredDay.set(event.selectedTime);
      mDesiredDay.normalize(true);
      boolean animateToday = (event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0;
      boolean delayAnimation = goTo(event.selectedTime.toMillis(true), animate, true, false);
      if (animateToday) {
        // If we need to flash today start the animation after any
        // movement from listView has ended.
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            ((MonthByWeekAdapter) adapter).animateToday();
            adapter.notifyDataSetChanged();
          }
        }, delayAnimation ? GOTO_SCROLL_DURATION : 0);
      }
    } else if (event.eventType == EventType.EVENTS_CHANGED) {
      eventsChanged();
    }
  }
}

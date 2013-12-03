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
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

/**
 * <p>
 * This displays a titled list of weeks with selectable days. It can be
 * configured to display the week number, start the week on a given day, show a
 * reduced number of days, or display an arbitrary number of weeks at a time. By
 * overriding methods and changing variables this fragment can be customized to
 * easily display a month selection component in a given style.
 * </p>
 */
public class SimpleDayPickerFragment extends ListFragment implements OnScrollListener {

    // The number of days to display in each week
    public static final    int           DAYS_PER_WEEK             = 7;
    // Affects when the month selection will change while scrolling up
    protected static final int           SCROLL_HYST_WEEKS         = 2;
    // How long the GoTo fling animation should last
    protected static final int           GOTO_SCROLL_DURATION      = 500;
    // How long to wait after receiving an onScrollStateChanged notification
    // before acting on it
    protected static final int           SCROLL_CHANGE_DELAY       = 40;
    // The size of the month name displayed above the week list
    protected static final int           MINI_MONTH_NAME_TEXT_SIZE = 18;
    private static final   String        TAG                       = "MonthFragment";
    private static final   String        KEY_CURRENT_TIME          = "current_time";
    public static          int           LIST_TOP_OFFSET           = -1;  // so that the top line will be under the separator
    private static         StringBuilder mSB                       = new StringBuilder(50);
    private static         Formatter     mF                        = new Formatter(mSB, Locale.getDefault());
    private static         float         mScale                    = 0;
    protected              int           WEEK_MIN_VISIBLE_HEIGHT   = 12;
    protected              int           BOTTOM_BUFFER             = 20;
    protected              int           saturdayColor             = 0;
    protected              int           sundayColor               = 0;
    protected              int           dayNameColor              = 0;
    // You can override these numbers to get a different appearance
    protected              int           numWeeks                  = 6;
    protected              boolean       showWeekNumber            = false;
    protected              int           daysPerWeek               = 7;
    // These affect the scroll speed and feel
    protected              float         friction                  = 1.0f;
    protected Context context;
    protected Handler handler;
    protected float   minimumFlingVelocity;
    // highlighted time
    protected Time selectedDay = new Time();
    protected SimpleWeeksAdapter adapter;
    protected ListView           listView;
    protected ViewGroup          dayNamesHeader;
    protected String[]           dayLabels;
    // disposable variable used for time calculations
    protected Time tempTime = new Time();
    // When the week starts; numbered like Time.<WEEKDAY> (e.g. SUNDAY=0).
    protected int firstDayOfWeek;
    // The first day of the focus month
    protected Time firstDayOfMonth = new Time();
    // The first day that is visible in the view
    protected Time firstVisibleDay = new Time();
    // The name of the month to display
    protected TextView     monthName;
    // The last name announced by accessibility
    protected CharSequence mPrevMonthName;
    // which month should be displayed/highlighted [0-11]
    protected int          currentMonthDisplayed;
    // used for tracking during a scroll
    protected long         previousScrollPosition;
    // used for tracking which direction the view is scrolling
    protected boolean             isScrollingUp               = false;
    // used for tracking what state listview is in
    protected int                 previousScrollState         = OnScrollListener.SCROLL_STATE_IDLE;
    // used for tracking what state listview is in
    protected int                 currentScrollState          = OnScrollListener.SCROLL_STATE_IDLE;
    // This causes an update of the view at midnight
    protected Runnable            mTodayUpdater               = new Runnable() {
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
    protected DataSetObserver     mObserver                   = new DataSetObserver() {
        @Override
        public void onChanged() {
            Time day = adapter.getSelectedDay();
            if (day.year != selectedDay.year || day.yearDay != selectedDay.yearDay) {
                goTo(day.toMillis(true), true, true, false);
            }
        }
    };
    protected ScrollStateRunnable mScrollStateChangedRunnable = new ScrollStateRunnable();

    public SimpleDayPickerFragment(long initialTime) {
        goTo(initialTime, false, true, true);
        handler = new Handler();
    }

    public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
        int diff = Time.THURSDAY - firstDayOfWeek;
        if (diff < 0) {
            diff += 7;
        }
        int refDay = Time.EPOCH_JULIAN_DAY - diff;
        return (julianDay - refDay) / 7;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        String tz = Time.getCurrentTimezone();
        ViewConfiguration viewConfig = ViewConfiguration.get(activity);
        minimumFlingVelocity = viewConfig.getScaledMinimumFlingVelocity();

        // Ensure we're in the correct time zone
        selectedDay.switchTimezone(tz);
        selectedDay.normalize(true);
        firstDayOfMonth.timezone = tz;
        firstDayOfMonth.normalize(true);
        firstVisibleDay.timezone = tz;
        firstVisibleDay.normalize(true);
        tempTime.timezone = tz;

        Resources res = activity.getResources();
        saturdayColor = res.getColor(R.color.month_saturday);
        sundayColor = res.getColor(R.color.month_sunday);
        dayNameColor = res.getColor(R.color.month_day_names_color);

        // Adjust sizes for screen density
        if (mScale == 0) {
            mScale = activity.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                WEEK_MIN_VISIBLE_HEIGHT *= mScale;
                BOTTOM_BUFFER *= mScale;
                LIST_TOP_OFFSET *= mScale;
            }
        }
        setUpAdapter();
        setListAdapter(adapter);
    }

    /**
     * Creates a new adapter if necessary and sets up its parameters. Override
     * this method to provide a custom adapter.
     */
    protected void setUpAdapter() {
        HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_NUM_WEEKS, numWeeks);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_SHOW_WEEK, showWeekNumber ? 1 : 0);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_WEEK_START, firstDayOfWeek);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_JULIAN_DAY,
                       Time.getJulianDay(selectedDay.toMillis(false), selectedDay.gmtoff));
        if (adapter == null) {
            adapter = new SimpleWeeksAdapter(getActivity(), weekParams);
            adapter.registerDataSetObserver(mObserver);
        } else {
            adapter.updateParams(weekParams);
        }
        // refresh the view with the new parameters
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_CURRENT_TIME)) {
            goTo(savedInstanceState.getLong(KEY_CURRENT_TIME), false, true, true);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setUpListView();
        setUpHeader();

        monthName = (TextView) getView().findViewById(R.id.month_name);
        SimpleWeekView child = (SimpleWeekView) listView.getChildAt(0);
        if (child == null) {
            return;
        }
        int julianDay = child.getFirstJulianDay();
        firstVisibleDay.setJulianDay(julianDay);
        // set the title to the month of the second week
        tempTime.setJulianDay(julianDay + DAYS_PER_WEEK);
        setMonthDisplayed(tempTime, true);
    }

    /**
     * Sets up the strings to be used by the header. Override this method to use different strings or modify the view params.
     */
    protected void setUpHeader() {
        dayLabels = new String[7];
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            dayLabels[i - Calendar.SUNDAY] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_MEDIUM).toUpperCase();
        }
    }

    /**
     * Sets all the required fields for the list view. Override this method to
     * set a different list view behavior.
     */
    protected void setUpListView() {
        // Configure the listview
        listView = getListView();
        // Transparent background on scroll
        listView.setCacheColorHint(0);
        // No dividers
        listView.setDivider(null);
        // Items are clickable
        listView.setItemsCanFocus(true);
        // The thumb gets in the way, so disable it
        listView.setFastScrollEnabled(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setOnScrollListener(this);
        listView.setFadingEdgeLength(0);
        // Make the scrolling behavior nicer
        listView.setFriction(ViewConfiguration.getScrollFriction() * friction);
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
        handler.removeCallbacks(mTodayUpdater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(KEY_CURRENT_TIME, selectedDay.toMillis(true));
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View v = inflater.inflate(R.layout.month_by_week,
//                container, false);
//        dayNamesHeader = (ViewGroup) v.findViewById(R.id.day_names);
//        return v;
//    }

    /**
     * Updates the user preference fields. Override this to use a different
     * preference space.
     */
    protected void doResumeUpdates() {
        // Get default week start based on locale, subtracting one for use with android Time.
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        firstDayOfWeek = cal.getFirstDayOfWeek() - 1;

        showWeekNumber = false;

        updateHeader();
        goTo(selectedDay.toMillis(true), false, false, false);
        adapter.setSelectedDay(selectedDay);
        mTodayUpdater.run();
    }

    /**
     * Fixes the day names header to provide correct spacing and updates the
     * label text. Override this to set up a custom header.
     */
    protected void updateHeader() {
        TextView label = (TextView) dayNamesHeader.findViewById(R.id.wk_label);
        if (showWeekNumber) {
            label.setVisibility(View.VISIBLE);
        } else {
            label.setVisibility(View.GONE);
        }
        int offset = firstDayOfWeek - 1;
        for (int i = 1; i < 8; i++) {
            label = (TextView) dayNamesHeader.getChildAt(i);
            if (i < daysPerWeek + 1) {
                int position = (offset + i) % 7;
                label.setText(dayLabels[position]);
                label.setVisibility(View.VISIBLE);
                if (position == Time.SATURDAY) {
                    label.setTextColor(saturdayColor);
                } else if (position == Time.SUNDAY) {
                    label.setTextColor(sundayColor);
                } else {
                    label.setTextColor(dayNameColor);
                }
            } else {
                label.setVisibility(View.GONE);
            }
        }
        dayNamesHeader.invalidate();
    }

    /**
     * Returns the UTC millis since epoch representation of the currently selected time.
     *
     * @return
     */
    public long getSelectedTime() {
        return selectedDay.toMillis(true);
    }

    /**
     * This moves to the specified time in the view. If the time is not already in range it will move the list so that the first of the month containing
     * the time is at the top of the view. If the new time is already in view the list will not be scrolled unless forceScroll is true. This time may
     * optionally be highlighted as selected as well.
     *
     * @param time        The time to move to
     * @param animate     Whether to scroll to the given time or just redraw at the new location
     * @param setSelected Whether to set the given time as selected
     * @param forceScroll Whether to recenter even if the time is already visible
     * @return Whether or not the view animated to the new location
     */
    public boolean goTo(long time, boolean animate, boolean setSelected, boolean forceScroll) {
        if (time == -1) {
            Log.e(TAG, "time is invalid");
            return false;
        }

        // Set the selected day
        if (setSelected) {
            selectedDay.set(time);
            selectedDay.normalize(true);
        }

        // If this view isn't returned yet we won't be able to load the lists
        // current position, so return after setting the selected day.
        if (!isResumed()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "We're not visible yet");
            }
            return false;
        }

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
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "child at " + (i - 1) + " has top " + top);
            }
        } while (top < 0);

        // Compute the first and last position visible
        int firstPosition;
        if (child != null) {
            firstPosition = listView.getPositionForView(child);
        } else {
            firstPosition = 0;
        }
        int lastPosition = firstPosition + numWeeks - 1;
        if (top > BOTTOM_BUFFER) {
            lastPosition--;
        }

        if (setSelected) {
            adapter.setSelectedDay(selectedDay);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "GoTo position " + position);
        }
        // Check if the selected day is now outside of our visible range
        // and if so scroll to the month that contains it
        if (position < firstPosition || position > lastPosition || forceScroll) {
            firstDayOfMonth.set(tempTime);
            firstDayOfMonth.monthDay = 1;
            millis = firstDayOfMonth.normalize(true);
            setMonthDisplayed(firstDayOfMonth, true);
            position = getWeeksSinceEpochFromJulianDay(
                    Time.getJulianDay(millis, firstDayOfMonth.gmtoff), firstDayOfWeek);

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

    /**
     * Updates the title and selected month if the view has moved to a new
     * month.
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        SimpleWeekView child = (SimpleWeekView) view.getChildAt(0);
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

    /**
     * Figures out if the month being shown has changed and updates the
     * highlight if needed
     *
     * @param view The ListView containing the weeks
     */
    private void updateMonthHighlight(AbsListView view) {
        SimpleWeekView child = (SimpleWeekView) view.getChildAt(0);
        if (child == null) {
            return;
        }

        // Figure out where we are
        int offset = child.getBottom() < WEEK_MIN_VISIBLE_HEIGHT ? 1 : 0;
        // Use some hysteresis for checking which month to highlight. This
        // causes the month to transition when two full weeks of a month are
        // visible.
        child = (SimpleWeekView) view.getChildAt(SCROLL_HYST_WEEKS + offset);

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

    /**
     * Sets the month displayed at the top of this view based on time. Override
     * to add custom events when the title is changed.
     *
     * @param time            A day in the new focus month.
     * @param updateHighlight TODO(epastern):
     */
    protected void setMonthDisplayed(Time time, boolean updateHighlight) {
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
    }

    public String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
        String date;
        String tz;
        if ((flags & DateUtils.FORMAT_UTC) != 0) {
            tz = Time.TIMEZONE_UTC;
        } else {
            tz = Time.getCurrentTimezone();
        }
        synchronized (mSB) {
            mSB.setLength(0);
            date = DateUtils.formatDateRange(context, mF, startMillis, endMillis, flags, tz).toString();
        }
        return date;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // use a post to prevent re-entering onScrollStateChanged before it
        // exits
        mScrollStateChangedRunnable.doScrollStateChange(view, scrollState);
    }

    protected class ScrollStateRunnable implements Runnable {
        private int newState;

        /**
         * Sets up the runnable with a short delay in case the scroll state
         * immediately changes again.
         *
         * @param view        The list view that changed state
         * @param scrollState The new state it changed to
         */
        public void doScrollStateChange(AbsListView view, int scrollState) {
            handler.removeCallbacks(this);
            newState = scrollState;
            handler.postDelayed(this, SCROLL_CHANGE_DELAY);
        }

        @Override
        public void run() {
            currentScrollState = newState;
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "New scroll state: " + newState + " old state: " + previousScrollState);
            }
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
//                    if (Log.isLoggable(TAG, Log.DEBUG)) {
//                        Log.d(TAG, "scrolling by " + dist + " up? " + isScrollingUp);
//                    }
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

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

// TODO Remove calendar imports when the required methods have been refactored into the public api

import android.content.Context;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

/**
 * <p>
 * This is a specialized adapter for creating a list of weeks with selectable
 * days. It can be configured to display the week number, start the week on a
 * given day, show a reduced number of days, or display an arbitrary number of
 * weeks at a time. See {@link MonthFragment} for usage.
 * </p>
 */
public class SimpleWeeksAdapter extends BaseAdapter implements OnTouchListener {
  private static final String TAG = SimpleWeeksAdapter.class.getSimpleName();

  /**
   * The number of weeks to display at a time.
   */
  public static final String WEEK_PARAMS_NUM_WEEKS     = "num_weeks";
  /**
   * Which month should be in focus currently.
   */
  public static final String WEEK_PARAMS_FOCUS_MONTH   = "focus_month";
  /**
   * Whether the week number should be shown. Non-zero to show them.
   */
  public static final String WEEK_PARAMS_SHOW_WEEK     = "week_numbers";
  /**
   * Which day the week should start on. {@link android.text.format.Time#SUNDAY} through {@link android.text.format.Time#SATURDAY}.
   */
  public static final String WEEK_PARAMS_WEEK_START    = "week_start";
  /**
   * The Julian day to highlight as selected.
   */
  public static final String WEEK_PARAMS_JULIAN_DAY    = "selected_day";
  public static final String WEEK_PARAMS_DAYS_PER_WEEK = "days_per_week";

  protected static int WEEK_COUNT             = 3497;
  protected static int DEFAULT_NUM_WEEKS      = 6;
  protected static int DEFAULT_MONTH_FOCUS    = 0;
  protected static int DEFAULT_DAYS_PER_WEEK  = 7;
  protected static int DEFAULT_WEEK_HEIGHT    = 32;
  protected static int WEEK_7_OVERHANG_HEIGHT = 7;

  protected Context         context;
  protected Time            selectedDay;
  protected int             selectedWeek;
  protected int             firstDayOfWeek;
  protected boolean         showWeekNumber;
  protected GestureDetector gestureDetector;
  protected int             numWeeks;
  protected int             daysPerWeek;
  protected int             focusMonth;
  protected ListView        listView;

  public SimpleWeeksAdapter(Context context, HashMap<String, Integer> params) {
    this.context = context;

    // Get default week start based on locale, subtracting one for use with android Time.
    Calendar cal = Calendar.getInstance(Locale.getDefault());
    firstDayOfWeek = cal.getFirstDayOfWeek() - 1;

    float scale = context.getResources().getDisplayMetrics().density;
    if (scale != 1) {
      WEEK_7_OVERHANG_HEIGHT *= scale;
    }
    init();
    updateParams(params);
  }

  protected void init() {
    gestureDetector = new GestureDetector(context, new CalendarGestureListener());
    selectedDay = new Time();
    selectedDay.setToNow();
    showWeekNumber = false;
    numWeeks = DEFAULT_NUM_WEEKS;
    daysPerWeek = DEFAULT_DAYS_PER_WEEK;
    focusMonth = DEFAULT_MONTH_FOCUS;
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

  /**
   * Updates the selected day and related parameters.
   *
   * @param selectedTime The time to highlight
   */
  public void setSelectedDay(Time selectedTime) {
    selectedDay.set(selectedTime);
    long millis = selectedDay.normalize(true);
    selectedWeek = getWeeksSinceEpochFromJulianDay(Time.getJulianDay(millis, selectedDay.gmtoff), firstDayOfWeek);
    notifyDataSetChanged();
  }

  public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
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

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    SimpleWeekView v;
    HashMap<String, Integer> drawingParams = null;
    if (convertView != null) {
      v = (SimpleWeekView) convertView;
      // We store the drawing parameters in the view so it can be recycled
      drawingParams = (HashMap<String, Integer>) v.getTag();
    } else {
      v = new SimpleWeekView(context);
      // Set up the new view
      LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
      v.setLayoutParams(params);
      v.setClickable(true);
      v.setOnTouchListener(this);
    }
    if (drawingParams == null) {
      drawingParams = new HashMap<String, Integer>();
    }
    drawingParams.clear();

    int selectedDay = -1;
    if (selectedWeek == position) {
      selectedDay = this.selectedDay.weekDay;
    }

    // pass in all the view parameters
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_HEIGHT, (parent.getHeight() - WEEK_7_OVERHANG_HEIGHT) / numWeeks);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_SELECTED_DAY, selectedDay);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_SHOW_WK_NUM, showWeekNumber ? 1 : 0);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_WEEK_START, firstDayOfWeek);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_NUM_DAYS, daysPerWeek);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_WEEK, position);
    drawingParams.put(SimpleWeekView.VIEW_PARAMS_FOCUS_MONTH, focusMonth);
    v.setWeekParams(drawingParams, this.selectedDay.timezone);
    v.invalidate();

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

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (gestureDetector.onTouchEvent(event)) {
      SimpleWeekView view = (SimpleWeekView) v;
      Time day = ((SimpleWeekView) v).getDayFromLocation(event.getX());
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Touched day at Row=" + view.mWeek + " day=" + day.toString());
      }
      if (day != null) {
        onDayTapped(day);
      }
      return true;
    }
    return false;
  }

  /**
   * Maintains the same hour/min/sec but moves the day to the tapped day.
   *
   * @param day The day that was tapped
   */
  protected void onDayTapped(Time day) {
    day.hour = selectedDay.hour;
    day.minute = selectedDay.minute;
    day.second = selectedDay.second;
    setSelectedDay(day);
  }

  /**
   * This is here so we can identify single tap events and set the selected
   * day correctly
   */
  protected class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
      return true;
    }
  }

  public void setListView(ListView lv) {
    listView = lv;
  }
}

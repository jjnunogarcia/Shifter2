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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewSwitcher;

/**
 * This is the base class for Day and Week Activities.
 */
public class DayFragment extends Fragment implements EventHandler, ViewSwitcher.ViewFactory {
  public static final    String   TAG                     = DayFragment.class.getSimpleName();
  public static final    String   TIME_MILLIS             = "time_millis";
  public static final    String   NUM_OF_DAYS             = "num_of_days";
  protected static final String   BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
  /**
   * The view id used for all the views we create. It's OK to have all child views have the same ID. This ID is used to pick which view receives
   * focus when a view hierarchy is saved / restore
   */
  private static final   int      VIEW_ID                 = 1;
  private final          Runnable timeZoneUpdater         = new Runnable() {
    @Override
    public void run() {
      if (DayFragment.this.isAdded()) {
        selectedDay.timezone = Utils.getTimeZone(getActivity().getApplicationContext(), this);
        selectedDay.normalize(true);
      }
    }
  };
  private ViewSwitcher viewSwitcher;
  private DayView      dayView;
  private Animation    inAnimationForward;
  private Animation    outAnimationForward;
  private Animation    inAnimationBackward;
  private Animation    outAnimationBackward;
  private EventLoader  eventLoader;
  private Time         selectedDay;
  private int          numDays;

  public DayFragment() {
    selectedDay = new Time();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Bundle arguments = getArguments();
    long timeMillis;

    if (arguments != null) {
      timeMillis = arguments.getLong(TIME_MILLIS, -1);
      numDays = arguments.getInt(NUM_OF_DAYS, 0);
    } else {
      timeMillis = -1;
      numDays = 0;
    }

    if (timeMillis == -1) {
      selectedDay.setToNow();
    } else {
      selectedDay.set(timeMillis);
    }

    View view = inflater.inflate(R.layout.day_fragment, null);

    viewSwitcher = (ViewSwitcher) view.findViewById(R.id.switcher);

    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Context context = getActivity().getApplicationContext();

    inAnimationForward = AnimationUtils.loadAnimation(context, R.anim.slide_left_in);
    outAnimationForward = AnimationUtils.loadAnimation(context, R.anim.slide_left_out);
    inAnimationBackward = AnimationUtils.loadAnimation(context, R.anim.slide_right_in);
    outAnimationBackward = AnimationUtils.loadAnimation(context, R.anim.slide_right_out);

    eventLoader = new EventLoader(context);
    viewSwitcher.setFactory(this);
    viewSwitcher.getCurrentView().requestFocus();
    ((DayView) viewSwitcher.getCurrentView()).updateTitle();
  }

  @Override
  public View makeView() {
    timeZoneUpdater.run();
    dayView = new DayView(getActivity().getApplicationContext(), ((CustomApplication) getActivity().getApplication()).getCalendarController(), viewSwitcher, eventLoader, numDays);
    dayView.setId(VIEW_ID);
    dayView.setLayoutParams(new ViewSwitcher.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    dayView.setSelected(selectedDay, false, false);
    return dayView;
  }

  @Override
  public void onResume() {
    super.onResume();
    eventLoader.startBackgroundThread();
    timeZoneUpdater.run();
    eventsChanged();
    DayView view = (DayView) viewSwitcher.getCurrentView();
    view.handleOnResume();
    view.restartCurrentTimeUpdates();

    view = (DayView) viewSwitcher.getNextView();
    view.handleOnResume();
    view.restartCurrentTimeUpdates();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    long time = getSelectedTimeInMillis();
    if (time != -1) {
      outState.putLong(BUNDLE_KEY_RESTORE_TIME, time);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    DayView view = (DayView) viewSwitcher.getCurrentView();
    view.cleanup();
    view = (DayView) viewSwitcher.getNextView();
    view.cleanup();
    eventLoader.stopBackgroundThread();

    // Stop events cross-fade animation
    view.stopEventsAnimation();
    ((DayView) viewSwitcher.getNextView()).stopEventsAnimation();
  }

  private void goTo(Time goToTime, boolean ignoreTime, boolean animateToday) {
    if (viewSwitcher == null) {
      // The view hasn't been set yet. Just save the time and use it later.
      selectedDay.set(goToTime);
      return;
    }

    DayView currentView = (DayView) viewSwitcher.getCurrentView();

    // How does goTo time compared to what's already displaying?
    int diff = currentView.compareToVisibleTimeRange(goToTime);

    if (diff == 0) {
      // In visible range. No need to switch view
      currentView.setSelected(goToTime, ignoreTime, animateToday);
    } else {
      // Figure out which way to animate
      if (diff > 0) {
        viewSwitcher.setInAnimation(inAnimationForward);
        viewSwitcher.setOutAnimation(outAnimationForward);
      } else {
        viewSwitcher.setInAnimation(inAnimationBackward);
        viewSwitcher.setOutAnimation(outAnimationBackward);
      }

      DayView next = (DayView) viewSwitcher.getNextView();
      if (ignoreTime) {
        next.setFirstVisibleHour(currentView.getFirstVisibleHour());
      }

      next.setSelected(goToTime, ignoreTime, animateToday);
      next.reloadEvents();
      viewSwitcher.showNext();
      next.requestFocus();
      next.updateTitle();
      next.restartCurrentTimeUpdates();
    }
  }

  /**
   * Returns the selected time in milliseconds. The milliseconds are measured in UTC milliseconds from the epoch and uniquely specifies any selectable time.
   *
   * @return the selected time in milliseconds
   */
  public long getSelectedTimeInMillis() {
    if (viewSwitcher == null) {
      return -1;
    }
    DayView view = (DayView) viewSwitcher.getCurrentView();
    if (view == null) {
      return -1;
    }
    return view.getSelectedTimeInMillis();
  }

  @Override
  public void eventsChanged() {
    if (viewSwitcher != null) {
      DayView view = (DayView) viewSwitcher.getCurrentView();
      view.clearCachedEvents();
      view.reloadEvents();

      view = (DayView) viewSwitcher.getNextView();
      view.clearCachedEvents();
    }
  }

  private Event getSelectedEvent() {
    DayView view = (DayView) viewSwitcher.getCurrentView();
    return view.getSelectedEvent();
  }

  private boolean isEventSelected() {
    DayView view = (DayView) viewSwitcher.getCurrentView();
    return view.isEventSelected();
  }

  private Event getNewEvent() {
    DayView view = (DayView) viewSwitcher.getCurrentView();
    return view.getNewEvent();
  }

  public DayView getNextView() {
    return (DayView) viewSwitcher.getNextView();
  }

  @Override
  public long getSupportedEventTypes() {
    return EventType.GO_TO | EventType.EVENTS_CHANGED;
  }

  @Override
  public void handleEvent(EventInfo msg) {
    if (msg.eventType == EventType.GO_TO) {
// TODO support a range of time
// TODO support event_id
// TODO support select message
      goTo(msg.selectedTime, (msg.extraLong & CalendarController.EXTRA_GOTO_DATE) != 0, (msg.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0);
    } else if (msg.eventType == EventType.EVENTS_CHANGED) {
      eventsChanged();
    }
  }
}

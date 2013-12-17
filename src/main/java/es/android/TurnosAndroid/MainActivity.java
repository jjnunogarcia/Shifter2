package es.android.TurnosAndroid;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;

public class MainActivity extends FragmentActivity implements EventHandler {

  private Fragment           monthFrag;
  private Fragment           dayFrag;
  private CalendarController calendarController;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    new ImportEntries(getApplicationContext()).execute();
    calendarController = new CalendarController(getApplicationContext());
    setContentView(R.layout.calendar_activity);
    addMonthFragment();
    calendarController.registerEventHandler(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

  @Override
  public long getSupportedEventTypes() {
    return EventType.GO_TO | EventType.VIEW_EVENT | EventType.UPDATE_TITLE;
  }

  @Override
  public void handleEvent(EventInfo event) {
    if (event.eventType == EventType.GO_TO) {
      addDayFragment(event);
    }

    if (event.eventType == EventType.VIEW_EVENT) {
      //					FragmentTransaction ft = getFragmentManager().beginTransaction();
//					edit = new EditEvent(event.id);
//					ft.replace(R.id.cal_frame, edit).addToBackStack(null).commit();
    }
  }

  @Override
  public void eventsChanged() {

  }

  private void addMonthFragment() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    Bundle bundle = new Bundle();
    bundle.putLong(MonthFragment.INITIAL_TIME, System.currentTimeMillis());
    monthFrag = new MonthFragment();
    monthFrag.setArguments(bundle);
    ft.replace(R.id.cal_frame, monthFrag, MonthFragment.TAG).commit();
  }

  private void addDayFragment(EventInfo event) {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    Bundle bundle = new Bundle();
    bundle.putLong(DayFragment.TIME_MILLIS, event.startTime.toMillis(true));
    bundle.putInt(DayFragment.NUM_OF_DAYS, 1);
    dayFrag = new DayFragment();
    dayFrag.setArguments(bundle);
    ft.replace(R.id.cal_frame, dayFrag, DayFragment.TAG).commit();
  }

  public CalendarController getCalendarController() {
    return calendarController;
  }
}

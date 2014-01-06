package es.android.TurnosAndroid;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import es.android.TurnosAndroid.controllers.CalendarController;
import es.android.TurnosAndroid.fragments.*;
import es.android.TurnosAndroid.model.EventInfo;
import es.android.TurnosAndroid.model.EventType;

public class MainActivity extends FragmentActivity implements EventHandler {
  private DrawerLayout          drawerLayout;
  private ListView              drawerList;
  private String[]              drawerElements;
  private CharSequence          windowTitle;
  private ActionBarDrawerToggle drawerToggle;
  private CharSequence          drawerTitle;
  private ActionBarManager      actionBarManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);

    // TODO load entries depending on the initial view
    drawerElements = getResources().getStringArray(R.array.drawer_elements);
    drawerTitle = getResources().getString(R.string.drawer_title);
    windowTitle = getTitle();
    actionBarManager = new ActionBarManager(getActionBar());

    drawerLayout = (DrawerLayout) findViewById(R.id.calendar_activity_drawer_layout);
    drawerList = (ListView) findViewById(R.id.left_drawer);
    drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, drawerElements));
    drawerList.setOnItemClickListener(new DrawerItemClickListener());

    CalendarController calendarController = ((CustomApplication) getApplication()).getCalendarController();
    calendarController.registerEventHandler(this);
    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setHomeButtonEnabled(true);
    initDrawer();
  }

  private void initDrawer() {
    drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

      @Override
      public void onDrawerClosed(View view) {
        getActionBar().setTitle(windowTitle);
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        getActionBar().setTitle(drawerTitle);
      }
    };

    drawerLayout.setDrawerListener(drawerToggle);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    drawerToggle.syncState();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.action_bar_menu, menu);
    actionBarManager.attachInternalMenu(menu);
    addMonthFragment();
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Pass the event to ActionBarDrawerToggle, if it returns true, then it has handled the app icon touch event
    return drawerToggle.onOptionsItemSelected(item) || actionBarManager.onActionBarItemSelected(item);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    drawerToggle.onConfigurationChanged(newConfig);
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

//    if (event.eventType == EventType.VIEW_EVENT) {
    //					FragmentTransaction ft = getFragmentManager().beginTransaction();
//					edit = new EditEvent(event.id);
//					ft.replace(R.id.cal_frame, edit).addToBackStack(null).commit();
//    }
  }

  @Override
  public void eventsChanged() {
  }

  @Override
  public void setTitle(CharSequence title) {
    windowTitle = title;
    getActionBar().setTitle(windowTitle);
  }

  /**
   * Reacts to the element clicked on the left drawer
   *
   * @param position The position clicked.
   */
  private void selectItem(int position) {
    if (drawerList.getSelectedItemPosition() != position) {
      switch (position) {
        case 0:
          addMonthFragment();
          break;
        case 1:
          addMyEventsFragment();
          break;
        case 2:
          addMyPatternsFragment();
          break;
        case 3:
          addStatisticsFragment();
          break;
        default:
          break;
      }
    }

    drawerList.setItemChecked(position, true);
    setTitle(drawerElements[position]);
    drawerLayout.closeDrawer(drawerList);
  }

  public void addMonthFragment() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    Bundle bundle = new Bundle();
    bundle.putLong(MonthFragment.INITIAL_TIME, System.currentTimeMillis());
    MonthFragment monthFragment = new MonthFragment();
    monthFragment.setArguments(bundle);
    ft.replace(R.id.calendar_frame, monthFragment, MonthFragment.TAG).commit();
    actionBarManager.setMonthFragmentActionBar();
  }

  public void addDayFragment(EventInfo event) {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    Bundle bundle = new Bundle();
    bundle.putLong(DayFragment.TIME_MILLIS, event.startTime.toMillis(true));
    bundle.putInt(DayFragment.NUM_OF_DAYS, 1);
    DayFragment dayFragment = new DayFragment();
    dayFragment.setArguments(bundle);
    ft.replace(R.id.calendar_frame, dayFragment, DayFragment.TAG).addToBackStack(DayFragment.TAG).commit();
    actionBarManager.setDayFragmentActionBar();
  }

  public void addMyEventsFragment() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    MyEventsFragment myEventsFragment = new MyEventsFragment();
    ft.replace(R.id.calendar_frame, myEventsFragment, MyEventsFragment.TAG).commit();
    actionBarManager.setMyEventsFragmentActionBar();
  }

  public void addCreateEventFragment() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    CreateEventFragment createEventFragment = new CreateEventFragment();
    ft.replace(R.id.calendar_frame, createEventFragment, CreateEventFragment.TAG).addToBackStack(CreateEventFragment.TAG).commit();
    actionBarManager.setCreateEventFragmentActionBar();
  }

  public void addMyPatternsFragment() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    MyPatternsFragment myPatternsFragment = new MyPatternsFragment();
    ft.replace(R.id.calendar_frame, myPatternsFragment, MyPatternsFragment.TAG).commit();
    actionBarManager.setMyPatternsFragmentActionBar();
  }

  public void addStatisticsFragment() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    StatisticsFragment statisticsFragment = new StatisticsFragment();
    ft.replace(R.id.calendar_frame, statisticsFragment, StatisticsFragment.TAG).commit();
    actionBarManager.setStatisticsFragmentActionBar();
  }

  public ActionBarManager getActionBarManager() {
    return actionBarManager;
  }

  private class DrawerItemClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
      selectItem(position);
    }
  }
}

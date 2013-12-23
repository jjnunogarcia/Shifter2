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

public class MainActivity extends FragmentActivity implements EventHandler {

  private DrawerLayout          drawerLayout;
  private ListView              drawerList;
  private String[]              drawerElements;
  private CharSequence          windowTitle;
  private ActionBarDrawerToggle drawerToggle;
  private CharSequence          drawerTitle;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.calendar_activity);

    new ImportEntries(getApplicationContext()).execute();
    drawerElements = getResources().getStringArray(R.array.drawer_elements);
    drawerTitle = getResources().getString(R.string.drawer_title);
    windowTitle = getTitle();

    drawerLayout = (DrawerLayout) findViewById(R.id.calendar_activity_drawer_layout);
    drawerList = (ListView) findViewById(R.id.left_drawer);
    drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, drawerElements));
    drawerList.setOnItemClickListener(new DrawerItemClickListener());

    CalendarController calendarController = ((CustomApplication) getApplication()).getCalendarController();
    calendarController.registerEventHandler(this);
    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setHomeButtonEnabled(true);
    initDrawer();

    addMonthFragment();
  }

  private void initDrawer() {
    drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

      @Override
      public void onDrawerClosed(View view) {
        getActionBar().setTitle(windowTitle);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        getActionBar().setTitle(drawerTitle);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
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

//  @Override
//  public boolean onPrepareOptionsMenu(Menu menu) {
  // TODO show or hide action bar buttons
  // If the nav drawer is open, hide action items related to the content view
//    boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
//    menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
//    return super.onPrepareOptionsMenu(menu);
//  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Pass the event to ActionBarDrawerToggle, if it returns true, then it has handled the app icon touch event
    if (drawerToggle.onOptionsItemSelected(item)) {
      return true;
    }
    // Handle your other action bar items...

    return super.onOptionsItemSelected(item);
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

    if (event.eventType == EventType.VIEW_EVENT) {
      //					FragmentTransaction ft = getFragmentManager().beginTransaction();
//					edit = new EditEvent(event.id);
//					ft.replace(R.id.cal_frame, edit).addToBackStack(null).commit();
    }
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
        default:
          break;
      }
    }

    drawerList.setItemChecked(position, true);
    setTitle(drawerElements[position]);
    drawerLayout.closeDrawer(drawerList);
  }

  private void addMonthFragment() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    Bundle bundle = new Bundle();
    bundle.putLong(MonthFragment.INITIAL_TIME, System.currentTimeMillis());
    MonthFragment monthFrag = new MonthFragment();
    monthFrag.setArguments(bundle);
    ft.replace(R.id.calendar_frame, monthFrag, MonthFragment.TAG).commit();
  }

  private void addDayFragment(EventInfo event) {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    Bundle bundle = new Bundle();
    bundle.putLong(DayFragment.TIME_MILLIS, event.startTime.toMillis(true));
    bundle.putInt(DayFragment.NUM_OF_DAYS, 1);
    DayFragment dayFrag = new DayFragment();
    dayFrag.setArguments(bundle);
    ft.replace(R.id.calendar_frame, dayFrag, DayFragment.TAG).addToBackStack(null).commit();
  }

  private void addMyEventsFragment() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    MyEventsFragment myEventsFragment = new MyEventsFragment();
    ft.replace(R.id.calendar_frame, myEventsFragment, MyEventsFragment.TAG).addToBackStack(null).commit();
  }

  private class DrawerItemClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
      selectItem(position);
    }
  }

}

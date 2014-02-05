package es.android.TurnosAndroid;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import sherlock.navigationdrawer.compat.SherlockActionBarDrawerToggle;
import es.android.TurnosAndroid.controllers.CalendarController;
import es.android.TurnosAndroid.fragments.MonthFragment;
import es.android.TurnosAndroid.fragments.MyEventsFragment;
import es.android.TurnosAndroid.fragments.MyPatternsFragment;
import es.android.TurnosAndroid.fragments.StatisticsFragment;
import es.android.TurnosAndroid.fragments.dialogs.SetEventDialogFragment;
import es.android.TurnosAndroid.helpers.Utils;
import es.android.TurnosAndroid.interfaces.EventInteractionInterface;
import es.android.TurnosAndroid.model.Event;
import es.android.TurnosAndroid.model.EventInfo;
import es.android.TurnosAndroid.model.EventType;

// TODO Check logcat: why does it need permissions for calendar?
public class MainActivity extends SherlockFragmentActivity implements EventHandler, EventInteractionInterface {
  private DrawerLayout                  drawerLayout;
  private ListView                      drawerList;
  private String[]                      drawerElements;
  private CharSequence                  windowTitle;
  private SherlockActionBarDrawerToggle drawerToggle;
  private CharSequence                  drawerTitle;
  private ActionBarManager              actionBarManager;

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
    drawerToggle = new SherlockActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

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
    getSupportMenuInflater().inflate(R.menu.action_bar_menu, menu);
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
    // TODO if the app is sent to the background and reopened through launcher, this fragment is opened many times.
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    Bundle bundle = new Bundle();
    bundle.putLong(MonthFragment.KEY_INITIAL_TIME, System.currentTimeMillis());
    MonthFragment monthFragment = new MonthFragment();
    monthFragment.setArguments(bundle);
    ft.replace(R.id.calendar_frame, monthFragment, MonthFragment.TAG).commit();
    actionBarManager.setMonthFragmentActionBar();
  }

  public void addMyEventsFragment() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    MyEventsFragment myEventsFragment = new MyEventsFragment();
    ft.replace(R.id.calendar_frame, myEventsFragment, MyEventsFragment.TAG).commit();
    actionBarManager.setMyEventsFragmentActionBar();
  }

  /**
   * Displays the dialog that allows to add a new event in the event list or to update the data of one already existin event.
   *
   * @param event The event to open
   * @return The dialog created
   */
  public SetEventDialogFragment addSetEventDialog(Event event) {
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    SetEventDialogFragment setEventDialog = new SetEventDialogFragment();
    Bundle arguments = new Bundle();
    arguments.putParcelable(Utils.KEY_EVENT_TO_MANAGE, event);
    setEventDialog.setArguments(arguments);
    setEventDialog.show(fragmentTransaction, SetEventDialogFragment.TAG);

    return setEventDialog;
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

  //---------------------------- EventInteractionInterface ----------------------------//
  @Override
  public void onSaveEventClicked(Event event) {
    MyEventsFragment myEventsFragment = (MyEventsFragment) getSupportFragmentManager().findFragmentByTag(MyEventsFragment.TAG);

    if (myEventsFragment != null) {
      myEventsFragment.addEvent(event);
    }
  }

  @Override
  public void onUpdateEventClicked(Event event) {
    MyEventsFragment myEventsFragment = (MyEventsFragment) getSupportFragmentManager().findFragmentByTag(MyEventsFragment.TAG);

    if (myEventsFragment != null) {
      myEventsFragment.refresh();
    }
  }

  @Override
  public void onDeleteEventClicked(Event event) {
    MyEventsFragment myEventsFragment = (MyEventsFragment) getSupportFragmentManager().findFragmentByTag(MyEventsFragment.TAG);

    if (myEventsFragment != null) {
      myEventsFragment.removeEvent(event);
    }
  }
  //-----------------------------------------------------------------------------------//

  private class DrawerItemClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
      selectItem(position);
    }
  }
}

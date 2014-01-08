package es.android.TurnosAndroid;

import android.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Date: 19.12.13
 *
 * @author jjnunogarcia@gmail.com
 */
public class ActionBarManager {
  private final ActionBar                     actionBar;
  private       MonthActionBarInterface       monthActionBarInterface;
  private       MyEventsActionBarInterface    myEventsActionBarInterface;
  private       CreateEventActionBarInterface createEventActionBarInterface;
  private       Menu                          menu;

  public ActionBarManager(ActionBar actionBar) {
    this.actionBar = actionBar;
  }

  public void attachInternalMenu(Menu menu) {
    this.menu = menu;
    setMonthFragmentActionBar();
  }

  public boolean onActionBarItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.today_button:
        if (monthActionBarInterface != null) {
          monthActionBarInterface.onTodayClicked();
        }
        break;
      case R.id.create_event_button:
        if (myEventsActionBarInterface != null) {
          myEventsActionBarInterface.onNewEventClicked();
        }
        break;
      case R.id.save_event_button:
        if (createEventActionBarInterface != null) {
          createEventActionBarInterface.onSaveEventClicked();
        }
        break;
      case R.id.delete_event_button:
        if (createEventActionBarInterface != null) {
          createEventActionBarInterface.onDeleteEventClicked();
        }
        break;
      default:
        break;
    }

    return true;
  }

  public void setMonthFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, true);
    menu.setGroupVisible(R.id.my_events_action_bar, false);
    menu.setGroupVisible(R.id.create_event_action_bar, false);
    menu.setGroupVisible(R.id.my_patterns_action_bar, false);
    menu.setGroupVisible(R.id.statistics_action_bar, false);
  }

  public void setDayFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, false);
    menu.setGroupVisible(R.id.create_event_action_bar, false);
    menu.setGroupVisible(R.id.my_patterns_action_bar, false);
    menu.setGroupVisible(R.id.statistics_action_bar, false);
  }

  public void setMyEventsFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, true);
    menu.setGroupVisible(R.id.create_event_action_bar, false);
    menu.setGroupVisible(R.id.my_patterns_action_bar, false);
    menu.setGroupVisible(R.id.statistics_action_bar, false);
  }

  public void setCreateEventFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, false);
    menu.setGroupVisible(R.id.create_event_action_bar, true);
    menu.setGroupVisible(R.id.my_patterns_action_bar, false);
    menu.setGroupVisible(R.id.statistics_action_bar, false);
  }

  public void setMyPatternsFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, false);
    menu.setGroupVisible(R.id.create_event_action_bar, false);
    menu.setGroupVisible(R.id.my_patterns_action_bar, true);
    menu.setGroupVisible(R.id.statistics_action_bar, false);
  }

  public void setStatisticsFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, false);
    menu.setGroupVisible(R.id.create_event_action_bar, false);
    menu.setGroupVisible(R.id.my_patterns_action_bar, false);
    menu.setGroupVisible(R.id.statistics_action_bar, true);
  }

  public void setMonthActionBarInterface(MonthActionBarInterface monthActionBarInterface) {
    this.monthActionBarInterface = monthActionBarInterface;
  }

  public void setMyEventsActionBarInterface(MyEventsActionBarInterface myEventsActionBarInterface) {
    this.myEventsActionBarInterface = myEventsActionBarInterface;
  }

  public void setCreateEventActionBarInterface(CreateEventActionBarInterface createEventActionBarInterface) {
    this.createEventActionBarInterface = createEventActionBarInterface;
  }
}

package es.android.TurnosAndroid;

import android.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Date: 19.12.13
 *
 * @author jjnunogarcia@gmail.com
 */
public class ActionBarManager {
  private final ActionBar                  actionBar;
  private       MonthActionBarInterface    monthActionBarInterface;
  private       MyEventsActionBarInterface myEventsActionBarInterface;
  private       Menu                       menu;

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
          myEventsActionBarInterface.onAddEventClicked();
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
    menu.setGroupVisible(R.id.my_patterns_action_bar, false);
    menu.setGroupVisible(R.id.statistics_action_bar, false);
  }

  public void setMyEventsFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, true);
    menu.setGroupVisible(R.id.my_patterns_action_bar, false);
    menu.setGroupVisible(R.id.statistics_action_bar, false);
  }

  public void setMyPatternsFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, false);
    menu.setGroupVisible(R.id.my_patterns_action_bar, true);
    menu.setGroupVisible(R.id.statistics_action_bar, false);
  }

  public void setStatisticsFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, false);
    menu.setGroupVisible(R.id.my_patterns_action_bar, false);
    menu.setGroupVisible(R.id.statistics_action_bar, true);
  }

  public void setMonthActionBarInterface(MonthActionBarInterface monthActionBarInterface) {
    this.monthActionBarInterface = monthActionBarInterface;
  }

  public void setMyEventsActionBarInterface(MyEventsActionBarInterface myEventsActionBarInterface) {
    this.myEventsActionBarInterface = myEventsActionBarInterface;
  }
}

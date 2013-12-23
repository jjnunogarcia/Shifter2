package es.android.TurnosAndroid;

import android.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Date: 19.12.13
 *
 * @author nuno@neofonie.de
 */
public class ActionBarManager {
  private final ActionBar          actionBar;
  private final ActionBarInterface actionBarInterface;
  private       Menu               menu;

  public ActionBarManager(ActionBar actionBar, ActionBarInterface actionBarInterface) {
    this.actionBar = actionBar;
    this.actionBarInterface = actionBarInterface;
  }

  public void attachInternalMenu(Menu menu) {
    this.menu = menu;
    setMonthFragmentActionBar();
  }

  public boolean onActionBarItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.today_button:
        actionBarInterface.onTodayClicked();
        break;
      case R.id.create_event_button:
        actionBarInterface.onNewEventClicked();
        break;
      case R.id.save_event_button:
        actionBarInterface.onSaveEventClicked();
        break;
      case R.id.delete_event_button:
        actionBarInterface.onDeleteEventClicked();
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
  }

  public void setDayFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, false);
    menu.setGroupVisible(R.id.create_event_action_bar, false);
  }

  public void setMyEventsFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, true);
    menu.setGroupVisible(R.id.create_event_action_bar, false);
  }

  public void setCreateEventFragmentActionBar() {
    menu.setGroupVisible(R.id.month_action_bar, false);
    menu.setGroupVisible(R.id.my_events_action_bar, false);
    menu.setGroupVisible(R.id.create_event_action_bar, true);
  }

}

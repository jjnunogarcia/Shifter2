package es.android.TurnosAndroid;

import android.app.Application;

/**
 * Date: 18.12.13
 *
 * @author jjnunogarcia@gmail.com
 */
public class CustomApplication extends Application {
  private CalendarController calendarController;

  @Override
  public void onCreate() {
    super.onCreate();
    calendarController = new CalendarController(getApplicationContext());
  }

  public CalendarController getCalendarController() {
    return calendarController;
  }
}

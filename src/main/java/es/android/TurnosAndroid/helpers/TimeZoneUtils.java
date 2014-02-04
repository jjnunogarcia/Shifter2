package es.android.TurnosAndroid.helpers;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.Time;

import java.util.HashSet;

/**
 * This class contains methods specific to reading and writing time zone values.
 * Date: 03.12.13
 *
 * @author jjnunogarcia@gmail.com
 */
public class TimeZoneUtils {
  public static final     String[]          CALENDAR_CACHE_POJECTION = {"key", "value"};
  /**
   * This is the key used for writing whether or not a home time zone should be used in the Calendar app to the Calendar Preferences.
   */
  public static final     String            KEY_HOME_TZ_ENABLED      = "preferences_home_tz_enabled";
  /**
   * This is the key used for writing the time zone that should be used if home time zones are enabled for the Calendar app.
   */
  public static final     String            KEY_HOME_TZ              = "preferences_home_tz";
  static final            String            SHARED_PREFS_NAME        = "calendar_preferences";
  private volatile static boolean           firstTimeZoneRequest     = true;
  private volatile static boolean           timeZoneQueryInProgress  = false;
  private volatile static boolean           useHomeTimeZone          = false;
  private volatile static String            homeTimeZone             = Time.getCurrentTimezone();
  private static          HashSet<Runnable> timeZoneCallbacks        = new HashSet<Runnable>();
  private static AsyncTimeZoneHandler asyncTimeZoneHandler;


  public TimeZoneUtils() {
  }

  /**
   * Gets the time zone that Calendar should be displayed in
   * <p/>
   * This is a helper method to get the appropriate time zone for Calendar. If this is the first time this method has been called it will initiate an asynchronous
   * query to verify that the data in preferences is correct. The callback supplied will only be called if this query returns a value other than what is stored in
   * preferences and should cause the calling activity to refresh anything that depends on calling this method.
   *
   * @param context  The calling activity
   * @param callback The runnable that should execute if a query returns new values
   * @return The string value representing the time zone Calendar should display
   */
  public static String getTimeZone(Context context, Runnable callback) {
    synchronized (timeZoneCallbacks) {
      if (firstTimeZoneRequest) {
        timeZoneQueryInProgress = true;
        firstTimeZoneRequest = false;

        SharedPreferences prefs = SharedPrefHelper.getSharedPreferences(context, SHARED_PREFS_NAME);
        useHomeTimeZone = prefs.getBoolean(KEY_HOME_TZ_ENABLED, false);
        homeTimeZone = prefs.getString(KEY_HOME_TZ, Time.getCurrentTimezone());

        // When the async query returns it should synchronize on timeZoneCallbacks, update useHomeTimeZone, homeTimeZone, and the
        // preferences, set timeZoneQueryInProgress to false, and call all the runnables in timeZoneCallbacks.
        if (asyncTimeZoneHandler == null) {
          asyncTimeZoneHandler = new AsyncTimeZoneHandler(context.getContentResolver());
        }
        asyncTimeZoneHandler.startQuery(0, context, CalendarContract.CalendarCache.URI, CALENDAR_CACHE_POJECTION, null, null, null);
      }

      if (timeZoneQueryInProgress) {
        timeZoneCallbacks.add(callback);
      }
    }

    return useHomeTimeZone ? homeTimeZone : Time.getCurrentTimezone();
  }

  /**
   * This is a helper class for handling the async queries and updates for the time zone settings in Calendar.
   */
  private static class AsyncTimeZoneHandler extends AsyncQueryHandler {
    public AsyncTimeZoneHandler(ContentResolver cr) {
      super(cr);
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
      synchronized (timeZoneCallbacks) {
        if (cursor == null) {
          timeZoneQueryInProgress = false;
          firstTimeZoneRequest = true;
          return;
        }

        boolean writePrefs = false;
        // Check the values in the db
        int keyColumn = cursor.getColumnIndexOrThrow(CalendarContract.CalendarCache.KEY);
        int valueColumn = cursor.getColumnIndexOrThrow(CalendarContract.CalendarCache.VALUE);
        while (cursor.moveToNext()) {
          String key = cursor.getString(keyColumn);
          String value = cursor.getString(valueColumn);
          if (TextUtils.equals(key, CalendarContract.CalendarCache.KEY_TIMEZONE_TYPE)) {
            boolean useHomeTZ = !TextUtils.equals(
                value, CalendarContract.CalendarCache.TIMEZONE_TYPE_AUTO);
            if (useHomeTZ != useHomeTimeZone) {
              writePrefs = true;
              useHomeTimeZone = useHomeTZ;
            }
          } else if (TextUtils.equals(key, CalendarContract.CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS)) {
            if (!TextUtils.isEmpty(value) && !TextUtils.equals(homeTimeZone, value)) {
              writePrefs = true;
              homeTimeZone = value;
            }
          }
        }
        cursor.close();
        if (writePrefs) {
          SharedPreferences prefs = SharedPrefHelper.getSharedPreferences((Context) cookie, SHARED_PREFS_NAME);
          // Write the prefs
          SharedPrefHelper.setSharedPreference(prefs, KEY_HOME_TZ_ENABLED, useHomeTimeZone);
          SharedPrefHelper.setSharedPreference(prefs, KEY_HOME_TZ, homeTimeZone);
        }

        timeZoneQueryInProgress = false;
        for (Runnable callback : timeZoneCallbacks) {
          if (callback != null) {
            callback.run();
          }
        }
        timeZoneCallbacks.clear();
      }
    }
  }
}

package es.android.TurnosAndroid.requests;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.AsyncTaskLoader;
import es.android.TurnosAndroid.database.DBConstants;
import es.android.TurnosAndroid.database.DatabaseHelper;

/**
 * User: Jesús
 * Date: 23/12/13
 */
public class CalendarEventsLoader extends AsyncTaskLoader {
  private Context context;
  private long    initialDay;
  private long    finalDay;

  public CalendarEventsLoader(Context context, long initialDay, long finalDay) {
    super(context);
    this.context = context;
    this.initialDay = initialDay;
    this.finalDay = finalDay;
  }

  @Override
  public Object loadInBackground() {
    DatabaseHelper databaseHelper = new DatabaseHelper(context);
    SQLiteDatabase db = databaseHelper.getReadableDatabase();

//    String query = "SELECT * FROM " + DBConstants.CALENDAR_EVENTS_TABLE + " AS a JOIN " +
//                   DBConstants.EVENTS_TABLE + " AS b ON a." + DBConstants.EVENT_ID + " = b." + DBConstants.ID +
//                   " WHERE a." + DBConstants.DATE + ">=? AND a." + DBConstants.DATE + "<=?";

    String query = "SELECT * FROM " + DBConstants.CALENDAR_EVENTS_TABLE + ", " +
                   DBConstants.EVENTS_TABLE + " WHERE " + DBConstants.CALENDAR_EVENTS_TABLE + "." + DBConstants.EVENT_ID + "=" +
                   DBConstants.EVENTS_TABLE + "." + DBConstants.ID;

//    String query = "SELECT " + DBConstants.DATE + " FROM " + DBConstants.CALENDAR_EVENTS_TABLE;

    String[] selectionArgs = new String[] {String.valueOf(initialDay), String.valueOf(finalDay)};

//    return db.rawQuery(query, selectionArgs);
    return db.rawQuery(query, null);
  }
}

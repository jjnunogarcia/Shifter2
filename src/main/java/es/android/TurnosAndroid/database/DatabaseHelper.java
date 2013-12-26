package es.android.TurnosAndroid.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * User: Jesús
 * Date: 17/11/13
 */
public class DatabaseHelper extends SQLiteOpenHelper {
  private static final String TAG = DatabaseHelper.class.getSimpleName();

  public DatabaseHelper(Context context) {
    super(context, DBConstants.DATABASE_NAME, null, DBConstants.DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    createTables(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
    db.execSQL("DROP TABLE IF EXISTS " + DBConstants.EVENTS_TABLE);
    onCreate(db);
  }

  private void createTables(SQLiteDatabase db) {
    // TODO it's necessary to create two tables: one for all the different event types and the other to store which events are in which days
    db.execSQL("CREATE TABLE " + DBConstants.EVENTS_TABLE + "(" +
               DBConstants.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
               DBConstants.NAME + " TEXT, " +
               DBConstants.DESCRIPTION + " TEXT, " +
               DBConstants.START + " INTEGER, " +
               DBConstants.DURATION + " INTEGER, " +
               DBConstants.LOCATION + " TEXT, " +
               DBConstants.COLOR + " INTEGER);");
  }
}
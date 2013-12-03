package es.android.TurnosAndroid;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * User: Jesús
 * Date: 17/11/13
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) {
        super(context, DBConstants.DATABASE_NAME, null, DBConstants.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("CalendarProvider", "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + DBConstants.EVENTS_TABLE);
        onCreate(db);
    }

    private void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DBConstants.EVENTS_TABLE + "(" + DBConstants.ID + " integer primary key autoincrement, " + DBConstants.EVENT + " TEXT, " + DBConstants.LOCATION + " TEXT, " + DBConstants.DESCRIPTION + " TEXT, "
                   + DBConstants.START + " INTEGER, " + DBConstants.END + " INTEGER, " + DBConstants.CALENDAR_ID + " INTEGER, " + DBConstants.START_DAY + " INTEGER, " + DBConstants.END_DAY + " INTEGER, " + DBConstants.START_TIME + " INTEGER, "
                   + DBConstants.END_TIME + " INTEGER, " + DBConstants.EVENT_ID + " INTEGER);");
    }
}
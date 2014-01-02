package es.android.TurnosAndroid.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.HashMap;

public class CalendarProvider extends ContentProvider {

  private static final String AUTHORITY           = "es.android.TurnosAndroid.database.calendarprovider";
  public static final  Uri    EVENTS_URI          = Uri.parse("content://" + AUTHORITY + "/" + DBConstants.EVENTS_TABLE);
  public static final  Uri    CALENDAR_EVENTS_URI = Uri.parse("content://" + AUTHORITY + "/" + DBConstants.CALENDAR_EVENTS_TABLE);
  public static final  Uri    PATTERNS_URI        = Uri.parse("content://" + AUTHORITY + "/" + DBConstants.PATTERNS_TABLE);
  private static final UriMatcher              uriMatcher;
  private static final HashMap<String, String> MY_EVENTS_PROJECTION_MAP;
  private static final HashMap<String, String> CALENDAR_EVENTS_PROJECTION_MAP;

  static {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(AUTHORITY, DBConstants.EVENTS_TABLE, 1);
    uriMatcher.addURI(AUTHORITY, DBConstants.CALENDAR_EVENTS_TABLE, 2);
    uriMatcher.addURI(AUTHORITY, DBConstants.PATTERNS_TABLE, 3);

    MY_EVENTS_PROJECTION_MAP = new HashMap<String, String>();
    MY_EVENTS_PROJECTION_MAP.put(DBConstants.ID, DBConstants.ID);
    MY_EVENTS_PROJECTION_MAP.put(DBConstants.NAME, DBConstants.NAME);
    MY_EVENTS_PROJECTION_MAP.put(DBConstants.DESCRIPTION, DBConstants.DESCRIPTION);
    MY_EVENTS_PROJECTION_MAP.put(DBConstants.START, DBConstants.START);
    MY_EVENTS_PROJECTION_MAP.put(DBConstants.DURATION, DBConstants.DURATION);
    MY_EVENTS_PROJECTION_MAP.put(DBConstants.LOCATION, DBConstants.LOCATION);
    MY_EVENTS_PROJECTION_MAP.put(DBConstants.COLOR, DBConstants.COLOR);

    CALENDAR_EVENTS_PROJECTION_MAP = new HashMap<String, String>();
    CALENDAR_EVENTS_PROJECTION_MAP.put(DBConstants.ID, DBConstants.ID);
    CALENDAR_EVENTS_PROJECTION_MAP.put(DBConstants.DATE, DBConstants.DATE);
    CALENDAR_EVENTS_PROJECTION_MAP.put(DBConstants.EVENT_ID, DBConstants.EVENT_ID);
  }

  private DatabaseHelper DBHelper;
  private SQLiteDatabase db;

  @Override
  public boolean onCreate() {
    DBHelper = new DatabaseHelper(getContext());
    db = DBHelper.getWritableDatabase();
    return (db != null);
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    int count = 0;
    int num = uriMatcher.match(uri);
    if (num == 1) {
      count = db.delete(DBConstants.EVENTS_TABLE, selection, selectionArgs);
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    Uri _uri = null;

    if (uriMatcher.match(uri) == 1) {
      long rowID = db.insert(DBConstants.EVENTS_TABLE, null, values);
      if (rowID > 0) {
        _uri = ContentUris.withAppendedId(EVENTS_URI, rowID);
        getContext().getContentResolver().notifyChange(uri, null);
      } else {
        throw new SQLException("Failed to insert row into " + uri);
      }
    } else if (uriMatcher.match(uri) == 2) {
      long rowID = db.insert(DBConstants.CALENDAR_EVENTS_TABLE, null, values);
      if (rowID > 0) {
        _uri = ContentUris.withAppendedId(CALENDAR_EVENTS_URI, rowID);
        getContext().getContentResolver().notifyChange(uri, null);
      } else {
        throw new SQLException("Failed to insert row into " + uri);
      }
    }

    return _uri;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();

    if (uriMatcher.match(uri) == 1) {
      sqlBuilder.setTables(DBConstants.EVENTS_TABLE);
    } else if (uriMatcher.match(uri) == 2) {
      sqlBuilder.setTables(DBConstants.EVENTS_TABLE);
      sqlBuilder.setTables(DBConstants.CALENDAR_EVENTS_TABLE);
    }

    Cursor c = sqlBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
//    c.setNotificationUri(getContext().getContentResolver(), uri);
    return c;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    int count;
    int num = uriMatcher.match(uri);

    if (num == 1) {
      count = db.update(DBConstants.EVENTS_TABLE, values, selection, selectionArgs);
    } else {
      throw new IllegalArgumentException("Unknown URI " + uri);
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }
}

package es.android.TurnosAndroid.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import es.android.TurnosAndroid.database.DBConstants;
import es.android.TurnosAndroid.database.DatabaseHelper;

import java.util.HashMap;
import java.util.List;

public class CalendarProvider extends ContentProvider {

  private static final String AUTHORITY   = "es.android.TurnosAndroid.calendarprovider";
  public static final  Uri    CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/events");
  private static final UriMatcher              uriMatcher;
  private static final HashMap<String, String> mMap;

  static {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(AUTHORITY, DBConstants.EVENTS_TABLE, 1);
    uriMatcher.addURI(AUTHORITY, DBConstants.EVENTS_TABLE + "/#", 2);
    uriMatcher.addURI(AUTHORITY, DBConstants.EVENTS_TABLE + "/#/#", 3);

    mMap = new HashMap<String, String>();
    mMap.put(DBConstants.ID, DBConstants.ID);
    mMap.put(DBConstants.NAME, DBConstants.NAME);
    mMap.put(DBConstants.DESCRIPTION, DBConstants.DESCRIPTION);
    mMap.put(DBConstants.START_TIME, DBConstants.START_TIME);
    mMap.put(DBConstants.DURATION, DBConstants.DURATION);
    mMap.put(DBConstants.START_DAY, DBConstants.START_DAY);
    mMap.put(DBConstants.END_DAY, DBConstants.END_DAY);
    mMap.put(DBConstants.LOCATION, DBConstants.LOCATION);
    mMap.put(DBConstants.DISPLAY_COLOR, DBConstants.DISPLAY_COLOR);
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
    } else if (num == 2) {
      String id = uri.getPathSegments().get(1);
      count = db.delete(DBConstants.EVENTS_TABLE, DBConstants.ID + " = " + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
    }
<<<<<<< HEAD:src/main/java/es/android/TurnosAndroid/database/CalendarProvider.java

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
    } else if (num == 2) {
      String id = uri.getPathSegments().get(1);
      count = db.delete(DBConstants.EVENTS_TABLE, DBConstants.ID + " = " + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
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
    long rowID = db.insert(DBConstants.EVENTS_TABLE, null, values);
    Uri _uri;
    if (rowID > 0) {
      _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
      getContext().getContentResolver().notifyChange(uri, null);
    } else {
      throw new SQLException("Failed to insert row into " + uri);
    }
=======
    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    long rowID = db.insert(DBConstants.EVENTS_TABLE, null, values);
    Uri _uri;
    if (rowID > 0) {
      _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
      getContext().getContentResolver().notifyChange(uri, null);
    } else {
      throw new SQLException("Failed to insert row into " + uri);
    }
>>>>>>> 32a8c06efd73f2912cb8bb5aeda1dddc59b22e2e:src/main/java/es/android/TurnosAndroid/CalendarProvider.java
    return _uri;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
    sqlBuilder.setTables(DBConstants.EVENTS_TABLE);

    if (uriMatcher.match(uri) == 1) {
      sqlBuilder.setProjectionMap(mMap);
    } else if (uriMatcher.match(uri) == 2) {
      sqlBuilder.setProjectionMap(mMap);
      sqlBuilder.appendWhere(DBConstants.ID + "=?");
<<<<<<< HEAD:src/main/java/es/android/TurnosAndroid/database/CalendarProvider.java
      selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
    } else if (uriMatcher.match(uri) == 3) {
      sqlBuilder.setProjectionMap(mMap);
      sqlBuilder.appendWhere(DBConstants.START + ">=? OR ");
      sqlBuilder.appendWhere(DBConstants.END + "<=?");
      List<String> list = uri.getPathSegments();
      String start = list.get(1);
      String end = list.get(2);
      selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{start, end});
    }

    if (sortOrder == null || sortOrder.equals("")) {
      sortOrder = DBConstants.START + " COLLATE LOCALIZED ASC";
    }

    Cursor c = sqlBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
=======
      selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[] {uri.getLastPathSegment()});
    } else if (uriMatcher.match(uri) == 3) {
      sqlBuilder.setProjectionMap(mMap);
      List<String> list = uri.getPathSegments();
      String start = list.get(1);
      String end = list.get(2);
      selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[] {start, end});
>>>>>>> 32a8c06efd73f2912cb8bb5aeda1dddc59b22e2e:src/main/java/es/android/TurnosAndroid/CalendarProvider.java
    }

    Cursor c = sqlBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    c.setNotificationUri(getContext().getContentResolver(), uri);
    return c;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    int count;
    int num = uriMatcher.match(uri);

    if (num == 1) {
      count = db.update(DBConstants.EVENTS_TABLE, values, selection, selectionArgs);
    } else if (num == 2) {
      count = db.update(DBConstants.EVENTS_TABLE, values, DBConstants.ID + " = " + uri.getPathSegments().get(1) + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
    } else {
      throw new IllegalArgumentException("Unknown URI " + uri);
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }
}

package es.android.TurnosAndroid.database;

/**
 * User: Jesús
 * Date: 17/11/13
 */
public class DBConstants {
  public static final String   DATABASE_NAME           = "turnos_android";
  public static final int      DATABASE_VERSION        = 1;
  public static final String   EVENTS_TABLE            = "events";
  public static final String   ID                      = "_id";
  public static final String   NAME                    = "name";
  public static final String   SORT_EVENTS_BY_NAME_ASC = NAME + " ASC";
  public static final String   DESCRIPTION             = "description";
  public static final String   START                   = "start";
  public static final String   DURATION                = "duration";
  public static final String   LOCATION                = "location";
  public static final String   COLOR                   = "color";
  public static final String[] EVENTS_PROJECTION       = new String[]{
      ID,
      NAME,
      DESCRIPTION,
      START,
      DURATION,
      LOCATION,
      COLOR
  };
  public static final String   CALENDAR_EVENTS_TABLE   = "calendarevents";
  public static final String   DAY                     = "date";
  public static final String   EVENT_ID                = "event_id";
  public static final String[] MONTH_PROJECTION        = new String[]{
      ID,
      DAY,
      EVENT_ID,
      NAME,
      DESCRIPTION,
      START,
      DURATION,
      LOCATION,
      COLOR
  };
  public static final String   PATTERNS_TABLE          = "patterns";
  public static final String[] PATTERNS_PROJECTION     = new String[]{
      // TODO fill the fields
  };
  // TODO
  public static final String   SORT_PATTERNS_BY        = "";
}

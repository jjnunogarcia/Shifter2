package es.android.TurnosAndroid.model;

/**
 * User: Jesús
 * Date: 2/12/13
 * One of the event types that are sent to or from the controller
 */
public class EventType {
  // TODO Change for enum
  public static final long CREATE_EVENT                    = 1L;
  // Simple view of an event
  public static final long VIEW_EVENT                      = 1L << 1;
  // Full detail view in read only mode
  public static final long VIEW_EVENT_DETAILS              = 1L << 2;
  // full detail view in edit mode
  public static final long EDIT_EVENT                      = 1L << 3;
  public static final long DELETE_EVENT                    = 1L << 4;
  public static final long GO_TO                           = 1L << 5;
  public static final long EVENTS_CHANGED                  = 1L << 7;
  public static final long SEARCH                          = 1L << 8;
  // User has pressed the home key
  public static final long USER_HOME                       = 1L << 9;
  // date range has changed, update the title
  public static final long UPDATE_TITLE                    = 1L << 10;
  // select which calendars to display
  public static final long LAUNCH_SELECT_VISIBLE_CALENDARS = 1L << 11;
}

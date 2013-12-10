package es.android.TurnosAndroid;

import android.content.ComponentName;
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;

/**
 * User: Jesús
 * Date: 2/12/13
 */
public class EventInfo {
  private static final String TAG                            = EventInfo.class.getSimpleName();
  private static final long   ALL_DAY_MASK                   = 0x100;
  private static final int    ATTENDEE_STATUS_NONE_MASK      = 0x01;
  private static final int    ATTENDEE_STATUS_ACCEPTED_MASK  = 0x02;
  private static final int    ATTENDEE_STATUS_DECLINED_MASK  = 0x04;
  private static final int    ATTENDEE_STATUS_TENTATIVE_MASK = 0x08;

  public long          eventType; // one of the EventType
  public ViewType      viewType; // one of the ViewType
  public long          id; // event id
  public Time          selectedTime; // the selected time in focus
  public Time          startTime; // start of a range of time.
  public Time          endTime; // end of a range of time.
  public int           x; // x coordinate in the activity space
  public int           y; // y coordinate in the activity space
  public String        query; // query for a user search
  public ComponentName componentName;  // used in combination with query

  /**
   * For EventType.VIEW_EVENT:
   * It is the default attendee response and an all day event indicator.
   * Set to Attendees.ATTENDEE_STATUS_NONE, Attendees.ATTENDEE_STATUS_ACCEPTED,
   * Attendees.ATTENDEE_STATUS_DECLINED, or Attendees.ATTENDEE_STATUS_TENTATIVE.
   * To signal the event is an all-day event, "or" ALL_DAY_MASK with the response.
   * Alternatively, use buildViewExtraLong(), getResponse(), and isAllDay().
   * <p/>
   * For EventType.CREATE_EVENT:
   * Set to {@link #EXTRA_CREATE_ALL_DAY} for creating an all-day event.
   * <p/>
   * For EventType.GO_TO:
   * Set to {@link #EXTRA_GOTO_TIME} to go to the specified date/time.
   * Set to {@link #EXTRA_GOTO_DATE} to consider the date but ignore the time.
   * Set to {@link #EXTRA_GOTO_BACK_TO_PREVIOUS} if back should bring back previous view.
   * Set to {@link #EXTRA_GOTO_TODAY} if this is a user request to go to the current time.
   * <p/>
   * For EventType.UPDATE_TITLE:
   * Set formatting flags for Utils.formatDateRange
   */
  public long extraLong;

  // Used to build the extra long for a VIEW event.
  public static long buildViewExtraLong(int response, boolean allDay) {
    long extra = allDay ? ALL_DAY_MASK : 0;

    switch (response) {
      case CalendarContract.Attendees.ATTENDEE_STATUS_NONE:
        extra |= ATTENDEE_STATUS_NONE_MASK;
        break;
      case CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED:
        extra |= ATTENDEE_STATUS_ACCEPTED_MASK;
        break;
      case CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED:
        extra |= ATTENDEE_STATUS_DECLINED_MASK;
        break;
      case CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE:
        extra |= ATTENDEE_STATUS_TENTATIVE_MASK;
        break;
      default:
        Log.e(TAG, "Unknown attendee response " + response);
        extra |= ATTENDEE_STATUS_NONE_MASK;
        break;
    }
    return extra;
  }
}

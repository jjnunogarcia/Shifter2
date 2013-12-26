package es.android.TurnosAndroid.views.day;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.CalendarContract.Attendees;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import es.android.TurnosAndroid.EventGeometry;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.controllers.CalendarController;
import es.android.TurnosAndroid.helpers.Utils;
import es.android.TurnosAndroid.model.CalendarData;
import es.android.TurnosAndroid.model.Event;
import es.android.TurnosAndroid.model.EventType;
import es.android.TurnosAndroid.requests.EventLoader;
import es.android.TurnosAndroid.views.ViewType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * View for multi-day view. So far only 1 and 7 day have been tested.
 */
public class DayView extends View implements View.OnCreateContextMenuListener, ScaleGestureDetector.OnScaleGestureListener, View.OnClickListener, View.OnLongClickListener {
  public static final  int     MINUTES_PER_HOUR             = 60;
  public static final  int     MINUTES_PER_DAY              = MINUTES_PER_HOUR * 24;
  public static final  int     MILLIS_PER_HOUR              = (3600 * 1000);
  private static final long    INVALID_EVENT_ID             = -1; //This is used for remembering a null event
  private static final long    ANIMATION_DURATION           = 400; // Duration of the allday expansion
  private static final long    ANIMATION_SECONDARY_DURATION = 200; // duration of the more allday event text fade
  private static final int     GOTO_SCROLL_DURATION         = 200; // duration of the scroll to go to a specified time
  private static final int     EVENTS_CROSS_FADE_DURATION   = 400; // duration for events' cross-fade animation
  private static final int     CLICK_DISPLAY_DURATION       = 50; // duration to show the event clicked
  private static final int     MENU_AGENDA                  = 2;
  private static final int     MENU_DAY                     = 3;
  private static final int     MENU_EVENT_VIEW              = 5;
  private static final int     MENU_EVENT_CREATE            = 6;
  private static final int     MENU_EVENT_EDIT              = 7;
  private static final int     MENU_EVENT_DELETE            = 8;
  private static final int     FROM_NONE                    = 0;
  private static final int     FROM_ABOVE                   = 1;
  private static final int     FROM_BELOW                   = 2;
  private static final int     FROM_LEFT                    = 4;
  private static final int     FROM_RIGHT                   = 8;
  private static final int     UPDATE_CURRENT_TIME_DELAY    = 300000; // Update the current time line every five minutes if the window is left open that long
  private static final int     POPUP_DISMISS_DELAY          = 3000; // The number of milliseconds to show the popup window
  private static final float   GRID_LINE_INNER_WIDTH        = 1;
  private static final int     DAY_GAP                      = 1;
  private static final int     HOUR_GAP                     = 1;
  private static final int     MORE_EVENTS_MAX_ALPHA        = 0x4C; // More events text will transition between invisible and this alpha
  private static final int     MAX_EVENT_TEXT_LEN           = 500;
  private static final int     TOUCH_MODE_INITIAL_STATE     = 0; // The initial state of the touch mode when we enter this view.
  private static final int     TOUCH_MODE_DOWN              = 1; // Indicates we just received the touch event and we are waiting to see if it is a tap or a scroll gesture.
  private static final int     TOUCH_MODE_VSCROLL           = 0x20; // Indicates the touch gesture is a vertical scroll
  private static final int     TOUCH_MODE_HSCROLL           = 0x40; // Indicates the touch gesture is a horizontal scroll
  private static final int     SELECTION_HIDDEN             = 0; // The selection modes are HIDDEN, PRESSED, SELECTED, and LONGPRESS.
  private static final int     SELECTION_PRESSED            = 1; // D-pad down but not up yet
  private static final int     SELECTION_SELECTED           = 2;
  private static final int     SELECTION_LONGPRESS          = 3;
  private static final int     MINIMUM_SNAP_VELOCITY        = 2200;
  private static       String  TAG                          = DayView.class.getSimpleName();
  private static       boolean DEBUG                        = false;
  private static       boolean DEBUG_SCALING                = false;
  private static       int     DEFAULT_CELL_HEIGHT          = 64;
  private static       int     MAX_CELL_HEIGHT              = 150;
  private static       int     MIN_Y_SPAN                   = 100;
  private static       int     horizontalSnapBackThreshold  = 128;
  private static int onDownDelay;
  private static float GRID_LINE_LEFT_MARGIN              = 0;
  private static int   SINGLE_ALLDAY_HEIGHT               = 34; // This is the standard height of an allday event with no restrictions
  /**
   * This is the minimum desired height of a allday event. When unexpanded, allday events will use this height.
   * When expanded allDay events will attempt to grow to fit all events at this height.
   */
  private static float MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT = 28.0F; // in pixels
  /**
   * This is how big the unexpanded allday height is allowed to be. It will get adjusted based on screen size
   */
  private static int   MAX_UNEXPANDED_ALLDAY_HEIGHT       = (int) (MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT * 4);
  /**
   * This is the minimum size reserved for displaying regular events. The expanded allDay region can't expand into this.
   */
  private static int   MIN_HOURS_HEIGHT                   = 180;
  private static int   ALLDAY_TOP_MARGIN                  = 1;
  private static int   MAX_HEIGHT_OF_ONE_ALLDAY_EVENT     = 34; // The largest a single allDay event will become.
  private static int   HOURS_TOP_MARGIN                   = 2;
  private static int   HOURS_LEFT_MARGIN                  = 2;
  private static int   HOURS_RIGHT_MARGIN                 = 4;
  private static int   HOURS_MARGIN                       = HOURS_LEFT_MARGIN + HOURS_RIGHT_MARGIN;
  private static int   NEW_EVENT_MARGIN                   = 4;
  private static int   NEW_EVENT_WIDTH                    = 2;
  private static int   NEW_EVENT_MAX_LENGTH               = 16;
  private static int   CURRENT_TIME_LINE_SIDE_BUFFER      = 4;
  private static int   CURRENT_TIME_LINE_TOP_OFFSET       = 2;
  private static int   DAY_HEADER_ONE_DAY_LEFT_MARGIN     = 0;
  private static int   DAY_HEADER_ONE_DAY_RIGHT_MARGIN    = 5;
  private static int   DAY_HEADER_ONE_DAY_BOTTOM_MARGIN   = 6;
  private static int   DAY_HEADER_RIGHT_MARGIN            = 4;
  private static int   DAY_HEADER_BOTTOM_MARGIN           = 3;
  private static float DAY_HEADER_FONT_SIZE               = 14;
  private static float DATE_HEADER_FONT_SIZE              = 32;
  private static float NORMAL_FONT_SIZE                   = 12;
  private static float EVENT_TEXT_FONT_SIZE               = 12;
  private static float HOURS_TEXT_SIZE                    = 12;
  private static float AMPM_TEXT_SIZE                     = 9;
  private static int   MIN_HOURS_WIDTH                    = 96;
  private static int   MIN_CELL_WIDTH_FOR_TEXT            = 20;
  private static float MIN_EVENT_HEIGHT                   = 24.0F; // smallest height to draw an event with (in pixels)
  //  private static int   CALENDAR_COLOR_SQUARE_SIZE         = 10;
  private static int   EVENT_RECT_TOP_MARGIN              = 1;
  private static int   EVENT_RECT_BOTTOM_MARGIN           = 0;
  private static int   EVENT_RECT_LEFT_MARGIN             = 1;
  private static int   EVENT_RECT_RIGHT_MARGIN            = 0;
  private static int   EVENT_RECT_STROKE_WIDTH            = 2;
  private static int   EVENT_TEXT_TOP_MARGIN              = 2;
  private static int   EVENT_TEXT_BOTTOM_MARGIN           = 2;
  private static int   EVENT_TEXT_LEFT_MARGIN             = 6;
  private static int   EVENT_TEXT_RIGHT_MARGIN            = 6;
  private static int   ALL_DAY_EVENT_RECT_BOTTOM_MARGIN   = 1;
  private static int   EVENT_ALL_DAY_TEXT_TOP_MARGIN      = EVENT_TEXT_TOP_MARGIN;
  private static int   EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN   = EVENT_TEXT_BOTTOM_MARGIN;
  private static int   EVENT_ALL_DAY_TEXT_LEFT_MARGIN     = EVENT_TEXT_LEFT_MARGIN;
  private static int   EVENT_ALL_DAY_TEXT_RIGHT_MARGIN    = EVENT_TEXT_RIGHT_MARGIN;
  private static int   EXPAND_ALL_DAY_BOTTOM_MARGIN       = 10; // margins and sizing for the expand allday icon
  private static int   EVENT_SQUARE_WIDTH                 = 10; // sizing for "box +n" in allDay events
  private static int   EVENT_LINE_PADDING                 = 4;
  private static int   NEW_EVENT_HINT_FONT_SIZE           = 12;
  private static int pressedColor;
  private static int clickedColor;
  private static int eventTextColor;
  private static int moreEventsTextColor;
  private static int weekSaturdayColor;
  private static int weekSundayColor;
  private static int calendarDateBannerTextColor;
  private static int calendarAmPmLabel;
  private static int calendarGridAreaSelected;
  private static int calendarGridLineInnerHorizontalColor;
  private static int calendarGridLineInnerVerticalColor;
  private static int futureBgColor;
  private static int futureBgColorRes;
  private static int bgColor;
  private static int newEventHintColor;
  private static int calendarHourLabelColor;
  private static int     moreAlldayEventsTextAlpha = MORE_EVENTS_MAX_ALPHA;
  private static int     cellHeight                = 0; // shared among all DayViews
  private static int     minCellHeight             = 32;
  private static int     scaledPagingTouchSlop     = 0;
  /**
   * Whether to use the expand or collapse icon.
   */
  private static boolean useExpandIcon             = true;
  /**
   * The height of the day names/numbers
   */
  private static int     DAY_HEADER_HEIGHT         = 45;
  /**
   * The height of the day names/numbers for multi-day views
   */
  private static int     MULTI_DAY_HEADER_HEIGHT   = DAY_HEADER_HEIGHT;
  /**
   * The height of the day names/numbers when viewing a single day
   */
  private static int     ONE_DAY_HEADER_HEIGHT     = DAY_HEADER_HEIGHT;
  /**
   * Whether or not to expand the allDay area to fill the screen
   */
  private static boolean showAllAllDayEvents       = false;
  //    private final DeleteEventHelper mDeleteEventHelper;
  private static int     counter                   = 0;
  private final EventGeometry eventGeometry;
  private final Resources     resources;
  private final Drawable      currentTimeLine;
  private final Drawable      currentTimeAnimateLine;
  private final Drawable      expandAlldayDrawable;
  private final Drawable      collapseAlldayDrawable;
  private final ContinueScroll    continueScroll    = new ContinueScroll();
  private final UpdateCurrentTime updateCurrentTime = new UpdateCurrentTime();
  private final Typeface          bold              = Typeface.DEFAULT_BOLD;
  private final CharSequence[] longPressItems;
  private final Runnable              timeZoneUpdater       = new Runnable() {
    @Override
    public void run() {
      String tz = Utils.getTimeZone(context, this);
      baseDate.timezone = tz;
      baseDate.normalize(true);
      currentTime.switchTimezone(tz);
      invalidate();
    }
  };
  // Sets the "clicked" color from the clicked event
  private final Runnable              setClick              = new Runnable() {
    @Override
    public void run() {
      clickedEvent = savedClickedEvent;
      savedClickedEvent = null;
      DayView.this.invalidate();
    }
  };
  // Clears the "clicked" color from the clicked event and launch the event
  private final Runnable              clearClick            = new Runnable() {
    @Override
    public void run() {
      if (clickedEvent != null) {
//        calendarController.sendEventRelatedEvent(EventType.VIEW_EVENT, clickedEvent.id, clickedEvent.startMillis, clickedEvent.endMillis,
//                                                 DayView.this.getWidth() / 2, clickedYLocation, getSelectedTimeInMillis());
      }
      clickedEvent = null;
      DayView.this.invalidate();
    }
  };
  private final TodayAnimatorListener todayAnimatorListener = new TodayAnimatorListener();
  private final Rect                  rect                  = new Rect(); // Pre-allocate these objects and re-use them
  private final Rect                  destRect              = new Rect();
  private final Rect                  selectionRect         = new Rect();
  private final Rect                  expandAllDayRect      = new Rect(); // This encloses the more allDay events icon
  // TODO Clean up paint usage
  private final Paint                 paint                 = new Paint();
  private final Paint                 eventTextPaint        = new Paint();
  private final Paint                 selectionPaint        = new Paint();
  private final DismissPopup          dismissPopup          = new DismissPopup();
  private final EventLoader eventLoader;
  private final ArrayList<Event> selectedEvents = new ArrayList<Event>();
  private final Rect             prevBox        = new Rect();
  private final CalendarController calendarController;
  private final ViewSwitcher       viewSwitcher;
  private final GestureDetector    gestureDetector;
  private final OverScroller       scroller;
  private final EdgeEffect         edgeEffectTop;
  private final EdgeEffect         edgeEffectBottom;
  private final int                overflingDistance;
  private final ScrollInterpolator hScrollInterpolator;
  private final String             newEventHintString;
  private final Runnable cancelCallback          = new Runnable() {
    @Override
    public void run() {
      clearCachedEvents();
    }
  };
  private final Pattern  drawTextSanitizerFilter = Pattern.compile("[\t\n],");
  private boolean              selectionAllday;
  private ScaleGestureDetector scaleGestureDetector;
  private ObjectAnimator       alldayAnimator; // Animates the height of the allday region
  private ObjectAnimator       alldayEventAnimator; // Animates the height of events in the allday region
  private ObjectAnimator       moreAlldayEventsAnimator; // Animates the transparency of the more events text
  private ObjectAnimator       todayAnimator; // Animates the current time marker when Today is pressed
  private boolean paused = true;
  private Context context;
  private int numDays = 7;
  private Time baseDate;
  private AnimatorListenerAdapter animatorListener = new AnimatorListenerAdapter() {
    @Override
    public void onAnimationStart(Animator animation) {
      scrolling = true;
    }

    @Override
    public void onAnimationCancel(Animator animation) {
      scrolling = false;
    }

    @Override
    public void onAnimationEnd(Animator animation) {
      scrolling = false;
      resetSelectedHour();
      invalidate();
    }
  };
  private boolean onFlingCalled;
  private boolean startingScroll = false;
  private Handler handler;
  /**
   * ID of the last event which was displayed with the toast popup.
   * <p/>
   * This is used to prevent popping up multiple quick views for the same event, especially
   * during calendar syncs. This becomes valid when an event is selected, either by default
   * on starting calendar or by scrolling to an event. It becomes invalid when the user
   * explicitly scrolls to an empty time slot, changes views, or deletes the event.
   */
  private long    lastPopupEventId;
  private Time    currentTime;
  private int     todayJulianDay;
  private int     firstJulianDay;
  private int loadedFirstJulianDay = -1;
  private int       lastJulianDay;
  private int       monthLength;
  private int       firstVisibleDate;
  private int       firstVisibleDayOfWeek;
  private int[]     earliestStartHour;    // indexed by the week day offset
  private boolean[] hasAllDayEvent;   // indexed by the week day offset
  private String    longPressTitle;
  private Event     clickedEvent;           // The event the user clicked on
  private Event     savedClickedEvent;
  private int       clickedYLocation;
  private long      downTouchTime;
  private int eventsAlpha = 255;
  private ObjectAnimator eventsCrossFadeAnimation;
  /**
   * This variable helps to avoid unnecessarily reloading events by keeping track of the start millis parameter used for the most recent loading
   * of events.  If the next reload matches this, then the events are not reloaded.  To force a reload, set this to zero (this is set to zero
   * in the method clearCachedEvents()).
   */
  private long           lastReloadMillis;
  private ArrayList<Event> events        = new ArrayList<Event>();
  private ArrayList<Event> allDayEvents  = new ArrayList<Event>();
  private StaticLayout[]   layouts       = null;
  private StaticLayout[]   allDayLayouts = null;
  private int         selectionDay;        // Julian day
  private int         selectionHour;
  private int         cellWidth; // Width of a day or non-conflicting event
  private float[]     lines;
  private int         firstDayOfWeek;
  private PopupWindow popup;
  private View        popupView;
  private boolean remeasure         = true;
  private float   animationDistance = 0;
  private int viewStartX;
  private int viewStartY;
  private int maxViewStartY;
  private int viewHeight;
  private int viewWidth;
  private int gridAreaHeight = -1;
  private int scrollStartY;
  private int previousDirection;
  /**
   * Vertical distance or span between the two touch points at the start of a scaling gesture
   */
  private float startingSpanY = 0;
  /**
   * Height of 1 hour in pixels at the start of a scaling gesture
   */
  private int cellHeightBeforeScaleGesture;
  /**
   * The hour at the center two touch points
   */
  private float   gestureCenterHour = 0;
  private boolean recalCenterHour   = false;
  /**
   * Flag to decide whether to handle the up event. Cases where up events should be ignored are 1) right after a scale gesture and 2) finger was
   * down before app launch
   */
  private boolean handleActionUp    = true;
  private int hoursTextHeight;
  /**
   * The height of the area used for allday events
   */
  private int allDayHeight;
  /**
   * The height of the allday event area used during animation
   */
  private int animateDayHeight      = 0;
  private int animateDayEventHeight = (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT; // The height of an individual allday event during animation
  private int      maxAlldayEvents; // Max of all day events in a given day in this view.
  private int[]    skippedAllDayEvents; // A count of the number of allday events that were not drawn for each day
  private int      maxUnexpandedAllDayEventCount; // The number of allDay events at which point we start hiding allDay events.
  private int      numHours;
  private int      hoursWidth; // Width of the time line (list of hours) to the left.
  private int      dateStrWidth;
  private int      firstCell; // Top of the scrollable region i.e. below date labels and all day events
  private int      firstHour; // First fully visibile hour
  private int      firstHourOffset; // Distance between the firstCell and the top of first fully visible hour.
  private String[] hourStrs;
  private String[] dayStrs;
  private String[] dayStrs2Letter;
  private boolean  is24HourFormat;
  private boolean  computeSelectedEvents;
  private boolean  updateToast;
  private Event    selectedEvent;
  private Event    prevSelectedEvent;
  private String   amString;
  private String   pmString;
  private int      touchMode;
  private int      selectionMode;
  private boolean  scrolling;
  // Pixels scrolled
  private float    initialScrollX;
  private float    initialScrollY;
  private boolean  animateToday;
  private int      animateTodayAlpha;
  // whether or not an event is stopping because it was cancelled
  private boolean  cancellingAnimations;
  // tracks whether a touch originated in the allday area
  private boolean  touchStartedInAllDayArea;
  private boolean  callEdgeEffectOnAbsorb;
  private float    lastVelocity;
  private boolean  touchExplorationEnabled;

  public DayView(Context context, CalendarController controller, ViewSwitcher viewSwitcher, EventLoader eventLoader, int numDays) {
    super(context);
    this.context = context;
    this.numDays = numDays;

    maxUnexpandedAllDayEventCount = 4;
    numHours = 10;
    firstHour = -1;
    touchMode = TOUCH_MODE_INITIAL_STATE;
    selectionMode = SELECTION_HIDDEN;
    scrolling = false;
    animateToday = false;
    animateTodayAlpha = 0;
    cancellingAnimations = false;
    touchStartedInAllDayArea = false;
    touchExplorationEnabled = false;
    resources = context.getResources();
    newEventHintString = resources.getString(R.string.day_view_new_event_hint);

    DATE_HEADER_FONT_SIZE = (int) resources.getDimension(R.dimen.date_header_text_size);
    DAY_HEADER_FONT_SIZE = (int) resources.getDimension(R.dimen.day_label_text_size);
    ONE_DAY_HEADER_HEIGHT = (int) resources.getDimension(R.dimen.one_day_header_height);
    DAY_HEADER_BOTTOM_MARGIN = (int) resources.getDimension(R.dimen.day_header_bottom_margin);
    EXPAND_ALL_DAY_BOTTOM_MARGIN = (int) resources.getDimension(R.dimen.all_day_bottom_margin);
    HOURS_TEXT_SIZE = (int) resources.getDimension(R.dimen.hours_text_size);
    AMPM_TEXT_SIZE = (int) resources.getDimension(R.dimen.ampm_text_size);
    MIN_HOURS_WIDTH = (int) resources.getDimension(R.dimen.min_hours_width);
    HOURS_LEFT_MARGIN = (int) resources.getDimension(R.dimen.hours_left_margin);
    HOURS_RIGHT_MARGIN = (int) resources.getDimension(R.dimen.hours_right_margin);
    MULTI_DAY_HEADER_HEIGHT = (int) resources.getDimension(R.dimen.day_header_height);
    int eventTextSizeId;
    if (this.numDays == 1) {
      eventTextSizeId = R.dimen.day_view_event_text_size;
    } else {
      eventTextSizeId = R.dimen.week_view_event_text_size;
    }
    EVENT_TEXT_FONT_SIZE = (int) resources.getDimension(eventTextSizeId);
    NEW_EVENT_HINT_FONT_SIZE = (int) resources.getDimension(R.dimen.new_event_hint_text_size);
    MIN_EVENT_HEIGHT = resources.getDimension(R.dimen.event_min_height);
    MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT = MIN_EVENT_HEIGHT;
    EVENT_TEXT_TOP_MARGIN = (int) resources.getDimension(R.dimen.event_text_vertical_margin);
    EVENT_TEXT_BOTTOM_MARGIN = EVENT_TEXT_TOP_MARGIN;
    EVENT_ALL_DAY_TEXT_TOP_MARGIN = EVENT_TEXT_TOP_MARGIN;
    EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN = EVENT_TEXT_TOP_MARGIN;

    EVENT_TEXT_LEFT_MARGIN = (int) resources.getDimension(R.dimen.event_text_horizontal_margin);
    EVENT_TEXT_RIGHT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
    EVENT_ALL_DAY_TEXT_LEFT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
    EVENT_ALL_DAY_TEXT_RIGHT_MARGIN = EVENT_TEXT_LEFT_MARGIN;

    float scale = resources.getDisplayMetrics().density;
    if (scale != 1) {
      SINGLE_ALLDAY_HEIGHT *= scale;
      ALLDAY_TOP_MARGIN *= scale;
      MAX_HEIGHT_OF_ONE_ALLDAY_EVENT *= scale;

      NORMAL_FONT_SIZE *= scale;
      GRID_LINE_LEFT_MARGIN *= scale;
      HOURS_TOP_MARGIN *= scale;
      MIN_CELL_WIDTH_FOR_TEXT *= scale;
      MAX_UNEXPANDED_ALLDAY_HEIGHT *= scale;
      animateDayEventHeight = (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;

      CURRENT_TIME_LINE_SIDE_BUFFER *= scale;
      CURRENT_TIME_LINE_TOP_OFFSET *= scale;

      MIN_Y_SPAN *= scale;
      MAX_CELL_HEIGHT *= scale;
      DEFAULT_CELL_HEIGHT *= scale;
      DAY_HEADER_HEIGHT *= scale;
      DAY_HEADER_RIGHT_MARGIN *= scale;
      DAY_HEADER_ONE_DAY_LEFT_MARGIN *= scale;
      DAY_HEADER_ONE_DAY_RIGHT_MARGIN *= scale;
      DAY_HEADER_ONE_DAY_BOTTOM_MARGIN *= scale;
      EVENT_RECT_TOP_MARGIN *= scale;
      EVENT_RECT_BOTTOM_MARGIN *= scale;
      ALL_DAY_EVENT_RECT_BOTTOM_MARGIN *= scale;
      EVENT_RECT_LEFT_MARGIN *= scale;
      EVENT_RECT_RIGHT_MARGIN *= scale;
      EVENT_RECT_STROKE_WIDTH *= scale;
      EVENT_SQUARE_WIDTH *= scale;
      EVENT_LINE_PADDING *= scale;
      NEW_EVENT_MARGIN *= scale;
      NEW_EVENT_WIDTH *= scale;
      NEW_EVENT_MAX_LENGTH *= scale;
    }
    HOURS_MARGIN = HOURS_LEFT_MARGIN + HOURS_RIGHT_MARGIN;
    DAY_HEADER_HEIGHT = this.numDays == 1 ? ONE_DAY_HEADER_HEIGHT : MULTI_DAY_HEADER_HEIGHT;

    currentTimeLine = resources.getDrawable(R.drawable.timeline_indicator_holo_light);
    currentTimeAnimateLine = resources.getDrawable(R.drawable.timeline_indicator_activated_holo_light);
    expandAlldayDrawable = resources.getDrawable(R.drawable.ic_expand_holo_light);
    collapseAlldayDrawable = resources.getDrawable(R.drawable.ic_collapse_holo_light);
    newEventHintColor = resources.getColor(R.color.new_event_hint_text_color);

    this.eventLoader = eventLoader;
    eventGeometry = new EventGeometry();
    eventGeometry.setMinEventHeight(MIN_EVENT_HEIGHT);
    eventGeometry.setHourGap(HOUR_GAP);
    eventGeometry.setCellMargin(DAY_GAP);
    longPressItems = new CharSequence[]{
        resources.getString(R.string.new_event_dialog_option)
    };
    longPressTitle = resources.getString(R.string.new_event_dialog_label);
//        mDeleteEventHelper = new DeleteEventHelper(context, null, false /* don't exit when done */);
    lastPopupEventId = INVALID_EVENT_ID;
    calendarController = controller;
    this.viewSwitcher = viewSwitcher;
    gestureDetector = new GestureDetector(context, new CalendarGestureListener());
    scaleGestureDetector = new ScaleGestureDetector(getContext(), this);
    if (cellHeight == 0) {
//            cellHeight = Utils.getSharedPreference(context,
//                    GeneralPreferences.KEY_DEFAULT_CELL_HEIGHT, DEFAULT_CELL_HEIGHT);
      cellHeight = DEFAULT_CELL_HEIGHT;
    }
    scroller = new OverScroller(context);
    hScrollInterpolator = new ScrollInterpolator();
    edgeEffectTop = new EdgeEffect(context);
    edgeEffectBottom = new EdgeEffect(context);
    ViewConfiguration vc = ViewConfiguration.get(context);
    scaledPagingTouchSlop = vc.getScaledPagingTouchSlop();
    onDownDelay = ViewConfiguration.getTapTimeout();
    overflingDistance = vc.getScaledOverflingDistance();

    init(context);
  }

  static Event getNewEvent(int julianDay, long utcMillis, int minutesSinceMidnight) {
    Event event = new Event();
    event.setStartDay(julianDay);
    event.setEndDay(julianDay);
//    event.startMillis = utcMillis;
//    event.endMillis = event.startMillis + MILLIS_PER_HOUR;
    event.setStartTime(minutesSinceMidnight);
//    event.endTime = event.startTime + MINUTES_PER_HOUR;
    return event;
  }

  @Override
  protected void onAttachedToWindow() {
    if (handler == null) {
      handler = getHandler();
      handler.post(updateCurrentTime);
    }
  }

  private void init(Context context) {
    setFocusable(true);

    // Allow focus in touch mode so that we can do keyboard shortcuts
    // even after we've entered touch mode.
    setFocusableInTouchMode(true);
    setClickable(true);
    setOnCreateContextMenuListener(this);

    firstDayOfWeek = Utils.getFirstDayOfWeek(context);

    currentTime = new Time(Utils.getTimeZone(context, timeZoneUpdater));
    long currentTime = System.currentTimeMillis();
    this.currentTime.set(currentTime);
    todayJulianDay = Time.getJulianDay(currentTime, this.currentTime.gmtoff);

    weekSaturdayColor = resources.getColor(R.color.week_saturday);
    weekSundayColor = resources.getColor(R.color.week_sunday);
    calendarDateBannerTextColor = resources.getColor(R.color.calendar_date_banner_text_color);
    futureBgColorRes = resources.getColor(R.color.calendar_future_bg_color);
    bgColor = resources.getColor(R.color.calendar_hour_background);
    calendarAmPmLabel = resources.getColor(R.color.calendar_ampm_label);
    calendarGridAreaSelected = resources.getColor(R.color.calendar_grid_area_selected);
    calendarGridLineInnerHorizontalColor = resources.getColor(R.color.calendar_grid_line_inner_horizontal_color);
    calendarGridLineInnerVerticalColor = resources.getColor(R.color.calendar_grid_line_inner_vertical_color);
    calendarHourLabelColor = resources.getColor(R.color.calendar_hour_label);
    pressedColor = resources.getColor(R.color.pressed);
    clickedColor = resources.getColor(R.color.day_event_clicked_background_color);
    eventTextColor = resources.getColor(R.color.calendar_event_text_color);
    moreEventsTextColor = resources.getColor(R.color.month_event_other_color);

    eventTextPaint.setTextSize(EVENT_TEXT_FONT_SIZE);
    eventTextPaint.setTextAlign(Paint.Align.LEFT);
    eventTextPaint.setAntiAlias(true);

    int gridLineColor = resources.getColor(R.color.calendar_grid_line_highlight_color);
    Paint p = selectionPaint;
    p.setColor(gridLineColor);
    p.setStyle(Style.FILL);
    p.setAntiAlias(false);

    p = paint;
    p.setAntiAlias(true);

    // Allocate space for 2 weeks worth of weekday names so that we can
    // easily start the week display at any week day.
    dayStrs = new String[14];

    // Also create an array of 2-letter abbreviations.
    dayStrs2Letter = new String[14];

    for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
      int index = i - Calendar.SUNDAY;
      // e.g. Tue for Tuesday
      dayStrs[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_MEDIUM).toUpperCase();
      dayStrs[index + 7] = dayStrs[index];
      // e.g. Tu for Tuesday
      dayStrs2Letter[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_SHORT).toUpperCase();

      // If we don't have 2-letter day strings, fall back to 1-letter.
      if (dayStrs2Letter[index].equals(dayStrs[index])) {
        dayStrs2Letter[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_SHORTEST);
      }

      dayStrs2Letter[index + 7] = dayStrs2Letter[index];
    }

    // Figure out how much space we need for the 3-letter abbrev names
    // in the worst case.
    p.setTextSize(DATE_HEADER_FONT_SIZE);
    p.setTypeface(bold);
    String[] dateStrs = {" 28", " 30"};
    dateStrWidth = computeMaxStringWidth(0, dateStrs, p);
    p.setTextSize(DAY_HEADER_FONT_SIZE);
    dateStrWidth += computeMaxStringWidth(0, dayStrs, p);

    p.setTextSize(HOURS_TEXT_SIZE);
    p.setTypeface(null);
    handleOnResume();

    amString = DateUtils.getAMPMString(Calendar.AM).toUpperCase();
    pmString = DateUtils.getAMPMString(Calendar.PM).toUpperCase();
    String[] ampm = {amString, pmString};
    p.setTextSize(AMPM_TEXT_SIZE);
    hoursWidth = Math.max(HOURS_MARGIN, computeMaxStringWidth(hoursWidth, ampm, p) + HOURS_RIGHT_MARGIN);
    hoursWidth = Math.max(MIN_HOURS_WIDTH, hoursWidth);

    LayoutInflater inflater;
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    popupView = inflater.inflate(R.layout.bubble_event, null);
    popupView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    popup = new PopupWindow(context);
    popup.setContentView(popupView);
    Resources.Theme dialogTheme = getResources().newTheme();
    dialogTheme.applyStyle(android.R.style.Theme_Dialog, true);
    TypedArray ta = dialogTheme.obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
    popup.setBackgroundDrawable(ta.getDrawable(0));
    ta.recycle();

    // Enable touching the popup window
    popupView.setOnClickListener(this);
    // Catch long clicks for creating a new event
    setOnLongClickListener(this);

    baseDate = new Time(Utils.getTimeZone(context, timeZoneUpdater));
    long millis = System.currentTimeMillis();
    baseDate.set(millis);
    if (numDays == 0) {
      numDays = 1;
    }
    earliestStartHour = new int[numDays];
    hasAllDayEvent = new boolean[numDays];

    // lines is the array of points used with Canvas.drawLines() in drawGridBackground() and drawAllDayEvents().  Its size depends
    // on the max number of lines that can ever be drawn by any single drawLines() call in either of those methods.
    final int maxGridLines = (24 + 1)  // max horizontal lines we might draw
                             + (numDays + 1); // max vertical lines we might draw
    lines = new float[maxGridLines * 4];
  }

  /**
   * This is called when the popup window is pressed.
   */
  @Override
  public void onClick(View v) {
    if (v == popupView) {
      // Pretend it was a trackball click because that will always
      // jump to the "View event" screen.
      switchViews(true /* trackball */);
    }
  }

  public void handleOnResume() {
//        if(Utils.getSharedPreference(context, OtherPreferences.KEY_OTHER_1, false)) {
//            futureBgColor = 0;
//        } else {
    futureBgColor = futureBgColorRes;
//        }
    is24HourFormat = DateFormat.is24HourFormat(context);
    hourStrs = is24HourFormat ? CalendarData.s24Hours : CalendarData.s12HoursNoAmPm;
    firstDayOfWeek = Utils.getFirstDayOfWeek(context);
    selectionMode = SELECTION_HIDDEN;
  }

  /**
   * Returns the start of the selected time in milliseconds since the epoch.
   *
   * @return selected time in UTC milliseconds since the epoch.
   */
  public long getSelectedTimeInMillis() {
    Time time = new Time(baseDate);
    time.setJulianDay(selectionDay);
    time.hour = selectionHour;

    // We ignore the "isDst" field because we want normalize() to figure
    // out the correct DST value and not adjust the selected time based
    // on the current setting of DST.
    return time.normalize(true /* ignore isDst */);
  }

  private Time getSelectedTime() {
    Time time = new Time(baseDate);
    time.setJulianDay(selectionDay);
    time.hour = selectionHour;

    // We ignore the "isDst" field because we want normalize() to figure
    // out the correct DST value and not adjust the selected time based
    // on the current setting of DST.
    time.normalize(true /* ignore isDst */);
    return time;
  }

  /**
   * Returns the start of the selected time in minutes since midnight,
   * local time.  The derived class must ensure that this is consistent
   * with the return value from getSelectedTimeInMillis().
   */
  int getSelectedMinutesSinceMidnight() {
    return selectionHour * MINUTES_PER_HOUR;
  }

  public int getFirstVisibleHour() {
    return firstHour;
  }

  public void setFirstVisibleHour(int firstHour) {
    this.firstHour = firstHour;
    firstHourOffset = 0;
  }

  public void setSelected(Time time, boolean ignoreTime, boolean animateToday) {
    baseDate.set(time);
    setSelectedHour(baseDate.hour);
    setSelectedEvent(null);
    prevSelectedEvent = null;
    long millis = baseDate.toMillis(false /* use isDst */);
    setSelectedDay(Time.getJulianDay(millis, baseDate.gmtoff));
    selectedEvents.clear();
    computeSelectedEvents = true;

    int gotoY = Integer.MIN_VALUE;

    if (!ignoreTime && gridAreaHeight != -1) {
      int lastHour = 0;

      if (baseDate.hour < firstHour) {
        // Above visible region
        gotoY = baseDate.hour * (cellHeight + HOUR_GAP);
      } else {
        lastHour = (gridAreaHeight - firstHourOffset) / (cellHeight + HOUR_GAP) + firstHour;

        if (baseDate.hour >= lastHour) {
          // Below visible region

          // target hour + 1 (to give it room to see the event) - grid height (to get the y of the top of the visible region)
          gotoY = (int) ((baseDate.hour + 1 + baseDate.minute / 60.0f) * (cellHeight + HOUR_GAP) - gridAreaHeight);
        }
      }

      if (gotoY > maxViewStartY) {
        gotoY = maxViewStartY;
      } else if (gotoY < 0 && gotoY != Integer.MIN_VALUE) {
        gotoY = 0;
      }
    }

    recalc();

    remeasure = true;
    invalidate();

    boolean delayAnimateToday = false;
    if (gotoY != Integer.MIN_VALUE) {
      ValueAnimator scrollAnim = ObjectAnimator.ofInt(this, "viewStartY", viewStartY, gotoY);
      scrollAnim.setDuration(GOTO_SCROLL_DURATION);
      scrollAnim.setInterpolator(new AccelerateDecelerateInterpolator());
      scrollAnim.addListener(animatorListener);
      scrollAnim.start();
      delayAnimateToday = true;
    }
    if (animateToday) {
      synchronized (todayAnimatorListener) {
        if (todayAnimator != null) {
          todayAnimator.removeAllListeners();
          todayAnimator.cancel();
        }
        todayAnimator = ObjectAnimator.ofInt(this, "animateTodayAlpha", animateTodayAlpha, 255);
        this.animateToday = true;
        todayAnimatorListener.setFadingIn(true);
        todayAnimatorListener.setAnimator(todayAnimator);
        todayAnimator.addListener(todayAnimatorListener);
        todayAnimator.setDuration(150);
        if (delayAnimateToday) {
          todayAnimator.setStartDelay(GOTO_SCROLL_DURATION);
        }
        todayAnimator.start();
      }
    }
  }

  private void setSelectedDay(long d) {
    selectionDay = (int) d;
  }

  public void updateTitle() {
    Time start = new Time(baseDate);
    start.normalize(true);
    Time end = new Time(start);
    end.monthDay += numDays - 1;
    // Move it forward one minute so the formatter doesn't lose a day
    end.minute += 1;
    end.normalize(true);

    long formatFlags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
    if (numDays != 1) {
      // Don't show day of the month if for multi-day view
      formatFlags |= DateUtils.FORMAT_NO_MONTH_DAY;

      // Abbreviate the month if showing multiple months
      if (start.month != end.month) {
        formatFlags |= DateUtils.FORMAT_ABBREV_MONTH;
      }
    }

    calendarController.sendEvent(EventType.UPDATE_TITLE, start, end, null, -1, ViewType.CURRENT, formatFlags, null, null);
  }

  /**
   * return a negative number if "time" is comes before the visible time
   * range, a positive number if "time" is after the visible time range, and 0
   * if it is in the visible time range.
   */
  public int compareToVisibleTimeRange(Time time) {
    int savedHour = baseDate.hour;
    int savedMinute = baseDate.minute;
    int savedSec = baseDate.second;

    baseDate.hour = 0;
    baseDate.minute = 0;
    baseDate.second = 0;

    if (DEBUG) {
      Log.d(TAG, "Begin " + baseDate.toString());
      Log.d(TAG, "Diff  " + time.toString());
    }

    // Compare beginning of range
    int diff = Time.compare(time, baseDate);
    if (diff > 0) {
      // Compare end of range
      baseDate.monthDay += numDays;
      baseDate.normalize(true);
      diff = Time.compare(time, baseDate);

      if (DEBUG) {
        Log.d(TAG, "End   " + baseDate.toString());
      }

      baseDate.monthDay -= numDays;
      baseDate.normalize(true);
      if (diff < 0) {
        // in visible time
        diff = 0;
      } else if (diff == 0) {
        // Midnight of following day
        diff = 1;
      }
    }

    if (DEBUG) {
      Log.d(TAG, "Diff: " + diff);
    }

    baseDate.hour = savedHour;
    baseDate.minute = savedMinute;
    baseDate.second = savedSec;
    return diff;
  }

  private void recalc() {
    // Set the base date to the beginning of the week if we are displaying
    // 7 days at a time.
    if (numDays == 7) {
      adjustToBeginningOfWeek(baseDate);
    }

    final long start = baseDate.toMillis(false /* use isDst */);
    firstJulianDay = Time.getJulianDay(start, baseDate.gmtoff);
    lastJulianDay = firstJulianDay + numDays - 1;

    monthLength = baseDate.getActualMaximum(Time.MONTH_DAY);
    firstVisibleDate = baseDate.monthDay;
    firstVisibleDayOfWeek = baseDate.weekDay;
  }

  private void adjustToBeginningOfWeek(Time time) {
    int dayOfWeek = time.weekDay;
    int diff = dayOfWeek - firstDayOfWeek;
    if (diff != 0) {
      if (diff < 0) {
        diff += 7;
      }
      time.monthDay -= diff;
      time.normalize(true /* ignore isDst */);
    }
  }

  @Override
  protected void onSizeChanged(int width, int height, int oldw, int oldh) {
    viewWidth = width;
    viewHeight = height;
    edgeEffectTop.setSize(viewWidth, viewHeight);
    edgeEffectBottom.setSize(viewWidth, viewHeight);
    int gridAreaWidth = width - hoursWidth;
    if (numDays == 0) {
      numDays = 1;
    }
    cellWidth = (gridAreaWidth - (numDays * DAY_GAP)) / numDays;

    // This would be about 1 day worth in a 7 day view
    horizontalSnapBackThreshold = width / 7;

    Paint p = new Paint();
    p.setTextSize(HOURS_TEXT_SIZE);
    hoursTextHeight = (int) Math.abs(p.ascent());
    remeasure(width, height);
  }

  /**
   * Measures the space needed for various parts of the view after
   * loading new events.  This can change if there are all-day events.
   */
  private void remeasure(int width, int height) {
    // Shrink to fit available space but make sure we can display at least two events
    MAX_UNEXPANDED_ALLDAY_HEIGHT = (int) (MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT * 4);
    MAX_UNEXPANDED_ALLDAY_HEIGHT = Math.min(MAX_UNEXPANDED_ALLDAY_HEIGHT, height / 6);
    MAX_UNEXPANDED_ALLDAY_HEIGHT = Math.max(MAX_UNEXPANDED_ALLDAY_HEIGHT, (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT * 2);
    maxUnexpandedAllDayEventCount = (int) (MAX_UNEXPANDED_ALLDAY_HEIGHT / MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);

    // First, clear the array of earliest start times, and the array
    // indicating presence of an all-day event.
    for (int day = 0; day < numDays; day++) {
      earliestStartHour[day] = 25;  // some big number
      hasAllDayEvent[day] = false;
    }

    int maxAllDayEvents = maxAlldayEvents;

    // The min is where 24 hours cover the entire visible area
    minCellHeight = Math.max((height - DAY_HEADER_HEIGHT) / 24, (int) MIN_EVENT_HEIGHT);
    if (cellHeight < minCellHeight) {
      cellHeight = minCellHeight;
    }

    // Calculate mAllDayHeight
    firstCell = DAY_HEADER_HEIGHT;
    int allDayHeight = 0;
    if (maxAllDayEvents > 0) {
      int maxAllAllDayHeight = height - DAY_HEADER_HEIGHT - MIN_HOURS_HEIGHT;
      // If there is at most one all-day event per day, then use less
      // space (but more than the space for a single event).
      if (maxAllDayEvents == 1) {
        allDayHeight = SINGLE_ALLDAY_HEIGHT;
      } else if (maxAllDayEvents <= maxUnexpandedAllDayEventCount) {
        // Allow the all-day area to grow in height depending on the
        // number of all-day events we need to show, up to a limit.
        allDayHeight = maxAllDayEvents * MAX_HEIGHT_OF_ONE_ALLDAY_EVENT;
        if (allDayHeight > MAX_UNEXPANDED_ALLDAY_HEIGHT) {
          allDayHeight = MAX_UNEXPANDED_ALLDAY_HEIGHT;
        }
      } else {
        // if we have more than the magic number, check if we're animating
        // and if not adjust the sizes appropriately
        if (animateDayHeight != 0) {
          // Don't shrink the space past the final allDay space. The animation
          // continues to hide the last event so the more events text can
          // fade in.
          allDayHeight = Math.max(animateDayHeight, MAX_UNEXPANDED_ALLDAY_HEIGHT);
        } else {
          // Try to fit all the events in
          allDayHeight = (int) (maxAllDayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);
          // But clip the area depending on which mode we're in
          if (!showAllAllDayEvents && allDayHeight > MAX_UNEXPANDED_ALLDAY_HEIGHT) {
            allDayHeight = (int) (maxUnexpandedAllDayEventCount * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);
          } else if (allDayHeight > maxAllAllDayHeight) {
            allDayHeight = maxAllAllDayHeight;
          }
        }
      }
      firstCell = DAY_HEADER_HEIGHT + allDayHeight + ALLDAY_TOP_MARGIN;
    } else {
      selectionAllday = false;
    }
    this.allDayHeight = allDayHeight;

    gridAreaHeight = height - firstCell;

    // Set up the expand icon position
    int allDayIconWidth = expandAlldayDrawable.getIntrinsicWidth();
    expandAllDayRect.left = Math.max((hoursWidth - allDayIconWidth) / 2,
                                     EVENT_ALL_DAY_TEXT_LEFT_MARGIN);
    expandAllDayRect.right = Math.min(expandAllDayRect.left + allDayIconWidth, hoursWidth - EVENT_ALL_DAY_TEXT_RIGHT_MARGIN);
    expandAllDayRect.bottom = firstCell - EXPAND_ALL_DAY_BOTTOM_MARGIN;
    expandAllDayRect.top = expandAllDayRect.bottom - expandAlldayDrawable.getIntrinsicHeight();

    numHours = gridAreaHeight / (cellHeight + HOUR_GAP);
    eventGeometry.setHourHeight(cellHeight);

    final long minimumDurationMillis = (long) (MIN_EVENT_HEIGHT * DateUtils.MINUTE_IN_MILLIS / (cellHeight / 60.0f));
    Event.computePositions(events);

    // Compute the top of our reachable view
    maxViewStartY = HOUR_GAP + 24 * (cellHeight + HOUR_GAP) - gridAreaHeight;
    if (DEBUG) {
      Log.e(TAG, "viewStartY: " + viewStartY);
      Log.e(TAG, "maxViewStartY: " + maxViewStartY);
    }
    if (viewStartY > maxViewStartY) {
      viewStartY = maxViewStartY;
      computeFirstHour();
    }

    if (firstHour == -1) {
      initFirstHour();
      firstHourOffset = 0;
    }

    // When we change the base date, the number of all-day events may
    // change and that changes the cell height.  When we switch dates,
    // we use the firstHourOffset from the previous view, but that may
    // be too large for the new view if the cell height is smaller.
    if (firstHourOffset >= cellHeight + HOUR_GAP) {
      firstHourOffset = cellHeight + HOUR_GAP - 1;
    }
    viewStartY = firstHour * (cellHeight + HOUR_GAP) - firstHourOffset;

    final int eventAreaWidth = numDays * (cellWidth + DAY_GAP);
    //When we get new events we don't want to dismiss the popup unless the event changes
    if (selectedEvent != null && lastPopupEventId != selectedEvent.getId()) {
      popup.dismiss();
    }
    popup.setWidth(eventAreaWidth - 20);
    popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  /**
   * Initialize the state for another view.  The given view is one that has
   * its own bitmap and will use an animation to replace the current view.
   * The current view and new view are either both Week views or both Day
   * views.  They differ in their base date.
   *
   * @param view the view to initialize.
   */
  private void initView(DayView view) {
    view.setSelectedHour(selectionHour);
    view.selectedEvents.clear();
    view.computeSelectedEvents = true;
    view.firstHour = firstHour;
    view.firstHourOffset = firstHourOffset;
    view.remeasure(getWidth(), getHeight());
    view.initAllDayHeights();

    view.setSelectedEvent(null);
    view.prevSelectedEvent = null;
    view.firstDayOfWeek = firstDayOfWeek;
    if (view.events.size() > 0) {
      view.selectionAllday = selectionAllday;
    } else {
      view.selectionAllday = false;
    }

    // Redraw the screen so that the selection box will be redrawn.  We may
    // have scrolled to a different part of the day in some other view
    // so the selection box in this view may no longer be visible.
    view.recalc();
  }

  /**
   * Switch to another view based on what was selected (an event or a free
   * slot) and how it was selected (by touch or by trackball).
   *
   * @param trackBallSelection true if the selection was made using the
   *                           trackball.
   */
  private void switchViews(boolean trackBallSelection) {
    Event selectedEvent = this.selectedEvent;

    popup.dismiss();
    lastPopupEventId = INVALID_EVENT_ID;
    if (numDays > 1) {
      // This is the Week view.
      // With touch, we always switch to Day/Agenda View
      // With track ball, if we selected a free slot, then create an event.
      // If we selected a specific event, switch to EventInfo view.
      if (trackBallSelection) {
        if (selectedEvent == null) {
          // Switch to the EditEvent view
          long startMillis = getSelectedTimeInMillis();
          long endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
          long extraLong = 0;
          if (selectionAllday) {
            extraLong = CalendarController.EXTRA_CREATE_ALL_DAY;
          }
          calendarController.sendEventRelatedEventWithExtra(EventType.CREATE_EVENT, -1, startMillis, endMillis, -1, -1, extraLong, -1);
        } else {
          // Switch to the EventInfo view
//          calendarController.sendEventRelatedEvent(EventType.VIEW_EVENT, selectedEvent.id, selectedEvent.startMillis, selectedEvent.endMillis, 0, 0, getSelectedTimeInMillis());
        }
      } else {
        // This was a touch selection.  If the touch selected a single
        // unambiguous event, then view that event.  Otherwise go to
        // Day/Agenda view.
        if (selectedEvents.size() == 1) {
//          calendarController.sendEventRelatedEvent(EventType.VIEW_EVENT, selectedEvent.id, selectedEvent.startMillis, selectedEvent.endMillis, 0, 0, getSelectedTimeInMillis());
        }
      }
    } else {
      // This is the Day view.
      // If we selected a free slot, then create an event.
      // If we selected an event, then go to the EventInfo view.
      if (selectedEvent == null) {
        // Switch to the EditEvent view
        long startMillis = getSelectedTimeInMillis();
        long endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
        long extraLong = 0;
        if (selectionAllday) {
          extraLong = CalendarController.EXTRA_CREATE_ALL_DAY;
        }
        calendarController.sendEventRelatedEventWithExtra(EventType.CREATE_EVENT, -1, startMillis, endMillis, -1, -1, extraLong, -1);
      } else {
//        calendarController.sendEventRelatedEvent(EventType.VIEW_EVENT, selectedEvent.id, selectedEvent.startMillis, selectedEvent.endMillis, 0, 0, getSelectedTimeInMillis());
      }
    }
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    scrolling = false;
    long duration = event.getEventTime() - event.getDownTime();

    switch (keyCode) {
      case KeyEvent.KEYCODE_DPAD_CENTER:
        if (selectionMode == SELECTION_HIDDEN) {
          // Don't do anything unless the selection is visible.
          break;
        }

        if (selectionMode == SELECTION_PRESSED) {
          // This was the first press when there was nothing selected.
          // Change the selection from the "pressed" state to the
          // the "selected" state.  We treat short-press and
          // long-press the same here because nothing was selected.
          selectionMode = SELECTION_SELECTED;
          invalidate();
          break;
        }

        // Check the duration to determine if this was a short press
        if (duration < ViewConfiguration.getLongPressTimeout()) {
          switchViews(true /* trackball */);
        } else {
          selectionMode = SELECTION_LONGPRESS;
          invalidate();
          performLongClick();
        }
        break;
//            case KeyEvent.KEYCODE_BACK:
//                if (event.isTracking() && !event.isCanceled()) {
//                    popup.dismiss();
//                    context.finish();
//                    return true;
//                }
//                break;
    }
    return super.onKeyUp(keyCode, event);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (selectionMode == SELECTION_HIDDEN) {
      if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
          || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP
          || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        // Display the selection box but don't move or select it
        // on this key press.
        selectionMode = SELECTION_SELECTED;
        invalidate();
        return true;
      } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        // Display the selection box but don't select it
        // on this key press.
        selectionMode = SELECTION_PRESSED;
        invalidate();
        return true;
      }
    }

    selectionMode = SELECTION_SELECTED;
    scrolling = false;
    boolean redraw;
    int selectionDay = this.selectionDay;

    switch (keyCode) {
      case KeyEvent.KEYCODE_DEL:
        // Delete the selected event, if any
//                Event selectedEvent = selectedEvent;
//                if (selectedEvent == null) {
//                    return false;
//                }
//                popup.dismiss();
//                lastPopupEventId = INVALID_EVENT_ID;
//
//                long begin = selectedEvent.startMillis;
//                long end = selectedEvent.endMillis;
//                long id = selectedEvent.id;
//                mDeleteEventHelper.delete(begin, end, id, -1);
        return true;
      case KeyEvent.KEYCODE_ENTER:
        switchViews(true /* trackball or keyboard */);
        return true;
      case KeyEvent.KEYCODE_BACK:
        if (event.getRepeatCount() == 0) {
          event.startTracking();
          return true;
        }
        return super.onKeyDown(keyCode, event);
      case KeyEvent.KEYCODE_DPAD_LEFT:
        if (selectedEvent != null) {
          setSelectedEvent(selectedEvent.nextLeft);
        }
        if (selectedEvent == null) {
          lastPopupEventId = INVALID_EVENT_ID;
          selectionDay -= 1;
        }
        redraw = true;
        break;

      case KeyEvent.KEYCODE_DPAD_RIGHT:
        if (selectedEvent != null) {
          setSelectedEvent(selectedEvent.nextRight);
        }
        if (selectedEvent == null) {
          lastPopupEventId = INVALID_EVENT_ID;
          selectionDay += 1;
        }
        redraw = true;
        break;

      case KeyEvent.KEYCODE_DPAD_UP:
        if (selectedEvent != null) {
          setSelectedEvent(selectedEvent.nextUp);
        }
        if (selectedEvent == null) {
          lastPopupEventId = INVALID_EVENT_ID;
          if (!selectionAllday) {
            setSelectedHour(selectionHour - 1);
            adjustHourSelection();
            selectedEvents.clear();
            computeSelectedEvents = true;
          }
        }
        redraw = true;
        break;

      case KeyEvent.KEYCODE_DPAD_DOWN:
        if (selectedEvent != null) {
          setSelectedEvent(selectedEvent.nextDown);
        }
        if (selectedEvent == null) {
          lastPopupEventId = INVALID_EVENT_ID;
          if (selectionAllday) {
            selectionAllday = false;
          } else {
            setSelectedHour(selectionHour + 1);
            adjustHourSelection();
            selectedEvents.clear();
            computeSelectedEvents = true;
          }
        }
        redraw = true;
        break;

      default:
        return super.onKeyDown(keyCode, event);
    }

    if ((selectionDay < firstJulianDay) || (selectionDay > lastJulianDay)) {
      DayView view = (DayView) viewSwitcher.getNextView();
      Time date = view.baseDate;
      date.set(baseDate);
      if (selectionDay < firstJulianDay) {
        date.monthDay -= numDays;
      } else {
        date.monthDay += numDays;
      }
      date.normalize(true /* ignore isDst */);
      view.setSelectedDay(selectionDay);

      initView(view);

      Time end = new Time(date);
      end.monthDay += numDays - 1;
      calendarController.sendEvent(EventType.GO_TO, date, end, -1, ViewType.CURRENT);
      return true;
    }
    if (this.selectionDay != selectionDay) {
      Time date = new Time(baseDate);
      date.setJulianDay(selectionDay);
      date.hour = selectionHour;
      calendarController.sendEvent(EventType.GO_TO, date, date, -1, ViewType.CURRENT);
    }
    setSelectedDay(selectionDay);
    selectedEvents.clear();
    computeSelectedEvents = true;
    updateToast = true;

    if (redraw) {
      invalidate();
      return true;
    }

    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onHoverEvent(MotionEvent event) {
    if (DEBUG) {
      int action = event.getAction();
      switch (action) {
        case MotionEvent.ACTION_HOVER_ENTER:
          Log.e(TAG, "ACTION_HOVER_ENTER");
          break;
        case MotionEvent.ACTION_HOVER_MOVE:
          Log.e(TAG, "ACTION_HOVER_MOVE");
          break;
        case MotionEvent.ACTION_HOVER_EXIT:
          Log.e(TAG, "ACTION_HOVER_EXIT");
          break;
        default:
          Log.e(TAG, "Unknown hover event action. " + event);
      }
    }

    // Mouse also generates hover events
    // Send accessibility events if accessibility and exploration are on.
    if (!touchExplorationEnabled) {
      return super.onHoverEvent(event);
    }
    if (event.getAction() != MotionEvent.ACTION_HOVER_EXIT) {
      setSelectionFromPosition((int) event.getX(), (int) event.getY(), true);
      invalidate();
    }
    return true;
  }

  private View switchViews(boolean forward, float xOffSet, float width, float velocity) {
    animationDistance = width - xOffSet;
    if (DEBUG) {
      Log.d(TAG, "switchViews(" + forward + ") O:" + xOffSet + " Dist:" + animationDistance);
    }

    float progress = Math.abs(xOffSet) / width;
    if (progress > 1.0f) {
      progress = 1.0f;
    }

    float inFromXValue, inToXValue;
    float outFromXValue, outToXValue;
    if (forward) {
      inFromXValue = 1.0f - progress;
      inToXValue = 0.0f;
      outFromXValue = -progress;
      outToXValue = -1.0f;
    } else {
      inFromXValue = progress - 1.0f;
      inToXValue = 0.0f;
      outFromXValue = progress;
      outToXValue = 1.0f;
    }

    final Time start = new Time(baseDate.timezone);
    start.set(calendarController.getTime());
    if (forward) {
      start.monthDay += numDays;
    } else {
      start.monthDay -= numDays;
    }
    calendarController.setTime(start.normalize(true));

    Time newSelected = start;

    if (numDays == 7) {
      newSelected = new Time(start);
      adjustToBeginningOfWeek(start);
    }

    final Time end = new Time(start);
    end.monthDay += numDays - 1;

    // We have to allocate these animation objects each time we switch views
    // because that is the only way to set the animation parameters.
    TranslateAnimation inAnimation = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, inFromXValue,
        Animation.RELATIVE_TO_SELF, inToXValue,
        Animation.ABSOLUTE, 0.0f,
        Animation.ABSOLUTE, 0.0f);

    TranslateAnimation outAnimation = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, outFromXValue,
        Animation.RELATIVE_TO_SELF, outToXValue,
        Animation.ABSOLUTE, 0.0f,
        Animation.ABSOLUTE, 0.0f);

    long duration = calculateDuration(width - Math.abs(xOffSet), width, velocity);
    inAnimation.setDuration(duration);
    inAnimation.setInterpolator(hScrollInterpolator);
    outAnimation.setInterpolator(hScrollInterpolator);
    outAnimation.setDuration(duration);
    outAnimation.setAnimationListener(new GotoBroadcaster(start, end));
    viewSwitcher.setInAnimation(inAnimation);
    viewSwitcher.setOutAnimation(outAnimation);

    DayView view = (DayView) viewSwitcher.getCurrentView();
    view.cleanup();
    viewSwitcher.showNext();
    view = (DayView) viewSwitcher.getCurrentView();
    view.setSelected(newSelected, true, false);
    view.requestFocus();
    view.reloadEvents();
    view.updateTitle();
    view.restartCurrentTimeUpdates();

    return view;
  }

  // This is called after scrolling stops to move the selected hour
  // to the visible part of the screen.
  private void resetSelectedHour() {
    if (selectionHour < firstHour + 1) {
      setSelectedHour(firstHour + 1);
      setSelectedEvent(null);
      selectedEvents.clear();
      computeSelectedEvents = true;
    } else if (selectionHour > firstHour + numHours - 3) {
      setSelectedHour(firstHour + numHours - 3);
      setSelectedEvent(null);
      selectedEvents.clear();
      computeSelectedEvents = true;
    }
  }

  private void initFirstHour() {
    firstHour = selectionHour - numHours / 5;
    if (firstHour < 0) {
      firstHour = 0;
    } else if (firstHour + numHours > 24) {
      firstHour = 24 - numHours;
    }
  }

  /**
   * Recomputes the first full hour that is visible on screen after the
   * screen is scrolled.
   */
  private void computeFirstHour() {
    // Compute the first full hour that is visible on screen
    firstHour = (viewStartY + cellHeight + HOUR_GAP - 1) / (cellHeight + HOUR_GAP);
    firstHourOffset = firstHour * (cellHeight + HOUR_GAP) - viewStartY;
  }

  private void adjustHourSelection() {
    if (selectionHour < 0) {
      setSelectedHour(0);
      if (maxAlldayEvents > 0) {
        prevSelectedEvent = null;
        selectionAllday = true;
      }
    }

    if (selectionHour > 23) {
      setSelectedHour(23);
    }

    // If the selected hour is at least 2 time slots from the top and
    // bottom of the screen, then don't scroll the view.
    if (selectionHour < firstHour + 1) {
      // If there are all-days events for the selected day but there
      // are no more normal events earlier in the day, then jump to
      // the all-day event area.
      // Exception 1: allow the user to scroll to 8am with the trackball
      // before jumping to the all-day event area.
      // Exception 2: if 12am is on screen, then allow the user to select
      // 12am before going up to the all-day event area.
      int daynum = selectionDay - firstJulianDay;
      if (maxAlldayEvents > 0 && earliestStartHour[daynum] > selectionHour && firstHour > 0 && firstHour < 8) {
        prevSelectedEvent = null;
        selectionAllday = true;
        setSelectedHour(firstHour + 1);
        return;
      }

      if (firstHour > 0) {
        firstHour -= 1;
        viewStartY -= (cellHeight + HOUR_GAP);
        if (viewStartY < 0) {
          viewStartY = 0;
        }
        return;
      }
    }

    if (selectionHour > firstHour + numHours - 3) {
      if (firstHour < 24 - numHours) {
        firstHour += 1;
        viewStartY += (cellHeight + HOUR_GAP);
        if (viewStartY > maxViewStartY) {
          viewStartY = maxViewStartY;
        }
      } else if (firstHour == 24 - numHours && firstHourOffset > 0) {
        viewStartY = maxViewStartY;
      }
    }
  }

  public void clearCachedEvents() {
    lastReloadMillis = 0;
  }

  public void reloadEvents() {
    // Protect against this being called before this view has been
    // initialized.
//        if (context == null) {
//            return;
//        }

    // Make sure our time zones are up to date
    timeZoneUpdater.run();

    setSelectedEvent(null);
    prevSelectedEvent = null;
    selectedEvents.clear();

    // The start date is the beginning of the week at 12am
    Time weekStart = new Time(Utils.getTimeZone(context, timeZoneUpdater));
    weekStart.set(baseDate);
    weekStart.hour = 0;
    weekStart.minute = 0;
    weekStart.second = 0;
    long millis = weekStart.normalize(true /* ignore isDst */);

    // Avoid reloading events unnecessarily.
    if (millis == lastReloadMillis) {
      return;
    }
    lastReloadMillis = millis;

    // load events in the background
//        context.startProgressSpinner();
    final ArrayList<Event> events = new ArrayList<Event>();
    eventLoader.loadEventsInBackground(numDays, events, firstJulianDay, new Runnable() {
      @Override
      public void run() {
        boolean fadeinEvents = firstJulianDay != loadedFirstJulianDay;
        DayView.this.events = events;
        loadedFirstJulianDay = firstJulianDay;
        if (allDayEvents == null) {
          allDayEvents = new ArrayList<Event>();
        } else {
          allDayEvents.clear();
        }

        // Create a shorter array for all day events
//        for (Event e : events) {
//          if (e.drawAsAllday()) {
//            allDayEvents.add(e);
//          }
//        }

        // New events, new layouts
        if (layouts == null || layouts.length < events.size()) {
          layouts = new StaticLayout[events.size()];
        } else {
          Arrays.fill(layouts, null);
        }

        if (allDayLayouts == null || allDayLayouts.length < allDayEvents.size()) {
          allDayLayouts = new StaticLayout[events.size()];
        } else {
          Arrays.fill(allDayLayouts, null);
        }

        computeEventRelations();

        remeasure = true;
        computeSelectedEvents = true;
        recalc();

        // Start animation to cross fade the events
        if (fadeinEvents) {
          if (eventsCrossFadeAnimation == null) {
            eventsCrossFadeAnimation = ObjectAnimator.ofInt(DayView.this, "EventsAlpha", 0, 255);
            eventsCrossFadeAnimation.setDuration(EVENTS_CROSS_FADE_DURATION);
          }
          eventsCrossFadeAnimation.start();
        } else {
          invalidate();
        }
      }
    }, cancelCallback);
  }

  public void stopEventsAnimation() {
    if (eventsCrossFadeAnimation != null) {
      eventsCrossFadeAnimation.cancel();
    }
    eventsAlpha = 255;
  }

  private void computeEventRelations() {
    // Compute the layout relation between each event before measuring cell width, as the cell width should be adjusted along with the relation.
    //
    // Examples: A (1:00pm - 1:01pm), B (1:02pm - 2:00pm)
    // We should mark them as "overwapped". Though they are not overwapped logically, but
    // minimum cell height implicitly expands the cell height of A and it should look like
    // (1:00pm - 1:15pm) after the cell height adjustment.

    // Compute the space needed for the all-day events, if any.
    // Make a pass over all the events, and keep track of the maximum
    // number of all-day events in any one day.  Also, keep track of
    // the earliest event in each day.
    int maxAllDayEvents = 0;
    final ArrayList<Event> events = this.events;
    final int len = events.size();
    // Num of all-day-events on each day.
    final int eventsCount[] = new int[lastJulianDay - firstJulianDay + 1];
    Arrays.fill(eventsCount, 0);

    for (Event event : events) {
      if (event.getStartDay() > lastJulianDay || event.getEndDay() < firstJulianDay) {
        continue;
      }
//      if (event.drawAsAllday()) {
        // Count all the events being drawn as allDay events
//        final int firstDay = Math.max(event.startDay, firstJulianDay);
//        final int lastDay = Math.min(event.endDay, lastJulianDay);
//        for (int day = firstDay; day <= lastDay; day++) {
//          final int count = ++eventsCount[day - firstJulianDay];
//          if (maxAllDayEvents < count) {
//            maxAllDayEvents = count;
//          }
//        }

//        long daynum = event.startDay - firstJulianDay;
//        long durationDays = event.endDay - event.startDay + 1;
//        if (daynum < 0) {
//          durationDays += daynum;
//          daynum = 0;
//        }
//        if (daynum + durationDays > numDays) {
//          durationDays = numDays - daynum;
//        }
//        for (long day = daynum; durationDays > 0; day++, durationDays--) {
//          hasAllDayEvent[((int) day)] = true;
//        }
//      } else {
        long daynum = event.getStartDay() - firstJulianDay;
        long hour = event.getStartTime() / 60;
        if (daynum >= 0 && hour < earliestStartHour[((int) daynum)]) {
          earliestStartHour[((int) daynum)] = (int) hour;
        }

        // Also check the end hour in case the event spans more than one day.
        daynum = event.getEndDay() - firstJulianDay;
//        hour = event.endTime / 60;
        if (daynum < numDays && hour < earliestStartHour[((int) daynum)]) {
          earliestStartHour[((int) daynum)] = (int) hour;
        }
//      }
    }
    maxAlldayEvents = maxAllDayEvents;
    initAllDayHeights();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (remeasure) {
      remeasure(getWidth(), getHeight());
      remeasure = false;
    }
    canvas.save();

    float yTranslate = -viewStartY + DAY_HEADER_HEIGHT + allDayHeight;
    // offset canvas by the current drag and header position
    canvas.translate(-viewStartX, yTranslate);
    // clip to everything below the allDay area
    Rect dest = destRect;
    dest.top = (int) (firstCell - yTranslate);
    dest.bottom = (int) (viewHeight - yTranslate);
    dest.left = 0;
    dest.right = viewWidth;
    canvas.save();
    canvas.clipRect(dest);
    // Draw the movable part of the view
    doDraw(canvas);
    // restore to having no clip
    canvas.restore();

    if ((touchMode & TOUCH_MODE_HSCROLL) != 0) {
      float xTranslate;
      if (viewStartX > 0) {
        xTranslate = viewWidth;
      } else {
        xTranslate = -viewWidth;
      }
      // Move the canvas around to prep it for the next view specifically, shift it by a screen and undo the
      // yTranslation which will be redone in the nextView's onDraw().
      canvas.translate(xTranslate, -yTranslate);
      DayView nextView = (DayView) viewSwitcher.getNextView();

      // Prevent infinite recursive calls to onDraw().
      nextView.touchMode = TOUCH_MODE_INITIAL_STATE;
      nextView.onDraw(canvas);

      // Move it back for this view
      canvas.translate(-xTranslate, 0);
    } else {
      // If we drew another view we already translated it back If we didn't draw another view we should be at the edge of the screen
      canvas.translate(viewStartX, -yTranslate);
    }

    // Draw the fixed areas (that don't scroll) directly to the canvas.
    drawAfterScroll(canvas);
    if (computeSelectedEvents && updateToast) {
      updateEventDetails();
      updateToast = false;
    }
    computeSelectedEvents = false;

    // Draw overscroll glow
    if (!edgeEffectTop.isFinished()) {
      if (DAY_HEADER_HEIGHT != 0) {
        canvas.translate(0, DAY_HEADER_HEIGHT);
      }
      if (edgeEffectTop.draw(canvas)) {
        invalidate();
      }
      if (DAY_HEADER_HEIGHT != 0) {
        canvas.translate(0, -DAY_HEADER_HEIGHT);
      }
    }

    if (!edgeEffectBottom.isFinished()) {
      canvas.rotate(180, viewWidth / 2, viewHeight / 2);
      if (edgeEffectBottom.draw(canvas)) {
        invalidate();
      }
    }
    canvas.restore();
  }

  private void drawAfterScroll(Canvas canvas) {
    Paint p = paint;
    Rect r = rect;

    drawAllDayHighlights(r, canvas, p);
    if (maxAlldayEvents != 0) {
      drawAllDayEvents(firstJulianDay, numDays, canvas, p);
      drawUpperLeftCorner(r, canvas, p);
    }

    drawScrollLine(r, canvas, p);
    drawDayHeaderLoop(r, canvas, p);

    // Draw the AM and PM indicators if we're in 12 hour mode
    if (!is24HourFormat) {
      drawAmPm(canvas, p);
    }
  }

  // This isn't really the upper-left corner. It's the square area just below the upper-left corner, above the hours and to the left of the
  // all-day area.
  private void drawUpperLeftCorner(Rect r, Canvas canvas, Paint p) {
    setupHourTextPaint(p);
    if (maxAlldayEvents > maxUnexpandedAllDayEventCount) {
      // Draw the allDay expand/collapse icon
      if (useExpandIcon) {
        expandAlldayDrawable.setBounds(expandAllDayRect);
        expandAlldayDrawable.draw(canvas);
      } else {
        collapseAlldayDrawable.setBounds(expandAllDayRect);
        collapseAlldayDrawable.draw(canvas);
      }
    }
  }

  private void drawScrollLine(Rect r, Canvas canvas, Paint p) {
    final int right = computeDayLeftPosition(numDays);
    final int y = firstCell - 1;

    p.setAntiAlias(false);
    p.setStyle(Style.FILL);
    p.setColor(calendarGridLineInnerHorizontalColor);
    p.setStrokeWidth(GRID_LINE_INNER_WIDTH);
    canvas.drawLine(GRID_LINE_LEFT_MARGIN, y, right, y, p);
    p.setAntiAlias(true);
  }

  // Computes the x position for the left side of the given day (base 0)
  private int computeDayLeftPosition(int day) {
    int effectiveWidth = viewWidth - hoursWidth;
    return day * effectiveWidth / numDays + hoursWidth;
  }

  private void drawAllDayHighlights(Rect r, Canvas canvas, Paint p) {
    if (futureBgColor != 0) {
      // First, color the labels area light gray
      r.top = 0;
      r.bottom = DAY_HEADER_HEIGHT;
      r.left = 0;
      r.right = viewWidth;
      p.setColor(bgColor);
      p.setStyle(Style.FILL);
      canvas.drawRect(r, p);
      // and the area that says All day
      r.top = DAY_HEADER_HEIGHT;
      r.bottom = firstCell - 1;
      r.left = 0;
      r.right = hoursWidth;
      canvas.drawRect(r, p);

      int startIndex = -1;

      int todayIndex = todayJulianDay - firstJulianDay;
      if (todayIndex < 0) {
        // Future
        startIndex = 0;
      } else if (todayIndex >= 1 && todayIndex + 1 < numDays) {
        // Multiday - tomorrow is visible.
        startIndex = todayIndex + 1;
      }

      if (startIndex >= 0) {
        // Draw the future highlight
        r.top = 0;
        r.bottom = firstCell - 1;
        r.left = computeDayLeftPosition(startIndex) + 1;
        r.right = computeDayLeftPosition(numDays);
        p.setColor(futureBgColor);
        p.setStyle(Style.FILL);
        canvas.drawRect(r, p);
      }
    }

    if (selectionAllday && selectionMode != SELECTION_HIDDEN) {
      // Draw the selection highlight on the selected all-day area
      rect.top = DAY_HEADER_HEIGHT + 1;
      rect.bottom = rect.top + allDayHeight + ALLDAY_TOP_MARGIN - 2;
      int daynum = selectionDay - firstJulianDay;
      rect.left = computeDayLeftPosition(daynum) + 1;
      rect.right = computeDayLeftPosition(daynum + 1);
      p.setColor(calendarGridAreaSelected);
      canvas.drawRect(rect, p);
    }
  }

  private void drawDayHeaderLoop(Rect r, Canvas canvas, Paint p) {
    // Draw the horizontal day background banner
    // p.setColor(mCalendarDateBannerBackground);
    // r.top = 0;
    // r.bottom = DAY_HEADER_HEIGHT;
    // r.left = 0;
    // r.right = hoursWidth + numDays * (cellWidth + DAY_GAP);
    // canvas.drawRect(r, p);
    //
    // Fill the extra space on the right side with the default background
    // r.left = r.right;
    // r.right = viewWidth;
    // p.setColor(mCalendarGridAreaBackground);
    // canvas.drawRect(r, p);
    if (numDays == 1 && ONE_DAY_HEADER_HEIGHT == 0) {
      return;
    }

    p.setTypeface(bold);
    p.setTextAlign(Paint.Align.RIGHT);
    int cell = firstJulianDay;

    String[] dayNames;
    if (dateStrWidth < cellWidth) {
      dayNames = dayStrs;
    } else {
      dayNames = dayStrs2Letter;
    }

    p.setAntiAlias(true);
    for (int day = 0; day < numDays; day++, cell++) {
      int dayOfWeek = day + firstVisibleDayOfWeek;
      if (dayOfWeek >= 14) {
        dayOfWeek -= 14;
      }

      int color = calendarDateBannerTextColor;
      if (numDays == 1) {
        if (dayOfWeek == Time.SATURDAY) {
          color = weekSaturdayColor;
        } else if (dayOfWeek == Time.SUNDAY) {
          color = weekSundayColor;
        }
      } else {
        final int column = day % 7;
        if (Utils.isSaturday(column, firstDayOfWeek)) {
          color = weekSaturdayColor;
        } else if (Utils.isSunday(column, firstDayOfWeek)) {
          color = weekSundayColor;
        }
      }

      p.setColor(color);
      drawDayHeader(dayNames[dayOfWeek], day, cell, canvas, p);
    }
    p.setTypeface(null);
  }

  private void drawAmPm(Canvas canvas, Paint p) {
    p.setColor(calendarAmPmLabel);
    p.setTextSize(AMPM_TEXT_SIZE);
    p.setTypeface(bold);
    p.setAntiAlias(true);
    p.setTextAlign(Paint.Align.RIGHT);
    String text = amString;
    if (firstHour >= 12) {
      text = pmString;
    }
    int y = firstCell + firstHourOffset + 2 * hoursTextHeight + HOUR_GAP;
    canvas.drawText(text, HOURS_LEFT_MARGIN, y, p);

    if (firstHour < 12 && firstHour + numHours > 12) {
      // Also draw the "PM"
      text = pmString;
      y = firstCell + firstHourOffset + (12 - firstHour) * (cellHeight + HOUR_GAP) + 2 * hoursTextHeight + HOUR_GAP;
      canvas.drawText(text, HOURS_LEFT_MARGIN, y, p);
    }
  }

  private void drawCurrentTimeLine(Rect r, final int day, final int top, Canvas canvas, Paint p) {
    r.left = computeDayLeftPosition(day) - CURRENT_TIME_LINE_SIDE_BUFFER + 1;
    r.right = computeDayLeftPosition(day + 1) + CURRENT_TIME_LINE_SIDE_BUFFER + 1;

    r.top = top - CURRENT_TIME_LINE_TOP_OFFSET;
    r.bottom = r.top + currentTimeLine.getIntrinsicHeight();

    currentTimeLine.setBounds(r);
    currentTimeLine.draw(canvas);
    if (animateToday) {
      currentTimeAnimateLine.setBounds(r);
      currentTimeAnimateLine.setAlpha(animateTodayAlpha);
      currentTimeAnimateLine.draw(canvas);
    }
  }

  private void doDraw(Canvas canvas) {
    Paint p = paint;
    Rect r = rect;

    if (futureBgColor != 0) {
      drawBgColors(r, canvas, p);
    }
    drawGridBackground(r, canvas, p);
    drawHours(canvas, p);

    // Draw each day
    int cell = firstJulianDay;
    p.setAntiAlias(false);
    int alpha = p.getAlpha();
    p.setAlpha(eventsAlpha);
    for (int day = 0; day < numDays; day++, cell++) {
      // TODO Wow, this needs cleanup. drawEvents loop through all the
      // events on every call.
      drawEvents(cell, day, HOUR_GAP, canvas, p);
      // If this is today
      if (cell == todayJulianDay) {
        int lineY = currentTime.hour * (cellHeight + HOUR_GAP) + ((currentTime.minute * cellHeight) / 60) + 1;

        // And the current time shows up somewhere on the screen
        if (lineY >= viewStartY && lineY < viewStartY + viewHeight - 2) {
          drawCurrentTimeLine(r, day, lineY, canvas, p);
        }
      }
    }
    p.setAntiAlias(true);
    p.setAlpha(alpha);

    drawSelectedRect(r, canvas, p);
  }

  private void drawSelectedRect(Rect r, Canvas canvas, Paint p) {
    // Draw a highlight on the selected hour (if needed)
    if (selectionMode != SELECTION_HIDDEN && !selectionAllday) {
      int daynum = selectionDay - firstJulianDay;
      r.top = selectionHour * (cellHeight + HOUR_GAP);
      r.bottom = r.top + cellHeight + HOUR_GAP;
      r.left = computeDayLeftPosition(daynum) + 1;
      r.right = computeDayLeftPosition(daynum + 1) + 1;

      saveSelectionPosition(r.left, r.top, r.right, r.bottom);

      // Draw the highlight on the grid
      p.setColor(calendarGridAreaSelected);
      r.top += HOUR_GAP;
      r.right -= DAY_GAP;
      p.setAntiAlias(false);
      canvas.drawRect(r, p);

      // Draw a "new event hint" on top of the highlight
      // For the week view, show a "+", for day view, show "+ New event"
      p.setColor(newEventHintColor);
      if (numDays > 1) {
        p.setStrokeWidth(NEW_EVENT_WIDTH);
        int width = r.right - r.left;
        int midX = r.left + width / 2;
        int midY = r.top + cellHeight / 2;
        int length = Math.min(cellHeight, width) - NEW_EVENT_MARGIN * 2;
        length = Math.min(length, NEW_EVENT_MAX_LENGTH);
        int verticalPadding = (cellHeight - length) / 2;
        int horizontalPadding = (width - length) / 2;
        canvas.drawLine(r.left + horizontalPadding, midY, r.right - horizontalPadding, midY, p);
        canvas.drawLine(midX, r.top + verticalPadding, midX, r.bottom - verticalPadding, p);
      } else {
        p.setStyle(Paint.Style.FILL);
        p.setTextSize(NEW_EVENT_HINT_FONT_SIZE);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        canvas.drawText(newEventHintString, r.left + EVENT_TEXT_LEFT_MARGIN, r.top + Math.abs(p.getFontMetrics().ascent) + EVENT_TEXT_TOP_MARGIN, p);
      }
    }
  }

  private void drawHours(Canvas canvas, Paint p) {
    setupHourTextPaint(p);

    int y = HOUR_GAP + hoursTextHeight + HOURS_TOP_MARGIN;

    for (int i = 0; i < 24; i++) {
      String time = hourStrs[i];
      canvas.drawText(time, HOURS_LEFT_MARGIN, y, p);
      y += cellHeight + HOUR_GAP;
    }
  }

  private void setupHourTextPaint(Paint p) {
    p.setColor(calendarHourLabelColor);
    p.setTextSize(HOURS_TEXT_SIZE);
    p.setTypeface(Typeface.DEFAULT);
    p.setTextAlign(Paint.Align.RIGHT);
    p.setAntiAlias(true);
  }

  private void drawDayHeader(String dayStr, int day, int cell, Canvas canvas, Paint p) {
    int dateNum = firstVisibleDate + day;
    int x;
    if (dateNum > monthLength) {
      dateNum -= monthLength;
    }
    p.setAntiAlias(true);

    int todayIndex = todayJulianDay - firstJulianDay;
    // Draw day of the month
    String dateNumStr = String.valueOf(dateNum);
    if (numDays > 1) {
      float y = DAY_HEADER_HEIGHT - DAY_HEADER_BOTTOM_MARGIN;

      // Draw day of the month
      x = computeDayLeftPosition(day + 1) - DAY_HEADER_RIGHT_MARGIN;
      p.setTextAlign(Align.RIGHT);
      p.setTextSize(DATE_HEADER_FONT_SIZE);

      p.setTypeface(todayIndex == day ? bold : Typeface.DEFAULT);
      canvas.drawText(dateNumStr, x, y, p);

      // Draw day of the week
      x -= p.measureText(" " + dateNumStr);
      p.setTextSize(DAY_HEADER_FONT_SIZE);
      p.setTypeface(Typeface.DEFAULT);
      canvas.drawText(dayStr, x, y, p);
    } else {
      float y = ONE_DAY_HEADER_HEIGHT - DAY_HEADER_ONE_DAY_BOTTOM_MARGIN;
      p.setTextAlign(Align.LEFT);

      // Draw day of the week
      x = computeDayLeftPosition(day) + DAY_HEADER_ONE_DAY_LEFT_MARGIN;
      p.setTextSize(DAY_HEADER_FONT_SIZE);
      p.setTypeface(Typeface.DEFAULT);
      canvas.drawText(dayStr, x, y, p);

      // Draw day of the month
      x += p.measureText(dayStr) + DAY_HEADER_ONE_DAY_RIGHT_MARGIN;
      p.setTextSize(DATE_HEADER_FONT_SIZE);
      p.setTypeface(todayIndex == day ? bold : Typeface.DEFAULT);
      canvas.drawText(dateNumStr, x, y, p);
    }
  }

  private void drawGridBackground(Rect r, Canvas canvas, Paint p) {
    Paint.Style savedStyle = p.getStyle();

    final float stopX = computeDayLeftPosition(numDays);
    float y = 0;
    final float deltaY = cellHeight + HOUR_GAP;
    int linesIndex = 0;
    final float startY = 0;
    final float stopY = HOUR_GAP + 24 * (cellHeight + HOUR_GAP);
    float x;

    // Draw the inner horizontal grid lines
    p.setColor(calendarGridLineInnerHorizontalColor);
    p.setStrokeWidth(GRID_LINE_INNER_WIDTH);
    p.setAntiAlias(false);

    for (int hour = 0; hour <= 24; hour++) {
      lines[linesIndex++] = GRID_LINE_LEFT_MARGIN;
      lines[linesIndex++] = y;
      lines[linesIndex++] = stopX;
      lines[linesIndex++] = y;
      y += deltaY;
    }

    if (calendarGridLineInnerVerticalColor != calendarGridLineInnerHorizontalColor) {
      canvas.drawLines(lines, 0, linesIndex, p);
      linesIndex = 0;
      p.setColor(calendarGridLineInnerVerticalColor);
    }

    // Draw the inner vertical grid lines
    for (int day = 0; day <= numDays; day++) {
      x = computeDayLeftPosition(day);
      lines[linesIndex++] = x;
      lines[linesIndex++] = startY;
      lines[linesIndex++] = x;
      lines[linesIndex++] = stopY;
    }

    canvas.drawLines(lines, 0, linesIndex, p);
    // Restore the saved style.
    p.setStyle(savedStyle);
    p.setAntiAlias(true);
  }

  private void drawBgColors(Rect r, Canvas canvas, Paint p) {
    int todayIndex = todayJulianDay - firstJulianDay;
    // Draw the hours background color
    r.top = destRect.top;
    r.bottom = destRect.bottom;
    r.left = 0;
    r.right = hoursWidth;
    p.setColor(bgColor);
    p.setStyle(Style.FILL);
    p.setAntiAlias(false);
    canvas.drawRect(r, p);

    // Draw background for grid area
    if (numDays == 1 && todayIndex == 0) {
      // Draw a white background for the time later than current time
      int lineY = currentTime.hour * (cellHeight + HOUR_GAP) + ((currentTime.minute * cellHeight) / 60) + 1;
      if (lineY < viewStartY + viewHeight) {
        lineY = Math.max(lineY, viewStartY);
        r.left = hoursWidth;
        r.right = viewWidth;
        r.top = lineY;
        r.bottom = viewStartY + viewHeight;
        p.setColor(futureBgColor);
        canvas.drawRect(r, p);
      }
    } else if (todayIndex >= 0 && todayIndex < numDays) {
      // Draw today with a white background for the time later than current time
      int lineY = currentTime.hour * (cellHeight + HOUR_GAP) + ((currentTime.minute * cellHeight) / 60) + 1;
      if (lineY < viewStartY + viewHeight) {
        lineY = Math.max(lineY, viewStartY);
        r.left = computeDayLeftPosition(todayIndex) + 1;
        r.right = computeDayLeftPosition(todayIndex + 1);
        r.top = lineY;
        r.bottom = viewStartY + viewHeight;
        p.setColor(futureBgColor);
        canvas.drawRect(r, p);
      }

      // Paint Tomorrow and later days with future color
      if (todayIndex + 1 < numDays) {
        r.left = computeDayLeftPosition(todayIndex + 1) + 1;
        r.right = computeDayLeftPosition(numDays);
        r.top = destRect.top;
        r.bottom = destRect.bottom;
        p.setColor(futureBgColor);
        canvas.drawRect(r, p);
      }
    } else if (todayIndex < 0) {
      // Future
      r.left = computeDayLeftPosition(0) + 1;
      r.right = computeDayLeftPosition(numDays);
      r.top = destRect.top;
      r.bottom = destRect.bottom;
      p.setColor(futureBgColor);
      canvas.drawRect(r, p);
    }
    p.setAntiAlias(true);
  }

  public Event getSelectedEvent() {
    if (selectedEvent == null) {
      // There is no event at the selected hour, so create a new event.
      return getNewEvent(selectionDay, getSelectedTimeInMillis(), getSelectedMinutesSinceMidnight());
    }
    return selectedEvent;
  }

  private void setSelectedEvent(Event e) {
    selectedEvent = e;
  }

  public boolean isEventSelected() {
    return (selectedEvent != null);
  }

  public Event getNewEvent() {
    return getNewEvent(selectionDay, getSelectedTimeInMillis(), getSelectedMinutesSinceMidnight());
  }

  private int computeMaxStringWidth(int currentMax, String[] strings, Paint p) {
    float maxWidthF = 0.0f;

    for (String string : strings) {
      float width = p.measureText(string);
      maxWidthF = Math.max(width, maxWidthF);
    }
    int maxWidth = (int) (maxWidthF + 0.5);
    if (maxWidth < currentMax) {
      maxWidth = currentMax;
    }
    return maxWidth;
  }

  private void saveSelectionPosition(float left, float top, float right, float bottom) {
    prevBox.left = (int) left;
    prevBox.right = (int) right;
    prevBox.top = (int) top;
    prevBox.bottom = (int) bottom;
  }

  private Rect getCurrentSelectionPosition() {
    Rect box = new Rect();
    box.top = selectionHour * (cellHeight + HOUR_GAP);
    box.bottom = box.top + cellHeight + HOUR_GAP;
    int daynum = selectionDay - firstJulianDay;
    box.left = computeDayLeftPosition(daynum) + 1;
    box.right = computeDayLeftPosition(daynum + 1);
    return box;
  }

  private void setupTextRect(Rect r) {
    if (r.bottom <= r.top || r.right <= r.left) {
      r.bottom = r.top;
      r.right = r.left;
      return;
    }

    if (r.bottom - r.top > EVENT_TEXT_TOP_MARGIN + EVENT_TEXT_BOTTOM_MARGIN) {
      r.top += EVENT_TEXT_TOP_MARGIN;
      r.bottom -= EVENT_TEXT_BOTTOM_MARGIN;
    }
    if (r.right - r.left > EVENT_TEXT_LEFT_MARGIN + EVENT_TEXT_RIGHT_MARGIN) {
      r.left += EVENT_TEXT_LEFT_MARGIN;
      r.right -= EVENT_TEXT_RIGHT_MARGIN;
    }
  }

  private void setupAllDayTextRect(Rect r) {
    if (r.bottom <= r.top || r.right <= r.left) {
      r.bottom = r.top;
      r.right = r.left;
      return;
    }

    if (r.bottom - r.top > EVENT_ALL_DAY_TEXT_TOP_MARGIN + EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN) {
      r.top += EVENT_ALL_DAY_TEXT_TOP_MARGIN;
      r.bottom -= EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN;
    }
    if (r.right - r.left > EVENT_ALL_DAY_TEXT_LEFT_MARGIN + EVENT_ALL_DAY_TEXT_RIGHT_MARGIN) {
      r.left += EVENT_ALL_DAY_TEXT_LEFT_MARGIN;
      r.right -= EVENT_ALL_DAY_TEXT_RIGHT_MARGIN;
    }
  }

  /**
   * Return the layout for a numbered event. Create it if not already existing
   */
  private StaticLayout getEventLayout(StaticLayout[] layouts, int i, Event event, Paint paint, Rect r) {
    if (i < 0 || i >= layouts.length) {
      return null;
    }

    StaticLayout layout = layouts[i];
    // Check if we have already initialized the StaticLayout and that the width hasn't changed (due to vertical resizing which causes
    // re-layout of events at min height)
    if (layout == null || r.width() != layout.getWidth()) {
      SpannableStringBuilder bob = new SpannableStringBuilder();
      if (event.getName() != null) {
        // MAX - 1 since we add a space
        bob.append(drawTextSanitizer(event.getName(), MAX_EVENT_TEXT_LEN - 1));
        bob.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, bob.length(), 0);
        bob.append(' ');
      }
      if (event.getLocation() != null) {
        bob.append(drawTextSanitizer(event.getLocation(), MAX_EVENT_TEXT_LEN - bob.length()));
      }

      // Leave a one pixel boundary on the left and right of the rectangle for the event
      layout = new StaticLayout(bob, 0, bob.length(), new TextPaint(paint), r.width(), Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true, null, r.width());
      layouts[i] = layout;
    }

    layout.getPaint().setAlpha(eventsAlpha);
    return layout;
  }

  private void drawAllDayEvents(int firstDay, int numDays, Canvas canvas, Paint p) {
    p.setTextSize(NORMAL_FONT_SIZE);
    p.setTextAlign(Paint.Align.LEFT);
    Paint eventTextPaint = this.eventTextPaint;

    final float startY = DAY_HEADER_HEIGHT;
    final float stopY = startY + allDayHeight + ALLDAY_TOP_MARGIN;
    float x;
    int linesIndex = 0;

    // Draw the inner vertical grid lines
    p.setColor(calendarGridLineInnerVerticalColor);
    p.setStrokeWidth(GRID_LINE_INNER_WIDTH);
    // Line bounding the top of the all day area
    lines[linesIndex++] = GRID_LINE_LEFT_MARGIN;
    lines[linesIndex++] = startY;
    lines[linesIndex++] = computeDayLeftPosition(this.numDays);
    lines[linesIndex++] = startY;

    for (int day = 0; day <= this.numDays; day++) {
      x = computeDayLeftPosition(day);
      lines[linesIndex++] = x;
      lines[linesIndex++] = startY;
      lines[linesIndex++] = x;
      lines[linesIndex++] = stopY;
    }

    p.setAntiAlias(false);
    canvas.drawLines(lines, 0, linesIndex, p);
    p.setStyle(Style.FILL);

    int y = DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN;
    int lastDay = firstDay + numDays - 1;
    final ArrayList<Event> events = allDayEvents;
    int numEvents = events.size();
    // Whether or not we should draw the more events text
    boolean hasMoreEvents = false;
    // size of the allDay area
    float drawHeight = allDayHeight;
    // max number of events being drawn in one day of the allday area
    float numRectangles = maxAlldayEvents;
    // Where to cut off drawn allday events
    int allDayEventClip = DAY_HEADER_HEIGHT + allDayHeight + ALLDAY_TOP_MARGIN;
    // The number of events that weren't drawn in each day
    skippedAllDayEvents = new int[numDays];
    if (maxAlldayEvents > maxUnexpandedAllDayEventCount && !showAllAllDayEvents && animateDayHeight == 0) {
      // We draw one fewer event than will fit so that more events text can be drawn
      numRectangles = maxUnexpandedAllDayEventCount - 1;
      // We also clip the events above the more events text
      allDayEventClip -= MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
      hasMoreEvents = true;
    } else if (animateDayHeight != 0) {
      // clip at the end of the animating space
      allDayEventClip = DAY_HEADER_HEIGHT + animateDayHeight + ALLDAY_TOP_MARGIN;
    }

    int alpha = eventTextPaint.getAlpha();
    eventTextPaint.setAlpha(eventsAlpha);

    for (int i = 0; i < numEvents; i++) {
      Event event = events.get(i);
      long startDay = event.getStartDay();
      long endDay = event.getEndDay();
      if (startDay > lastDay || endDay < firstDay) {
        continue;
      }
      if (startDay < firstDay) {
        startDay = firstDay;
      }
      if (endDay > lastDay) {
        endDay = lastDay;
      }
      long startIndex = startDay - firstDay;
      long endIndex = endDay - firstDay;
      float height = maxAlldayEvents > maxUnexpandedAllDayEventCount ? animateDayEventHeight : drawHeight / numRectangles;

      // Prevent a single event from getting too big
      if (height > MAX_HEIGHT_OF_ONE_ALLDAY_EVENT) {
        height = MAX_HEIGHT_OF_ONE_ALLDAY_EVENT;
      }

      // Leave a one-pixel space between the vertical day lines and the event rectangle.
      event.left = computeDayLeftPosition((int) startIndex);
      event.right = computeDayLeftPosition((int) (endIndex + 1)) - DAY_GAP;
      event.top = y + height * event.getColumn();
      event.bottom = event.top + height - ALL_DAY_EVENT_RECT_BOTTOM_MARGIN;
      if (maxAlldayEvents > maxUnexpandedAllDayEventCount) {
        // check if we should skip this event. We skip if it starts after the clip bound or ends after the skip bound and we're not animating.
        if (event.top >= allDayEventClip) {
          incrementSkipCount(skippedAllDayEvents, startIndex, endIndex);
          continue;
        } else if (event.bottom > allDayEventClip) {
          if (hasMoreEvents) {
            incrementSkipCount(skippedAllDayEvents, startIndex, endIndex);
            continue;
          }
          event.bottom = allDayEventClip;
        }
      }

      Rect r = drawEventRect(event, canvas, p, eventTextPaint, (int) event.top, (int) event.bottom);
      setupAllDayTextRect(r);
      StaticLayout layout = getEventLayout(allDayLayouts, i, event, eventTextPaint, r);
      drawEventText(layout, r, canvas, r.top, r.bottom, true);

      // Check if this all-day event intersects the selected day
      if (selectionAllday && computeSelectedEvents) {
        if (startDay <= selectionDay && endDay >= selectionDay) {
          selectedEvents.add(event);
        }
      }
    }
    eventTextPaint.setAlpha(alpha);

    if (moreAlldayEventsTextAlpha != 0 && skippedAllDayEvents != null) {
      // If the more allday text should be visible, draw it.
      alpha = p.getAlpha();
      p.setAlpha(eventsAlpha);
      p.setColor(moreAlldayEventsTextAlpha << 24 & moreEventsTextColor);
      for (int i = 0; i < skippedAllDayEvents.length; i++) {
        if (skippedAllDayEvents[i] > 0) {
          drawMoreAlldayEvents(canvas, skippedAllDayEvents[i], i, p);
        }
      }
      p.setAlpha(alpha);
    }

    if (selectionAllday) {
      // Compute the neighbors for the list of all-day events that intersect the selected day.
      computeAllDayNeighbors();

      // Set the selection position to zero so that when we move down to the normal event area, we will highlight the topmost event.
      saveSelectionPosition(0f, 0f, 0f, 0f);
    }
  }

  // Helper method for counting the number of allday events skipped on each day
  private void incrementSkipCount(int[] counts, long startIndex, long endIndex) {
    if (counts == null || startIndex < 0 || endIndex > counts.length) {
      return;
    }
    for (long i = startIndex; i <= endIndex; i++) {
      counts[((int) i)]++;
    }
  }

  // Draws the "box +n" text for hidden allday events
  protected void drawMoreAlldayEvents(Canvas canvas, int remainingEvents, int day, Paint p) {
    int x = computeDayLeftPosition(day) + EVENT_ALL_DAY_TEXT_LEFT_MARGIN;
    int y = (int) (allDayHeight - .5f * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT - .5f * EVENT_SQUARE_WIDTH + DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN);
    Rect r = rect;
    r.top = y;
    r.left = x;
    r.bottom = y + EVENT_SQUARE_WIDTH;
    r.right = x + EVENT_SQUARE_WIDTH;
    p.setColor(moreEventsTextColor);
    p.setStrokeWidth(EVENT_RECT_STROKE_WIDTH);
    p.setStyle(Style.STROKE);
    p.setAntiAlias(false);
    canvas.drawRect(r, p);
    p.setAntiAlias(true);
    p.setStyle(Style.FILL);
    p.setTextSize(EVENT_TEXT_FONT_SIZE);
    String text = resources.getQuantityString(R.plurals.month_more_events, remainingEvents);
    y += EVENT_SQUARE_WIDTH;
    x += EVENT_SQUARE_WIDTH + EVENT_LINE_PADDING;
    canvas.drawText(String.format(text, remainingEvents), x, y, p);
  }

  private void computeAllDayNeighbors() {
    int len = selectedEvents.size();
    if (len == 0 || selectedEvent != null) {
      return;
    }

    // First, clear all the links
    for (Event event : selectedEvents) {
      event.nextUp = null;
      event.nextDown = null;
      event.nextLeft = null;
      event.nextRight = null;
    }

    // For each event in the selected event list "selectedEvents", find its neighbors in the up and down directions. This could be done
    // more efficiently by sorting on the Event.getColumn() field, but the list is expected to be very small.

    // Find the event in the same row as the previously selected all-day event, if any.
    int startPosition = -1;
//    if (prevSelectedEvent != null && prevSelectedEvent.drawAsAllday()) {
//      startPosition = prevSelectedEvent.getColumn();
//    }
    int maxPosition = -1;
    Event startEvent = null;
    Event maxPositionEvent = null;
    for (int ii = 0; ii < len; ii++) {
      Event ev = selectedEvents.get(ii);
      int position = ev.getColumn();
      if (position == startPosition) {
        startEvent = ev;
      } else if (position > maxPosition) {
        maxPositionEvent = ev;
        maxPosition = position;
      }
      for (int jj = 0; jj < len; jj++) {
        if (jj != ii) {
          Event neighbor = selectedEvents.get(jj);
          int neighborPosition = neighbor.getColumn();
          if (neighborPosition == position - 1) {
            ev.nextUp = neighbor;
          } else if (neighborPosition == position + 1) {
            ev.nextDown = neighbor;
          }
        }
      }
    }
    if (startEvent != null) {
      setSelectedEvent(startEvent);
    } else {
      setSelectedEvent(maxPositionEvent);
    }
  }

  private void drawEvents(int date, int dayIndex, int top, Canvas canvas, Paint p) {
    Paint eventTextPaint = this.eventTextPaint;
    int left = computeDayLeftPosition(dayIndex) + 1;
    int cellWidth = computeDayLeftPosition(dayIndex + 1) - left + 1;
    int cellHeight = DayView.cellHeight;

    // Use the selected hour as the selection region
    Rect selectionArea = selectionRect;
    selectionArea.top = top + selectionHour * (cellHeight + HOUR_GAP);
    selectionArea.bottom = selectionArea.top + cellHeight;
    selectionArea.left = left;
    selectionArea.right = selectionArea.left + cellWidth;

    final ArrayList<Event> events = this.events;
    int numEvents = events.size();
    EventGeometry geometry = eventGeometry;

    final int viewEndY = viewStartY + viewHeight - DAY_HEADER_HEIGHT - allDayHeight;

    int alpha = eventTextPaint.getAlpha();
    eventTextPaint.setAlpha(eventsAlpha);
    for (int i = 0; i < numEvents; i++) {
      Event event = events.get(i);
      if (!geometry.computeEventRect(date, left, top, cellWidth, event)) {
        continue;
      }

      // Don't draw it if it is not visible
      if (event.bottom < viewStartY || event.top > viewEndY) {
        continue;
      }

      if (date == selectionDay && !selectionAllday && computeSelectedEvents && geometry.eventIntersectsSelection(event, selectionArea)) {
        selectedEvents.add(event);
      }

      Rect r = drawEventRect(event, canvas, p, eventTextPaint, viewStartY, viewEndY);
      setupTextRect(r);

      // Don't draw text if it is not visible
      if (r.top > viewEndY || r.bottom < viewStartY) {
        continue;
      }
      StaticLayout layout = getEventLayout(layouts, i, event, eventTextPaint, r);
      // TODO: not sure why we are 4 pixels off
      drawEventText(layout, r, canvas, viewStartY + 4, viewStartY + viewHeight - DAY_HEADER_HEIGHT - allDayHeight, false);
    }
    eventTextPaint.setAlpha(alpha);

    if (date == selectionDay && !selectionAllday && isFocused() && selectionMode != SELECTION_HIDDEN) {
      computeNeighbors();
    }
  }

  // Computes the "nearest" neighbor event in four directions (left, right, up, down) for each of the events in the selectedEvents array.
  private void computeNeighbors() {
    int len = selectedEvents.size();
    if (len == 0 || selectedEvent != null) {
      return;
    }

    // First, clear all the links
    for (Event event : selectedEvents) {
      event.nextUp = null;
      event.nextDown = null;
      event.nextLeft = null;
      event.nextRight = null;
    }

    Event startEvent = selectedEvents.get(0);
    int startEventDistance1 = 100000; // any large number
    int startEventDistance2 = 100000; // any large number
    int prevLocation = FROM_NONE;
    int prevTop;
    int prevBottom;
    int prevLeft;
    int prevRight;
    int prevCenter = 0;
    Rect box = getCurrentSelectionPosition();
    if (prevSelectedEvent != null) {
      prevTop = (int) prevSelectedEvent.top;
      prevBottom = (int) prevSelectedEvent.bottom;
      prevLeft = (int) prevSelectedEvent.left;
      prevRight = (int) prevSelectedEvent.right;
      // Check if the previously selected event intersects the previous
      // selection box. (The previously selected event may be from a
      // much older selection box.)
      if (prevTop >= prevBox.bottom || prevBottom <= prevBox.top || prevRight <= prevBox.left || prevLeft >= prevBox.right) {
        prevSelectedEvent = null;
        prevTop = prevBox.top;
        prevBottom = prevBox.bottom;
        prevLeft = prevBox.left;
        prevRight = prevBox.right;
      } else {
        // Clip the top and bottom to the previous selection box.
        if (prevTop < prevBox.top) {
          prevTop = prevBox.top;
        }
        if (prevBottom > prevBox.bottom) {
          prevBottom = prevBox.bottom;
        }
      }
    } else {
      // Just use the previously drawn selection box
      prevTop = prevBox.top;
      prevBottom = prevBox.bottom;
      prevLeft = prevBox.left;
      prevRight = prevBox.right;
    }

    // Figure out where we came from and compute the center of that area.
    if (prevLeft >= box.right) {
      // The previously selected event was to the right of us.
      prevLocation = FROM_RIGHT;
      prevCenter = (prevTop + prevBottom) / 2;
    } else if (prevRight <= box.left) {
      // The previously selected event was to the left of us.
      prevLocation = FROM_LEFT;
      prevCenter = (prevTop + prevBottom) / 2;
    } else if (prevBottom <= box.top) {
      // The previously selected event was above us.
      prevLocation = FROM_ABOVE;
      prevCenter = (prevLeft + prevRight) / 2;
    } else if (prevTop >= box.bottom) {
      // The previously selected event was below us.
      prevLocation = FROM_BELOW;
      prevCenter = (prevLeft + prevRight) / 2;
    }

    // For each event in the selected event list "selectedEvents", search
    // all the other events in that list for the nearest neighbor in 4
    // directions.
    for (int ii = 0; ii < len; ii++) {
      Event ev = selectedEvents.get(ii);

      long startTime = ev.getStartTime();
//      int endTime = ev.endTime;
      int left = (int) ev.left;
      int right = (int) ev.right;
      int top = (int) ev.top;
      if (top < box.top) {
        top = box.top;
      }
      int bottom = (int) ev.bottom;
      if (bottom > box.bottom) {
        bottom = box.bottom;
      }
//            if (false) {
//                int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL
//                        | DateUtils.FORMAT_CAP_NOON_MIDNIGHT;
//                if (DateFormat.is24HourFormat(context)) {
//                    flags |= DateUtils.FORMAT_24HOUR;
//                }
//                String timeRange = DateUtils.formatDateRange(context, ev.startMillis,
//                        ev.endMillis, flags);
//            }
      int upDistanceMin = 10000; // any large number
      int downDistanceMin = 10000; // any large number
      int leftDistanceMin = 10000; // any large number
      int rightDistanceMin = 10000; // any large number
      Event upEvent = null;
      Event downEvent = null;
      Event leftEvent = null;
      Event rightEvent = null;

      // Pick the starting event closest to the previously selected event,
      // if any. distance1 takes precedence over distance2.
      int distance1 = 0;
      int distance2 = 0;
      if (prevLocation == FROM_ABOVE) {
        if (left >= prevCenter) {
          distance1 = left - prevCenter;
        } else if (right <= prevCenter) {
          distance1 = prevCenter - right;
        }
        distance2 = top - prevBottom;
      } else if (prevLocation == FROM_BELOW) {
        if (left >= prevCenter) {
          distance1 = left - prevCenter;
        } else if (right <= prevCenter) {
          distance1 = prevCenter - right;
        }
        distance2 = prevTop - bottom;
      } else if (prevLocation == FROM_LEFT) {
        if (bottom <= prevCenter) {
          distance1 = prevCenter - bottom;
        } else if (top >= prevCenter) {
          distance1 = top - prevCenter;
        }
        distance2 = left - prevRight;
      } else if (prevLocation == FROM_RIGHT) {
        if (bottom <= prevCenter) {
          distance1 = prevCenter - bottom;
        } else if (top >= prevCenter) {
          distance1 = top - prevCenter;
        }
        distance2 = prevLeft - right;
      }
      if (distance1 < startEventDistance1
          || (distance1 == startEventDistance1 && distance2 < startEventDistance2)) {
        startEvent = ev;
        startEventDistance1 = distance1;
        startEventDistance2 = distance2;
      }

      // For each neighbor, figure out if it is above or below or left
      // or right of me and compute the distance.
      for (int jj = 0; jj < len; jj++) {
        if (jj == ii) {
          continue;
        }
        Event neighbor = selectedEvents.get(jj);
        int neighborLeft = (int) neighbor.left;
        int neighborRight = (int) neighbor.right;
//        if (neighbor.endTime <= startTime) {
//          // This neighbor is entirely above me.
//          // If we overlap the same column, then compute the distance.
//          if (neighborLeft < right && neighborRight > left) {
//            int distance = startTime - neighbor.endTime;
//            if (distance < upDistanceMin) {
//              upDistanceMin = distance;
//              upEvent = neighbor;
//            } else if (distance == upDistanceMin) {
//              int center = (left + right) / 2;
//              int currentDistance = 0;
//              int currentLeft = (int) upEvent.left;
//              int currentRight = (int) upEvent.right;
//              if (currentRight <= center) {
//                currentDistance = center - currentRight;
//              } else if (currentLeft >= center) {
//                currentDistance = currentLeft - center;
//              }
//
//              int neighborDistance = 0;
//              if (neighborRight <= center) {
//                neighborDistance = center - neighborRight;
//              } else if (neighborLeft >= center) {
//                neighborDistance = neighborLeft - center;
//              }
//              if (neighborDistance < currentDistance) {
//                upDistanceMin = distance;
//                upEvent = neighbor;
//              }
//            }
//          }
//        } else if (neighbor.startTime >= endTime) {
//          // This neighbor is entirely below me.
//          // If we overlap the same column, then compute the distance.
//          if (neighborLeft < right && neighborRight > left) {
//            int distance = neighbor.startTime - endTime;
//            if (distance < downDistanceMin) {
//              downDistanceMin = distance;
//              downEvent = neighbor;
//            } else if (distance == downDistanceMin) {
//              int center = (left + right) / 2;
//              int currentDistance = 0;
//              int currentLeft = (int) downEvent.left;
//              int currentRight = (int) downEvent.right;
//              if (currentRight <= center) {
//                currentDistance = center - currentRight;
//              } else if (currentLeft >= center) {
//                currentDistance = currentLeft - center;
//              }
//
//              int neighborDistance = 0;
//              if (neighborRight <= center) {
//                neighborDistance = center - neighborRight;
//              } else if (neighborLeft >= center) {
//                neighborDistance = neighborLeft - center;
//              }
//              if (neighborDistance < currentDistance) {
//                downDistanceMin = distance;
//                downEvent = neighbor;
//              }
//            }
//          }
//        }

        if (neighborLeft >= right) {
          // This neighbor is entirely to the right of me.
          // Take the closest neighbor in the y direction.
          int center = (top + bottom) / 2;
          int distance = 0;
          int neighborBottom = (int) neighbor.bottom;
          int neighborTop = (int) neighbor.top;
          if (neighborBottom <= center) {
            distance = center - neighborBottom;
          } else if (neighborTop >= center) {
            distance = neighborTop - center;
          }
          if (distance < rightDistanceMin) {
            rightDistanceMin = distance;
            rightEvent = neighbor;
          } else if (distance == rightDistanceMin) {
            // Pick the closest in the x direction
            int neighborDistance = neighborLeft - right;
            int currentDistance = (int) rightEvent.left - right;
            if (neighborDistance < currentDistance) {
              rightDistanceMin = distance;
              rightEvent = neighbor;
            }
          }
        } else if (neighborRight <= left) {
          // This neighbor is entirely to the left of me.
          // Take the closest neighbor in the y direction.
          int center = (top + bottom) / 2;
          int distance = 0;
          int neighborBottom = (int) neighbor.bottom;
          int neighborTop = (int) neighbor.top;
          if (neighborBottom <= center) {
            distance = center - neighborBottom;
          } else if (neighborTop >= center) {
            distance = neighborTop - center;
          }
          if (distance < leftDistanceMin) {
            leftDistanceMin = distance;
            leftEvent = neighbor;
          } else if (distance == leftDistanceMin) {
            // Pick the closest in the x direction
            int neighborDistance = left - neighborRight;
            int currentDistance = left - (int) leftEvent.right;
            if (neighborDistance < currentDistance) {
              leftDistanceMin = distance;
              leftEvent = neighbor;
            }
          }
        }
      }
      ev.nextUp = upEvent;
      ev.nextDown = downEvent;
      ev.nextLeft = leftEvent;
      ev.nextRight = rightEvent;
    }
    setSelectedEvent(startEvent);
  }

  private Rect drawEventRect(Event event, Canvas canvas, Paint p, Paint eventTextPaint, int visibleTop, int visibleBot) {
    // Draw the Event Rect
    Rect r = rect;
    r.top = Math.max((int) event.top + EVENT_RECT_TOP_MARGIN, visibleTop);
    r.bottom = Math.min((int) event.bottom - EVENT_RECT_BOTTOM_MARGIN, visibleBot);
    r.left = (int) event.left + EVENT_RECT_LEFT_MARGIN;
    r.right = (int) event.right;

    int color;
    if (event == clickedEvent) {
      color = clickedColor;
    } else {
//      color = event.color;
    }

//    switch (event.selfAttendeeStatus) {
//      case Attendees.ATTENDEE_STATUS_INVITED:
//        if (event != clickedEvent) {
//          p.setStyle(Style.STROKE);
//        }
//        break;
//      case Attendees.ATTENDEE_STATUS_DECLINED:
//        if (event != clickedEvent) {
//          color = Utils.getDeclinedColorFromColor(color);
//        }
//      case Attendees.ATTENDEE_STATUS_NONE: // Your own events
//      case Attendees.ATTENDEE_STATUS_ACCEPTED:
//      case Attendees.ATTENDEE_STATUS_TENTATIVE:
//      default:
//        p.setStyle(Style.FILL_AND_STROKE);
//        break;
//    }

    p.setAntiAlias(false);

    int floorHalfStroke = (int) Math.floor(EVENT_RECT_STROKE_WIDTH / 2.0f);
    int ceilHalfStroke = (int) Math.ceil(EVENT_RECT_STROKE_WIDTH / 2.0f);
    r.top = Math.max((int) event.top + EVENT_RECT_TOP_MARGIN + floorHalfStroke, visibleTop);
    r.bottom = Math.min((int) event.bottom - EVENT_RECT_BOTTOM_MARGIN - ceilHalfStroke, visibleBot);
    r.left += floorHalfStroke;
    r.right -= ceilHalfStroke;
    p.setStrokeWidth(EVENT_RECT_STROKE_WIDTH);
//    p.setColor(color);
    int alpha = p.getAlpha();
    p.setAlpha(eventsAlpha);
    canvas.drawRect(r, p);
    p.setAlpha(alpha);
    p.setStyle(Style.FILL);

    // If this event is selected, then use the selection color
    if (selectedEvent == event && clickedEvent != null) {
      boolean paintIt = false;
      color = 0;
      if (selectionMode == SELECTION_PRESSED) {
        // Also, remember the last selected event that we drew
        prevSelectedEvent = event;
        color = pressedColor;
        paintIt = true;
      } else if (selectionMode == SELECTION_SELECTED) {
        // Also, remember the last selected event that we drew
        prevSelectedEvent = event;
        color = pressedColor;
        paintIt = true;
      }

      if (paintIt) {
        p.setColor(color);
        canvas.drawRect(r, p);
      }
      p.setAntiAlias(true);
    }

    // Draw cal color square border
    // r.top = (int) event.top + CALENDAR_COLOR_SQUARE_V_OFFSET;
    // r.left = (int) event.left + CALENDAR_COLOR_SQUARE_H_OFFSET;
    // r.bottom = r.top + CALENDAR_COLOR_SQUARE_SIZE + 1;
    // r.right = r.left + CALENDAR_COLOR_SQUARE_SIZE + 1;
    // p.setColor(0xFFFFFFFF);
    // canvas.drawRect(r, p);

    // Draw cal color
    // r.top++;
    // r.left++;
    // r.bottom--;
    // r.right--;
    // p.setColor(event.color);
    // canvas.drawRect(r, p);

    // Setup rect for drawEventText which follows
    r.top = (int) event.top + EVENT_RECT_TOP_MARGIN;
    r.bottom = (int) event.bottom - EVENT_RECT_BOTTOM_MARGIN;
    r.left = (int) event.left + EVENT_RECT_LEFT_MARGIN;
    r.right = (int) event.right - EVENT_RECT_RIGHT_MARGIN;
    return r;
  }

  // This is to replace p.setStyle(Style.STROKE); canvas.drawRect() since it doesn't work well with hardware acceleration
//    private void drawEmptyRect(Canvas canvas, Rect r, int color) {
//        int linesIndex = 0;
//        lines[linesIndex++] = r.left;
//        lines[linesIndex++] = r.top;
//        lines[linesIndex++] = r.right;
//        lines[linesIndex++] = r.top;
//
//        lines[linesIndex++] = r.left;
//        lines[linesIndex++] = r.bottom;
//        lines[linesIndex++] = r.right;
//        lines[linesIndex++] = r.bottom;
//
//        lines[linesIndex++] = r.left;
//        lines[linesIndex++] = r.top;
//        lines[linesIndex++] = r.left;
//        lines[linesIndex++] = r.bottom;
//
//        lines[linesIndex++] = r.right;
//        lines[linesIndex++] = r.top;
//        lines[linesIndex++] = r.right;
//        lines[linesIndex++] = r.bottom;
//        paint.setColor(color);
//        canvas.drawLines(lines, 0, linesIndex, paint);
//    }

  // Sanitize a string before passing it to drawText or else we get little
  // squares. For newlines and tabs before a comma, delete the character.
  // Otherwise, just replace them with a space.
  private String drawTextSanitizer(String string, int maxEventTextLen) {
    Matcher m = drawTextSanitizerFilter.matcher(string);
    string = m.replaceAll(",");

    if (maxEventTextLen <= 0) {
      string = "";
    } else if (string.length() > maxEventTextLen) {
      string = string.substring(0, maxEventTextLen);
    }

    return string.replace('\n', ' ');
  }

  private void drawEventText(StaticLayout eventLayout, Rect rect, Canvas canvas, int top, int bottom, boolean center) {
    // drawEmptyRect(canvas, rect, 0xFFFF00FF); // for debugging

    int width = rect.right - rect.left;
    int height = rect.bottom - rect.top;

    // If the rectangle is too small for text, then return
    if (eventLayout == null || width < MIN_CELL_WIDTH_FOR_TEXT) {
      return;
    }

    int totalLineHeight = 0;
    int lineCount = eventLayout.getLineCount();
    for (int i = 0; i < lineCount; i++) {
      int lineBottom = eventLayout.getLineBottom(i);
      if (lineBottom <= height) {
        totalLineHeight = lineBottom;
      } else {
        break;
      }
    }

    if (totalLineHeight == 0 || rect.top > bottom || rect.top + totalLineHeight < top) {
      return;
    }

    // Use a StaticLayout to format the string.
    canvas.save();
    //  canvas.translate(rect.left, rect.top + (rect.bottom - rect.top / 2));
    int padding = center ? (rect.bottom - rect.top - totalLineHeight) / 2 : 0;
    canvas.translate(rect.left, rect.top + padding);
    rect.left = 0;
    rect.right = width;
    rect.top = 0;
    rect.bottom = totalLineHeight;

    // There's a bug somewhere. If this rect is outside of a previous
    // cliprect, this becomes a no-op. What happens is that the text draw
    // past the event rect. The current fix is to not draw the staticLayout
    // at all if it is completely out of bound.
    canvas.clipRect(rect);
    eventLayout.draw(canvas);
    canvas.restore();
  }

  private void updateEventDetails() {
    if (selectedEvent == null || selectionMode == SELECTION_HIDDEN || selectionMode == SELECTION_LONGPRESS) {
      popup.dismiss();
      return;
    }
    if (lastPopupEventId == selectedEvent.getId()) {
      return;
    }

    lastPopupEventId = selectedEvent.getId();

    // Remove any outstanding callbacks to dismiss the popup.
    handler.removeCallbacks(dismissPopup);

    Event event = selectedEvent;
    TextView titleView = (TextView) popupView.findViewById(R.id.event_title);
    titleView.setText(event.getName());

    ImageView imageView = (ImageView) popupView.findViewById(R.id.reminder_icon);
//    imageView.setVisibility(event.hasAlarm ? View.VISIBLE : View.GONE);

    imageView = (ImageView) popupView.findViewById(R.id.repeat_icon);
//    imageView.setVisibility(event.isRepeating ? View.VISIBLE : View.GONE);

    int flags;
//    if (event.allDay) {
//      flags = DateUtils.FORMAT_UTC | DateUtils.FORMAT_SHOW_DATE
//              | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_ALL;
//    } else {
      flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE
              | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_ALL
              | DateUtils.FORMAT_CAP_NOON_MIDNIGHT;
//    }
    if (DateFormat.is24HourFormat(context)) {
      flags |= DateUtils.FORMAT_24HOUR;
    }
//    String timeRange = Utils.formatDateRange(context, event.startMillis, event.endMillis, flags);
    TextView timeView = (TextView) popupView.findViewById(R.id.time);
//    timeView.setText(timeRange);

    TextView whereView = (TextView) popupView.findViewById(R.id.where);
    final boolean empty = TextUtils.isEmpty(event.getLocation());
    whereView.setVisibility(empty ? View.GONE : View.VISIBLE);
    if (!empty) {
      whereView.setText(event.getLocation());
    }

    popup.showAtLocation(this, Gravity.BOTTOM | Gravity.LEFT, hoursWidth, 5);
    handler.postDelayed(dismissPopup, POPUP_DISMISS_DELAY);
  }

  // The following routines are called from the parent activity when certain
  // touch events occur.
  private void doDown(MotionEvent ev) {
    touchMode = TOUCH_MODE_DOWN;
    viewStartX = 0;
    onFlingCalled = false;
    handler.removeCallbacks(continueScroll);
    int x = (int) ev.getX();
    int y = (int) ev.getY();

    // Save selection information: we use setSelectionFromPosition to find the selected event
    // in order to show the "clicked" color. But since it is also setting the selected info
    // for new events, we need to restore the old info after calling the function.
    Event oldSelectedEvent = selectedEvent;
    int oldSelectionDay = selectionDay;
    int oldSelectionHour = selectionHour;
    if (setSelectionFromPosition(x, y, false)) {
      // If a time was selected (a blue selection box is visible) and the click location
      // is in the selected time, do not show a click on an event to prevent a situation
      // of both a selection and an event are clicked when they overlap.
      boolean pressedSelected = (selectionMode != SELECTION_HIDDEN) && oldSelectionDay == selectionDay && oldSelectionHour == selectionHour;
      if (!pressedSelected && selectedEvent != null) {
        savedClickedEvent = selectedEvent;
        downTouchTime = System.currentTimeMillis();
        postDelayed(setClick, onDownDelay);
      } else {
        eventClickCleanup();
      }
    }
    selectedEvent = oldSelectedEvent;
    selectionDay = oldSelectionDay;
    selectionHour = oldSelectionHour;
    invalidate();
  }

  // Kicks off all the animations when the expand allday area is tapped
  private void doExpandAllDayClick() {
    showAllAllDayEvents = !showAllAllDayEvents;

    ValueAnimator.setFrameDelay(0);

    // Determine the starting height
    if (animateDayHeight == 0) {
      animateDayHeight = showAllAllDayEvents ? allDayHeight - (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT : allDayHeight;
    }
    // Cancel current animations
    cancellingAnimations = true;
    if (alldayAnimator != null) {
      alldayAnimator.cancel();
    }
    if (alldayEventAnimator != null) {
      alldayEventAnimator.cancel();
    }
    if (moreAlldayEventsAnimator != null) {
      moreAlldayEventsAnimator.cancel();
    }
    cancellingAnimations = false;
    // get new animators
    alldayAnimator = getAllDayAnimator();
    alldayEventAnimator = getAllDayEventAnimator();
    moreAlldayEventsAnimator = ObjectAnimator.ofInt(this, "moreAllDayEventsTextAlpha", showAllAllDayEvents ? MORE_EVENTS_MAX_ALPHA : 0, showAllAllDayEvents ? 0 : MORE_EVENTS_MAX_ALPHA);

    // Set up delays and start the animators
    alldayAnimator.setStartDelay(showAllAllDayEvents ? ANIMATION_SECONDARY_DURATION : 0);
    alldayAnimator.start();
    moreAlldayEventsAnimator.setStartDelay(showAllAllDayEvents ? 0 : ANIMATION_DURATION);
    moreAlldayEventsAnimator.setDuration(ANIMATION_SECONDARY_DURATION);
    moreAlldayEventsAnimator.start();
    if (alldayEventAnimator != null) {
      // This is the only animator that can return null, so check it
      alldayEventAnimator.setStartDelay(showAllAllDayEvents ? ANIMATION_SECONDARY_DURATION : 0);
      alldayEventAnimator.start();
    }
  }

  /**
   * Figures out the initial heights for allDay events and space when a view is being set up.
   */
  public void initAllDayHeights() {
    if (maxAlldayEvents <= maxUnexpandedAllDayEventCount) {
      return;
    }
    if (showAllAllDayEvents) {
      int maxADHeight = viewHeight - DAY_HEADER_HEIGHT - MIN_HOURS_HEIGHT;
      maxADHeight = Math.min(maxADHeight, (int) (maxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT));
      animateDayEventHeight = maxADHeight / maxAlldayEvents;
    } else {
      animateDayEventHeight = (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
    }
  }

  // Sets up an animator for changing the height of allday events
  private ObjectAnimator getAllDayEventAnimator() {
    // First calculate the absolute max height
    int maxADHeight = viewHeight - DAY_HEADER_HEIGHT - MIN_HOURS_HEIGHT;
    // Now expand to fit but not beyond the absolute max
    maxADHeight = Math.min(maxADHeight, (int) (maxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT));
    // calculate the height of individual events in order to fit
    int fitHeight = maxADHeight / maxAlldayEvents;
    int currentHeight = animateDayEventHeight;
    int desiredHeight = showAllAllDayEvents ? fitHeight : (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
    // if there's nothing to animate just return
    if (currentHeight == desiredHeight) {
      return null;
    }

    // Set up the animator with the calculated values
    ObjectAnimator animator = ObjectAnimator.ofInt(this, "animateDayEventHeight", currentHeight, desiredHeight);
    animator.setDuration(ANIMATION_DURATION);
    return animator;
  }

  // Sets up an animator for changing the height of the allday area
  private ObjectAnimator getAllDayAnimator() {
    // Calculate the absolute max height
    int maxADHeight = viewHeight - DAY_HEADER_HEIGHT - MIN_HOURS_HEIGHT;
    // Find the desired height but don't exceed abs max
    maxADHeight = Math.min(maxADHeight, (int) (maxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT));
    // calculate the current and desired heights
    int currentHeight = animateDayHeight != 0 ? animateDayHeight : allDayHeight;
    int desiredHeight = showAllAllDayEvents ? maxADHeight : (int) (MAX_UNEXPANDED_ALLDAY_HEIGHT - MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT - 1);

    // Set up the animator with the calculated values
    ObjectAnimator animator = ObjectAnimator.ofInt(this, "animateDayHeight", currentHeight, desiredHeight);
    animator.setDuration(ANIMATION_DURATION);

    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        if (!cancellingAnimations) {
          // when finished, set this to 0 to signify not animating
          animateDayHeight = 0;
          useExpandIcon = !showAllAllDayEvents;
        }
        remeasure = true;
        invalidate();
      }
    });
    return animator;
  }

  private void doSingleTapUp(MotionEvent ev) {
    if (!handleActionUp || scrolling) {
      return;
    }

    int x = (int) ev.getX();
    int y = (int) ev.getY();
    int selectedDay = selectionDay;
    int selectedHour = selectionHour;

    if (maxAlldayEvents > maxUnexpandedAllDayEventCount) {
      // check if the tap was in the allday expansion area
      int bottom = firstCell;
      if ((x < hoursWidth && y > DAY_HEADER_HEIGHT && y < DAY_HEADER_HEIGHT + allDayHeight)
          || (!showAllAllDayEvents && animateDayHeight == 0 && y < bottom &&
              y >= bottom - MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT)) {
        doExpandAllDayClick();
        return;
      }
    }

    boolean validPosition = setSelectionFromPosition(x, y, false);
    if (!validPosition) {
      if (y < DAY_HEADER_HEIGHT) {
        Time selectedTime = new Time(baseDate);
        selectedTime.setJulianDay(selectionDay);
        selectedTime.hour = selectionHour;
        selectedTime.normalize(true /* ignore isDst */);
        calendarController.sendEvent(EventType.GO_TO, null, null, selectedTime, -1, ViewType.DAY, CalendarController.EXTRA_GOTO_DATE, null, null);
      }
      return;
    }

    boolean hasSelection = selectionMode != SELECTION_HIDDEN;
    boolean pressedSelected = (hasSelection || touchExplorationEnabled) && selectedDay == selectionDay && selectedHour == selectionHour;

    if (pressedSelected && savedClickedEvent == null) {
      // If the tap is on an already selected hour slot, then create a new
      // event
      long extraLong = 0;
      if (selectionAllday) {
        extraLong = CalendarController.EXTRA_CREATE_ALL_DAY;
      }
      selectionMode = SELECTION_SELECTED;
      calendarController.sendEventRelatedEventWithExtra(EventType.CREATE_EVENT, -1, getSelectedTimeInMillis(), 0, (int) ev.getRawX(), (int) ev.getRawY(), extraLong, -1);
    } else if (selectedEvent != null) {
      // If the tap is on an event, launch the "View event" view
      selectionMode = SELECTION_HIDDEN;

      int yLocation = (int) ((selectedEvent.top + selectedEvent.bottom) / 2);
      // Y location is affected by the position of the event in the scrolling
      // view (viewStartY) and the presence of all day events (firstCell)
//      if (!selectedEvent.allDay) {
        yLocation += (firstCell - viewStartY);
//      }
      clickedYLocation = yLocation;
      long clearDelay = (CLICK_DISPLAY_DURATION + onDownDelay) - (System.currentTimeMillis() - downTouchTime);
      if (clearDelay > 0) {
        this.postDelayed(clearClick, clearDelay);
      } else {
        this.post(clearClick);
      }
    } else {
      // Select time
      Time startTime = new Time(baseDate);
      startTime.setJulianDay(selectionDay);
      startTime.hour = selectionHour;
      startTime.normalize(true /* ignore isDst */);

      Time endTime = new Time(startTime);
      endTime.hour++;

      selectionMode = SELECTION_SELECTED;
      calendarController.sendEvent(EventType.GO_TO, startTime, endTime, -1, ViewType.CURRENT, CalendarController.EXTRA_GOTO_TIME, null, null);
    }
    invalidate();
  }

  private void doLongPress(MotionEvent ev) {
    eventClickCleanup();
    if (scrolling) {
      return;
    }

    // Scale gesture in progress
    if (startingSpanY != 0) {
      return;
    }

    int x = (int) ev.getX();
    int y = (int) ev.getY();

    boolean validPosition = setSelectionFromPosition(x, y, false);
    if (!validPosition) {
      // return if the touch wasn't on an area of concern
      return;
    }

    selectionMode = SELECTION_LONGPRESS;
    invalidate();
    performLongClick();
  }

  private void doScroll(MotionEvent e1, MotionEvent e2, float deltaX, float deltaY) {
    cancelAnimation();
    if (startingScroll) {
      initialScrollX = 0;
      initialScrollY = 0;
      startingScroll = false;
    }

    initialScrollX += deltaX;
    initialScrollY += deltaY;
    int distanceX = (int) initialScrollX;
    int distanceY = (int) initialScrollY;

    final float focusY = getAverageY(e2);
    if (recalCenterHour) {
      // Calculate the hour that correspond to the average of the Y touch points
      gestureCenterHour = (viewStartY + focusY - DAY_HEADER_HEIGHT - allDayHeight) / (cellHeight + DAY_GAP);
      recalCenterHour = false;
    }

    // If we haven't figured out the predominant scroll direction yet, then do it now.
    if (touchMode == TOUCH_MODE_DOWN) {
      int absDistanceX = Math.abs(distanceX);
      int absDistanceY = Math.abs(distanceY);
      scrollStartY = viewStartY;
      previousDirection = 0;

      if (absDistanceX > absDistanceY) {
        int slopFactor = scaleGestureDetector.isInProgress() ? 20 : 2;
        if (absDistanceX > scaledPagingTouchSlop * slopFactor) {
          touchMode = TOUCH_MODE_HSCROLL;
          viewStartX = distanceX;
          initNextView(-viewStartX);
        }
      } else {
        touchMode = TOUCH_MODE_VSCROLL;
      }
    } else if ((touchMode & TOUCH_MODE_HSCROLL) != 0) {
      // We are already scrolling horizontally, so check if we changed the direction of scrolling so that the other week is now visible.
      viewStartX = distanceX;
      if (distanceX != 0) {
        int direction = (distanceX > 0) ? 1 : -1;
        if (direction != previousDirection) {
          // The user has switched the direction of scrolling so re-init the next view
          initNextView(-viewStartX);
          previousDirection = direction;
        }
      }
    }

    if ((touchMode & TOUCH_MODE_VSCROLL) != 0) {
      // Calculate the top of the visible region in the calendar grid.
      // Increasing/decrease this will scroll the calendar grid up/down.
      viewStartY = (int) ((gestureCenterHour * (cellHeight + DAY_GAP)) - focusY + DAY_HEADER_HEIGHT + allDayHeight);

      // If dragging while already at the end, do a glow
      final int pulledToY = (int) (scrollStartY + deltaY);
      if (pulledToY < 0) {
        edgeEffectTop.onPull(deltaY / viewHeight);
        if (!edgeEffectBottom.isFinished()) {
          edgeEffectBottom.onRelease();
        }
      } else if (pulledToY > maxViewStartY) {
        edgeEffectBottom.onPull(deltaY / viewHeight);
        if (!edgeEffectTop.isFinished()) {
          edgeEffectTop.onRelease();
        }
      }

      if (viewStartY < 0) {
        viewStartY = 0;
        recalCenterHour = true;
      } else if (viewStartY > maxViewStartY) {
        viewStartY = maxViewStartY;
        recalCenterHour = true;
      }
      if (recalCenterHour) {
        // Calculate the hour that correspond to the average of the Y touch points
        gestureCenterHour = (viewStartY + focusY - DAY_HEADER_HEIGHT - allDayHeight) / (cellHeight + DAY_GAP);
        recalCenterHour = false;
      }
      computeFirstHour();
    }

    scrolling = true;

    selectionMode = SELECTION_HIDDEN;
    invalidate();
  }

  private float getAverageY(MotionEvent me) {
    int count = me.getPointerCount();
    float focusY = 0;
    for (int i = 0; i < count; i++) {
      focusY += me.getY(i);
    }
    focusY /= count;
    return focusY;
  }

  private void cancelAnimation() {
    Animation in = viewSwitcher.getInAnimation();
    if (in != null) {
      // cancel() doesn't terminate cleanly.
      in.scaleCurrentDuration(0);
    }
    Animation out = viewSwitcher.getOutAnimation();
    if (out != null) {
      // cancel() doesn't terminate cleanly.
      out.scaleCurrentDuration(0);
    }
  }

  private void doFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    cancelAnimation();
    selectionMode = SELECTION_HIDDEN;
    eventClickCleanup();
    onFlingCalled = true;

    if ((touchMode & TOUCH_MODE_HSCROLL) != 0) {
      // Horizontal fling.
      // initNextView(deltaX);
      touchMode = TOUCH_MODE_INITIAL_STATE;
      int deltaX = (int) e2.getX() - (int) e1.getX();
      switchViews(deltaX < 0, viewStartX, viewWidth, velocityX);
      viewStartX = 0;
      return;
    }

    if ((touchMode & TOUCH_MODE_VSCROLL) == 0) {
      return;
    }

    // Vertical fling.
    touchMode = TOUCH_MODE_INITIAL_STATE;
    viewStartX = 0;

    // Continue scrolling vertically
    scrolling = true;
    scroller.fling(0 /* startX */, viewStartY /* startY */, 0 /* velocityX */,
                   (int) -velocityY, 0 /* minX */, 0 /* maxX */, 0 /* minY */,
                   maxViewStartY /* maxY */, overflingDistance, overflingDistance);

    // When flinging down, show a glow when it hits the end only if it wasn't started at the top
    if (velocityY > 0 && viewStartY != 0) {
      callEdgeEffectOnAbsorb = true;
    }
    // When flinging up, show a glow when it hits the end only if it wasn't started at the bottom
    else if (velocityY < 0 && viewStartY != maxViewStartY) {
      callEdgeEffectOnAbsorb = true;
    }
    handler.post(continueScroll);
  }

  private boolean initNextView(int deltaX) {
    // Change the view to the previous day or week
    DayView view = (DayView) viewSwitcher.getNextView();
    Time date = view.baseDate;
    date.set(baseDate);
    boolean switchForward;
    if (deltaX > 0) {
      date.monthDay -= numDays;
      view.setSelectedDay(selectionDay - numDays);
      switchForward = false;
    } else {
      date.monthDay += numDays;
      view.setSelectedDay(selectionDay + numDays);
      switchForward = true;
    }
    date.normalize(true /* ignore isDst */);
    initView(view);
    view.layout(getLeft(), getTop(), getRight(), getBottom());
    view.reloadEvents();
    return switchForward;
  }

  @Override
  public boolean onScaleBegin(ScaleGestureDetector detector) {
    handleActionUp = false;
    float gestureCenterInPixels = detector.getFocusY() - DAY_HEADER_HEIGHT - allDayHeight;
    gestureCenterHour = (viewStartY + gestureCenterInPixels) / (cellHeight + DAY_GAP);
    startingSpanY = Math.max(MIN_Y_SPAN, Math.abs(detector.getCurrentSpanY()));
    cellHeightBeforeScaleGesture = cellHeight;

    return true;
  }

  @Override
  public boolean onScale(ScaleGestureDetector detector) {
    float spanY = Math.max(MIN_Y_SPAN, Math.abs(detector.getCurrentSpanY()));

    cellHeight = (int) (cellHeightBeforeScaleGesture * spanY / startingSpanY);

    if (cellHeight < minCellHeight) {
      // If startingSpanY is too small, even a small increase in the
      // gesture can bump the cellHeight beyond MAX_CELL_HEIGHT
      startingSpanY = spanY;
      cellHeight = minCellHeight;
      cellHeightBeforeScaleGesture = minCellHeight;
    } else if (cellHeight > MAX_CELL_HEIGHT) {
      startingSpanY = spanY;
      cellHeight = MAX_CELL_HEIGHT;
      cellHeightBeforeScaleGesture = MAX_CELL_HEIGHT;
    }

    int gestureCenterInPixels = (int) detector.getFocusY() - DAY_HEADER_HEIGHT - allDayHeight;
    viewStartY = (int) (gestureCenterHour * (cellHeight + DAY_GAP)) - gestureCenterInPixels;
    maxViewStartY = HOUR_GAP + 24 * (cellHeight + HOUR_GAP) - gridAreaHeight;

    if (viewStartY < 0) {
      viewStartY = 0;
      gestureCenterHour = (viewStartY + gestureCenterInPixels) / (float) (cellHeight + DAY_GAP);
    } else if (viewStartY > maxViewStartY) {
      viewStartY = maxViewStartY;
      gestureCenterHour = (viewStartY + gestureCenterInPixels) / (float) (cellHeight + DAY_GAP);
    }
    computeFirstHour();

    remeasure = true;
    invalidate();
    return true;
  }

  @Override
  public void onScaleEnd(ScaleGestureDetector detector) {
    scrollStartY = viewStartY;
    initialScrollY = 0;
    initialScrollX = 0;
    startingSpanY = 0;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    int action = ev.getAction();

    if ((ev.getActionMasked() == MotionEvent.ACTION_DOWN) ||
        (ev.getActionMasked() == MotionEvent.ACTION_UP) ||
        (ev.getActionMasked() == MotionEvent.ACTION_POINTER_UP) ||
        (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN)) {
      recalCenterHour = true;
    }

    if ((touchMode & TOUCH_MODE_HSCROLL) == 0) {
      scaleGestureDetector.onTouchEvent(ev);
    }

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        startingScroll = true;
        int bottom = allDayHeight + DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN;
        touchStartedInAllDayArea = ev.getY() < bottom;
        handleActionUp = true;
        gestureDetector.onTouchEvent(ev);
        return true;

      case MotionEvent.ACTION_MOVE:
        gestureDetector.onTouchEvent(ev);
        return true;

      case MotionEvent.ACTION_UP:
        edgeEffectTop.onRelease();
        edgeEffectBottom.onRelease();
        startingScroll = false;
        gestureDetector.onTouchEvent(ev);
        if (!handleActionUp) {
          handleActionUp = true;
          viewStartX = 0;
          invalidate();
          return true;
        }

        if (onFlingCalled) {
          return true;
        }

        // If we were scrolling, then reset the selected hour so that it
        // is visible.
        if (scrolling) {
          scrolling = false;
          resetSelectedHour();
          invalidate();
        }

        if ((touchMode & TOUCH_MODE_HSCROLL) != 0) {
          touchMode = TOUCH_MODE_INITIAL_STATE;
          if (Math.abs(viewStartX) > horizontalSnapBackThreshold) {
            // The user has gone beyond the threshold so switch views
            switchViews(viewStartX > 0, viewStartX, viewWidth, 0);
            viewStartX = 0;
            return true;
          } else {
            // Not beyond the threshold so invalidate which will cause
            // the view to snap back. Also call recalc() to ensure
            // that we have the correct starting date and title.
            recalc();
            invalidate();
            viewStartX = 0;
          }
        }

        return true;

      // This case isn't expected to happen.
      case MotionEvent.ACTION_CANCEL:
        gestureDetector.onTouchEvent(ev);
        scrolling = false;
        resetSelectedHour();
        return true;

      default:
        return gestureDetector.onTouchEvent(ev) || super.onTouchEvent(ev);
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
    popup.dismiss();
  }

  /**
   * Sets selectionDay and selectionHour based on the (x,y) touch position.
   * If the touch position is not within the displayed grid, then this
   * method returns false.
   *
   * @param x                the x position of the touch
   * @param y                the y position of the touch
   * @param keepOldSelection - do not change the selection info (used for invoking accessibility
   *                         messages)
   * @return true if the touch position is valid
   */
  private boolean setSelectionFromPosition(int x, final int y, boolean keepOldSelection) {

    Event savedEvent = null;
    int savedDay = 0;
    int savedHour = 0;
    boolean savedAllDay = false;
    if (keepOldSelection) {
      // Store selection info and restore it at the end. This way, we can invoke the
      // right accessibility message without affecting the selection.
      savedEvent = selectedEvent;
      savedDay = selectionDay;
      savedHour = selectionHour;
      savedAllDay = selectionAllday;
    }
    if (x < hoursWidth) {
      x = hoursWidth;
    }

    int day = (x - hoursWidth) / (cellWidth + DAY_GAP);
    if (day >= numDays) {
      day = numDays - 1;
    }
    day += firstJulianDay;
    setSelectedDay(day);

    if (y < DAY_HEADER_HEIGHT) {
      return false;
    }

    setSelectedHour(firstHour); /* First fully visible hour */

    if (y < firstCell) {
      selectionAllday = true;
    } else {
      // y is now offset from top of the scrollable region
      int adjustedY = y - firstCell;

      if (adjustedY < firstHourOffset) {
        setSelectedHour(selectionHour - 1); /* In the partially visible hour */
      } else {
        setSelectedHour(selectionHour + (adjustedY - firstHourOffset) / (cellHeight + HOUR_GAP));
      }

      selectionAllday = false;
    }

    findSelectedEvent(x, y);

//        if (selectedEvent != null) {
//            for (Event ev : selectedEvents) {
//                int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL
//                        | DateUtils.FORMAT_CAP_NOON_MIDNIGHT;
//                String timeRange = formatDateRange(context, ev.startMillis, ev.endMillis, flags);
//            }
//        }

    // Restore old values
    if (keepOldSelection) {
      selectedEvent = savedEvent;
      selectionDay = savedDay;
      selectionHour = savedHour;
      selectionAllday = savedAllDay;
    }
    return true;
  }

  private void findSelectedEvent(int x, int y) {
    int date = selectionDay;
    int cellWidth = this.cellWidth;
    ArrayList<Event> events = this.events;
    int numEvents = events.size();
    int left = computeDayLeftPosition(selectionDay - firstJulianDay);
    int top = 0;
    setSelectedEvent(null);

    selectedEvents.clear();
    if (selectionAllday) {
      float yDistance;
      float minYdistance = 10000.0f; // any large number
      Event closestEvent = null;
      float drawHeight = allDayHeight;
      int yOffset = DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN;
      int maxUnexpandedColumn = maxUnexpandedAllDayEventCount;
      if (maxAlldayEvents > maxUnexpandedAllDayEventCount) {
        // Leave a gap for the 'box +n' text
        maxUnexpandedColumn--;
      }
      events = allDayEvents;
      numEvents = events.size();
      for (int i = 0; i < numEvents; i++) {
        Event event = events.get(i);
//        if (!event.drawAsAllday() || (!showAllAllDayEvents && event.getColumn() >= maxUnexpandedColumn)) {
          // Don't check non-allday events or events that aren't shown
//          continue;
//        }

        if (event.getStartDay() <= selectionDay && event.getEndDay() >= selectionDay) {
          float numRectangles = showAllAllDayEvents ? maxAlldayEvents : maxUnexpandedAllDayEventCount;
          float height = drawHeight / numRectangles;
          if (height > MAX_HEIGHT_OF_ONE_ALLDAY_EVENT) {
            height = MAX_HEIGHT_OF_ONE_ALLDAY_EVENT;
          }
          float eventTop = yOffset + height * event.getColumn();
          float eventBottom = eventTop + height;
          if (eventTop < y && eventBottom > y) {
            // If the touch is inside the event rectangle, then
            // add the event.
            selectedEvents.add(event);
            closestEvent = event;
            break;
          } else {
            // Find the closest event
            if (eventTop >= y) {
              yDistance = eventTop - y;
            } else {
              yDistance = y - eventBottom;
            }
            if (yDistance < minYdistance) {
              minYdistance = yDistance;
              closestEvent = event;
            }
          }
        }
      }
      setSelectedEvent(closestEvent);
      return;
    }

    // Adjust y for the scrollable bitmap
    y += viewStartY - firstCell;

    // Use a region around (x,y) for the selection region
    Rect region = rect;
    region.left = x - 10;
    region.right = x + 10;
    region.top = y - 10;
    region.bottom = y + 10;

    EventGeometry geometry = eventGeometry;

    for (int i = 0; i < numEvents; i++) {
      Event event = events.get(i);
      // Compute the event rectangle.
      if (!geometry.computeEventRect(date, left, top, cellWidth, event)) {
        continue;
      }

      // If the event intersects the selection region, then add it to
      // selectedEvents.
      if (geometry.eventIntersectsSelection(event, region)) {
        selectedEvents.add(event);
      }
    }

    // If there are any events in the selected region, then assign the
    // closest one to selectedEvent.
    if (selectedEvents.size() > 0) {
      Event closestEvent = null;
      float minDist = viewWidth + viewHeight; // some large distance
      for (Event event : selectedEvents) {
        float dist = geometry.pointToEvent(x, y, event);
        if (dist < minDist) {
          minDist = dist;
          closestEvent = event;
        }
      }
      setSelectedEvent(closestEvent);

      // Keep the selected hour and day consistent with the selected
      // event. They could be different if we touched on an empty hour
      // slot very close to an event in the previous hour slot. In
      // that case we will select the nearby event.
      long startDay = selectedEvent.getStartDay();
      long endDay = selectedEvent.getEndDay();
      if (selectionDay < startDay) {
        setSelectedDay(startDay);
      } else if (selectionDay > endDay) {
        setSelectedDay(endDay);
      }

      long startHour = selectedEvent.getStartTime() / 60;
      int endHour = 0;
//      if (selectedEvent.startTime < selectedEvent.endTime) {
//        endHour = (selectedEvent.endTime - 1) / 60;
//      } else {
//        endHour = selectedEvent.endTime / 60;
//      }

      if (selectionHour < startHour && selectionDay == startDay) {
        setSelectedHour(startHour);
      } else if (selectionHour > endHour && selectionDay == endDay) {
        setSelectedHour(endHour);
      }
    }
  }

  /**
   * Cleanup the pop-up and timers.
   */
  public void cleanup() {
    // Protect against null-pointer exceptions
    if (popup != null) {
      popup.dismiss();
    }
    paused = true;
    lastPopupEventId = INVALID_EVENT_ID;
    if (handler != null) {
      handler.removeCallbacks(dismissPopup);
      handler.removeCallbacks(updateCurrentTime);
    }

//        Utils.setSharedPreference(context, GeneralPreferences.KEY_DEFAULT_CELL_HEIGHT, cellHeight);
    // Clear all click animations
    eventClickCleanup();
    // Turn off redraw
    remeasure = false;
    // Turn off scrolling to make sure the view is in the correct state if we fling back to it
    scrolling = false;
  }

  private void eventClickCleanup() {
    this.removeCallbacks(clearClick);
    this.removeCallbacks(setClick);
    clickedEvent = null;
    savedClickedEvent = null;
  }

  private void setSelectedHour(long h) {
    selectionHour = (int) h;
  }

  /**
   * Restart the update timer
   */
  public void restartCurrentTimeUpdates() {
    paused = false;
    if (handler != null) {
      handler.removeCallbacks(updateCurrentTime);
      handler.post(updateCurrentTime);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    cleanup();
    super.onDetachedFromWindow();
  }

  @Override
  public boolean onLongClick(View v) {
    int flags = DateUtils.FORMAT_SHOW_WEEKDAY;
    long time = getSelectedTimeInMillis();
    if (!selectionAllday) {
      flags |= DateUtils.FORMAT_SHOW_TIME;
    }
    if (DateFormat.is24HourFormat(context)) {
      flags |= DateUtils.FORMAT_24HOUR;
    }
    longPressTitle = Utils.formatDateRange(context, time, time, flags);
    new AlertDialog.Builder(context).setTitle(longPressTitle)
                                    .setItems(longPressItems, new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                          long extraLong = 0;
                                          if (selectionAllday) {
                                            extraLong = CalendarController.EXTRA_CREATE_ALL_DAY;
                                          }
                                          calendarController.sendEventRelatedEventWithExtra(EventType.CREATE_EVENT, -1, getSelectedTimeInMillis(), 0, -1, -1, extraLong, -1);
                                        }
                                      }
                                    }).show().setCanceledOnTouchOutside(true);
    return true;
  }

  private long calculateDuration(float delta, float width, float velocity) {
        /*
         * Here we compute a "distance" that will be used in the computation of
         * the overall snap duration. This is a function of the actual distance
         * that needs to be traveled; we keep this value close to half screen
         * size in order to reduce the variance in snap duration as a function
         * of the distance the page needs to travel.
         */
    final float halfScreenSize = width / 2;
    float distanceRatio = delta / width;
    float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
    float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration;

    velocity = Math.abs(velocity);
    velocity = Math.max(MINIMUM_SNAP_VELOCITY, velocity);

        /*
         * we want the page's snap velocity to approximately match the velocity
         * at which the user flings, so we scale the duration by a value near to
         * the derivative of the scroll interpolator at zero, ie. 5. We use 6 to
         * make it a little slower.
         */
    return (long) (6 * Math.round(1000 * Math.abs(distance / velocity)));
  }

  /*
     * We want the duration of the page snap animation to be influenced by the
     * distance that the screen has to travel, however, we don't want this
     * duration to be effected in a purely linear fashion. Instead, we use this
     * method to moderate the effect that the distance of travel has on the
     * overall snap duration.
     */
  private float distanceInfluenceForSnapDuration(float f) {
    f -= 0.5f; // center the values about 0.
    f *= 0.3f * Math.PI / 2.0f;
    return (float) Math.sin(f);
  }

  private class TodayAnimatorListener extends AnimatorListenerAdapter {
    private volatile Animator animator = null;
    private volatile boolean  fadingIn = false;

    @Override
    public void onAnimationEnd(Animator animation) {
      synchronized (this) {
        if (animator != animation) {
          animation.removeAllListeners();
          animation.cancel();
          return;
        }
        if (fadingIn) {
          if (todayAnimator != null) {
            todayAnimator.removeAllListeners();
            todayAnimator.cancel();
          }
          todayAnimator = ObjectAnimator.ofInt(DayView.this, "animateTodayAlpha", 255, 0);
          animator = todayAnimator;
          fadingIn = false;
          todayAnimator.addListener(this);
          todayAnimator.setDuration(600);
          todayAnimator.start();
        } else {
          animateToday = false;
          animateTodayAlpha = 0;
          animator.removeAllListeners();
          animator = null;
          todayAnimator = null;
          invalidate();
        }
      }
    }

    public void setAnimator(Animator animation) {
      animator = animation;
    }

    public void setFadingIn(boolean fadingIn) {
      this.fadingIn = fadingIn;
    }

  }

  private class GotoBroadcaster implements Animation.AnimationListener {
    private final int  mCounter;
    private final Time mStart;
    private final Time mEnd;

    public GotoBroadcaster(Time start, Time end) {
      mCounter = ++counter;
      mStart = start;
      mEnd = end;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
      DayView view = (DayView) viewSwitcher.getCurrentView();
      view.viewStartX = 0;
      view = (DayView) viewSwitcher.getNextView();
      view.viewStartX = 0;

      if (mCounter == counter) {
        calendarController.sendEvent(EventType.GO_TO, mStart, mEnd, null, -1, ViewType.CURRENT, CalendarController.EXTRA_GOTO_DATE, null, null);
      }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }
  }

  private class ContextMenuHandler implements MenuItem.OnMenuItemClickListener {
    @Override
    public boolean onMenuItemClick(MenuItem item) {
      switch (item.getItemId()) {
        case MENU_EVENT_VIEW: {
          if (selectedEvent != null) {
//            calendarController.sendEventRelatedEvent(EventType.VIEW_EVENT_DETAILS, selectedEvent.id, selectedEvent.startMillis, selectedEvent.endMillis, 0, 0, -1);
          }
          break;
        }
        case MENU_EVENT_EDIT: {
          if (selectedEvent != null) {
//            calendarController.sendEventRelatedEvent(EventType.EDIT_EVENT, selectedEvent.id, selectedEvent.startMillis, selectedEvent.endMillis, 0, 0, -1);
          }
          break;
        }
        case MENU_DAY: {
          calendarController.sendEvent(EventType.GO_TO, getSelectedTime(), null, -1, ViewType.DAY);
          break;
        }
        case MENU_AGENDA: {
          calendarController.sendEvent(EventType.GO_TO, getSelectedTime(), null, -1, ViewType.AGENDA);
          break;
        }
        case MENU_EVENT_CREATE: {
          long startMillis = getSelectedTimeInMillis();
          long endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
          calendarController.sendEventRelatedEvent(EventType.CREATE_EVENT, -1, startMillis, endMillis, 0, 0, -1);
          break;
        }
        case MENU_EVENT_DELETE: {
          if (selectedEvent != null) {
            Event selectedEvent = DayView.this.selectedEvent;
//            long begin = selectedEvent.startMillis;
//            long end = selectedEvent.endMillis;
//            long id = selectedEvent.id;
//            calendarController.sendEventRelatedEvent(EventType.DELETE_EVENT, id, begin, end, 0, 0, -1);
          }
          break;
        }
        default: {
          return false;
        }
      }
      return true;
    }
  }

  // Encapsulates the code to continue the scrolling after the
  // finger is lifted. Instead of stopping the scroll immediately,
  // the scroll continues to "free spin" and gradually slows down.
  private class ContinueScroll implements Runnable {
    @Override
    public void run() {
      scrolling = scrolling && scroller.computeScrollOffset();
      if (!scrolling || paused) {
        resetSelectedHour();
        invalidate();
        return;
      }

      viewStartY = scroller.getCurrY();

      if (callEdgeEffectOnAbsorb) {
        if (viewStartY < 0) {
          edgeEffectTop.onAbsorb((int) lastVelocity);
          callEdgeEffectOnAbsorb = false;
        } else if (viewStartY > maxViewStartY) {
          edgeEffectBottom.onAbsorb((int) lastVelocity);
          callEdgeEffectOnAbsorb = false;
        }
        lastVelocity = scroller.getCurrVelocity();
      }

      if (scrollStartY == 0 || scrollStartY == maxViewStartY) {
        // Allow overscroll/springback only on a fling, not a pull/fling from the end
        if (viewStartY < 0) {
          viewStartY = 0;
        } else if (viewStartY > maxViewStartY) {
          viewStartY = maxViewStartY;
        }
      }

      computeFirstHour();
      handler.post(this);
      invalidate();
    }
  }

  private class DismissPopup implements Runnable {
    @Override
    public void run() {
      // Protect against null-pointer exceptions
      if (popup != null) {
        popup.dismiss();
      }
    }
  }

  private class UpdateCurrentTime implements Runnable {
    @Override
    public void run() {
      long currentTime = System.currentTimeMillis();
      DayView.this.currentTime.set(currentTime);
      //% causes update to occur on 5 minute marks (11:10, 11:15, 11:20, etc.)
      if (!DayView.this.paused) {
        handler.postDelayed(updateCurrentTime, UPDATE_CURRENT_TIME_DELAY - (currentTime % UPDATE_CURRENT_TIME_DELAY));
      }
      todayJulianDay = Time.getJulianDay(currentTime, DayView.this.currentTime.gmtoff);
      invalidate();
    }
  }

  private class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapUp(MotionEvent ev) {
      DayView.this.doSingleTapUp(ev);
      return true;
    }

    @Override
    public void onLongPress(MotionEvent ev) {
      DayView.this.doLongPress(ev);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      eventClickCleanup();
      if (touchStartedInAllDayArea) {
        if (Math.abs(distanceX) < Math.abs(distanceY)) {
          // Make sure that click feedback is gone when you scroll from the
          // all day area
          invalidate();
          return false;
        }
        // don't scroll vertically if this started in the allday area
        distanceY = 0;
      }
      DayView.this.doScroll(e1, e2, distanceX, distanceY);
      return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      if (touchStartedInAllDayArea) {
        if (Math.abs(velocityX) < Math.abs(velocityY)) {
          return false;
        }
        // don't fling vertically if this started in the allday area
        velocityY = 0;
      }
      DayView.this.doFling(e1, e2, velocityX, velocityY);
      return true;
    }

    @Override
    public boolean onDown(MotionEvent ev) {
      DayView.this.doDown(ev);
      return true;
    }
  }

  private class ScrollInterpolator implements Interpolator {
    public ScrollInterpolator() {
    }

    @Override
    public float getInterpolation(float t) {
      t -= 1.0f;
      t = t * t * t * t * t + 1;

      if ((1 - t) * animationDistance < 1) {
        cancelAnimation();
      }

      return t;
    }
  }
}

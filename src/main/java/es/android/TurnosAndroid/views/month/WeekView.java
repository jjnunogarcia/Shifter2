package es.android.TurnosAndroid.views.month;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.text.TextPaint;
import android.text.format.Time;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.fragments.MonthFragment;
import es.android.TurnosAndroid.model.CalendarEvent;
import es.android.TurnosAndroid.model.Event;
import es.android.TurnosAndroid.model.EventPoints;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * This is a dynamic view for drawing a single week. It can be configured to display the week number, start the week on a given day, or show a reduced
 * number of days. It is intended for use as a single view within a ListView. See {@link MonthAdapter} for usage.
 */
public class WeekView extends View {
  public static final  String  KEY_WEEK_HEIGHT           = "week_height";
  public static final  String  KEY_WEEK_TO_DISPLAY       = "week_number";
  public static final  String  KEY_SELECTED_DAY          = "selected_day";
  /**
   * Which month is currently in focus, as defined by {@link android.text.format.Time#month} [0-11].
   */
  public static final  String  KEY_FOCUS_MONTH           = "focus_month";
  private static final int     DEFAULT_SELECTED_DAY      = -1;
  private static final int     DEFAULT_FOCUS_MONTH       = -1;
  private static final int     CLICKED_ALPHA             = 128;
  private static       int     DEFAULT_HEIGHT            = 32;
  private static       int     DAY_SEPARATOR_WIDTH       = 1;
  private static       int     MINI_DAY_NUMBER_TEXT_SIZE = 14;
  private static       int     TEXT_SIZE_MONTH_NUMBER    = 32;
  private static       int     TEXT_SIZE_EVENT           = 12;
  private static       int     TEXT_SIZE_EVENT_TITLE     = 14;
  private static       int     DNA_MARGIN                = 4;
  private static       int     DNA_ALL_DAY_HEIGHT        = 4;
  private static       int     DNA_MIN_SEGMENT_HEIGHT    = 4;
  private static       int     EVENT_STROKE_THICKNESS    = 15;
  private static       int     DNA_SIDE_PADDING          = 6;
  private static       int     EVENT_TEXT_COLOR          = Color.WHITE;
  private static       int     DEFAULT_EDGE_SPACING      = 0;
  private static       int     SIDE_PADDING_MONTH_NUMBER = 4;
  private static       int     TOP_PADDING_MONTH_NUMBER  = 4;
  private static       int     DAY_SEPARATOR_INNER_WIDTH = 1;
  private static       int     MIN_WEEK_WIDTH            = 50;
  private static       int     EVENT_SQUARE_BORDER       = 2;
  private static       int     EVENT_BOTTOM_PADDING      = 3;
  private static       int     TODAY_HIGHLIGHT_WIDTH     = 2;
  private static       boolean initialized               = false;
  private static       float   scale                     = 0;

  private Paint                             paint;
  private Paint                             monthNumPaint;
  // Cache the number strings so we don't have to recompute them each time
  private String[]                          dayNumbers;
  // Quick lookup for checking which days are in the focus month
  private boolean[]                         focusDay;
  // Quick lookup for checking which days are in an odd month (to set a different background)
  private boolean[]                         oddMonth;
  // The month of the first day in this week
  private int                               firstMonth;
  // The month of the last day in this week
  private int                               lastMonth;
  // The position of this week, equivalent to weeks since the week of Jan 1st, 1970
  private int                               week;
  private int                               weekWidth;
  private int                               weekHeight;
  // The timezone to display times/dates in (used for determining when Today is)
  private String                            timeZone;
  private int                               focusMonthColor;
  private Time                              today;
  private boolean                           hasToday;
  private int                               todayIndex;
  private ArrayList<CalendarEvent>          weekEvents;
  private ArrayList<ArrayList<EventPoints>> eventsPoints;
  private TextPaint                         eventPaint;
  private TextPaint                         solidBackgroundEventPaint;
  private TextPaint                         eventExtrasPaint;
  private Paint                             eventsPaint;
  private int                               monthNumAscentHeight;
  private int                               eventHeight;
  private int                               eventAscentHeight;
  private int                               extrasHeight;
  private int                               extrasAscentHeight;
  private int                               extrasDescent;
  private int                               monthBGOtherColor;
  private int                               monthBGTodayColor;
  private int                               monthNumColor;
  private int                               monthNumOtherColor;
  private int                               monthNumTodayColor;
  private int                               monthEventColor;
  private int                               monthEventExtraColor;
  private int                               clickedDayColor;
  private int                               daySeparatorInnerColor;
  private int                               todayAnimateColor;
  private int                               clickedDayIndex;
  private int                               animateTodayAlpha;
  private int                               mondayJulianDay;

  public WeekView(Context context) {
    super(context);
    Resources res = context.getResources();

    focusMonthColor = res.getColor(R.color.month_mini_day_number);
    paint = new Paint();
    firstMonth = -1;
    lastMonth = -1;
    week = -1;
    weekHeight = DEFAULT_HEIGHT;
    timeZone = Time.getCurrentTimezone();
    today = new Time();
    hasToday = false;
    todayIndex = -1;
    weekEvents = new ArrayList<CalendarEvent>();
    eventsPoints = new ArrayList<ArrayList<EventPoints>>(MonthFragment.DAYS_PER_WEEK);
    for (int i = 0; i < MonthFragment.DAYS_PER_WEEK; i++) {
      eventsPoints.add(new ArrayList<EventPoints>());
    }
    clickedDayIndex = -1;
    animateTodayAlpha = 0;

    if (scale == 0) {
      scale = context.getResources().getDisplayMetrics().density;
      if (scale != 1) {
        DEFAULT_HEIGHT *= scale;
        MINI_DAY_NUMBER_TEXT_SIZE *= scale;
        DAY_SEPARATOR_WIDTH *= scale;
      }
    }

    initView();
  }

  private void initView() {
    paint.setFakeBoldText(false);
    paint.setAntiAlias(true);
    paint.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
    paint.setStyle(Style.FILL);

    monthNumPaint = new Paint();
    monthNumPaint.setFakeBoldText(true);
    monthNumPaint.setAntiAlias(true);
    monthNumPaint.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
    monthNumPaint.setColor(focusMonthColor);
    monthNumPaint.setStyle(Style.FILL);
    monthNumPaint.setTextAlign(Align.CENTER);

    if (!initialized) {
      Resources resources = getContext().getResources();
      TEXT_SIZE_EVENT_TITLE = resources.getInteger(R.integer.text_size_event_title);
      TEXT_SIZE_MONTH_NUMBER = resources.getInteger(R.integer.text_size_month_number);
      EVENT_TEXT_COLOR = resources.getColor(R.color.calendar_event_text_color);
      if (scale != 1) {
        TOP_PADDING_MONTH_NUMBER *= scale;
        SIDE_PADDING_MONTH_NUMBER *= scale;
        TEXT_SIZE_MONTH_NUMBER *= scale;
        TEXT_SIZE_EVENT *= scale;
        TEXT_SIZE_EVENT_TITLE *= scale;
        DAY_SEPARATOR_INNER_WIDTH *= scale;
        EVENT_SQUARE_BORDER *= scale;
        EVENT_BOTTOM_PADDING *= scale;
        DNA_MARGIN *= scale;
        EVENT_STROKE_THICKNESS *= scale;
        DNA_ALL_DAY_HEIGHT *= scale;
        DNA_MIN_SEGMENT_HEIGHT *= scale;
        DNA_SIDE_PADDING *= scale;
        DEFAULT_EDGE_SPACING *= scale;
        TODAY_HIGHLIGHT_WIDTH *= scale;
      }
      TOP_PADDING_MONTH_NUMBER += DNA_ALL_DAY_HEIGHT + DNA_MARGIN;
      initialized = true;
    }

    loadColors(getContext());

    monthNumPaint = new Paint();
    monthNumPaint.setFakeBoldText(false);
    monthNumPaint.setAntiAlias(true);
    monthNumPaint.setTextSize(TEXT_SIZE_MONTH_NUMBER);
    monthNumPaint.setColor(monthNumColor);
    monthNumPaint.setStyle(Style.FILL);
    monthNumPaint.setTextAlign(Align.RIGHT);
    monthNumPaint.setTypeface(Typeface.DEFAULT);

//    monthNumAscentHeight = (int) (-monthNumPaint.ascent() + 0.5f);
    monthNumAscentHeight = 10; // TODO create constant for this margin?

    eventPaint = new TextPaint();
    eventPaint.setFakeBoldText(true);
    eventPaint.setAntiAlias(true);
    eventPaint.setTextSize(TEXT_SIZE_EVENT_TITLE);
    eventPaint.setColor(monthEventColor);

    solidBackgroundEventPaint = new TextPaint(eventPaint);
    solidBackgroundEventPaint.setColor(EVENT_TEXT_COLOR);

    eventAscentHeight = (int) (-eventPaint.ascent() + 0.5f);
    eventHeight = (int) (eventPaint.descent() - eventPaint.ascent() + 0.5f);

    eventExtrasPaint = new TextPaint();
    eventExtrasPaint.setFakeBoldText(false);
    eventExtrasPaint.setAntiAlias(true);
    eventExtrasPaint.setStrokeWidth(EVENT_SQUARE_BORDER);
    eventExtrasPaint.setTextSize(TEXT_SIZE_EVENT);
    eventExtrasPaint.setColor(monthEventExtraColor);
    eventExtrasPaint.setStyle(Style.FILL);
    eventExtrasPaint.setTextAlign(Align.LEFT);
    extrasHeight = (int) (eventExtrasPaint.descent() - eventExtrasPaint.ascent() + 0.5f);
    extrasAscentHeight = (int) (-eventExtrasPaint.ascent() + 0.5f);
    extrasDescent = (int) (eventExtrasPaint.descent() + 0.5f);

    eventsPaint = new Paint();
    eventsPaint.setStyle(Style.FILL_AND_STROKE);
    eventsPaint.setStrokeWidth(EVENT_STROKE_THICKNESS);
    eventsPaint.setAntiAlias(false);

    setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    setClickable(true);
  }

  private void loadColors(Context context) {
    Resources res = context.getResources();
    monthNumColor = res.getColor(R.color.month_day_number);
    monthNumOtherColor = res.getColor(R.color.month_day_number_other);
    monthNumTodayColor = res.getColor(R.color.month_today_number);
    monthEventColor = res.getColor(R.color.month_event_color);
    monthEventExtraColor = res.getColor(R.color.month_event_extra_color);
    monthBGTodayColor = res.getColor(R.color.month_today_bgcolor);
    monthBGOtherColor = res.getColor(R.color.month_other_bgcolor);
    daySeparatorInnerColor = res.getColor(R.color.month_grid_lines);
    todayAnimateColor = res.getColor(R.color.today_highlight_color);
    clickedDayColor = res.getColor(R.color.day_clicked_background_color);
  }

  public void setEvents(ArrayList<CalendarEvent> weekEvents) {
    this.weekEvents = weekEvents;
    createEventsPoints();
  }

  public void clearDrawnEvents() {
    eventsPaint.reset();
    eventsPaint.setStyle(Style.FILL_AND_STROKE);
    eventsPaint.setStrokeWidth(EVENT_STROKE_THICKNESS);
    eventsPaint.setAntiAlias(false);
    eventsPoints = new ArrayList<ArrayList<EventPoints>>(MonthFragment.DAYS_PER_WEEK);
    for (int i = 0; i < MonthFragment.DAYS_PER_WEEK; i++) {
      eventsPoints.add(new ArrayList<EventPoints>());
    }
  }

  private void createEventsPoints() {
    if (weekEvents == null || weekWidth <= MIN_WEEK_WIDTH || getContext() == null) {
      // Stash the list of events for use when this view is ready, or just clear it if a null set has been passed to this view
//      this.weekEvents = weekEvents;
      eventsPoints = null;
      return;
    }

    // Create the drawing coordinates for dna
    int[] dayXs = new int[MonthFragment.DAYS_PER_WEEK];

    for (int day = 0; day < MonthFragment.DAYS_PER_WEEK; day++) {
      dayXs[day] = computeDayLeftPosition(day);
    }

    if (hasThisWeekEventsInside()) {
      eventsPoints = convertCalendarEventsToPoints(dayXs, 55);
    }
  }

  private boolean hasThisWeekEventsInside() {
    for (CalendarEvent event : weekEvents) {
      if (event.getDay() >= mondayJulianDay && event.getDay() < mondayJulianDay + MonthFragment.DAYS_PER_WEEK) {
        return true;
      }
    }

    return false;
  }

  private ArrayList<ArrayList<EventPoints>> convertCalendarEventsToPoints(int[] dayXs, int marginTop) {
    ArrayList<ArrayList<EventPoints>> organizedStrands = new ArrayList<ArrayList<EventPoints>>(MonthFragment.DAYS_PER_WEEK);
    for (int i = 0; i < MonthFragment.DAYS_PER_WEEK; i++) {
      organizedStrands.add(new ArrayList<EventPoints>());
    }

    for (CalendarEvent calendarEvent : weekEvents) {
      Event event = calendarEvent.getEvent();
      int dayOfWeek = getDayOfWeek(calendarEvent.getDay(), mondayJulianDay);
      int x = dayXs[dayOfWeek];
      EventPoints strand = new EventPoints();
      int alreadyInsertedStrandsInDay = organizedStrands.get(dayOfWeek).size();
      int position = 0;
      strand.getPoints()[position++] = x;
      strand.getPoints()[position++] = marginTop + EVENT_STROKE_THICKNESS * alreadyInsertedStrandsInDay;
      strand.getPoints()[position++] = x + weekWidth / MonthFragment.DAYS_PER_WEEK;
      strand.getPoints()[position] = marginTop + EVENT_STROKE_THICKNESS * alreadyInsertedStrandsInDay;
      strand.setColor(event.getColor());
      organizedStrands.get(dayOfWeek).add(strand);
    }

    return organizedStrands;
  }

  private static int getDayOfWeek(int julianDay, int mondayJulianDay) {
    return julianDay - mondayJulianDay;
  }

  /**
   * Sets all the parameters for displaying this week. The only required parameter is the week number. Other parameters have a default value and will only update if a new value is
   * included, except for focus month, which will always default to no focus month if no value is passed in. See {@link #KEY_WEEK_HEIGHT} for more info on parameters.
   *
   * @param params   A map of the new parameters, see {@link #KEY_WEEK_HEIGHT}
   * @param timeZone The time zone this view should reference times in
   */
  public void setWeekParams(HashMap<String, Integer> params, String timeZone) {
    if (!params.containsKey(KEY_WEEK_TO_DISPLAY)) {
      throw new InvalidParameterException("You must specify the week number for this view");
    }
    setTag(params);
    week = params.get(KEY_WEEK_TO_DISPLAY);
    this.timeZone = timeZone;

    if (params.containsKey(KEY_WEEK_HEIGHT)) {
      weekHeight = params.get(KEY_WEEK_HEIGHT);
    }

    // Allocate space for caching the day numbers and focus values
    dayNumbers = new String[MonthFragment.DAYS_PER_WEEK];
    focusDay = new boolean[MonthFragment.DAYS_PER_WEEK];
    oddMonth = new boolean[MonthFragment.DAYS_PER_WEEK];
    mondayJulianDay = Time.getJulianMondayFromWeeksSinceEpoch(week);
    Time time = new Time(this.timeZone);
    time.setJulianDay(mondayJulianDay);

    firstMonth = time.month;

    int focusMonth = params.containsKey(KEY_FOCUS_MONTH) ? params.get(KEY_FOCUS_MONTH) : DEFAULT_FOCUS_MONTH;

    for (int i = 0; i < MonthFragment.DAYS_PER_WEEK; i++) {
      oddMonth[i] = (time.month % 2) == 1;
      focusDay[i] = time.month == focusMonth;
      dayNumbers[i] = Integer.toString(time.monthDay++);
      time.normalize(true);
    }

    // We do one extra add at the end of the loop, if that pushed us to a new month undo it
    if (time.monthDay == 1) {
      time.monthDay--;
      time.normalize(true);
    }

    lastMonth = time.month;

    setTodayValue();
  }

  public boolean setTodayValue() {
    today.timezone = timeZone;
    today.setToNow();
    today.normalize(true);
    int julianToday = Time.getJulianDay(today.toMillis(false), today.gmtoff);
    if (julianToday >= mondayJulianDay && julianToday < mondayJulianDay + MonthFragment.DAYS_PER_WEEK) {
      hasToday = true;
      todayIndex = julianToday - mondayJulianDay;
    } else {
      hasToday = false;
      todayIndex = -1;
    }
    return hasToday;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    drawBackground(canvas);
    drawMonthDayNumbers(canvas);
    drawDaySeparators(canvas);
    if (hasToday) {
      drawToday(canvas);
    }
    if (eventsPoints == null) {
      createEventsPoints();
    }
    drawEvents(canvas);
    drawClickedDay(canvas);
  }

  /**
   * This draws the selection highlight if a day is selected in this week.
   *
   * @param canvas The canvas to draw on
   */
  private void drawBackground(Canvas canvas) {
    Rect rect = new Rect();
    int i = 0;
    int offset = 0;
    rect.top = DAY_SEPARATOR_INNER_WIDTH;
    rect.bottom = weekHeight;

    if (!oddMonth[i]) {
      while (++i < oddMonth.length && !oddMonth[i]) {
      }
      rect.right = computeDayLeftPosition(i - offset);
      rect.left = 0;
      paint.setColor(monthBGOtherColor);
      canvas.drawRect(rect, paint);
      // compute left edge for i, set up rect, draw
    } else if (!oddMonth[(i = oddMonth.length - 1)]) {
      while (--i >= offset && !oddMonth[i]) {
      }
      i++;
      // compute left edge for i, set up rect, draw
      rect.right = weekWidth;
      rect.left = computeDayLeftPosition(i - offset);
      paint.setColor(monthBGOtherColor);
      canvas.drawRect(rect, paint);
    }

    if (hasToday) {
      paint.setColor(monthBGTodayColor);
      rect.left = computeDayLeftPosition(todayIndex);
      rect.right = computeDayLeftPosition(todayIndex + 1);
      canvas.drawRect(rect, paint);
    }
  }

  private void drawMonthDayNumbers(Canvas canvas) {
    int y;
    int i = 0;
    int offset = -1;
    int x;
    int numCount = MonthFragment.DAYS_PER_WEEK;

    y = monthNumAscentHeight + TOP_PADDING_MONTH_NUMBER;
    boolean isFocusMonth = focusDay[i];
    boolean isBold = false;
    monthNumPaint.setColor(isFocusMonth ? monthNumColor : monthNumOtherColor);

    for (; i < numCount; i++) {
      if (hasToday && todayIndex == i) {
        monthNumPaint.setColor(monthNumTodayColor);
        monthNumPaint.setFakeBoldText(isBold = true);
        if (i + 1 < numCount) {
          // Make sure the color will be set back on the next iteration
          isFocusMonth = !focusDay[i + 1];
        }
      } else if (focusDay[i] != isFocusMonth) {
        isFocusMonth = focusDay[i];
        monthNumPaint.setColor(isFocusMonth ? monthNumColor : monthNumOtherColor);
      }
      x = computeDayLeftPosition(i - offset) - (SIDE_PADDING_MONTH_NUMBER);
      canvas.drawText(dayNumbers[i], x, y, monthNumPaint);
      if (isBold) {
        monthNumPaint.setFakeBoldText(isBold = false);
      }
    }
  }

  /**
   * Draws a horizontal line for separating the weeks.
   *
   * @param canvas The canvas to draw on
   */
  private void drawDaySeparators(Canvas canvas) {
    float lines[] = new float[8 * 4];
    int count = 6 * 4;
    int wkNumOffset = 0;
    int i = 0;
    count += 4;
    lines[i++] = 0;
    lines[i++] = 0;
    lines[i++] = weekWidth;
    lines[i++] = 0;
    int y0 = 0;
    int y1 = weekHeight;

    while (i < count) {
      int x = computeDayLeftPosition(i / 4 - wkNumOffset);
      lines[i++] = x;
      lines[i++] = y0;
      lines[i++] = x;
      lines[i++] = y1;
    }
    paint.setColor(daySeparatorInnerColor);
    paint.setStrokeWidth(DAY_SEPARATOR_INNER_WIDTH);
    canvas.drawLines(lines, 0, count, paint);
  }

  private void drawToday(Canvas canvas) {
    Rect r = new Rect();
    r.top = DAY_SEPARATOR_INNER_WIDTH + (TODAY_HIGHLIGHT_WIDTH / 2);
    r.bottom = weekHeight - (int) Math.ceil(TODAY_HIGHLIGHT_WIDTH / 2.0f);
    paint.setStyle(Style.STROKE);
    paint.setStrokeWidth(TODAY_HIGHLIGHT_WIDTH);
    r.left = computeDayLeftPosition(todayIndex) + (TODAY_HIGHLIGHT_WIDTH / 2);
    r.right = computeDayLeftPosition(todayIndex + 1) - (int) Math.ceil(TODAY_HIGHLIGHT_WIDTH / 2.0f);
    paint.setColor(todayAnimateColor | (animateTodayAlpha << 24));
    canvas.drawRect(r, paint);
    paint.setStyle(Style.FILL);
  }

  private void drawEvents(Canvas canvas) {
    if (eventsPoints != null) {
      for (int i = 0; i < MonthFragment.DAYS_PER_WEEK; i++) {
        ArrayList<EventPoints> eventPoints = eventsPoints.get(i);
        for (int j = 0, size = eventPoints.size(); j < size; j++) {
          EventPoints strand = eventPoints.get(j);
          eventsPaint.setColor(strand.getColor());
          // TODO replace with Path class?
          canvas.drawLines(strand.getPoints(), eventsPaint);
          if (j == 3) {
            // TODO draw "more events..."
            break;
          }
        }
      }
    }
  }

  private void drawClickedDay(Canvas canvas) {
    if (clickedDayIndex != -1) {
      Rect r = new Rect();
      int alpha = paint.getAlpha();
      paint.setColor(clickedDayColor);
      paint.setAlpha(CLICKED_ALPHA);
      r.left = computeDayLeftPosition(clickedDayIndex);
      r.right = computeDayLeftPosition(clickedDayIndex + 1);
      r.top = 0;
      r.bottom = weekHeight;
      canvas.drawRect(r, paint);
      paint.setAlpha(alpha);
    }
  }

  private void drawMoreEvents(Canvas canvas, int remainingEvents, int x) {
    int y = weekHeight - (extrasDescent + EVENT_BOTTOM_PADDING);
    String text = getContext().getResources().getQuantityString(R.plurals.month_more_events, remainingEvents);
    eventExtrasPaint.setAntiAlias(true);
    eventExtrasPaint.setFakeBoldText(true);
    canvas.drawText(String.format(text, remainingEvents), x, y, eventExtrasPaint);
    eventExtrasPaint.setFakeBoldText(false);
  }

  private int computeDayLeftPosition(int day) {
    return day * weekWidth / MonthFragment.DAYS_PER_WEEK;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), weekHeight);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    weekWidth = w;
  }

  public int getDayIndexFromLocation(float x) {
    if (x < 0 || x > weekWidth) {
      return -1;
    }

    return (int) (x / (weekWidth / MonthFragment.DAYS_PER_WEEK));
  }

  /**
   * Calculates the day that the given x position is in, accounting for week number. Returns a Time referencing that day or null if
   *
   * @param x The x position of the touch event
   * @return A time object for the tapped day or null if the position wasn't in a day
   */
  public Time getDayFromLocation(float x) {
    int dayPosition = getDayIndexFromLocation(x);
    if (dayPosition == -1) {
      return null;
    }
    int day = mondayJulianDay + dayPosition;

    Time time = new Time(timeZone);
    if (week == 0) {
      // This week is weird...
      if (day < Time.EPOCH_JULIAN_DAY) {
        day++;
      } else if (day == Time.EPOCH_JULIAN_DAY) {
        time.set(1, Calendar.JANUARY, 1970);
        time.normalize(true);
        return time;
      }
    }

    time.setJulianDay(day);
    return time;
  }

  public void setClickedDay(float xLocation) {
    clickedDayIndex = getDayIndexFromLocation(xLocation);
    invalidate();
  }

  public void clearClickedDay() {
    clickedDayIndex = -1;
    invalidate();
  }

  public int getMondayJulianDay() {
    return mondayJulianDay;
  }

  public int getFirstVisibleMonth() {
    return firstMonth;
  }

  public int getLastVisibleMonth() {
    return lastMonth;
  }
}

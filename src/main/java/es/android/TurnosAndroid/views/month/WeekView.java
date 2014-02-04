package es.android.TurnosAndroid.views.month;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
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
import es.android.TurnosAndroid.helpers.Utils;
import es.android.TurnosAndroid.model.CalendarEvent;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * This is a dynamic view for drawing a single week. It can be configured to display the week number, start the week on a given day, or show a reduced
 * number of days. It is intended for use as a single view within a ListView. See {@link MonthAdapter} for usage.
 */
public class WeekView extends View {
  public static final    String  KEY_ANIMATE_TODAY           = "animate_today";
  public static final    String  KEY_WEEK_HEIGHT             = "week_height";
  public static final    String  KEY_WEEK_TO_DISPLAY         = "week";
  /**
   * This sets one of the days in this view as selected {@link android.text.format.Time#SUNDAY} through {@link android.text.format.Time#SATURDAY}.
   */
  public static final    String  KEY_SELECTED_DAY            = "selected_day";
  /**
   * Which day the week should start on. {@link android.text.format.Time#SUNDAY} through {@link android.text.format.Time#SATURDAY}.
   */
  public static final    String  KEY_WEEK_START              = "week_start";
  /**
   * Which month is currently in focus, as defined by {@link android.text.format.Time#month} [0-11].
   */
  public static final    String  KEY_FOCUS_MONTH             = "focus_month";
  protected static final int     DEFAULT_SELECTED_DAY        = -1;
  protected static final int     DEFAULT_WEEK_START          = Time.SUNDAY;
  protected static final int     DEFAULT_FOCUS_MONTH         = -1;
  private static final   String  TAG                         = WeekView.class.getSimpleName();
  private static final   int     CLICKED_ALPHA               = 128;
  protected static       int     DEFAULT_HEIGHT              = 32;
  protected static       int     DAY_SEPARATOR_WIDTH         = 1;
  protected static       int     MINI_DAY_NUMBER_TEXT_SIZE   = 14;
  protected static       int     MINI_WK_NUMBER_TEXT_SIZE    = 12;
  protected static       int     MINI_TODAY_NUMBER_TEXT_SIZE = 18;
  protected static       int     MINI_TODAY_OUTLINE_WIDTH    = 2;
  protected static       int     WEEK_NUM_MARGIN_BOTTOM      = 4;
  // used for scaling to the device density
  protected static       float   scale                       = 0;
  private static         int     TEXT_SIZE_MONTH_NUMBER      = 32;
  private static         int     TEXT_SIZE_EVENT             = 12;
  private static         int     TEXT_SIZE_EVENT_TITLE       = 14;
  private static         int     TEXT_SIZE_WEEK_NUM          = 12;
  private static         int     DNA_MARGIN                  = 4;
  private static         int     DNA_ALL_DAY_HEIGHT          = 4;
  private static         int     DNA_MIN_SEGMENT_HEIGHT      = 4;
  private static         int     DNA_WIDTH                   = 15;
  private static         int     DNA_SIDE_PADDING            = 6;
  private static         int     EVENT_TEXT_COLOR            = Color.WHITE;
  private static         int     DEFAULT_EDGE_SPACING        = 0;
  private static         int     SIDE_PADDING_MONTH_NUMBER   = 4;
  private static         int     TOP_PADDING_MONTH_NUMBER    = 4;
  private static         int     TOP_PADDING_WEEK_NUMBER     = 4;
  private static         int     SIDE_PADDING_WEEK_NUMBER    = 20;
  private static         int     DAY_SEPARATOR_INNER_WIDTH   = 1;
  private static         int     MIN_WEEK_WIDTH              = 50;
  private static         int     EVENT_SQUARE_BORDER         = 2;
  private static         int     EVENT_BOTTOM_PADDING        = 3;
  private static         int     TODAY_HIGHLIGHT_WIDTH       = 2;
  private static         int     SPACING_WEEK_NUMBER         = 24;
  private static         boolean initialized                 = false;

  private Paint                        p;
  private Paint                        monthNumPaint;
  // Cache the number strings so we don't have to recompute them each time
  private String[]                     dayNumbers;
  // Quick lookup for checking which days are in the focus month
  private boolean[]                    focusDay;
  // Quick lookup for checking which days are in an odd month (to set a different background)
  private boolean[]                    oddMonth;
  // The Julian day of the first day displayed by this item
  private int                          firstJulianDay;
  // The month of the first day in this week
  private int                          firstMonth;
  // The month of the last day in this week
  private int                          lastMonth;
  // The position of this week, equivalent to weeks since the week of Jan 1st, 1970
  private int                          week;
  // Quick reference to the width of this view, matches parent
  private int                          width;
  // The height this view should draw at in pixels, set by height param
  private int                          height;
  // Which day is selected [0-6] or -1 if no day is selected
  private int                          selectedDay;
  // Which day of the week to start on [0-6]
  private int                          weekStart;
  // The timezone to display times/dates in (used for determining when Today is)
  private String                       timeZone;
  private int                          focusMonthColor;
  private Time                         today;
  private boolean                      hasToday;
  private int                          todayIndex;
  private ArrayList<CalendarEvent>     weekEvents;
  private ArrayList<Utils.EventStrand> dna;
  private TextPaint                    eventPaint;
  private TextPaint                    solidBackgroundEventPaint;
  private TextPaint                    eventExtrasPaint;
  private Paint                        dnaTimePaint;
  private Paint                        eventSquarePaint;
  private int                          monthNumAscentHeight;
  private int                          eventHeight;
  private int                          eventAscentHeight;
  private int                          extrasHeight;
  private int                          extrasAscentHeight;
  private int                          extrasDescent;
  private int                          monthBGOtherColor;
  private int                          monthBGTodayColor;
  private int                          monthNumColor;
  private int                          monthNumOtherColor;
  private int                          monthNumTodayColor;
  private int                          monthEventColor;
  private int                          monthEventExtraColor;
  private int                          clickedDayColor;
  private int                          daySeparatorInnerColor;
  private int                          todayAnimateColor;
  private boolean                      animateToday;
  private int                          clickedDayIndex;
  private int                          animateTodayAlpha;
  private ObjectAnimator               todayAnimator;
  private TodayAnimatorListener        animatorListener;
  private int                          mondayJulianDay;

  public WeekView(Context context) {
    super(context);
    Resources res = context.getResources();

    focusMonthColor = res.getColor(R.color.month_mini_day_number);
    p = new Paint();
    firstJulianDay = -1;
    firstMonth = -1;
    lastMonth = -1;
    week = -1;
    height = DEFAULT_HEIGHT;
    selectedDay = DEFAULT_SELECTED_DAY;
    weekStart = DEFAULT_WEEK_START;
    timeZone = Time.getCurrentTimezone();
    today = new Time();
    hasToday = false;
    todayIndex = -1;
    weekEvents = new ArrayList<CalendarEvent>();
    dna = null;
    clickedDayIndex = -1;
    animateTodayAlpha = 0;
    todayAnimator = null;
    animatorListener = new TodayAnimatorListener();

    if (scale == 0) {
      scale = context.getResources().getDisplayMetrics().density;
      if (scale != 1) {
        DEFAULT_HEIGHT *= scale;
        MINI_DAY_NUMBER_TEXT_SIZE *= scale;
        MINI_TODAY_NUMBER_TEXT_SIZE *= scale;
        MINI_TODAY_OUTLINE_WIDTH *= scale;
        WEEK_NUM_MARGIN_BOTTOM *= scale;
        DAY_SEPARATOR_WIDTH *= scale;
        MINI_WK_NUMBER_TEXT_SIZE *= scale;
      }
    }

    initView();
  }

  private void initView() {
    p.setFakeBoldText(false);
    p.setAntiAlias(true);
    p.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
    p.setStyle(Style.FILL);

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
        TOP_PADDING_WEEK_NUMBER *= scale;
        SIDE_PADDING_MONTH_NUMBER *= scale;
        SIDE_PADDING_WEEK_NUMBER *= scale;
        SPACING_WEEK_NUMBER *= scale;
        TEXT_SIZE_MONTH_NUMBER *= scale;
        TEXT_SIZE_EVENT *= scale;
        TEXT_SIZE_EVENT_TITLE *= scale;
        TEXT_SIZE_WEEK_NUM *= scale;
        DAY_SEPARATOR_INNER_WIDTH *= scale;
        EVENT_SQUARE_BORDER *= scale;
        EVENT_BOTTOM_PADDING *= scale;
        DNA_MARGIN *= scale;
        DNA_WIDTH *= scale;
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

    dnaTimePaint = new Paint();
    dnaTimePaint.setStyle(Style.FILL_AND_STROKE);
    dnaTimePaint.setStrokeWidth(DNA_WIDTH);
    dnaTimePaint.setAntiAlias(false);

    eventSquarePaint = new Paint();
    eventSquarePaint.setStrokeWidth(EVENT_SQUARE_BORDER);
    eventSquarePaint.setAntiAlias(false);

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
    createDna();
  }

  /**
   * Sets up the dna bits for the view. This will return early if the view isn't in a state that will create a valid set of dna yet (such as the views width not being set correctly yet).
   */
  private void createDna() {
    if (weekEvents == null || width <= MIN_WEEK_WIDTH || getContext() == null) {
      // Stash the list of events for use when this view is ready, or just clear it if a null set has been passed to this view
//      this.weekEvents = weekEvents;
      dna = null;
      return;
    }

    // Create the drawing coordinates for dna
    int[] dayXs = new int[MonthFragment.DAYS_PER_WEEK];

    for (int day = 0; day < MonthFragment.DAYS_PER_WEEK; day++) {
//      dayXs[day] = computeDayLeftPosition(day) + DNA_WIDTH / 2 + DNA_SIDE_PADDING;
      dayXs[day] = computeDayLeftPosition(day);
    }

    int top = DAY_SEPARATOR_INNER_WIDTH + DNA_MARGIN + 1;
    int bottom = height - DNA_MARGIN;

    if (hasThisWeekEventsInside()) {
      dna = Utils.createDNAStrands(firstJulianDay, weekEvents, top, bottom, dayXs, getContext(), mondayJulianDay);
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

  /**
   * Sets all the parameters for displaying this week. The only required parameter is the week number. Other parameters have a default value and will only update if a new value is
   * included, except for focus month, which will always default to no focus month if no value is passed in. See {@link #KEY_WEEK_HEIGHT} for more info on parameters.
   *
   * @param params A map of the new parameters, see {@link #KEY_WEEK_HEIGHT}
   * @param tz     The time zone this view should reference times in
   */
  public void setWeekParams(HashMap<String, Integer> params, String tz) {
    if (!params.containsKey(KEY_WEEK_TO_DISPLAY)) {
      throw new InvalidParameterException("You must specify the week number for this view");
    }
    setTag(params);
    timeZone = tz;

    if (params.containsKey(KEY_WEEK_HEIGHT)) {
      height = params.get(KEY_WEEK_HEIGHT);
    }

    if (params.containsKey(KEY_SELECTED_DAY)) {
      selectedDay = params.get(KEY_SELECTED_DAY);
    }

    // Allocate space for caching the day numbers and focus values
    dayNumbers = new String[MonthFragment.DAYS_PER_WEEK];
    focusDay = new boolean[MonthFragment.DAYS_PER_WEEK];
    oddMonth = new boolean[MonthFragment.DAYS_PER_WEEK];
    week = params.get(KEY_WEEK_TO_DISPLAY);
    mondayJulianDay = Time.getJulianMondayFromWeeksSinceEpoch(week);
    Time time = new Time(tz);
    time.setJulianDay(mondayJulianDay);

    if (params.containsKey(KEY_WEEK_START)) {
      weekStart = params.get(KEY_WEEK_START);
    }

    // Now adjust our starting day based on the start day of the week. If the week is set to start on a Saturday the first week will be Dec 27th 1969 -Jan 2nd, 1970
    if (time.weekDay != weekStart) {
      int diff = time.weekDay - weekStart;
      if (diff < 0) {
        diff += 7;
      }
      time.monthDay -= diff;
      time.normalize(true);
    }

    firstJulianDay = Time.getJulianDay(time.toMillis(true), time.gmtoff);
    firstMonth = time.month;

    // Figure out what day today is
    Time today = new Time(tz);
    today.setToNow();
    hasToday = false;

    int focusMonth = params.containsKey(KEY_FOCUS_MONTH) ? params.get(KEY_FOCUS_MONTH) : DEFAULT_FOCUS_MONTH;

    for (int i = 0; i < MonthFragment.DAYS_PER_WEEK; i++) {
      oddMonth[i] = (time.month % 2) == 1;
      focusDay[i] = time.month == focusMonth;

      if (time.year == today.year && time.yearDay == today.yearDay) {
        hasToday = true;
      }

      dayNumbers[i] = Integer.toString(time.monthDay++);
      time.normalize(true);
    }

    // We do one extra add at the end of the loop, if that pushed us to a new month undo it
    if (time.monthDay == 1) {
      time.monthDay--;
      time.normalize(true);
    }

    lastMonth = time.month;

//    updateSelectionPositions();
    updateToday(tz);

    if (params.containsKey(KEY_ANIMATE_TODAY) && hasToday) {
      synchronized (animatorListener) {
        if (todayAnimator != null) {
          todayAnimator.removeAllListeners();
          todayAnimator.cancel();
        }
        todayAnimator = ObjectAnimator.ofInt(this, "animateTodayAlpha", Math.max(animateTodayAlpha, 80), 255);
        todayAnimator.setDuration(150);
        animatorListener.setAnimator(todayAnimator);
        animatorListener.setFadingIn(true);
        todayAnimator.addListener(animatorListener);
        animateToday = true;
        todayAnimator.start();
      }
    }
  }

  public boolean updateToday(String tz) {
    today.timezone = tz;
    today.setToNow();
    today.normalize(true);
    int julianToday = Time.getJulianDay(today.toMillis(false), today.gmtoff);
    if (julianToday >= firstJulianDay && julianToday < firstJulianDay + MonthFragment.DAYS_PER_WEEK) {
      hasToday = true;
      todayIndex = julianToday - firstJulianDay;
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
    if (hasToday && animateToday) {
      drawToday(canvas);
    }
    if (dna == null) {
      createDna();
    }
    drawEvent(canvas);
    drawClick(canvas);
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
    rect.bottom = height;

    if (!oddMonth[i]) {
      while (++i < oddMonth.length && !oddMonth[i]) {
      }
      rect.right = computeDayLeftPosition(i - offset);
      rect.left = 0;
      p.setColor(monthBGOtherColor);
      canvas.drawRect(rect, p);
      // compute left edge for i, set up rect, draw
    } else if (!oddMonth[(i = oddMonth.length - 1)]) {
      while (--i >= offset && !oddMonth[i]) {
      }
      i++;
      // compute left edge for i, set up rect, draw
      rect.right = width;
      rect.left = computeDayLeftPosition(i - offset);
      p.setColor(monthBGOtherColor);
      canvas.drawRect(rect, p);
    }

    if (hasToday) {
      p.setColor(monthBGTodayColor);
      rect.left = computeDayLeftPosition(todayIndex);
      rect.right = computeDayLeftPosition(todayIndex + 1);
      canvas.drawRect(rect, p);
    }
  }

  private void drawToday(Canvas canvas) {
    Rect r = new Rect();
    r.top = DAY_SEPARATOR_INNER_WIDTH + (TODAY_HIGHLIGHT_WIDTH / 2);
    r.bottom = height - (int) Math.ceil(TODAY_HIGHLIGHT_WIDTH / 2.0f);
    p.setStyle(Style.STROKE);
    p.setStrokeWidth(TODAY_HIGHLIGHT_WIDTH);
    r.left = computeDayLeftPosition(todayIndex) + (TODAY_HIGHLIGHT_WIDTH / 2);
    r.right = computeDayLeftPosition(todayIndex + 1) - (int) Math.ceil(TODAY_HIGHLIGHT_WIDTH / 2.0f);
    p.setColor(todayAnimateColor | (animateTodayAlpha << 24));
    canvas.drawRect(r, p);
    p.setStyle(Style.FILL);
  }

  private int computeDayLeftPosition(int day) {
    return day * width / MonthFragment.DAYS_PER_WEEK;
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
    lines[i++] = width;
    lines[i++] = 0;
    int y0 = 0;
    int y1 = height;

    while (i < count) {
      int x = computeDayLeftPosition(i / 4 - wkNumOffset);
      lines[i++] = x;
      lines[i++] = y0;
      lines[i++] = x;
      lines[i++] = y1;
    }
    p.setColor(daySeparatorInnerColor);
    p.setStrokeWidth(DAY_SEPARATOR_INNER_WIDTH);
    canvas.drawLines(lines, 0, count, p);
  }

  // Draw the "clicked" color on the tapped day
  private void drawClick(Canvas canvas) {
    if (clickedDayIndex != -1) {
      Rect r = new Rect();
      int alpha = p.getAlpha();
      p.setColor(clickedDayColor);
      p.setAlpha(CLICKED_ALPHA);
      r.left = computeDayLeftPosition(clickedDayIndex);
      r.right = computeDayLeftPosition(clickedDayIndex + 1);
      r.top = DAY_SEPARATOR_INNER_WIDTH;
      r.bottom = height;
      canvas.drawRect(r, p);
      p.setAlpha(alpha);
    }
  }

  /**
   * Draws the month day numbers for this week.
   *
   * @param canvas The canvas to draw on
   */
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

  private void drawEvent(Canvas canvas) {
    if (dna != null) {
      for (Utils.EventStrand strand : dna) {
        dnaTimePaint.setColor(strand.color);
        // TODO replace with Path class?
        canvas.drawLines(strand.points, dnaTimePaint);
      }
    }
  }

  private void drawMoreEvents(Canvas canvas, int remainingEvents, int x) {
    int y = height - (extrasDescent + EVENT_BOTTOM_PADDING);
    String text = getContext().getResources().getQuantityString(R.plurals.month_more_events, remainingEvents);
    eventExtrasPaint.setAntiAlias(true);
    eventExtrasPaint.setFakeBoldText(true);
    canvas.drawText(String.format(text, remainingEvents), x, y, eventExtrasPaint);
    eventExtrasPaint.setFakeBoldText(false);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    width = w;
//    updateSelectionPositions();
  }

  /**
   * This calculates the positions for the selected day lines.
   */
  private void updateSelectionPositions() {
    if (selectedDay != -1) {
      int selectedPosition = selectedDay - weekStart;
      if (selectedPosition < 0) {
        selectedPosition += 7;
      }
      int effectiveWidth = width - DEFAULT_EDGE_SPACING * 2;
      effectiveWidth -= SPACING_WEEK_NUMBER;
    }
  }

  public int getDayIndexFromLocation(float x) {
    if (x < DEFAULT_EDGE_SPACING || x > width - DEFAULT_EDGE_SPACING) {
      return -1;
    }
    // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
    return ((int) ((x - DEFAULT_EDGE_SPACING) * MonthFragment.DAYS_PER_WEEK / (width - DEFAULT_EDGE_SPACING - DEFAULT_EDGE_SPACING)));
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
    int day = firstJulianDay + dayPosition;

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

  public int getFirstJulianDay() {
    return firstJulianDay;
  }

  public int getFirstMonth() {
    return firstMonth;
  }

  public int getLastMonth() {
    return lastMonth;
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
          todayAnimator = ObjectAnimator.ofInt(WeekView.this, "animateTodayAlpha", 255, 0);
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
}

/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.android.TurnosAndroid.colorpicker;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {
  private final static int   PANEL_SAT_VAL                 = 0;
  private final static int   PANEL_HUE                     = 1;
  /**
   * The width in pixels of the border surrounding all color panels.
   */
  private final static float BORDER_WIDTH_PX               = 1;
  /**
   * The dp which the tracker of the hue or alpha panel will extend outside of its bounds.
   */
  private static       float RECTANGLE_TRACKER_OFFSET      = 2f;
  /**
   * The width in dp of the hue panel.
   */
  private              float HUE_PANEL_WIDTH               = 30f;
  /**
   * The distance in dp between the different color panels.
   */
  private              float PANEL_SPACING                 = 10f;
  /**
   * The radius in dp of the color palette tracker circle.
   */
  private              float PALETTE_CIRCLE_TRACKER_RADIUS = 5f;
  private float                  density;
  private OnColorChangedListener colorChangedListener;
  private Paint                  satValPaint;
  private Paint                  satValTrackerPaint;
  private Paint                  huePaint;
  private Paint                  hueTrackerPaint;
  private Paint                  borderPaint;
  private Shader                 valShader;
  private Shader                 satShader;
  private Shader                 hueShader;
  private int                    alpha;
  private float                  hue;
  private float                  sat;
  private float                  val;
  private int                    mSliderTrackerColor;
  private int                    borderColor;
  /*
   * To remember which panel that has the "focus" when processing hardware button data.
   */
  private int lastTouchedPanel = PANEL_SAT_VAL;
  /**
   * Offset from the edge we must have or else the finger tracker will get clipped when it is drawn outside of the view.
   */
  private float drawingOffset;
  /*
   * Distance form the edges of the view of where we are allowed to draw.
   */
  private RectF drawingRect;
  private RectF satValRect;
  private RectF hueRect;
  private Point startTouchPoint;

  public ColorPickerView(Context context) {
    super(context);
    init();
  }

  public ColorPickerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    density = 1f;
    alpha = 0xff;
    hue = 360f;
    sat = 0f;
    val = 0f;
    mSliderTrackerColor = 0xff1c1c1c;
    borderColor = 0xff6E6E6E;
    startTouchPoint = null;
    density = getContext().getResources().getDisplayMetrics().density;
    PALETTE_CIRCLE_TRACKER_RADIUS *= density;
    RECTANGLE_TRACKER_OFFSET *= density;
    HUE_PANEL_WIDTH *= density;
    PANEL_SPACING = PANEL_SPACING * density;
    drawingOffset = calculateRequiredOffset();

    initPaintTools();

    //Needed for receiving trackball motion events.
    setFocusable(true);
    setFocusableInTouchMode(true);
  }

  private void initPaintTools() {
    satValPaint = new Paint();
    satValTrackerPaint = new Paint();
    huePaint = new Paint();
    hueTrackerPaint = new Paint();
    borderPaint = new Paint();

    satValTrackerPaint.setStyle(Style.STROKE);
    satValTrackerPaint.setStrokeWidth(2f * density);
    satValTrackerPaint.setAntiAlias(true);

    hueTrackerPaint.setColor(mSliderTrackerColor);
    hueTrackerPaint.setStyle(Style.STROKE);
    hueTrackerPaint.setStrokeWidth(2f * density);
    hueTrackerPaint.setAntiAlias(true);
  }

  private float calculateRequiredOffset() {
    float offset = Math.max(PALETTE_CIRCLE_TRACKER_RADIUS, RECTANGLE_TRACKER_OFFSET);
    offset = Math.max(offset, BORDER_WIDTH_PX * density);

    return offset * 1.5f;
  }

  private int[] buildHueColorArray() {

    int[] hue = new int[361];

    int count = 0;
    for (int i = hue.length - 1; i >= 0; i--, count++) {
      hue[count] = Color.HSVToColor(new float[]{i, 1f, 1f});
    }

    return hue;
  }

  @Override
  protected void onDraw(Canvas canvas) {

    if (drawingRect.width() <= 0 || drawingRect.height() <= 0) {
      return;
    }

    drawSatValPanel(canvas);
    drawHuePanel(canvas);
  }

  private void drawSatValPanel(Canvas canvas) {
    final RectF rect = satValRect;

    if (BORDER_WIDTH_PX > 0) {
      borderPaint.setColor(borderColor);
      canvas.drawRect(drawingRect.left, drawingRect.top, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX, borderPaint);
    }

    if (valShader == null) {
      valShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, 0xffffffff, 0xff000000, TileMode.CLAMP);
    }

    int rgb = Color.HSVToColor(new float[]{hue, 1f, 1f});

    satShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, 0xffffffff, rgb, TileMode.CLAMP);
    ComposeShader mShader = new ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY);
    satValPaint.setShader(mShader);
    canvas.drawRect(rect, satValPaint);

    Point p = satValToPoint(sat, val);

    satValTrackerPaint.setColor(0xff000000);
    canvas.drawCircle(p.x, p.y, PALETTE_CIRCLE_TRACKER_RADIUS - 1f * density, satValTrackerPaint);

    satValTrackerPaint.setColor(0xffdddddd);
    canvas.drawCircle(p.x, p.y, PALETTE_CIRCLE_TRACKER_RADIUS, satValTrackerPaint);
  }

  private void drawHuePanel(Canvas canvas) {
    final RectF rect = hueRect;

    if (BORDER_WIDTH_PX > 0) {
      borderPaint.setColor(borderColor);
      canvas.drawRect(rect.left - BORDER_WIDTH_PX, rect.top - BORDER_WIDTH_PX, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX, borderPaint);
    }

    if (hueShader == null) {
      hueShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, buildHueColorArray(), null, TileMode.CLAMP);
      huePaint.setShader(hueShader);
    }

    canvas.drawRect(rect, huePaint);

    float rectHeight = 4 * density / 2;

    Point p = hueToPoint(hue);

    RectF r = new RectF();
    r.left = rect.left - RECTANGLE_TRACKER_OFFSET;
    r.right = rect.right + RECTANGLE_TRACKER_OFFSET;
    r.top = p.y - rectHeight;
    r.bottom = p.y + rectHeight;


    canvas.drawRoundRect(r, 2, 2, hueTrackerPaint);

  }

  private Point hueToPoint(float hue) {
    final RectF rect = hueRect;
    final float height = rect.height();
    Point p = new Point();
    p.y = (int) (height - (hue * height / 360f) + rect.top);
    p.x = (int) rect.left;

    return p;
  }

  private Point satValToPoint(float sat, float val) {
    final RectF rect = satValRect;
    final float height = rect.height();
    final float width = rect.width();
    Point p = new Point();
    p.x = (int) (sat * width + rect.left);
    p.y = (int) ((1f - val) * height + rect.top);

    return p;
  }

  private float[] pointToSatVal(float x, float y) {
    final RectF rect = satValRect;
    float[] result = new float[2];
    float width = rect.width();
    float height = rect.height();

    if (x < rect.left) {
      x = 0f;
    } else if (x > rect.right) {
      x = width;
    } else {
      x = x - rect.left;
    }

    if (y < rect.top) {
      y = 0f;
    } else if (y > rect.bottom) {
      y = height;
    } else {
      y = y - rect.top;
    }

    result[0] = 1.f / width * x;
    result[1] = 1.f - (1.f / height * y);

    return result;
  }

  private float pointToHue(float y) {
    final RectF rect = hueRect;
    float height = rect.height();

    if (y < rect.top) {
      y = 0f;
    } else if (y > rect.bottom) {
      y = height;
    } else {
      y = y - rect.top;
    }

    return 360f - (y * 360f / height);
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();
    boolean update = false;

    if (event.getAction() == MotionEvent.ACTION_MOVE) {
      switch (lastTouchedPanel) {
        case PANEL_SAT_VAL:
          float sat, val;
          sat = this.sat + x / 50f;
          val = this.val - y / 50f;

          if (sat < 0f) {
            sat = 0f;
          } else if (sat > 1f) {
            sat = 1f;
          }

          if (val < 0f) {
            val = 0f;
          } else if (val > 1f) {
            val = 1f;
          }

          this.sat = sat;
          this.val = val;
          update = true;
          break;
        case PANEL_HUE:
          float hue = this.hue - y * 10f;

          if (hue < 0f) {
            hue = 0f;
          } else if (hue > 360f) {
            hue = 360f;
          }

          this.hue = hue;
          update = true;
          break;
      }
    }

    if (update) {
      if (colorChangedListener != null) {
        colorChangedListener.onColorChanged(Color.HSVToColor(alpha, new float[]{hue, sat, val}));
      }

      invalidate();
      return true;
    }

    return super.onTrackballEvent(event);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean update = false;

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        startTouchPoint = new Point((int) event.getX(), (int) event.getY());
        update = moveTrackersIfNeeded(event);
        break;
      case MotionEvent.ACTION_MOVE:
        update = moveTrackersIfNeeded(event);
        break;
      case MotionEvent.ACTION_UP:
        startTouchPoint = null;
        update = moveTrackersIfNeeded(event);
        break;
    }

    if (update) {
      if (colorChangedListener != null) {
        colorChangedListener.onColorChanged(Color.HSVToColor(alpha, new float[]{hue, sat, val}));
      }

      invalidate();
      return true;
    }

    return super.onTouchEvent(event);
  }

  private boolean moveTrackersIfNeeded(MotionEvent event) {
    if (startTouchPoint == null) {
      return false;
    }

    int startX = startTouchPoint.x;
    int startY = startTouchPoint.y;

    if (hueRect.contains(startX, startY)) {
      lastTouchedPanel = PANEL_HUE;
      hue = pointToHue(event.getY());
      return true;
    } else if (satValRect.contains(startX, startY)) {
      lastTouchedPanel = PANEL_SAT_VAL;
      float[] result = pointToSatVal(event.getX(), event.getY());
      sat = result[0];
      val = result[1];
      return true;
    }

    return false;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width;
    int height;

    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);

    int widthAllowed = MeasureSpec.getSize(widthMeasureSpec);
    int heightAllowed = MeasureSpec.getSize(heightMeasureSpec);

    widthAllowed = chooseWidth(widthMode, widthAllowed);
    heightAllowed = chooseHeight(heightMode, heightAllowed);

    height = (int) (widthAllowed - PANEL_SPACING - HUE_PANEL_WIDTH);

    //If calculated height (based on the width) is more than the allowed height.
    if (height > heightAllowed || getTag().equals("landscape")) {
      height = heightAllowed;
      width = (int) (height + PANEL_SPACING + HUE_PANEL_WIDTH);
    } else {
      width = widthAllowed;
    }

    setMeasuredDimension(width, height);
  }

  private int chooseWidth(int mode, int size) {
    if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
      return size;
    } else { // (mode == MeasureSpec.UNSPECIFIED)
      return getPreferredWidth();
    }
  }

  private int chooseHeight(int mode, int size) {
    if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
      return size;
    } else { // (mode == MeasureSpec.UNSPECIFIED)
      return getPreferredHeight();
    }
  }

  private int getPreferredWidth() {
    return (int) (getPreferredHeight() + HUE_PANEL_WIDTH + PANEL_SPACING);
  }

  private int getPreferredHeight() {
    return (int) (200 * density);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    drawingRect = new RectF();
    drawingRect.left = drawingOffset + getPaddingLeft();
    drawingRect.right = w - drawingOffset - getPaddingRight();
    drawingRect.top = drawingOffset + getPaddingTop();
    drawingRect.bottom = h - drawingOffset - getPaddingBottom();

    setUpSatValRect();
    setUpHueRect();
  }

  private void setUpSatValRect() {
    final RectF dRect = drawingRect;
    float panelSide = dRect.height() - BORDER_WIDTH_PX * 2;
    float left = dRect.left + BORDER_WIDTH_PX;
    float top = dRect.top + BORDER_WIDTH_PX;
    float bottom = top + panelSide;
    float right = left + panelSide;

    satValRect = new RectF(left, top, right, bottom);
  }

  private void setUpHueRect() {
    final RectF dRect = drawingRect;

    float left = dRect.right - HUE_PANEL_WIDTH + BORDER_WIDTH_PX;
    float top = dRect.top + BORDER_WIDTH_PX;
    float bottom = dRect.bottom - BORDER_WIDTH_PX;
    float right = dRect.right - BORDER_WIDTH_PX;

    hueRect = new RectF(left, top, right, bottom);
  }

  public void setOnColorChangedListener(OnColorChangedListener listener) {
    this.colorChangedListener = listener;
  }

  public int getColor() {
    return Color.HSVToColor(alpha, new float[]{hue, sat, val});
  }

  public void setColor(int color) {
    setColor(color, false);
  }

  public void setColor(int color, boolean callback) {
    int alpha = Color.alpha(color);
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);

    this.alpha = alpha;
    hue = hsv[0];
    sat = hsv[1];
    val = hsv[2];

    if (callback && colorChangedListener != null) {
      colorChangedListener.onColorChanged(Color.HSVToColor(this.alpha, new float[]{hue, sat, val}));
    }

    invalidate();
  }

  /**
   * Get the drawing offset of the color picker view. The drawing offset is the distance from the side of a panel to the side of the view minus the padding.
   * Useful if you want to have your own panel below showing the currently selected color and want to align it perfectly.
   *
   * @return The offset in pixels.
   */
  public float getDrawingOffset() {
    return drawingOffset;
  }

  public interface OnColorChangedListener {
    public void onColorChanged(int color);
  }
}
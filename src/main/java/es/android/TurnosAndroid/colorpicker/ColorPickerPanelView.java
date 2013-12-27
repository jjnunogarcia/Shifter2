package es.android.TurnosAndroid.colorpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * This class draws a panel which which will be filled with a color which can be set.
 * It can be used to show the currently selected color which you will get from
 * the {@link ColorPickerView}.
 *
 * @author Daniel Nilsson
 */
public class ColorPickerPanelView extends View {

  /**
   * The width in pixels of the border surrounding the color panel.
   */
  private final static float BORDER_WIDTH_PX = 1;
  private              float density         = 1f;
  private              int   borderColor     = 0xff6E6E6E;
  private              int   color           = 0xff000000;
  private Paint                borderPaint;
  private Paint                colorPaint;
  private RectF                drawingRect;
  private RectF                colorRect;
  private AlphaPatternDrawable alphaPattern;

  public ColorPickerPanelView(Context context) {
    super(context);
    init();
  }

  public ColorPickerPanelView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public ColorPickerPanelView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    borderPaint = new Paint();
    colorPaint = new Paint();
    density = getContext().getResources().getDisplayMetrics().density;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    final RectF rect = colorRect;

    if (BORDER_WIDTH_PX > 0) {
      borderPaint.setColor(borderColor);
      canvas.drawRect(drawingRect, borderPaint);
    }

    if (alphaPattern != null) {
      alphaPattern.draw(canvas);
    }

    colorPaint.setColor(color);

    canvas.drawRect(rect, colorPaint);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    setMeasuredDimension(width, height);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    drawingRect = new RectF();
    drawingRect.left = getPaddingLeft();
    drawingRect.right = w - getPaddingRight();
    drawingRect.top = getPaddingTop();
    drawingRect.bottom = h - getPaddingBottom();

    setUpColorRect();

  }

  private void setUpColorRect() {
    final RectF dRect = drawingRect;

    float left = dRect.left + BORDER_WIDTH_PX;
    float top = dRect.top + BORDER_WIDTH_PX;
    float bottom = dRect.bottom - BORDER_WIDTH_PX;
    float right = dRect.right - BORDER_WIDTH_PX;

    colorRect = new RectF(left, top, right, bottom);
    alphaPattern = new AlphaPatternDrawable((int) (5 * density));

    alphaPattern.setBounds(Math.round(colorRect.left), Math.round(colorRect.top), Math.round(colorRect.right), Math.round(colorRect.bottom));
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
    invalidate();
  }

  public int getBorderColor() {
    return borderColor;
  }

  public void setBorderColor(int color) {
    borderColor = color;
    invalidate();
  }

}
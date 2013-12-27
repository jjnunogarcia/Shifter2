package es.android.TurnosAndroid.colorpicker;

import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;

/**
 * This drawable that draws a simple white and gray chessboard pattern. It's pattern you will often see as a background behind a partly transparent image in many applications.
 *
 * @author Daniel Nilsson
 */
public class AlphaPatternDrawable extends Drawable {
  private int    rectangleSize;
  private Paint  paint;
  private Paint  paintWhite;
  private Paint  paintGray;
  private int    numRectanglesHorizontal;
  private int    numRectanglesVertical;
  private Bitmap bitmap;

  public AlphaPatternDrawable(int rectangleSize) {
    this.rectangleSize = rectangleSize;
    paint = new Paint();
    paintWhite = new Paint();
    paintGray = new Paint();
    paintWhite.setColor(0xffffffff);
    paintGray.setColor(0xffcbcbcb);
  }

  @Override
  public void draw(Canvas canvas) {
    canvas.drawBitmap(bitmap, null, getBounds(), paint);
  }

  @Override
  public int getOpacity() {
    return 0;
  }

  @Override
  public void setAlpha(int alpha) {
    throw new UnsupportedOperationException("Alpha is not supported by this drawable.");
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    throw new UnsupportedOperationException("ColorFilter is not supported by this drawable.");
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);

    numRectanglesHorizontal = (int) Math.ceil((bounds.width() / rectangleSize));
    numRectanglesVertical = (int) Math.ceil(bounds.height() / rectangleSize);

    generatePatternBitmap();
  }

  /**
   * This will generate a bitmap with the pattern as big as the rectangle we were allow to draw on.
   * We do this to cache the bitmap so we don't need to recreate it each time draw() is called since it takes a few milliseconds.
   */
  private void generatePatternBitmap() {
    if (getBounds().width() <= 0 || getBounds().height() <= 0) {
      return;
    }

    bitmap = Bitmap.createBitmap(getBounds().width(), getBounds().height(), Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);

    Rect r = new Rect();
    boolean verticalStartWhite = true;

    for (int i = 0; i <= numRectanglesVertical; i++) {
      boolean isWhite = verticalStartWhite;

      for (int j = 0; j <= numRectanglesHorizontal; j++) {
        r.top = i * rectangleSize;
        r.left = j * rectangleSize;
        r.bottom = r.top + rectangleSize;
        r.right = r.left + rectangleSize;

        canvas.drawRect(r, isWhite ? paintWhite : paintGray);
        isWhite = !isWhite;
      }

      verticalStartWhite = !verticalStartWhite;
    }
  }

}
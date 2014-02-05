package es.android.TurnosAndroid.model;

/**
 * User: Jesús
 * Date: 5/02/14
 */
public class EventPoints {
  private float[] points;
  private int     color;

  public EventPoints() {
    this.points = new float[4];
  }

  public float[] getPoints() {
    return points;
  }

  public void setPoints(float[] points) {
    this.points = points;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }
}

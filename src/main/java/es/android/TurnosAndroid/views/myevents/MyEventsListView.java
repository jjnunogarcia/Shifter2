package es.android.TurnosAndroid.views.myevents;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * User: Jesús
 * Date: 23/12/13
 */
public class MyEventsListView extends ListView {
  public MyEventsListView(Context context) {
    super(context);
    init(context);
  }

  public MyEventsListView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public MyEventsListView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {

  }
}

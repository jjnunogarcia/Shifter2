package es.android.TurnosAndroid.views.mypatterns;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * User: Jesús
 * Date: 23/12/13
 */
public class MyPatternsListView extends ListView {
  public MyPatternsListView(Context context) {
    super(context);
    init(context);
  }

  public MyPatternsListView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public MyPatternsListView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {

  }
}

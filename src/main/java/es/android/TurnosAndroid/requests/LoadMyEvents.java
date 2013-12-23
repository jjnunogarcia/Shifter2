package es.android.TurnosAndroid.requests;

import android.content.Context;
import es.android.TurnosAndroid.views.myevents.MyEventsAdapter;

/**
 * User: Jesús
 * Date: 23/12/13
 */
public class LoadMyEvents extends Thread {
  private Context         context;
  private MyEventsAdapter myEventsAdapter;

  public LoadMyEvents(Context context, MyEventsAdapter myEventsAdapter) {
    this.context = context;
    this.myEventsAdapter = myEventsAdapter;
  }

  @Override
  public void run() {
    // TODO load my events with content resolver and, when finished, set them to the adapter. Is this maybe better in a Loader?? --> Read documentation
  }
}

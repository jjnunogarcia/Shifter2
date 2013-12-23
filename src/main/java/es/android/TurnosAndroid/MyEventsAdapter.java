package es.android.TurnosAndroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * User: Jesús
 * Date: 23/12/13
 */
public class MyEventsAdapter extends BaseAdapter {
  private final Context context;
  private ArrayList<Event> events;

  public MyEventsAdapter(Context context, ArrayList<Event> events) {
    this.context = context;
    this.events = events;
  }

  @Override
  public int getCount() {
    return events.size();
  }

  @Override
  public Object getItem(int position) {
    return events.get(position);
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view;

    // TODO implement ViewHolder
    if (convertView != null) {
      view = convertView;
    } else {
      view = LayoutInflater.from(context).inflate(R.layout.my_events_row, parent, false);
    }

    Event event = events.get(position);
    TextView eventTitle = (TextView) view.findViewById(R.id.event_title);
    eventTitle.setText(event.title);

    return view;
  }
}

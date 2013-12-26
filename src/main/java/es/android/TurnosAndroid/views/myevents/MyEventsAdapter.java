package es.android.TurnosAndroid.views.myevents;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.model.Event;

import java.util.ArrayList;

/**
 * User: Jesús
 * Date: 23/12/13
 */
public class MyEventsAdapter extends BaseAdapter {
  private final Context          context;
  private       ArrayList<Event> events;

  public MyEventsAdapter(Context context, ArrayList<Event> events) {
    this.context = context;
    this.events = events;
  }

  public void setMyEvents(ArrayList<Event> events) {
    this.events = events;
    notifyDataSetChanged();
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
    TextView name = (TextView) view.findViewById(R.id.my_events_row_name);
    TextView description = (TextView) view.findViewById(R.id.my_events_row_description);
    TextView startTime = (TextView) view.findViewById(R.id.my_events_row_start_time);
    TextView duration = (TextView) view.findViewById(R.id.my_events_row_duration);
    TextView location = (TextView) view.findViewById(R.id.my_events_row_location);
    RelativeLayout color = (RelativeLayout) view.findViewById(R.id.my_events_row_color);
    name.setText(event.getName());
    description.setText(event.getDescription());
//    startTime.setText((int) event.getStartTime());
//    duration.setText((int) event.getDuration());
    location.setText(event.getLocation());
//    color.setBackgroundColor(Color.parseColor(event.getColor()));

    return view;
  }
}

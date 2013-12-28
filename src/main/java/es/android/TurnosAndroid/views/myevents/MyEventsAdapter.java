package es.android.TurnosAndroid.views.myevents;

import android.content.Context;
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
    View view = convertView;
    ViewHolder viewHolder;

    if (convertView == null) {
      view = LayoutInflater.from(context).inflate(R.layout.my_events_row, parent, false);
      viewHolder = new ViewHolder();
      viewHolder.name = (TextView) view.findViewById(R.id.my_events_row_name);
      viewHolder.description = (TextView) view.findViewById(R.id.my_events_row_description);
      viewHolder.startTime = (TextView) view.findViewById(R.id.my_events_row_start_time);
      viewHolder.duration = (TextView) view.findViewById(R.id.my_events_row_duration);
      viewHolder.location = (TextView) view.findViewById(R.id.my_events_row_location);
      viewHolder.color = (RelativeLayout) view.findViewById(R.id.my_events_row_color);
      view.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder) view.getTag();
    }

    Event event = events.get(position);
    viewHolder.name.setText(event.getName());
    viewHolder.description.setText(event.getDescription());
    viewHolder.startTime.setText(String.valueOf(event.getStartTime()));
    viewHolder.duration.setText(String.valueOf(event.getDuration()));
    viewHolder.location.setText(event.getLocation());
    viewHolder.color.setBackgroundColor(event.getColor());

    return view;
  }

  private static class ViewHolder {
    TextView       name;
    TextView       description;
    TextView       startTime;
    TextView       duration;
    TextView       location;
    RelativeLayout color;
  }
}

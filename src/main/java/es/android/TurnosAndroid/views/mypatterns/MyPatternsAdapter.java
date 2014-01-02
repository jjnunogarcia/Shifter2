package es.android.TurnosAndroid.views.mypatterns;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.model.Pattern;

import java.util.ArrayList;

/**
 * User: Jesús
 * Date: 23/12/13
 */
public class MyPatternsAdapter extends BaseAdapter {
  private final Context            context;
  private       ArrayList<Pattern> patterns;

  public MyPatternsAdapter(Context context, ArrayList<Pattern> patterns) {
    this.context = context;
    this.patterns = patterns;
  }

  public void setMyPatterns(ArrayList<Pattern> patterns) {
    this.patterns = patterns;
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return patterns.size();
  }

  @Override
  public Object getItem(int position) {
    return patterns.get(position);
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
      view = LayoutInflater.from(context).inflate(R.layout.my_patterns_row, parent, false);
      viewHolder = new ViewHolder();
      viewHolder.id = (TextView) view.findViewById(R.id.my_patterns_row_id);
      view.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder) view.getTag();
    }

    Pattern pattern = patterns.get(position);
    viewHolder.id.setText(pattern.getId());

    return view;
  }

  private static class ViewHolder {
    TextView id;
  }
}

package es.android.TurnosAndroid.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockFragment;
import es.android.TurnosAndroid.R;

/**
 * User: Jesús
 * Date: 2/01/14
 */
public class StatisticsFragment extends SherlockFragment {
  public static final String TAG = StatisticsFragment.class.getSimpleName();

  public StatisticsFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.statistics_fragment, container, false);
    return view;
  }
}

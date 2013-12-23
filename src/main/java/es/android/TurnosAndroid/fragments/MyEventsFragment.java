package es.android.TurnosAndroid.fragments;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import es.android.TurnosAndroid.model.Event;
import es.android.TurnosAndroid.views.myevents.MyEventsAdapter;
import es.android.TurnosAndroid.R;

import java.util.ArrayList;

/**
 * Date: 18.12.13
 *
 * @author jjnunogarcia@gmail.com
 */
public class MyEventsFragment extends ListFragment {
  public static final String TAG = MyEventsFragment.class.getSimpleName();
  private MyEventsAdapter adapter;

  public MyEventsFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.my_events_fragment, container, false);
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // TODO pass events retrieved from content resolver. Probably in the Event.java there's something useful
    adapter = new MyEventsAdapter(getActivity().getApplicationContext(), new ArrayList<Event>());
    setListAdapter(adapter);
  }
}

package es.android.TurnosAndroid.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.model.Event;
import es.android.TurnosAndroid.views.myevents.MyEventsAdapter;

import java.util.ArrayList;

/**
 * Date: 18.12.13
 *
 * @author jjnunogarcia@gmail.com
 */
public class MyEventsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
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
    // TODO move this to loader?
    Cursor eventsCursor = getActivity().getApplicationContext().getContentResolver().query(CalendarProvider.CONTENT_URI,
                                                                                           Event.EVENT_PROJECTION,                                                                                           null,
                                                                                           null,
                                                                                           Event.SORT_EVENTS_BY);
    adapter.setMyEvents(Event.getMyEvents(eventsCursor));
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(getActivity().getApplicationContext(), CalendarProvider.CONTENT_URI, Event.EVENT_PROJECTION, null, null, null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    adapter.setMyEvents(Event.getMyEvents(data));
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {}
}

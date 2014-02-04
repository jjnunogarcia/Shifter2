package es.android.TurnosAndroid.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import es.android.TurnosAndroid.MainActivity;
import es.android.TurnosAndroid.MyEventsActionBarInterface;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.database.DBConstants;
import es.android.TurnosAndroid.helpers.Utils;
import es.android.TurnosAndroid.model.Event;
import es.android.TurnosAndroid.views.myevents.MyEventsAdapter;

import java.util.ArrayList;

/**
 * Date: 18.12.13
 *
 * @author jjnunogarcia@gmail.com
 */
public class MyEventsFragment extends ListFragment implements LoaderCallbacks<Cursor>, MyEventsActionBarInterface {
  public static final String TAG       = MyEventsFragment.class.getSimpleName();
  public static final int    LOADER_ID = 1;
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
    adapter = new MyEventsAdapter(getActivity().getApplicationContext(), new ArrayList<Event>());
    setListAdapter(adapter);
    getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    ((MainActivity) getActivity()).getActionBarManager().setMyEventsActionBarInterface(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (getActivity() != null && getActivity().getSupportLoaderManager().getLoader(LOADER_ID) != null) {
      getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(getActivity().getApplicationContext(), CalendarProvider.EVENTS_URI, DBConstants.EVENTS_PROJECTION, null, null, DBConstants.SORT_EVENTS_BY_NAME_ASC);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    adapter.setMyEvents(Utils.getMyEvents(data));
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {}

  @Override
  public void onNewEventClicked() {
    ((MainActivity) getActivity()).addCreateEventFragment();
  }
}

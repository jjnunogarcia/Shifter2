package es.android.TurnosAndroid.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockListFragment;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.database.DBConstants;
import es.android.TurnosAndroid.model.Pattern;
import es.android.TurnosAndroid.views.mypatterns.MyPatternsAdapter;

import java.util.ArrayList;

/**
 * Date: 18.12.13
 *
 * @author jjnunogarcia@gmail.com
 */
public class MyPatternsFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {
  public static final String TAG       = MyPatternsFragment.class.getSimpleName();
  public static final int    LOADER_ID = 2;
  private MyPatternsAdapter adapter;

  public MyPatternsFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.my_patterns_fragment, container, false);
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    adapter = new MyPatternsAdapter(getActivity().getApplicationContext(), new ArrayList<Pattern>());
    setListAdapter(adapter);
//    getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, this);
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
    return new CursorLoader(getActivity().getApplicationContext(), CalendarProvider.PATTERNS_URI, DBConstants.PATTERNS_PROJECTION, null, null, DBConstants.SORT_PATTERNS_BY);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//    adapter.setMyPatterns(Utils.getMyPatterns(data));
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {}
}

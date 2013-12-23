package es.android.TurnosAndroid;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.database.DBConstants;

/**
 * Date: 19.12.13
 *
 * @author nuno@neofonie.de
 */
public class CreateEventFragment extends Fragment {
  public static final String TAG = CreateEventFragment.class.getSimpleName();

  public CreateEventFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.create_event_fragment, container, false);

    return view;
  }

  public void saveEvent() {
    if (getActivity() != null) {
      ContentResolver contentResolver = getActivity().getApplicationContext().getContentResolver();
      ContentValues contentValues = new ContentValues();
      contentValues.put(DBConstants.NAME, "name");
      contentValues.put(DBConstants.DESCRIPTION, "description");
      contentValues.put(DBConstants.START_TIME, 10000000);
      contentValues.put(DBConstants.DURATION, 3600);
      contentValues.put(DBConstants.START_DAY, 10000000);
      contentValues.put(DBConstants.END_DAY, 10002000);
      contentValues.put(DBConstants.LOCATION, "location");
      contentValues.put(DBConstants.DISPLAY_COLOR, "the_color");
      Uri eventUri = contentResolver.insert(CalendarProvider.CONTENT_URI, contentValues);
      Toast.makeText(getActivity().getApplicationContext(), eventUri.toString(), Toast.LENGTH_SHORT).show();
    }
  }

  public void deleteEvent() {
    if (getActivity() != null) {
      ContentResolver contentResolver = getActivity().getApplicationContext().getContentResolver();
      String where = DBConstants.ID + "=?";
      String[] selectionArgs = new String[] {"2"};
      int rowsDeleted = contentResolver.delete(CalendarProvider.CONTENT_URI, where, selectionArgs);
      Toast.makeText(getActivity().getApplicationContext(), "Rows deleted: " + rowsDeleted, Toast.LENGTH_SHORT).show();
    }
  }
}

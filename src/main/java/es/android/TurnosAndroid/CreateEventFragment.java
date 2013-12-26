package es.android.TurnosAndroid;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
  private EditText name;
  private EditText description;
  private EditText startTime;
  private EditText duration;
  private EditText location;
  private Button   color;

  public CreateEventFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.create_event_fragment, container, false);
    name = (EditText) view.findViewById(R.id.create_event_name);
    description = (EditText) view.findViewById(R.id.create_event_description);
    startTime = (EditText) view.findViewById(R.id.create_event_start_time);
    duration = (EditText) view.findViewById(R.id.create_event_duration);
    location = (EditText) view.findViewById(R.id.create_event_location);
    color = (Button) view.findViewById(R.id.create_event_color);
    return view;
  }

  public void saveEvent() {
    if (getActivity() != null) {
      ContentResolver contentResolver = getActivity().getApplicationContext().getContentResolver();
      ContentValues contentValues = new ContentValues();
      contentValues.put(DBConstants.NAME, name.getText().toString());
      contentValues.put(DBConstants.DESCRIPTION, description.getText().toString());
      contentValues.put(DBConstants.START, Long.valueOf(startTime.getText().toString()));
      contentValues.put(DBConstants.DURATION, Long.valueOf(duration.getText().toString()));
      contentValues.put(DBConstants.LOCATION, location.getText().toString());
      contentValues.put(DBConstants.COLOR, Color.parseColor("#ff000000"));
      Uri eventUri = contentResolver.insert(CalendarProvider.CONTENT_URI, contentValues);
      Toast.makeText(getActivity().getApplicationContext(), eventUri.toString(), Toast.LENGTH_SHORT).show();
    }
  }

  public void deleteEvent() {
    if (getActivity() != null) {
      ContentResolver contentResolver = getActivity().getApplicationContext().getContentResolver();
      String where = DBConstants.ID + "=?";
      String[] selectionArgs = new String[]{"2"};
      int rowsDeleted = contentResolver.delete(CalendarProvider.CONTENT_URI, where, selectionArgs);
      Toast.makeText(getActivity().getApplicationContext(), "Rows deleted: " + rowsDeleted, Toast.LENGTH_SHORT).show();
    }
  }
}

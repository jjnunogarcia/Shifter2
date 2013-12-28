package es.android.TurnosAndroid.fragments;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import es.android.TurnosAndroid.CreateEventActionBarInterface;
import es.android.TurnosAndroid.MainActivity;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.colorpicker.ColorPickerDialog;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.database.DBConstants;

/**
 * Date: 19.12.13
 *
 * @author jjnunogarcia@gmail.com
 */
public class CreateEventFragment extends Fragment implements ColorPickerDialog.OnColorChangedListener, CreateEventActionBarInterface {
  public static final String TAG = CreateEventFragment.class.getSimpleName();
  private EditText name;
  private EditText description;
  private EditText startTime;
  private EditText duration;
  private EditText location;
  private Button   colorButton;
  private int      colorValue;
  private OnClickListener onClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      showDialog();
    }
  };

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
    colorButton = (Button) view.findViewById(R.id.create_event_color);
    colorValue = Color.BLACK;
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    colorButton.setOnClickListener(onClickListener);
    ((MainActivity) getActivity()).getActionBarManager().setCreateEventActionBarInterface(this);
  }

  @Override
  public void onSaveEventClicked() {
    saveEvent();
  }

  @Override
  public void onDeleteEventClicked() {
    deleteEvent();
  }

  private void saveEvent() {
    if (getActivity() != null) {
      ContentResolver contentResolver = getActivity().getApplicationContext().getContentResolver();
      ContentValues contentValues = new ContentValues();
      contentValues.put(DBConstants.NAME, name.getText().toString());
      contentValues.put(DBConstants.DESCRIPTION, description.getText().toString());
      contentValues.put(DBConstants.START, Long.valueOf(startTime.getText().toString()));
      contentValues.put(DBConstants.DURATION, Long.valueOf(duration.getText().toString()));
      contentValues.put(DBConstants.LOCATION, location.getText().toString());
      contentValues.put(DBConstants.COLOR, colorValue);
      Uri eventUri = contentResolver.insert(CalendarProvider.EVENTS_URI, contentValues);
      ((MainActivity) getActivity()).addMyEventsFragment();
      Toast.makeText(getActivity().getApplicationContext(), eventUri.toString(), Toast.LENGTH_SHORT).show();
    }
  }

  private void deleteEvent() {
    if (getActivity() != null) {
      ContentResolver contentResolver = getActivity().getApplicationContext().getContentResolver();
      String where = DBConstants.ID + "=?";
      String[] selectionArgs = new String[]{"2"};
      int rowsDeleted = contentResolver.delete(CalendarProvider.EVENTS_URI, where, selectionArgs);
      Toast.makeText(getActivity().getApplicationContext(), "Rows deleted: " + rowsDeleted, Toast.LENGTH_SHORT).show();
    }
  }

  private void showDialog() {
    ColorPickerDialog dialog = new ColorPickerDialog();
    dialog.setOnColorChangedListener(this);
    Bundle bundle = new Bundle();
    bundle.putInt(ColorPickerDialog.INITIAL_COLOR, colorValue);
    dialog.setArguments(bundle);
    FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
    fragmentTransaction.add(dialog, ColorPickerDialog.TAG).addToBackStack(ColorPickerDialog.TAG).commit();
  }

  @Override
  public void onColorChanged(int color) {
    colorValue = color;
    colorButton.setBackgroundColor(color);
  }
}

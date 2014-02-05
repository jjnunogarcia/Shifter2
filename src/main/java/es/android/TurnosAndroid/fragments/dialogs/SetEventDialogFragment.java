package es.android.TurnosAndroid.fragments.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.colorpicker.ColorPickerDialog;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.database.DBConstants;
import es.android.TurnosAndroid.helpers.Utils;
import es.android.TurnosAndroid.interfaces.EventInteractionInterface;
import es.android.TurnosAndroid.model.Event;

public class SetEventDialogFragment extends DialogFragment implements ColorPickerDialog.OnColorChangedListener {
  public static final String TAG = SetEventDialogFragment.class.getSimpleName();

  private EditText                  name;
  private EditText                  description;
  private EditText                  startTime;
  private EditText                  duration;
  private EditText                  location;
  private View                      colorView;
  private int                       colorValue;
  private Event                     eventToOpen;
  private EventInteractionInterface eventInteractionInterface;
  private OnClickListener onClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      showColorPickerDialog();
    }
  };

  public SetEventDialogFragment() {
    eventToOpen = null;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = getActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.set_event_fragment, null);
    initViews(view);

    final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
        .setView(view)
        .setPositiveButton(getResources().getString(R.string.save), null)
        .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();
          }
        }).create();

    setDialogOnShowListener(alertDialog);

    return alertDialog;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    eventInteractionInterface = (EventInteractionInterface) getActivity();
    colorValue = getResources().getColor(R.color.turnos_black);
    Bundle arguments = getArguments();

    if (arguments != null) {
      eventToOpen = arguments.getParcelable(Utils.KEY_EVENT_TO_MANAGE);

      if (eventToOpen != null) {
        setValuesToDialogFields();
        getDialog().setTitle(R.string.edit_event_dialog_title);
      } else {
        getDialog().setTitle(R.string.new_event_dialog_title);
        GradientDrawable drawable = (GradientDrawable) colorView.getBackground();
        drawable.setColor(colorValue);
      }
    }
  }

  private void initViews(View view) {
    name = (EditText) view.findViewById(R.id.set_event_name);
    description = (EditText) view.findViewById(R.id.set_event_description);
    startTime = (EditText) view.findViewById(R.id.set_event_start_time);
    duration = (EditText) view.findViewById(R.id.set_event_duration);
    location = (EditText) view.findViewById(R.id.set_event_location);
    colorView = view.findViewById(R.id.set_event_button_color);
    colorView.setOnClickListener(onClickListener);

//    name.setTypeface(Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), Utils.FONT_PATH_ROBOTO_REGULAR));
//    description.setTypeface(Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), Utils.FONT_PATH_ROBOTO_REGULAR));
//    colorTextView.setTypeface(Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), Utils.FONT_PATH_ROBOTO_REGULAR));
  }

  private void setDialogOnShowListener(final AlertDialog alertDialog) {
    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialogInterface) {
        Button b = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        b.setOnClickListener(new OnClickListener() {

          @Override
          public void onClick(View view) {
            if (name.getText().toString().length() == 0) {
              Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.save_event_error_message), Toast.LENGTH_SHORT).show();
            } else if (eventToOpen == null) {
              saveEvent();
              alertDialog.dismiss();
            } else {
              updateEvent();
              alertDialog.dismiss();
            }
          }
        });
      }
    });
  }

  private void setValuesToDialogFields() {
    name.setText(eventToOpen.getName());
    description.setText(eventToOpen.getDescription());
    startTime.setText(String.valueOf(eventToOpen.getStartTime()));
    duration.setText(String.valueOf(eventToOpen.getDuration()));
    location.setText(eventToOpen.getLocation());
    GradientDrawable drawable = (GradientDrawable) colorView.getBackground();
    drawable.setColor(eventToOpen.getColor());
    colorValue = eventToOpen.getColor();
  }

  private void saveEvent() {
    if (getActivity() != null) {
      Event event = new Event();
      long creationTime = System.currentTimeMillis();

      event.setName(name.getText().toString());
      event.setDescription(description.getText().toString());
      event.setStartTime(Integer.parseInt(startTime.getText().toString()));
      event.setDuration(Integer.parseInt(duration.getText().toString()));
      event.setLocation(location.getText().toString());
      event.setColor(colorValue);
      event.setCreationTime(creationTime);

      ContentResolver contentResolver = getActivity().getApplicationContext().getContentResolver();
      ContentValues contentValues = new ContentValues();
      contentValues.put(DBConstants.NAME, name.getText().toString());
      contentValues.put(DBConstants.DESCRIPTION, description.getText().toString());
      contentValues.put(DBConstants.START_TIME, Long.valueOf(startTime.getText().toString()));
      contentValues.put(DBConstants.DURATION, Long.valueOf(duration.getText().toString()));
      contentValues.put(DBConstants.LOCATION, location.getText().toString());
      contentValues.put(DBConstants.COLOR, colorValue);
      contentValues.put(DBConstants.CREATION_TIME, creationTime);
      Uri uriInserted = contentResolver.insert(CalendarProvider.EVENTS_URI, contentValues);

      if (uriInserted != null) { // success
        event.setId(Integer.valueOf(uriInserted.getLastPathSegment())); // set the id of the task according to where it is inserted
        eventInteractionInterface.onSaveEventClicked(event);
      }
    }
  }

  private void updateEvent() {
    if (getActivity() != null) {
      ContentResolver contentResolver = getActivity().getApplicationContext().getContentResolver();
      ContentValues contentValues = new ContentValues();

      String newName = name.getText().toString();
      if (!eventToOpen.getName().equals(newName)) {
        contentValues.put(DBConstants.NAME, newName);
        eventToOpen.setName(newName);
      }

      String newDescription = description.getText().toString();
      if (!eventToOpen.getDescription().equals(newDescription)) {
        contentValues.put(DBConstants.DESCRIPTION, newDescription);
        eventToOpen.setDescription(newDescription);
      }

      int newStartTime = Integer.parseInt(startTime.getText().toString());
      if (eventToOpen.getStartTime() != newStartTime) {
        contentValues.put(DBConstants.START_TIME, newStartTime);
        eventToOpen.setStartTime(newStartTime);
      }

      int newDuration = Integer.parseInt(duration.getText().toString());
      if (eventToOpen.getDuration() != newDuration) {
        contentValues.put(DBConstants.DURATION, newDuration);
        eventToOpen.setDuration(newDuration);
      }

      String newLocation = location.getText().toString();
      if (!eventToOpen.getLocation().equals(newLocation)) {
        contentValues.put(DBConstants.LOCATION, newLocation);
        eventToOpen.setLocation(newLocation);
      }

      if (eventToOpen.getColor() != colorValue) {
        contentValues.put(DBConstants.COLOR, colorValue);
        eventToOpen.setColor(colorValue);
      }

      String where = DBConstants.ID + "=?";
      String[] selectionArgs = new String[]{String.valueOf(eventToOpen.getId())};
      int rowsUpdated = contentResolver.update(CalendarProvider.EVENTS_URI, contentValues, where, selectionArgs);

      if (rowsUpdated > 0) { // success
        eventInteractionInterface.onUpdateEventClicked(eventToOpen);
      }
    }
  }

  private void showColorPickerDialog() {
    ColorPickerDialog colorPickerDialog = new ColorPickerDialog();
    colorPickerDialog.setOnColorChangedListener(this);
    Bundle bundle = new Bundle();
    bundle.putInt(ColorPickerDialog.INITIAL_COLOR, colorValue);
    colorPickerDialog.setArguments(bundle);
    FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
    colorPickerDialog.show(fragmentTransaction, ColorPickerDialog.TAG);
  }

  @Override
  public void onColorChanged(int color) {
    colorValue = color;
    GradientDrawable drawable = (GradientDrawable) colorView.getBackground();
    drawable.setColor(color);
  }
}

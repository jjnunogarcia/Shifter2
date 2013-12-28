package es.android.TurnosAndroid.colorpicker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.helpers.Utils;

import java.util.Locale;

public class ColorPickerDialog extends DialogFragment implements ColorPickerView.OnColorChangedListener, OnClickListener, OnEditorActionListener {
  public static final String TAG           = ColorPickerDialog.class.getSimpleName();
  public static final String INITIAL_COLOR = "initial_color";
  private ColorPickerView        colorPickerView;
  private ColorPickerPanelView   oldColor;
  private ColorPickerPanelView   newColor;
  private EditText               hexVal;
  private boolean                hexValueEnabled;
  private ColorStateList         hexDefaultTextColor;
  private OnColorChangedListener listener;

  public ColorPickerDialog() {
    hexValueEnabled = false;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // To fight color banding.
    getDialog().getWindow().setFormat(PixelFormat.RGBA_8888);
    View view = inflater.inflate(R.layout.dialog_color_picker, container, false);
    colorPickerView = (ColorPickerView) view.findViewById(R.id.color_picker_view);
    oldColor = (ColorPickerPanelView) view.findViewById(R.id.old_color_panel);
    newColor = (ColorPickerPanelView) view.findViewById(R.id.new_color_panel);
    hexVal = (EditText) view.findViewById(R.id.hex_val);

    hexVal.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    hexDefaultTextColor = hexVal.getTextColors();
    getDialog().setTitle(R.string.dialog_color_picker);
    hexVal.setOnEditorActionListener(this);
    ((LinearLayout) oldColor.getParent()).setPadding(Math.round(colorPickerView.getDrawingOffset()), 0, Math.round(colorPickerView.getDrawingOffset()), 0);

    oldColor.setOnClickListener(this);
    newColor.setOnClickListener(this);
    colorPickerView.setOnColorChangedListener(this);

    Bundle bundle = getArguments();
    int initialColor = Color.BLACK;

    if (bundle != null) {
      initialColor = bundle.getInt(INITIAL_COLOR, Color.BLACK);
    }

    oldColor.setColor(initialColor);
    colorPickerView.setColor(initialColor);
    return view;
  }

  @Override
  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
    if (actionId == EditorInfo.IME_ACTION_DONE) {
      InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
      String s = hexVal.getText().toString();
      if (s.length() > 5 || s.length() < 10) {
        try {
          int c = Color.parseColor(s);
          colorPickerView.setColor(c);
          hexVal.setTextColor(hexDefaultTextColor);
        } catch (IllegalArgumentException e) {
          hexVal.setTextColor(Color.RED);
        }
      } else {
        hexVal.setTextColor(Color.RED);
      }
      return true;
    }
    return false;
  }

  @Override
  public void onColorChanged(int color) {
    newColor.setColor(color);

    if (hexValueEnabled) {
      updateHexValue(color);
    }

    if (listener != null) {
      listener.onColorChanged(color);
    }
  }

  private void updateHexValue(int color) {
    hexVal.setText(Utils.convertToRGB(color).toUpperCase(Locale.getDefault()));
    hexVal.setTextColor(hexDefaultTextColor);
  }

  public int getColor() {
    return colorPickerView.getColor();
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.new_color_panel && listener != null) {
      listener.onColorChanged(newColor.getColor());
    }
    dismiss();
  }

  public void setOnColorChangedListener(OnColorChangedListener listener) {
    this.listener = listener;
  }

  public interface OnColorChangedListener {
    public void onColorChanged(int color);
  }
}

package es.android.TurnosAndroid.colorpicker;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import es.android.TurnosAndroid.R;

public class ColorPickerDialog extends DialogFragment implements ColorPickerView.OnColorChangedListener, OnClickListener {
  public static final String TAG           = ColorPickerDialog.class.getSimpleName();
  public static final String INITIAL_COLOR = "initial_color";

  private ColorPickerView        colorPickerView;
  private ColorPickerPanelView   oldColor;
  private ColorPickerPanelView   newColor;
  private OnColorChangedListener listener;

  public ColorPickerDialog() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // To fight color banding.
    getDialog().getWindow().setFormat(PixelFormat.RGBA_8888);
    View view = inflater.inflate(R.layout.dialog_color_picker, container, false);
    colorPickerView = (ColorPickerView) view.findViewById(R.id.color_picker_view);
    oldColor = (ColorPickerPanelView) view.findViewById(R.id.old_color_panel);
    newColor = (ColorPickerPanelView) view.findViewById(R.id.new_color_panel);

    getDialog().setTitle(R.string.dialog_color_picker);

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
  public void onColorChanged(int color) {
    newColor.setColor(color);
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

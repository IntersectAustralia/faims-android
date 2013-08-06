package au.org.intersect.faims.android.ui.dialog;

import java.util.HashMap;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import au.org.intersect.faims.android.exceptions.MapException;

public class SettingsDialog extends AlertDialog {
	
	public static class Builder {
		
		protected Context context;
		protected HashMap<String, View> fields;
		protected ScrollView scrollView;
		protected LinearLayout layout;
		protected String title;
		protected String message;
		protected String positiveLabel;
		protected OnClickListener positiveListener;
		protected String negativeLabel;
		protected OnClickListener negativeListener;
		private OnDismissListener dismissListener;

		public Builder(Context context) {
			this.context = context;
			this.fields = new HashMap<String, View>();
			scrollView = new ScrollView(context);
			layout = new LinearLayout(context);
			layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			layout.setOrientation(LinearLayout.VERTICAL);
			scrollView.addView(layout);
		}
		
		public Builder addTextField(String name, String label, String defaultValue) {
			return addEditField(name, label, defaultValue, InputType.TYPE_CLASS_TEXT);
		}
		
		public Builder addNumberField(String name, String label, String defaultValue) {
			return addEditField(name, label, defaultValue, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		}
		
		public Builder addEditField(String name, String label, String defaultValue, int type) {
			TextView labelView = new TextView(context);
			labelView.setText(label);
			
			EditText text = new EditText(context);
			text.setText(defaultValue.toUpperCase(Locale.ENGLISH));
			
			text.setInputType(type);
			
			layout.addView(labelView);
			layout.addView(text);
			
			fields.put(name, text);
			
			return this;
		}
		
		public Builder addCheckBox(String name, String label, boolean defaultValue) {
			TextView labelView = new TextView(context);
			labelView.setText(label);
			
			CheckBox box = new CheckBox(context);
			box.setChecked(defaultValue);
			
			layout.addView(labelView);
			layout.addView(box);
			
			fields.put(name, box);
			
			return this;
		}
		
		public Builder addSlider(String name, final String label, float defaultValue) {
			return addSlider(name, label, defaultValue, 0, 1);
		}
		
		public Builder addSlider(String name, final String label, float defaultValue, final float minRange, final float maxRange) {
			final TextView labelView = new TextView(context);
			labelView.setText(label + " " + Float.toString(defaultValue));
			
			final SeekBar seekBar = new SeekBar(context);
			seekBar.setMax(100);
			seekBar.setProgress((int) (100 * (defaultValue / (maxRange - minRange))));
			
			seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
					labelView.setText(label + " " + Float.toString(getSliderValue(seekBar.getProgress(), minRange, maxRange)));
				}

				@Override
				public void onStartTrackingTouch(SeekBar arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onStopTrackingTouch(SeekBar arg0) {
					// TODO Auto-generated method stub
					
				}
				
			});
			
			layout.addView(labelView);
			layout.addView(seekBar);
			
			fields.put(name, seekBar);
			
			return this;
		}
		
		public Builder addRange(String name, final String label, int defaultValue, final int minRange, final int maxRange) {
			final TextView labelView = new TextView(context);
			labelView.setText(label + " " + Integer.toString(defaultValue));
			
			final SeekBar seekBar = new SeekBar(context);
			seekBar.setMax(maxRange - minRange);
			seekBar.setProgress(defaultValue - minRange);
			
			seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
					labelView.setText(label + " " + Integer.toString(getRangeValue(seekBar.getProgress(), minRange, maxRange)));
				}

				@Override
				public void onStartTrackingTouch(SeekBar arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onStopTrackingTouch(SeekBar arg0) {
					// TODO Auto-generated method stub
					
				}
				
			});
			
			layout.addView(labelView);
			layout.addView(seekBar);
			
			fields.put(name, seekBar);
			
			return this;
		}
		
		public Builder setTitle(String title) {
			this.title = title;
			return this;
		}
		
		public Builder setMessage(String message) {
			this.message = message;
			return this;
		}
		
		public Builder setPositiveButton(String label, DialogInterface.OnClickListener listener) {
			this.positiveLabel = label;
			this.positiveListener = listener;
			return this;
		}
		
		public Builder setNegativeButton(String label, DialogInterface.OnClickListener listener) {
			this.negativeLabel = label;
			this.negativeListener = listener;
			return this;
		}
		
		public Builder setDismissListener(DialogInterface.OnDismissListener listener) {
			this.dismissListener = listener;
			return this;
		}
		
		public SettingsDialog createDialog() {
			return new SettingsDialog(context);
		}
		
		public SettingsDialog create() {
			SettingsDialog d = createDialog();
			if (title != null) d.setTitle(title);
			if (message != null) d.setMessage(message);
			if (positiveListener != null) {
				d.setButton(DialogInterface.BUTTON_POSITIVE, positiveLabel, positiveListener);
			}
			if (negativeListener != null) {
				d.setButton(DialogInterface.BUTTON_NEGATIVE, negativeLabel, negativeListener);
			}
			if (dismissListener != null) {
				d.setOnDismissListener(dismissListener);
			}
			d.setView(scrollView);
			d.setFields(fields);
			return d;
		}
		
	}

	private HashMap<String, View> fields;

	protected SettingsDialog(Context context) {
		super(context);
	}
	
	public void setFields(HashMap<String, View> fields) {
		this.fields = fields;
	}
	
	public View getField(String name) {
		return fields.get(name);
	}

	public int parseColor(String name) throws Exception {
		EditText text = (EditText) getField(name);
		if (text == null) {
			throw new MapException("Cannot find setting " + name);
		}
		
		Integer color = (int) Long.parseLong(text.getText().toString(), 16);
		if (color == null) {
			throw new MapException("Invalid color specified for " + name);
		}
		return color;
	}
	
	public float parseSlider(String name) throws Exception {
		return parseSlider(name, 0, 1);
	}
	
	public float parseSlider(String name, float minRange, float maxRange) throws Exception {
		SeekBar slider = (SeekBar) getField(name);
		if (slider == null) {
			throw new MapException("Cannot find setting " + name);
		}
		return getSliderValue(slider.getProgress(), minRange, maxRange);
	}
	
	public int parseRange(String name, int minRange, int maxRange) throws Exception {
		SeekBar slider = (SeekBar) getField(name);
		if (slider == null) {
			throw new MapException("Cannot find setting " + name);
		}
		return getRangeValue(slider.getProgress(), minRange, maxRange);
	}
	
	public boolean parseCheckBox(String name) throws Exception {
		CheckBox checkBox = (CheckBox) getField(name);
		if (checkBox == null) {
			throw new MapException("Cannot find setting " + name);
		}
		return checkBox.isChecked();
	}
	
	protected static float getSliderValue(int progress, float minRange, float maxRange) {
		return (((float) progress) / 100) * (maxRange - minRange);
	}
	
	protected static int getRangeValue(int progress, int minRange, int maxRange) {
		return progress + minRange;
	}
	
	public void showError(String message) {
		new ErrorDialog(this.getContext(), "Tool Error", message).show();
	}

}

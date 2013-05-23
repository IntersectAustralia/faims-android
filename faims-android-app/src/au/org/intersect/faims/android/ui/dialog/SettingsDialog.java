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
	
	public class Builder {
		
		private Context context;
		private HashMap<String, View> fields;
		private ScrollView scrollView;
		private LinearLayout layout;
		private String title;
		private String message;
		private String positiveLabel;
		private OnClickListener positiveListener;
		private String negativeLabel;
		private OnClickListener negativeListener;

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
			
			return this;
		}
		
		public Builder addCheckbox(String name, String label, boolean defaultValue) {
			TextView labelView = new TextView(context);
			labelView.setText(name);
			
			CheckBox box = new CheckBox(context);
			box.setChecked(defaultValue);
			
			layout.addView(labelView);
			layout.addView(box);
			
			fields.put(name, box);
			
			return this;
		}
		
		public Builder addSlider(String name, final String label, float defaultValue) {
			return addSlider(name, label, defaultValue, 0, 100);
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
			
			return this;
		}
		
		public Builder addRange(String name, final String label, int defaultValue, final int minRange, final int maxRange) {
			final TextView labelView = new TextView(context);
			labelView.setText(label + " " + Integer.toString(defaultValue));
			
			final SeekBar seekBar = new SeekBar(context);
			seekBar.setMax(maxRange);
			seekBar.setProgress((int) (100 * (defaultValue / (maxRange - minRange))));
			
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
		
		public SettingsDialog create() {
			SettingsDialog d = new SettingsDialog(context);
			if (title != null) d.setTitle(title);
			if (message != null) d.setMessage(message);
			if (positiveLabel != null) {
				d.setButton(DialogInterface.BUTTON_POSITIVE, positiveLabel, positiveListener);
			}
			if (negativeLabel != null) {
				d.setButton(DialogInterface.BUTTON_NEGATIVE, negativeLabel, negativeListener);
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

	protected int parseColor(String name) throws Exception {
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
	
	protected float parseSlider(String name) throws Exception {
		return parseSlider(name, 0, 100);
	}
	
	protected float parseSlider(String name, float minRange, float maxRange) throws Exception {
		SeekBar slider = (SeekBar) getField(name);
		if (slider == null) {
			throw new MapException("Cannot find setting " + name);
		}
		return getSliderValue(slider.getProgress(), minRange, maxRange);
	}
	
	protected int parseRange(String name, int minRange, int maxRange) throws Exception {
		SeekBar slider = (SeekBar) getField(name);
		if (slider == null) {
			throw new MapException("Cannot find setting " + name);
		}
		return getRangeValue(slider.getProgress(), minRange, maxRange);
	}
	
	protected float getSliderValue(int progress, float minRange, float maxRange) {
		return (((float) progress) / 100) * (maxRange - minRange);
	}
	
	protected int getRangeValue(int progress, int minRange, int maxRange) {
		return (int) getSliderValue(progress, minRange, maxRange);
	}

}

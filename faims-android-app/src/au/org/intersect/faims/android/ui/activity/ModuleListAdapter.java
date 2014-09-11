package au.org.intersect.faims.android.ui.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.ui.activity.MainActivity.ModuleItem;

public class ModuleListAdapter extends ArrayAdapter<ModuleItem> {
	
	private ArrayList<ModuleItem> items;
	
	private WeakReference<MainActivity> activityRef;

	public ModuleListAdapter(Context context, int textViewResourceId, MainActivity activity)
	{
		super(context, textViewResourceId);
		items = new ArrayList<ModuleItem>();
		activityRef = new WeakReference<MainActivity>(activity);
	}
	
	@Override
	public ModuleItem getItem(int position) {
		return items.get(position);
	}
	
	public View getView(int position, View convertView, final ViewGroup parent)
	{
		View v = convertView;
		
		if (v == null)
		{
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.module_list_item, null);
		}
		
		final ModuleItem module = items.get(position);
		
		if (module != null) {
			TextView moduleName = (TextView) v.findViewById(R.id.module_name);
			TextView moduleVersion = (TextView) v.findViewById(R.id.module_version);
			ImageView localIcon = (ImageView) v.findViewById(R.id.module_local_icon);
			ImageView serverIcon = (ImageView) v.findViewById(R.id.module_server_icon);
			
			localIcon.setVisibility(View.INVISIBLE);
			serverIcon.setVisibility(View.INVISIBLE);
			
			Button itemOverlay = (Button) v.findViewById(R.id.list_item_overlay);
			itemOverlay.setBackgroundResource(R.drawable.label_selector);
			itemOverlay.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (module.isLocal()) {
						Intent showModulesIntent = new Intent(activityRef.get(), ShowModuleActivity.class);
						showModulesIntent.putExtra("key", module.getKey());
						FAIMSApplication.getInstance().saveModuleKey(module.getKey());
						activityRef.get().startActivityForResult(showModulesIntent, 1);
					} else {
						activityRef.get().selectedDownloadModule = new Module(module.getName(), module.getKey());
						activityRef.get().showDownloadModuleDialog(module.getName());
					}
				}
			});
			
			if (module.isLocal() && module.isServer()) {
				localIcon.setVisibility(View.VISIBLE);
				serverIcon.setVisibility(View.VISIBLE);
				itemOverlay.setOnLongClickListener(new OnLongClickListener() {
					
					@Override
					public boolean onLongClick(View v) {
						activityRef.get().selectedDownloadModule = new Module(module.getName(), module.getKey());
						activityRef.get().showDownloadModuleDialog(module.getName());
						return false;
					}
				});
			} else {
				itemOverlay.setOnLongClickListener(null);
			}
			
			moduleName.setText(items.get(position).getName());
			String version = items.get(position).getVersion() == null ? "?" : items.get(position).getVersion();
			moduleVersion.setText("Version: " + version);
			if (items.get(position).isLocal()) {
				localIcon.setVisibility(View.VISIBLE);
			} else {
				serverIcon.setVisibility(View.VISIBLE);
			}
		}
		
		
		return v;
	}

	@Override
	public void add(ModuleItem object) {
		for (ModuleItem m : items) {
			if (m.getKey().equals(object.getKey())) {
				// local module exists on server too
				m.setServer(true);
				return;
			}
		}
		super.add(object);
		items.add(object);
	}
	
	@Override
	public void clear() {
		super.clear();
		items.clear();
	}

}

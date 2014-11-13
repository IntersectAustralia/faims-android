package au.org.intersect.faims.android.ui.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.data.ModuleItem;

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
			TextView moduleServer = (TextView) v.findViewById(R.id.module_server);
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
						activityRef.get().openStaticModulePanel(module.getKey());
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
				if (module.isLocal()) {
					itemOverlay.setOnLongClickListener(new OnLongClickListener() {
						
						@Override
						public boolean onLongClick(View v) {
							showNoServerOptionsDialog();
							return false;
						}
					});
				} else {
					itemOverlay.setOnLongClickListener(null);
				}
			}
			
			moduleName.setText(items.get(position).getName());
			moduleServer.setText("Server: " + items.get(position).getHost());
			if (items.get(position).getVersion() != null && !items.get(position).getVersion().isEmpty()) {
				moduleVersion.setText("Version: " + items.get(position).getVersion());
			} else {
				moduleVersion.setText("");
			}
			if (items.get(position).isLocal()) {
				localIcon.setVisibility(View.VISIBLE);
			} else {
				serverIcon.setVisibility(View.VISIBLE);
			}
		}
		
		
		return v;
	}

	private void showNoServerOptionsDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(activityRef.get());
		
		builder.setTitle("Server not connected");
		builder.setMessage("The server this module was downloaded from is not connected or the module has been deleted from the server");
		builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User clicked OK button
			}
		});
		Dialog dialog = builder.create();
		dialog.show();
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

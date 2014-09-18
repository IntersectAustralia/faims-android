package au.org.intersect.faims.android.ui.activity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import roboguice.activity.RoboActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.data.ModuleItem;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.services.DownloadModuleService;
import au.org.intersect.faims.android.services.UpdateModuleDataService;
import au.org.intersect.faims.android.services.UpdateModuleSettingService;
import au.org.intersect.faims.android.tasks.FetchModulesListTask;
import au.org.intersect.faims.android.tasks.ITaskListener;
import au.org.intersect.faims.android.tasks.LocateServerTask;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.ConfirmDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;
import au.org.intersect.faims.android.util.ModuleUtil;

import com.google.inject.Inject;

public class MainActivity extends RoboActivity {
	
	public static class DownloadModuleHandler extends Handler {
		
		private WeakReference<MainActivity> activityRef;

		public DownloadModuleHandler(MainActivity activity) {
			this.activityRef = new WeakReference<MainActivity>(activity);
		}
		
		public void handleMessage(Message message) {
			MainActivity activity = activityRef.get();
			if (activity == null) {
				FLog.d("FetchModulesHandler cannot get activity");
				return;
			}
			
			activity.busyDialog.dismiss();
			
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				// Show module static panel
				activity.openStaticModulePanel(activity.selectedDownloadModule.key);
				activity.readModules();
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE ||
					result.resultCode == FAIMSClientResultCode.INTERRUPTED) {
				if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
					activity.showBusyErrorDialog();
				} else if (result.errorCode == FAIMSClientErrorCode.STORAGE_LIMIT_ERROR) {
					activity.showDownloadModuleErrorDialog();
				} else {
					activity.showDownloadModuleFailureDialog();
				}
			}
		}
		
	};

	public static class UpdateModuleSettingHandler extends Handler {
		
		private WeakReference<MainActivity> activityRef;

		public UpdateModuleSettingHandler(MainActivity activity) {
			this.activityRef = new WeakReference<MainActivity>(activity);
		}
		
		public void handleMessage(Message message) {
			MainActivity activity = activityRef.get();
			if (activity == null) {
				FLog.d("FetchModulesHandler cannot get activity");
				return;
			}
			
			activity.busyDialog.dismiss();
			
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				// Show module static panel
				activity.openStaticModulePanel(activity.selectedDownloadModule.key);
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE ||
					result.resultCode == FAIMSClientResultCode.INTERRUPTED) {
				if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
					activity.showBusyErrorDialog();
				} else if (result.errorCode == FAIMSClientErrorCode.STORAGE_LIMIT_ERROR) {
					activity.showUpdateModuleErrorDialog();
				} else {
					activity.showUpdateModuleSettingFailureDialog();
				}
			}
		}
		
	};

	public static class UpdateModuleDataHandler extends Handler {
		
		private WeakReference<MainActivity> activityRef;

		public UpdateModuleDataHandler(MainActivity activity) {
			this.activityRef = new WeakReference<MainActivity>(activity);
		}
		
		public void handleMessage(Message message) {
			MainActivity activity = activityRef.get();
			if (activity == null) {
				FLog.d("FetchModulesHandler cannot get activity");
				return;
			}
			
			activity.busyDialog.dismiss();
			
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				// Show module static panel
				activity.openStaticModulePanel(activity.selectedDownloadModule.key);
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE ||
					result.resultCode == FAIMSClientResultCode.INTERRUPTED) {
				if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
					activity.showBusyErrorDialog();
				} else if (result.errorCode == FAIMSClientErrorCode.STORAGE_LIMIT_ERROR) {
					activity.showUpdateModuleErrorDialog();
				} else {
					activity.showUpdateModuleDataFailureDialog();
				}
			}
		}
		
	};

	@Inject
	FAIMSClient faimsClient;
	
	@Inject
	ServerDiscovery serverDiscovery;
	
	private ModuleListAdapter moduleListAdapter;
	
	protected Module selectedDownloadModule;
	
	protected BusyDialog busyDialog;
	protected ChoiceDialog choiceDialog;
	protected ConfirmDialog confirmDialog;
	
	private DrawerLayout staticPanel;
	
	private AsyncTask<Void, Void, Void> locateTask;
	
	protected final DownloadModuleHandler downloadHandler = new DownloadModuleHandler(MainActivity.this);
	protected final UpdateModuleSettingHandler updateModuleSettingHandler = new UpdateModuleSettingHandler(MainActivity.this);
	protected final UpdateModuleDataHandler updateModuleDataHandler = new UpdateModuleDataHandler(MainActivity.this);
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        FAIMSApplication.getInstance().setApplication(getApplication());
        
        setContentView(R.layout.activity_main);
        
        ListView moduleList = (ListView) findViewById(R.id.module_list);
        
        staticPanel = (DrawerLayout) findViewById(R.id.static_module_drawer_layout);
        staticPanel.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        
        final SwipeRefreshLayout swipe = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        int blueColour = R.color.color_blue;
        swipe.setColorScheme(blueColour, blueColour, blueColour, blueColour);
        swipe.setRefreshing(false);
        swipe.setOnRefreshListener(new OnRefreshListener() {
			
			@Override
			public void onRefresh() {
				readModules();
				swipe.setRefreshing(false);
			}
		});
        
        moduleListAdapter = new ModuleListAdapter(this, R.layout.module_list_item, this);
        moduleList.setAdapter(moduleListAdapter);
    }
    
    protected void showDownloadModuleDialog(final String moduleName) {
    	File moduleDir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + selectedDownloadModule.key);
    	if (!moduleDir.exists()) {
    		choiceDialog = new ChoiceDialog(MainActivity.this, 
					getString(R.string.confirm_download_module_title),
					getString(R.string.confirm_download_module_message) + " " + moduleName + "?",
					new IDialogListener() {

						@Override
						public void handleDialogResponse(
								DialogResultCode resultCode) {
							if (resultCode == DialogResultCode.SELECT_YES) {
								downloadModule(true);
							}
						}
				
			});
			choiceDialog.show();
    	} else {
	    	showUpdateOrDownloadModuleDialog(moduleName);
    	}
	}
    
    protected void showUpdateOrDownloadModuleDialog(final String selectedItem) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.confirm_download_or_update_module_title);
		builder.setMessage(getString(R.string.confirm_download_or_update_module_message) + " " + selectedItem + "?");

		builder.setPositiveButton("Cancel", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		
		builder.setNeutralButton("Restore", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				choiceDialog = new ChoiceDialog(MainActivity.this, 
						getString(R.string.confirm_download_module_title),
						getString(R.string.confirm_download_module_message) + " " + selectedItem + "?",
						new IDialogListener() {

							@Override
							public void handleDialogResponse(
									DialogResultCode resultCode) {
								if (resultCode == DialogResultCode.SELECT_YES) {
									choiceDialog = new ChoiceDialog(MainActivity.this, 
											getString(R.string.confirm_download_warning_module_title),
											getString(R.string.confirm_download_warning_module_message),
											new IDialogListener() {
	
												@Override
												public void handleDialogResponse(
														DialogResultCode resultCode) {
													if (resultCode == DialogResultCode.SELECT_YES) {
														downloadModule(true);
													}
												}	
									});
									choiceDialog.show();
								}
							}
					
				});
				choiceDialog.show();
			}
		});
		
		builder.setNegativeButton("Update", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showUpdateModuleDialog(selectedItem);
			}
		});
		
		builder.create().show();
	}
    
    protected void showUpdateModuleDialog(final String selectedItem) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.select_update_module_title);
		builder.setMessage(R.string.select_update_module_message);

		builder.setPositiveButton("Cancel", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		
		builder.setNeutralButton("Update Maps", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				updateModuleData(true);
			}
		});
		
		builder.setNegativeButton("Update Settings", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				updateModuleSettingArchive(true);
			}
		});
		builder.create().show();
	}
    
    protected void downloadModule(final boolean overwrite) {
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyDownloadingModulesDialog();
    		
    		// start service
    		Intent intent = new Intent(MainActivity.this, DownloadModuleService.class);
    		
		    Messenger messenger = new Messenger(downloadHandler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("module", selectedDownloadModule);
		    intent.putExtra("overwrite", overwrite);
		    startService(intent);
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				MainActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					downloadModule(overwrite);
    				} else {
    					showLocateServerDownloadArchiveFailureDialog(overwrite);
    				}
    			}
        		
        	}).execute();
    	}
    	
    }

	protected void updateModuleSettingArchive(final boolean overwrite) {
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyUpdatingModuleSettingDialog();
    		
    		// start service
    		Intent intent = new Intent(MainActivity.this, UpdateModuleSettingService.class);
    		
		    Messenger messenger = new Messenger(updateModuleSettingHandler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("module", selectedDownloadModule);
		    intent.putExtra("overwrite", overwrite);
		    startService(intent);
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				MainActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					updateModuleSettingArchive(overwrite);
    				} else {
    					showLocateServerDownloadArchiveFailureDialog(overwrite);
    				}
    			}
        		
        	}).execute();
    	}
    	
    }

	protected void updateModuleData(final boolean overwrite) {
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyUpdatingModuleDataDialog();
    		
    		// start service
    		Intent intent = new Intent(MainActivity.this, UpdateModuleDataService.class);
    		
		    Messenger messenger = new Messenger(updateModuleDataHandler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("module", selectedDownloadModule);
		    intent.putExtra("overwrite", overwrite);
		    startService(intent);
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				MainActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					updateModuleData(overwrite);
    				} else {
    					showLocateServerDownloadArchiveFailureDialog(overwrite);
    				}
    			}
        		
        	}).execute();
    	}
    	
    }
    
    private void showLocateServerDownloadArchiveFailureDialog(final boolean overwrite) {
    	choiceDialog = new ChoiceDialog(MainActivity.this,
				getString(R.string.locate_server_failure_title),
				getString(R.string.locate_server_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							downloadModule(overwrite);
						}
					}
    		
    	});
    	choiceDialog.show();
    }
    
    private void showDownloadModuleFailureDialog() {
    	choiceDialog = new ChoiceDialog(MainActivity.this,
				getString(R.string.download_module_failure_title),
				getString(R.string.download_module_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							downloadModule(false);
						}
					}
    		
    	});
    	choiceDialog.show();
    }
    
    private void showBusyErrorDialog() {
    	confirmDialog = new ConfirmDialog(MainActivity.this,
				getString(R.string.download_busy_module_error_title),
				getString(R.string.download_busy_module_error_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						// do nothing
					}
    		
    	});
    	confirmDialog.show();
    }
    
    private void showDownloadModuleErrorDialog() {
    	confirmDialog = new ConfirmDialog(MainActivity.this,
				getString(R.string.download_module_error_title),
				getString(R.string.download_module_error_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						// do nothing
					}
    		
    	});
    	confirmDialog.show();
    }
    
    private void showBusyLocatingServerDialog() {
    	busyDialog = new BusyDialog(MainActivity.this, 
				getString(R.string.locate_server_title),
				getString(R.string.locate_server_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							MainActivity.this.locateTask.cancel(true);
						}
					}
			
		});
		busyDialog.show();
    }
    
    private void showBusyDownloadingModulesDialog() {
    	busyDialog = new BusyDialog(MainActivity.this, 
				getString(R.string.download_module_title),
				getString(R.string.download_module_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
				    		Intent intent = new Intent(MainActivity.this, DownloadModuleService.class);
				    		
				    		stopService(intent);
						}
					}
			
		});
	    busyDialog.show();
    }
    
    private void showBusyUpdatingModuleSettingDialog() {
    	busyDialog = new BusyDialog(MainActivity.this, 
				getString(R.string.update_module_title),
				getString(R.string.update_module_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
				    		Intent intent = new Intent(MainActivity.this, UpdateModuleSettingService.class);
				    		
				    		stopService(intent);
						}
					}
			
		});
	    busyDialog.show();
    }

    private void showBusyUpdatingModuleDataDialog() {
    	busyDialog = new BusyDialog(MainActivity.this, 
				getString(R.string.update_module_title),
				getString(R.string.update_module_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
				    		Intent intent = new Intent(MainActivity.this, UpdateModuleDataService.class);
				    		
				    		stopService(intent);
						}
					}
			
		});
	    busyDialog.show();
    }

    private void showUpdateModuleSettingFailureDialog() {
    	choiceDialog = new ChoiceDialog(MainActivity.this,
				getString(R.string.update_module_failure_title),
				getString(R.string.update_module_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							updateModuleSettingArchive(false);
						}
					}
    		
    	});
    	choiceDialog.show();
    }

    private void showUpdateModuleDataFailureDialog() {
    	choiceDialog = new ChoiceDialog(MainActivity.this,
				getString(R.string.update_module_failure_title),
				getString(R.string.update_module_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							updateModuleData(false);
						}
					}
    		
    	});
    	choiceDialog.show();
    }

    private void showUpdateModuleErrorDialog() {
    	confirmDialog = new ConfirmDialog(MainActivity.this,
				getString(R.string.update_module_error_title),
				getString(R.string.update_module_error_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						// do nothing
					}
    		
    	});
    	confirmDialog.show();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (staticPanel.isDrawerOpen(Gravity.END)) {
        	staticPanel.closeDrawer(Gravity.END);
        }
    	readModules();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	if (busyDialog != null)
    		busyDialog.dismiss();
    	if (choiceDialog != null)
    		choiceDialog.dismiss();
    	if (confirmDialog != null)
    		confirmDialog.dismiss();
    	
    	// kill all services
		Intent intent = new Intent(MainActivity.this, DownloadModuleService.class);
		stopService(intent);
		Intent dataIntent = new Intent(MainActivity.this, UpdateModuleDataService.class);
		stopService(dataIntent);
		Intent serviceIntent = new Intent(MainActivity.this, UpdateModuleSettingService.class);
		stopService(serviceIntent);
    }

    @Override
	public void onBackPressed() {
		Intent splash = new Intent(MainActivity.this, SplashActivity.class);
		startActivity(splash);
		finish();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.faims_server_setting:
				showFaimsServerSettings();
				return true;
			default:
				return (super.onOptionsItemSelected(item));
		}
	}

	private void showFaimsServerSettings() {
		Intent serverSettingsIntent = new Intent(MainActivity.this, ServerSettingsActivity.class);
		startActivity(serverSettingsIntent);
	}
    
	public void readModules() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serverDiscovery.initiateServerIPAndPort(preferences);
        
		moduleListAdapter.clear();
		readStoredModules();
		
		// if preferences has no selection the don't fetch
		if (!preferences.getString("pref_server_ip", "").isEmpty()) {
			fetchServerModules();
		}
	}
	
	private void readStoredModules() {
		List<Module> modules = ModuleUtil.getModules();
		if (modules != null) {
			for (Module p : modules) {
				moduleListAdapter.add(new ModuleItem(p.name, p.key, p.host, p.version, true, false));
			}
			moduleListAdapter.notifyDataSetChanged();
		}
	}
	
	private void fetchServerModules() {
		final TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
		connectionStatus.setText("Not connected to a server");
		if (serverDiscovery.isServerHostValid()) {
    		
			new FetchModulesListTask(faimsClient, new ITaskListener() {

				@Override
				public void handleTaskCompleted(Object result) {
					Result fetchResult = (Result) result;
					if (fetchResult.resultCode == FAIMSClientResultCode.SUCCESS) {
						connectionStatus.setText("Connected to " + serverDiscovery.getServerIP()+ ":" + serverDiscovery.getServerPort());
		    			ArrayList<Module> modules = parseModules((JSONArray) fetchResult.data);
		    			for (Module p : modules) {
	    					moduleListAdapter.add(new ModuleItem(p.name, p.key, p.host, p.version, false, true));
		    			}
		    			moduleListAdapter.notifyDataSetChanged();
					}
				}
    			
    		}).execute();
    	} else {
    		
    		new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				if ((Boolean) result) {
    					fetchServerModules();
    				}
    			}
        		
        	}).execute();
    	}
	}
	
	private ArrayList<Module> parseModules(JSONArray objects) {
		ArrayList<Module> modules = new ArrayList<Module>();
		try {
			for (int i = 0; i < objects.length(); i++) {
				JSONObject moduleJson = objects.getJSONObject(i);
				moduleJson.put("host", faimsClient.getPlainHost());
				modules.add(Module.fromJson(moduleJson));
			}
		} catch (JSONException e) {
			FLog.e("error trying to parse module list");
		}
		return modules;
	}
	
	public void loadModule(String key) {
		Intent showModulesIntent = new Intent(MainActivity.this, ShowModuleActivity.class);
		showModulesIntent.putExtra("key", key);
		FAIMSApplication.getInstance().saveModuleKey(key);
		startActivityForResult(showModulesIntent, 1);
	}

	public void openStaticModulePanel(final String key) {
		Module module = ModuleUtil.getModule(key);
		
		updateStaticPanelData(module);
		
		Button loadModule = (Button) findViewById(R.id.static_load_module);
		loadModule.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				loadModule(key);
			}
		});
		
		((ScrollView)findViewById(R.id.static_module_panel_scroll)).fullScroll(ScrollView.FOCUS_UP);
		staticPanel.openDrawer(Gravity.END);
		staticPanel.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
	}
	
	private void updateStaticPanelData(Module module) {
		TextView moduleName = (TextView) findViewById(R.id.static_module_name);
		moduleName.setText(module.getName());
		
		TextView moduleDescription = (TextView) findViewById(R.id.static_module_description);
		if (module.getDescription() == null || module.getDescription().isEmpty()) {
			moduleDescription.setVisibility(View.GONE);
		} else {
			moduleDescription.setVisibility(View.VISIBLE);
			moduleDescription.setText(module.getDescription());
		}
		
		TableLayout dataTable = (TableLayout) findViewById(R.id.module_static_data);
		dataTable.removeAllViews();
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for (NameValuePair dataItem : module.getStaticData()) {
			TableRow rowView = (TableRow) inflater.inflate(R.layout.static_module_data_row, null);
			TextView label = (TextView) rowView.findViewById(R.id.static_data_label);
			label.setText(dataItem.getName());
			TextView value = (TextView) rowView.findViewById(R.id.static_data_value);
			value.setText(dataItem.getValue());
			dataTable.addView(rowView);
		}
		
		TextView moduleVersion = (TextView) findViewById(R.id.static_module_version);
		if (module.getVersion() == null || module.getVersion().isEmpty()) {
			((TextView)findViewById(R.id.static_module_version_label)).setVisibility(View.GONE);
			moduleVersion.setVisibility(View.GONE);
		} else {
			((TextView)findViewById(R.id.static_module_version_label)).setVisibility(View.VISIBLE);
			moduleVersion.setVisibility(View.VISIBLE);
			moduleVersion.setText(module.getVersion());
		}
		
		TextView moduleServer = (TextView) findViewById(R.id.static_module_server);
		if (module.getHost() == null || module.getHost().isEmpty()) {
			((TextView)findViewById(R.id.static_module_server_label)).setVisibility(View.GONE);
			moduleServer.setVisibility(View.GONE);
		} else {
			((TextView)findViewById(R.id.static_module_server_label)).setVisibility(View.VISIBLE);
			moduleServer.setVisibility(View.VISIBLE);
			moduleServer.setText(module.getHost());
		}
	}
	
}

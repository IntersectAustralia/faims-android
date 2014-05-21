package au.org.intersect.faims.android.ui.activity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import roboguice.activity.RoboActivity;
import android.app.AlertDialog;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Module;
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

import com.google.inject.Inject;

public class FetchModulesActivity extends RoboActivity {
	
	public static class DownloadModuleHandler extends Handler {
		
		private WeakReference<FetchModulesActivity> activityRef;

		public DownloadModuleHandler(FetchModulesActivity activity) {
			this.activityRef = new WeakReference<FetchModulesActivity>(activity);
		}
		
		public void handleMessage(Message message) {
			FetchModulesActivity activity = activityRef.get();
			if (activity == null) {
				FLog.d("FetchModulesHandler cannot get activity");
				return;
			}
			
			activity.busyDialog.dismiss();
			
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				// start show module activity
				activity.showModuleActivity();
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
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
		
		private WeakReference<FetchModulesActivity> activityRef;

		public UpdateModuleSettingHandler(FetchModulesActivity activity) {
			this.activityRef = new WeakReference<FetchModulesActivity>(activity);
		}
		
		public void handleMessage(Message message) {
			FetchModulesActivity activity = activityRef.get();
			if (activity == null) {
				FLog.d("FetchModulesHandler cannot get activity");
				return;
			}
			
			activity.busyDialog.dismiss();
			
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				// start show module activity
				activity.showModuleActivity();
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
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
		
		private WeakReference<FetchModulesActivity> activityRef;

		public UpdateModuleDataHandler(FetchModulesActivity activity) {
			this.activityRef = new WeakReference<FetchModulesActivity>(activity);
		}
		
		public void handleMessage(Message message) {
			FetchModulesActivity activity = activityRef.get();
			if (activity == null) {
				FLog.d("FetchModulesHandler cannot get activity");
				return;
			}
			
			activity.busyDialog.dismiss();
			
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				// start show module activity
				activity.showModuleActivity();
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
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
	
	private ArrayAdapter<String> moduleListAdapter;
	
	protected List<Module> modules;
	protected Module selectedModule;
	
	protected BusyDialog busyDialog;
	protected ChoiceDialog choiceDialog;
	protected ConfirmDialog confirmDialog;
	
	private AsyncTask<Void, Void, Void> locateTask;
	private AsyncTask<Void, Void, Void> fetchTask;
	
	protected final DownloadModuleHandler downloadHandler = new DownloadModuleHandler(FetchModulesActivity.this);
	protected final UpdateModuleSettingHandler updateModuleSettingHandler = new UpdateModuleSettingHandler(FetchModulesActivity.this);
	protected final UpdateModuleDataHandler updateModuleDataHandler = new UpdateModuleDataHandler(FetchModulesActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_fetch_modules);
        
        serverDiscovery.setApplication(getApplication());
        
        ListView moduleList = (ListView) findViewById(R.id.module_list);
        
        moduleListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        moduleList.setAdapter(moduleListAdapter);
        
        moduleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        		final String selectedItem = moduleListAdapter.getItem(arg2).toString();
        		selectedModule = modules.get(arg2);
        		
        		showDownloadModuleDialog(selectedItem);
        	}
        });
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serverDiscovery.initiateServerIPAndPort(preferences);
        
        fetchModulesList();
    }

    protected void showDownloadModuleDialog(final String selectedItem) {
    	File moduleDir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + selectedModule.key);
    	if(!moduleDir.exists()){
    		choiceDialog = new ChoiceDialog(FetchModulesActivity.this, 
					getString(R.string.confirm_download_module_title),
					getString(R.string.confirm_download_module_message) + " " + selectedItem + "?",
					new IDialogListener() {

						@Override
						public void handleDialogResponse(
								DialogResultCode resultCode) {
							if (resultCode == DialogResultCode.SELECT_YES) {
								downloadModuleArchive();
							}
						}
				
			});
			choiceDialog.show();
    	}else{
	    	showUpdateOrDownloadModuleDialog(selectedItem);
    	}
	}

	protected void showUpdateOrDownloadModuleDialog(final String selectedItem) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Download");
		builder.setMessage("Do you want to download or update module?");

		builder.setPositiveButton("Cancel", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		
		builder.setNeutralButton("Download", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				choiceDialog = new ChoiceDialog(FetchModulesActivity.this, 
						getString(R.string.confirm_download_module_title),
						getString(R.string.confirm_download_module_message) + " " + selectedItem + "?",
						new IDialogListener() {

							@Override
							public void handleDialogResponse(
									DialogResultCode resultCode) {
								if (resultCode == DialogResultCode.SELECT_YES) {
									downloadModuleArchive();
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
		builder.setTitle("Update");
		builder.setMessage("Do you want to update module settings or module data?");

		builder.setPositiveButton("Cancel", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		
		builder.setNeutralButton("Update Data", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				updateModuleDataArchive();
			}
		});
		
		builder.setNegativeButton("Update Settings", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				updateModuleSettingArchive();
			}
		});
		builder.create().show();
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
		Intent intent = new Intent(FetchModulesActivity.this, DownloadModuleService.class);
		stopService(intent);
		Intent dataIntent = new Intent(FetchModulesActivity.this, UpdateModuleDataService.class);
		stopService(dataIntent);
		Intent serviceIntent = new Intent(FetchModulesActivity.this, UpdateModuleSettingService.class);
		stopService(serviceIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.acitvity_fetch_modules, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh_module_list:
				fetchModulesList();
				return (true);
			default:
				return (super.onOptionsItemSelected(item));
		}
	}
    
    /**
     * Fetch modules from the server to load into list
     */
    protected void fetchModulesList() {
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyFetchingModulesDialog();
    		
    		fetchTask = new FetchModulesListTask(faimsClient, new ITaskListener() {

				@Override
				public void handleTaskCompleted(Object result) {
					FetchModulesActivity.this.busyDialog.dismiss();
					
					Result fetchResult = (Result) result;		
					if (fetchResult.resultCode == FAIMSClientResultCode.SUCCESS) {
						if (moduleListAdapter != null) moduleListAdapter.clear();
		    			FetchModulesActivity.this.modules = parseModules((JSONArray) fetchResult.data);
		    			Collections.reverse(FetchModulesActivity.this.modules);
		    			for (Module p : modules) {
		    				FetchModulesActivity.this.moduleListAdapter.add(p.name);
		    			}
					} else if (fetchResult.resultCode == FAIMSClientResultCode.FAILURE) {
						showFetchModulesFailureDialog();
					} else {
						// ignore
					}
				}
    			
    		}).execute();
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				FetchModulesActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					fetchModulesList();
    				} else {
    					showLocateServerFetchModulesFailureDialog();
    				}
    			}
        		
        	}).execute();
    	}
    	
    }

	protected void downloadModuleArchive() {
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyDownloadingModulesDialog();
    		
    		// start service
    		Intent intent = new Intent(FetchModulesActivity.this, DownloadModuleService.class);
    		
		    Messenger messenger = new Messenger(downloadHandler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("module", selectedModule);
		    startService(intent);
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				FetchModulesActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					downloadModuleArchive();
    				} else {
    					showLocateServerDownloadArchiveFailureDialog();
    				}
    			}
        		
        	}).execute();
    	}
    	
    }

	protected void updateModuleSettingArchive() {
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyUpdatingModuleSettingDialog();
    		
    		// start service
    		Intent intent = new Intent(FetchModulesActivity.this, UpdateModuleSettingService.class);
    		
		    Messenger messenger = new Messenger(updateModuleSettingHandler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("module", selectedModule);
		    startService(intent);
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				FetchModulesActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					updateModuleSettingArchive();
    				} else {
    					showLocateServerDownloadArchiveFailureDialog();
    				}
    			}
        		
        	}).execute();
    	}
    	
    }

	protected void updateModuleDataArchive() {
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyUpdatingModuleDataDialog();
    		
    		// start service
    		Intent intent = new Intent(FetchModulesActivity.this, UpdateModuleDataService.class);
    		
		    Messenger messenger = new Messenger(updateModuleDataHandler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("module", selectedModule);
		    startService(intent);
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				FetchModulesActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					updateModuleDataArchive();
    				} else {
    					showLocateServerDownloadArchiveFailureDialog();
    				}
    			}
        		
        	}).execute();
    	}
    	
    }

	private void showLocateServerFetchModulesFailureDialog() {
    	choiceDialog = new ChoiceDialog(FetchModulesActivity.this,
				getString(R.string.locate_server_failure_title),
				getString(R.string.locate_server_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							fetchModulesList();
						}
					}
    		
    	});
    	choiceDialog.show();
    }
    
    private void showLocateServerDownloadArchiveFailureDialog() {
    	choiceDialog = new ChoiceDialog(FetchModulesActivity.this,
				getString(R.string.locate_server_failure_title),
				getString(R.string.locate_server_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							downloadModuleArchive();
						}
					}
    		
    	});
    	choiceDialog.show();
    }
    
    private void showFetchModulesFailureDialog() {
    	choiceDialog = new ChoiceDialog(FetchModulesActivity.this,
				getString(R.string.fetch_modules_failure_title),
				getString(R.string.fetch_modules_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							fetchModulesList();
						}
					}
    		
    	});
    	choiceDialog.show();
    }
    
    private void showDownloadModuleFailureDialog() {
    	choiceDialog = new ChoiceDialog(FetchModulesActivity.this,
				getString(R.string.download_module_failure_title),
				getString(R.string.download_module_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							downloadModuleArchive();
						}
					}
    		
    	});
    	choiceDialog.show();
    }
    
    private void showBusyErrorDialog() {
    	confirmDialog = new ConfirmDialog(FetchModulesActivity.this,
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
    	confirmDialog = new ConfirmDialog(FetchModulesActivity.this,
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
    	busyDialog = new BusyDialog(FetchModulesActivity.this, 
				getString(R.string.locate_server_title),
				getString(R.string.locate_server_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							FetchModulesActivity.this.locateTask.cancel(true);
						}
					}
			
		});
		busyDialog.show();
    }
    
    private void showBusyFetchingModulesDialog() {
    	busyDialog = new BusyDialog(FetchModulesActivity.this, 
				getString(R.string.fetch_modules_title),
				getString(R.string.fetch_modules_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							FetchModulesActivity.this.fetchTask.cancel(true);
						}
					}
			
		});
		busyDialog.show();
    }
    
    private void showBusyDownloadingModulesDialog() {
    	busyDialog = new BusyDialog(FetchModulesActivity.this, 
				getString(R.string.download_module_title),
				getString(R.string.download_module_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
				    		Intent intent = new Intent(FetchModulesActivity.this, DownloadModuleService.class);
				    		
				    		stopService(intent);
						}
					}
			
		});
	    busyDialog.show();
    }
    
    private void showBusyUpdatingModuleSettingDialog() {
    	busyDialog = new BusyDialog(FetchModulesActivity.this, 
				getString(R.string.update_module_title),
				getString(R.string.update_module_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
				    		Intent intent = new Intent(FetchModulesActivity.this, UpdateModuleSettingService.class);
				    		
				    		stopService(intent);
						}
					}
			
		});
	    busyDialog.show();
    }

    private void showBusyUpdatingModuleDataDialog() {
    	busyDialog = new BusyDialog(FetchModulesActivity.this, 
				getString(R.string.update_module_title),
				getString(R.string.update_module_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
				    		Intent intent = new Intent(FetchModulesActivity.this, UpdateModuleDataService.class);
				    		
				    		stopService(intent);
						}
					}
			
		});
	    busyDialog.show();
    }

    private void showUpdateModuleSettingFailureDialog() {
    	choiceDialog = new ChoiceDialog(FetchModulesActivity.this,
				getString(R.string.update_module_failure_title),
				getString(R.string.update_module_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							updateModuleSettingArchive();
						}
					}
    		
    	});
    	choiceDialog.show();
    }

    private void showUpdateModuleDataFailureDialog() {
    	choiceDialog = new ChoiceDialog(FetchModulesActivity.this,
				getString(R.string.update_module_failure_title),
				getString(R.string.update_module_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							updateModuleDataArchive();
						}
					}
    		
    	});
    	choiceDialog.show();
    }

    private void showUpdateModuleErrorDialog() {
    	confirmDialog = new ConfirmDialog(FetchModulesActivity.this,
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
    
    private void showModuleActivity() {
    	Intent showModulesIntent = new Intent(this, ShowModuleActivity.class);
		showModulesIntent.putExtra("key", selectedModule.key);
		startActivityForResult(showModulesIntent, 1);
		finish();
    }

	protected List<Module> parseModules(JSONArray objects) {
		ArrayList<Module> modules = new ArrayList<Module>();
		try {
			for (int i = 0; i < objects.length(); i++) {
				modules.add(Module.fromJson(objects.getJSONObject(i)));
			}
		} catch (JSONException e) {
			FLog.e("error trying to parse module list");
		}
		return modules;
	}
}

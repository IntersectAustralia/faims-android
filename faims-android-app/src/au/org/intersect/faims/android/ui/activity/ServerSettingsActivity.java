package au.org.intersect.faims.android.ui.activity;

import java.util.ArrayList;

import com.google.inject.Inject;

import roboguice.activity.RoboActivity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.tasks.FaimsServerConnectionTestingTask;
import au.org.intersect.faims.android.tasks.ITaskListener;
import au.org.intersect.faims.android.tasks.LocateServerTask;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;

public class ServerSettingsActivity extends RoboActivity {
	
	private static final String AUTODISCOVER_SERVER = "autodiscover";
	
	@Inject
	ServerDiscovery serverDiscovery;
	
	private AsyncTask<Void, Void, Void> connectionTestingTask;
	private AsyncTask<Void, Void, Void> locateServerTask;
	
	private BusyDialog busyDialog;
	
	private Spinner pastSessions; 
	private EditText hostField;
	private EditText portField;

	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        FAIMSApplication.getInstance().setApplication(getApplication());
        
        setContentView(R.layout.server_settings);
        
        hostField = (EditText) findViewById(R.id.host_field);
        portField = (EditText) findViewById(R.id.port_field);
        pastSessions = (Spinner) findViewById(R.id.previous_sessions);
        
        setupPreviousSessions();
        
        Button testConnection = (Button) findViewById(R.id.test_connection);
        testConnection.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				testConnection(hostField.getText().toString(), portField.getText().toString());
			}
		});
        
        Button connectToServer = (Button) findViewById(R.id.connect_to_server);
        connectToServer.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				connectToServer(hostField.getText().toString(), portField.getText().toString());
			}
		});
	}
	
	@SuppressLint("NewApi")
	protected void testConnection(String ipAddress, String port) {
		
		if (((NameValuePair)pastSessions.getSelectedItem()).getValue().equals(AUTODISCOVER_SERVER)) {
			serverDiscovery.setServerHostFixed(false);
			serverDiscovery.invalidateServerHost();
			locateServerTask = createLocateServerTask(false).execute();
			showBusyTestingConnectionDialog(locateServerTask);
		} else {
			connectionTestingTask = createConnectionTask(ipAddress, port, false).execute();
			showBusyTestingConnectionDialog(connectionTestingTask);
		}
	}
	
	@SuppressLint("NewApi")
	protected void connectToServer(String ipAddress, String port) {
		if (((NameValuePair)pastSessions.getSelectedItem()).getValue().equals(AUTODISCOVER_SERVER)) {
			serverDiscovery.setServerHostFixed(false);
			serverDiscovery.invalidateServerHost();
			locateServerTask = createLocateServerTask(true).execute();
			showBusyTestingConnectionDialog(locateServerTask);
		} else {
			connectionTestingTask = createConnectionTask(ipAddress, port, true).execute();
			showBusyTestingConnectionDialog(connectionTestingTask);
		}
	}
	
	private AsyncTask<Void,Void,Void> createLocateServerTask(final boolean connectToServer) {
		return new LocateServerTask(serverDiscovery, new ITaskListener() {

			@Override
			public void handleTaskCompleted(Object result) {
				busyDialog.dismiss();
				if (!locateServerTask.isCancelled()) {
					if ((Boolean) result) {
						if (connectToServer) {
							FAIMSApplication.getInstance().updateServerSettings(serverDiscovery.getServerIP(),
	    							serverDiscovery.getServerPort(), true);
	    					Intent mainIntent = new Intent(ServerSettingsActivity.this, MainActivity.class);
							startActivity(mainIntent);
							finish();
						} else {
							showWarning("Settings", "Connection test succeeded");
						}
					} else {
						showWarning("Settings", connectToServer ? "There is no server available with the provided host and port" : 
							"No server found on the current network");
					}
				}
			}
    		
    	});
	}
	
	private FaimsServerConnectionTestingTask createConnectionTask(String ipAddress, String port, final boolean connectToServer) {
		return new FaimsServerConnectionTestingTask(ipAddress.trim(), port.trim(), new ITaskListener() {
			
			@Override
			public void handleTaskCompleted(Object result) {
				busyDialog.dismiss();
				if (!connectionTestingTask.isCancelled()) {
					if ((Boolean) result) {
						if (connectToServer) {
							FAIMSApplication.getInstance().updateServerSettings(hostField.getText().toString().trim(),
									portField.getText().toString().trim(), false);
							Intent mainIntent = new Intent(ServerSettingsActivity.this, MainActivity.class);
							startActivity(mainIntent);
							finish();
						} else {
							showWarning("Settings", "Connection test succeeded");
						}
					} else {
						showWarning("Settings", "There is no server available with the provided host and port");
					}
				}
			}
		});
	}
	
	private void setupPreviousSessions() {
        ArrayList<NameValuePair> sessions = FAIMSApplication.getInstance().getPastServers();
        
        ArrayAdapter<NameValuePair> sessionsAdapter = new ArrayAdapter<NameValuePair>(this, android.R.layout.simple_spinner_dropdown_item, sessions);
        pastSessions.setAdapter(sessionsAdapter);
        
        pastSessions.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String server = ((NameValuePair)parent.getItemAtPosition(position)).getValue();
				if (server.equals("autodiscover")) {
					hostField.setText("");
					hostField.setEnabled(false);
					portField.setText("");
					portField.setEnabled(false);
				} else {
					if (server.contains(":") && server.split(":").length == 2) {
						hostField.setText(server.split(":")[0]);
						portField.setText(server.split(":")[1]);
					} else {
						hostField.setText("");
						portField.setText("");
					}
					hostField.setEnabled(true);
					portField.setEnabled(true);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	private void showBusyTestingConnectionDialog(final AsyncTask<Void,Void,Void> task) {
		busyDialog = new BusyDialog(ServerSettingsActivity.this, 
				"Settings",
				"Connecting to server",
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							if (task != null) {
								task.cancel(true);
							}
						}
					}
			
		});
		busyDialog.show();
    }
	
	private void showWarning(final String title, final String message){
		
		AlertDialog.Builder builder = new AlertDialog.Builder(ServerSettingsActivity.this);
		
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User clicked OK button
		           }
		       });
		builder.create().show();
		
	}
}

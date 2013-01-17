package au.org.intersect.faims.android.ui.activity;

import org.javarosa.form.api.FormEntryController;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.tasks.BluetoothActionListener;
import au.org.intersect.faims.android.tasks.ExternalGPSTasks;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.DialogType;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;
import au.org.intersect.faims.android.util.BeanShellLinker;
import au.org.intersect.faims.android.util.DatabaseManager;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.UIRenderer;

public class ShowProjectActivity extends FragmentActivity implements IDialogListener, BluetoothActionListener, LocationListener {

	public static final int CAMERA_REQUEST_CODE = 1;

	private FormEntryController fem;

	private UIRenderer renderer;

	protected ChoiceDialog choiceDialog;

	private String directory;
	
	private BeanShellLinker linker;
	
	private DatabaseManager databaseManager;
	
	private BluetoothDevice gpsDevice;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FAIMSLog.log();
		
		setContentView(R.layout.activity_show_project);
		Intent data = getIntent();
		setTitle(data.getStringExtra("name"));
		this.gpsDevice = data.getParcelableExtra("gpsDevice");
		directory = data.getStringExtra("directory");
		
		choiceDialog = DialogFactory.createChoiceDialog(ShowProjectActivity.this, 
				DialogType.CONFIRM_RENDER_PROJECT, 
				getString(R.string.render_project_title),
				getString(R.string.render_project_message));
		choiceDialog.show();
		
		databaseManager = new DatabaseManager(Environment.getExternalStorageDirectory() + directory + "/db.sqlite3");
	}
	
	@Override
	protected void onDestroy() {
		this.linker.destroyListener();
		super.onDestroy();
	}

	private void startGPSListener() {
		int gpsUpdateInterval = this.linker.getGpsUpdateInterval();
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Handler handler = new Handler();
		ExternalGPSTasks externalGPSTasks = new ExternalGPSTasks(this.gpsDevice,handler, this, gpsUpdateInterval);
		handler.postDelayed(externalGPSTasks, gpsUpdateInterval);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsUpdateInterval, 0, this);
		this.linker.setLocationManager(locationManager);
		this.linker.setHandler(handler);
		this.linker.setGpsDevice(this.gpsDevice);
		this.linker.setExternalGPSTasks(externalGPSTasks);
		this.linker.setContext(this);
	}

	/*
	@Override
	protected void onResume() {
		super.onResume();
		FAIMSLog.log();
		this.manager.dispatchResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		FAIMSLog.log();
		this.manager.dispatchPause(isFinishing());
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		FAIMSLog.log();
		// after taking picture using camera
		if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			this.renderer.getCurrentImageView().setImageBitmap(photo);
			this.renderer.clearCurrentImageView();
		}
	}
	*/
	
	@Override
	public void handleDialogResponse(DialogResultCode resultCode, Object data,
			DialogType type, Dialog dialog) {
		if (type == DialogType.CONFIRM_RENDER_PROJECT) {
			if (resultCode == DialogResultCode.SELECT_YES) {
				renderUI();
				startGPSListener();
			}
		}
		
	}
	
	private void renderUI() {
		// Read, validate and parse the xforms
		this.fem = FileUtil.readXmlContent(Environment
				.getExternalStorageDirectory() + directory + "/ui_schema.xml");

		// render the ui definition
		this.renderer = new UIRenderer(this.fem, this);
		this.renderer.createUI();
		this.renderer.showTabGroup(this, 0);
		
		// bind the logic to the ui
		Log.d("FAIMS","Binding logic to the UI");
		linker = new BeanShellLinker(this, getAssets(), renderer, databaseManager);
		linker.setBaseDir(Environment.getExternalStorageDirectory() + directory);
		linker.sourceFromAssets("ui_commands.bsh");
		linker.execute(FileUtil.readFileIntoString(Environment.getExternalStorageDirectory() + directory + "/ui_logic.bsh"));
	}
	
	public BeanShellLinker getBeanShellLinker(){
		return this.linker;
	}

	@Override
	public void handleGPSUpdates(String GGAMessage, String BODMessage) {
		this.linker.setGGAMessage(GGAMessage);
		this.linker.setBODMessage(BODMessage);
	}

	@Override
	public void onLocationChanged(Location location) {
		this.linker.setAccuracy(location.getAccuracy());
		this.linker.setLocation(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
		this.linker.setAccuracy(0.0f);
		this.linker.setLocation(null);
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

}

package au.org.intersect.faims.android.ui.activity;

import org.javarosa.form.api.FormEntryController;

import android.app.Dialog;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.gps.GPSDataManager;
import au.org.intersect.faims.android.managers.DatabaseManager;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.DialogType;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;
import au.org.intersect.faims.android.ui.form.Arch16n;
import au.org.intersect.faims.android.ui.form.BeanShellLinker;
import au.org.intersect.faims.android.ui.form.UIRenderer;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;
import au.org.intersect.faims.android.util.FileUtil;

public class ShowProjectActivity extends FragmentActivity implements IDialogListener {

	public static final int CAMERA_REQUEST_CODE = 1;

	private FormEntryController fem;

	private UIRenderer renderer;

	protected ChoiceDialog choiceDialog;

	private String directory;
	
	private BeanShellLinker linker;
	
	private DatabaseManager databaseManager;
	
	private GPSDataManager gpsDataManager;

	private Arch16n arch16n;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FAIMSLog.log();
		
		setContentView(R.layout.activity_show_project);
		Intent data = getIntent();
		String name = data.getStringExtra("name");
		setTitle(name);
		directory = data.getStringExtra("directory");
		
		choiceDialog = DialogFactory.createChoiceDialog(ShowProjectActivity.this, 
				DialogType.CONFIRM_RENDER_PROJECT, 
				getString(R.string.render_project_title),
				getString(R.string.render_project_message));
		choiceDialog.show();
		
		databaseManager = new DatabaseManager(Environment.getExternalStorageDirectory() + directory + "/db.sqlite3");
		arch16n = new Arch16n(Environment.getExternalStorageDirectory() + directory, name);
		gpsDataManager = new GPSDataManager((LocationManager) getSystemService(LOCATION_SERVICE));
	}
	
	@Override
	protected void onDestroy() {
		if(this.linker != null){
			this.linker.destroyListener();
		}
		if(this.gpsDataManager != null){
			this.gpsDataManager.destroyListener();
		}
		super.onDestroy();
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
				gpsDataManager.startGPSListener();
			}
		}
		
	}
	
	private void renderUI() {
		// Read, validate and parse the xforms
		this.runOnUiThread(new Thread(new Runnable() {

			@Override
			public void run() {
				arch16n.generatePropertiesMap();
				fem = FileUtil.readXmlContent(Environment
						.getExternalStorageDirectory() + directory + "/ui_schema.xml");

				// render the ui definition
				renderer = new UIRenderer(ShowProjectActivity.this.fem, ShowProjectActivity.this.arch16n, ShowProjectActivity.this);
				renderer.createUI();
				renderer.showTabGroup(ShowProjectActivity.this, 0);
				
				// bind the logic to the ui
				Log.d("FAIMS","Binding logic to the UI");
				linker = new BeanShellLinker(ShowProjectActivity.this, ShowProjectActivity.this.arch16n, getAssets(), renderer, databaseManager, gpsDataManager);
				linker.setBaseDir(Environment.getExternalStorageDirectory() + directory);
				linker.sourceFromAssets("ui_commands.bsh");
				linker.execute(FileUtil.readFileIntoString(Environment.getExternalStorageDirectory() + directory + "/ui_logic.bsh"));
			}
			
		}));
	}
	
	public BeanShellLinker getBeanShellLinker(){
		return this.linker;
	}

}

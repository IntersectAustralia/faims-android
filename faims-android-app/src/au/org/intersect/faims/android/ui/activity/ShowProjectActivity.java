package au.org.intersect.faims.android.ui.activity;

import org.javarosa.form.api.FormEntryController;

import android.app.Activity;
import android.app.Dialog;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TabHost;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.tasks.ActionResultCode;
import au.org.intersect.faims.android.tasks.ActionType;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.IFAIMSDialogListener;
import au.org.intersect.faims.android.util.BeanShellLinker;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.UIRenderer;

@SuppressWarnings("deprecation")
public class ShowProjectActivity extends Activity implements IFAIMSDialogListener {

	public static final int CAMERA_REQUEST_CODE = 1;

	private FormEntryController fem;

	private LocalActivityManager manager;

	private TabHost tabHost;

	private UIRenderer renderer;

	private ChoiceDialog choiceDialog;

	private String directory;
	
	private BeanShellLinker linker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FAIMSLog.log();
		
		setContentView(R.layout.activity_show_project);
		Intent data = getIntent();
		setTitle(data.getStringExtra("name"));
		directory = data.getStringExtra("directory");
		
		choiceDialog = DialogFactory.createChoiceDialog(ShowProjectActivity.this, 
				ActionType.CONFIRM_RENDER_PROJECT, 
				getString(R.string.render_project_title),
				getString(R.string.render_project_message));
		choiceDialog.show();
		
		// initialise the tabhost for each tab
		this.tabHost = (TabHost) findViewById(R.id.tabhost);
		this.manager = new LocalActivityManager(this, false);
		this.manager.dispatchCreate(savedInstanceState);
		tabHost.setup(this.manager);

	}

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

	@Override
	public void handleDialogResponse(ActionResultCode resultCode, Object data,
			ActionType type, Dialog dialog) {
		if (type == ActionType.CONFIRM_RENDER_PROJECT) {
			if (resultCode == ActionResultCode.SELECT_YES) {
				renderUI();
			}
		}
		
	}
	
	private void renderUI() {
		// Read, validate and parse the xforms
		this.fem = FileUtil.readXmlContent(Environment
				.getExternalStorageDirectory() + directory + "/ui_schema.xml");

		// render the ui definition
		this.renderer = new UIRenderer(this.fem, this.tabHost, this);
		this.renderer.render();
		
		// bind the logic to the ui
		linker = new BeanShellLinker(this);
		linker.source("test_script.bsh");
	}
}

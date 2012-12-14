package au.org.intersect.faims.android;

import org.javarosa.form.api.FormEntryController;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TabHost;
import au.org.intersect.faims.util.FileUtil;

@SuppressWarnings("deprecation")
public class ShowProjectActivity extends Activity {

	protected static final int CAMERA_REQUEST_CODE = 1;

	private FormEntryController fem;

	private LocalActivityManager manager;

	private TabHost tabHost;

	private UIRenderer renderer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_project);
		Intent data = getIntent();
		setTitle(data.getStringExtra("name"));
		String directory = data.getStringExtra("directory");

		// Read, validate and parse the xforms
		this.fem = FileUtil.readXmlContent(Environment
				.getExternalStorageDirectory() + directory + "/ui_schema.xml");

		// initialise the tabhost for each tab
		this.tabHost = (TabHost) findViewById(R.id.tabhost);
		this.manager = new LocalActivityManager(this, false);
		this.manager.dispatchCreate(savedInstanceState);
		tabHost.setup(this.manager);

		// render the ui definition
		this.renderer = new UIRenderer(this.fem, this.tabHost, this);
		this.renderer.render();

	}

	@Override
	protected void onResume() {
		super.onResume();
		this.manager.dispatchResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.manager.dispatchPause(isFinishing());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// after taking picture using camera
		if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			this.renderer.getCurrentImageView().setImageBitmap(photo);
			this.renderer.clearCurrentImageView();
		}
	}
}

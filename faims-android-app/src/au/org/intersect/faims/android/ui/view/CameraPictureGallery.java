package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.managers.AutoSaveManager;

import com.google.inject.Inject;

public class CameraPictureGallery extends FilePictureGallery {
	
	public static final String IMAGE_PREVIEW = "Image Preview";

	@Inject
	AutoSaveManager autoSaveManager;

	@Inject
	BeanShellLinker linker;
	
	public CameraPictureGallery(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CameraPictureGallery(Context context, FormInputDef attribute, boolean sync, String ref, boolean dynamic) {
		super(context, attribute, ref, dynamic, sync);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	@Override
	protected void longSelectImage(View v) {
		previewCameraPicture(v);
	}
	
	private void previewCameraPicture(View v) {
		CustomImageView selectedImageView = (CustomImageView) v;
		String path = selectedImageView.getPicture().getUrl();
		File file = new File(path);
		if (!file.exists()) {
			linker.showPreviewWarning(IMAGE_PREVIEW, file);
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());

		builder.setTitle(IMAGE_PREVIEW);

		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);

		builder.setView(scrollView);
		ImageView imageView = new ImageView(this.getContext());
		
		imageView.setImageBitmap(decodeFile(new File(selectedImageView.getPicture().getUrl()), 500, 500));
		layout.addView(imageView);
		
		TextView text = new TextView(this.getContext());
		text.setText(getCameraMetaData(path));
		layout.addView(text);
		
		builder.setNeutralButton("Done", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// Nothing
			}

		});
		builder.create().show();
	}

	private String getCameraMetaData(String path) {
		File videoFile = new File(path);

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Picture Metadata:");
		stringBuilder.append("\n");
		stringBuilder.append("File name: " + videoFile.getName());
		stringBuilder.append("\n");
		stringBuilder.append("File size: " + videoFile.length() + " bytes");
		stringBuilder.append("\n");
		Date lastModifiedDate = new Date(videoFile.lastModified());
		stringBuilder.append("Picture date: " + lastModifiedDate.toString());
		return stringBuilder.toString();
	}

	@Override
	public void addFile(String value, String annotation, String certainty) {
		Picture picture = new Picture(value, null, value);
		addGallery(picture);
		super.addFile(value, annotation, certainty);
	}

}

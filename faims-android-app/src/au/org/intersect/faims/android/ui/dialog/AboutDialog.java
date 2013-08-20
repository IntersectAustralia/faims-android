package au.org.intersect.faims.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import au.org.intersect.faims.android.R;

public class AboutDialog extends Dialog {

	public AboutDialog(Context context) {
		super(context);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		
		TextView tv = (TextView)findViewById(R.id.info_text);
		tv.setText(Html.fromHtml("<h3>Developed by Intersect Australia Ltd.</h3>"));
		tv.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.intersect.org.au/attribution-policy"));
				getContext().startActivity(browserIntent);
			}
			
		});
	}

}

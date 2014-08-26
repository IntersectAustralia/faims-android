package au.org.intersect.faims.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.util.ModuleUtil;

public class SplashActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.splashscreen);
	    
	    FAIMSApplication.getInstance().setApplication(getApplication());
	    
	    WebView webview = (WebView) findViewById(R.id.splashscreen_webview);
	    webview.loadUrl("file:///android_asset/splash.html");
	    
	    WebView attribution = (WebView) findViewById(R.id.splashscreen_attribution);
	    attribution.loadUrl("file:///android_asset/attribution.html");
	    attribution.setLongClickable(false);
	    attribution.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_UP) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.intersect.org.au/attribution-policy"));
					SplashActivity.this.startActivity(browserIntent);
				}
				return true;
			}
		});
	
	    Button loadModule = (Button) findViewById(R.id.splash_load);
	    loadModule.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
	            SplashActivity.this.startActivity(mainIntent);
			}
		});
	    
	    updateContinueSessionButton();
	}
	

    @Override
    protected void onResume() {
    	super.onResume();
    	updateContinueSessionButton();
    }


	private void updateContinueSessionButton() {
		Button continueSession = (Button) findViewById(R.id.splash_continue);
		final String key = FAIMSApplication.getInstance().getSessionModuleKey();
	    if (key != null && ModuleUtil.getModule(key) != null) {
		    continueSession.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent showModuleIntent = new Intent(SplashActivity.this, ShowModuleActivity.class);
					showModuleIntent.putExtra("key", key);
					SplashActivity.this.startActivityForResult(showModuleIntent, 1);
				}
			});
	    } else {
	    	continueSession.setVisibility(View.GONE);
	    }
	}
	 
}

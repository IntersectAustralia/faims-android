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
import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.util.ModuleUtil;

public class SplashActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.splashscreen);
	    
	    FAIMSApplication.getInstance().setApplication(getApplication());
	    
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
	
	    updateButtons();
	}
	

    @Override
    protected void onResume() {
    	super.onResume();
    	updateButtons();
    }


	@Override
	public void onBackPressed() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}


	private void updateButtons() {
		Button connectDemo = (Button) findViewById(R.id.splash_connect_demo);
		Button connectServer = (Button) findViewById(R.id.splash_connect_server);
		Button loadModule = (Button) findViewById(R.id.splash_load);
		connectDemo.setVisibility(View.GONE);
		connectServer.setVisibility(View.GONE);
		loadModule.setVisibility(View.GONE);
		
		if (ModuleUtil.getModules() == null || ModuleUtil.getModules().isEmpty()) {
			connectDemo.setVisibility(View.VISIBLE);
			connectDemo.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					connectToDemoServer();
				}
			});
			
			connectServer.setVisibility(View.VISIBLE);
			connectServer.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent serverSettings = new Intent(SplashActivity.this, ServerSettingsActivity.class);
					startActivity(serverSettings);
				}
			});
		} else {
			loadModule.setVisibility(View.VISIBLE);
			loadModule.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
		            SplashActivity.this.startActivity(mainIntent);
				}
			});
		}
		
		Button continueSession = (Button) findViewById(R.id.splash_continue);
		final String key = FAIMSApplication.getInstance().getSessionModuleKey();
		final String arch16n = FAIMSApplication.getInstance().getSessionModuleArch16n();
	    if (key != null && ModuleUtil.getModule(key) != null) {
		    continueSession.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent showModuleIntent = new Intent(SplashActivity.this, ShowModuleActivity.class);
					showModuleIntent.putExtra("key", key);
					showModuleIntent.putExtra("arch16n", arch16n);
					SplashActivity.this.startActivityForResult(showModuleIntent, 1);
				}
			});
	    } else {
	    	continueSession.setVisibility(View.GONE);
	    }
	}
	
	private void connectToDemoServer() {
		FAIMSApplication.getInstance().updateServerSettings(getResources().getString(R.string.demo_server_host),
				getResources().getString(R.string.demo_server_port), false);
		
		Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(mainIntent);
	}
	 
}

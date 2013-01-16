package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.graphics.Color;
import au.org.intersect.faims.android.R;

import com.nutiteq.MapView;
import com.nutiteq.components.Components;
import com.nutiteq.components.Options;
import com.nutiteq.utils.UnscaledBitmapLoader;

public class CustomMapView extends MapView {

	public CustomMapView(Context context) {
		super(context);
		
        this.setComponents(new Components());
		
		// Activate some mapview options to make it smoother - optional
		this.getOptions().setPreloading(true);
		this.getOptions().setSeamlessHorizontalPan(true);
		this.getOptions().setTileFading(true);
		this.getOptions().setKineticPanning(true);
		this.getOptions().setDoubleClickZoomIn(true);
		this.getOptions().setDualClickZoomOut(true);
		
		 // set sky bitmap - optional, default - white
		this.getOptions().setSkyDrawMode(Options.DRAW_BITMAP);
		this.getOptions().setSkyOffset(4.86f);
		this.getOptions().setSkyBitmap(
                UnscaledBitmapLoader.decodeResource(getResources(),
                        R.drawable.sky_small));
		
		// Map background, visible if no map tiles loaded - optional, default - white
		this.getOptions().setBackgroundPlaneDrawMode(Options.DRAW_BITMAP);
		this.getOptions().setBackgroundPlaneBitmap(
                UnscaledBitmapLoader.decodeResource(getResources(),
                        R.drawable.background_plane));
		this.getOptions().setClearColor(Color.WHITE);
		
		// configure texture caching - optional, suggested 
        //this.getOptions().setTextureMemoryCacheSize(40 * 1024 * 1024);
        //this.getOptions().setCompressedMemoryCacheSize(8 * 1024 * 1024);
	}
	
}

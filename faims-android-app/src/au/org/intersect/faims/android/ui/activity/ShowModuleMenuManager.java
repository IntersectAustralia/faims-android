package au.org.intersect.faims.android.ui.activity;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.gps.GPSDataManager;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.managers.BluetoothManager;
import au.org.intersect.faims.android.util.BitmapUtil;
import au.org.intersect.faims.android.util.MeasurementUtil;

import com.google.inject.Inject;
import com.nutiteq.utils.UnscaledBitmapLoader;

public class ShowModuleMenuManager {
	
	@Inject
	GPSDataManager gpsDataManager;
	
	@Inject
	BluetoothManager bluetoothManager;
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	private boolean pathIndicatorVisible;
	private float pathDistance;
	private boolean pathValid;
	private float pathBearing;
	private Float pathHeading;
	private int pathIndex;
	private int pathLength;

	private BitmapDrawable whiteArrow;
	private BitmapDrawable greyArrow;
	private Bitmap tempBitmap;
	private Animation rotation;
	private ImageView syncAnimImage;
	private ImageView autoSaveImage;
	
	private WeakReference<ShowModuleActivity> activityRef;
	
	public ShowModuleMenuManager(ShowModuleActivity activity) {
		FAIMSApplication.getInstance().injectMembers(this);
		this.activityRef = new WeakReference<ShowModuleActivity>(activity);
		
		rotation = AnimationUtils.loadAnimation(activity, R.anim.clockwise);
		rotation.setRepeatCount(Animation.INFINITE);

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		syncAnimImage = (ImageView) inflater.inflate(R.layout.sync_rotate, null);
		autoSaveImage = (ImageView) inflater.inflate(R.layout.autosave_rotate, null);
	}

	public void prepare(Menu menu) {
		// gps status
				menu.findItem(R.id.action_gps_inactive).setVisible(false);
				menu.findItem(R.id.action_gps_active_has_signal).setVisible(false);
				menu.findItem(R.id.action_gps_active_no_signal).setVisible(false);
				if (gpsDataManager.isExternalGPSStarted()
						|| gpsDataManager.isInternalGPSStarted()) {
					if (gpsDataManager.hasValidExternalGPSSignal()
							|| gpsDataManager.hasValidInternalGPSSignal()) {
						menu.findItem(R.id.action_gps_active_has_signal).setVisible(
								true);
					} else {
						menu.findItem(R.id.action_gps_active_no_signal)
								.setVisible(true);
					}
				} else {
					menu.findItem(R.id.action_gps_inactive).setVisible(true);
				}

				// tracker status
				menu.findItem(R.id.action_tracker_active_no_gps).setVisible(false);
				menu.findItem(R.id.action_tracker_active_has_gps).setVisible(false);
				menu.findItem(R.id.action_tracker_inactive).setVisible(false);
				if (gpsDataManager.isTrackingStarted()) {
					if (gpsDataManager.hasValidExternalGPSSignal()
							|| gpsDataManager.hasValidInternalGPSSignal()) {
						menu.findItem(R.id.action_tracker_active_has_gps).setVisible(
								true);
					} else {
						menu.findItem(R.id.action_tracker_active_no_gps).setVisible(
								true);
					}
				} else {
					menu.findItem(R.id.action_tracker_inactive).setVisible(true);
				}

				// sync status
				menu.findItem(R.id.action_sync).setVisible(false);
				menu.findItem(R.id.action_sync_active).setVisible(false);
				menu.findItem(R.id.action_sync_error).setVisible(false);
				menu.findItem(R.id.action_sync_has_changes).setVisible(false);
				menu.findItem(R.id.action_sync_inactive).setVisible(false);

				syncAnimImage.clearAnimation();

				switch (activityRef.get().getSyncStatus()) {
				case ACTIVE_SYNCING:
					MenuItem syncItem = menu.findItem(R.id.action_sync_active)
							.setVisible(true);

					syncAnimImage.startAnimation(rotation);

					syncItem.setActionView(syncAnimImage);

					break;
				case ERROR:
					menu.findItem(R.id.action_sync_error).setVisible(true);
					break;
				case ACTIVE_NO_CHANGES:
					menu.findItem(R.id.action_sync).setVisible(true);
					break;
				case ACTIVE_HAS_CHANGES:
					menu.findItem(R.id.action_sync_has_changes).setVisible(true);
					break;
				default:
					menu.findItem(R.id.action_sync_inactive).setVisible(true);
					break;
				}
				
				// bluetooth status
				menu.findItem(R.id.action_bluetooth_active).setVisible(false);
				menu.findItem(R.id.action_bluetooth_connected).setVisible(false);
				menu.findItem(R.id.action_bluetooth_disconnected).setVisible(false);
				menu.findItem(R.id.action_bluetooth_error).setVisible(false);
				
				switch (bluetoothManager.getBluetoothStatus()) {
				case ACTIVE:
					menu.findItem(R.id.action_bluetooth_active).setVisible(true);
					break;
				case CONNECTED:
					menu.findItem(R.id.action_bluetooth_connected).setVisible(true);
					break;
				case ERROR:
					menu.findItem(R.id.action_bluetooth_error).setVisible(true);
					break;
				default:
					menu.findItem(R.id.action_bluetooth_disconnected).setVisible(true);
					break;
				}

				// follow status
				MenuItem distance_text = menu.findItem(R.id.distance_text);
				distance_text.setVisible(pathIndicatorVisible);
				String distanceInfo = pathIndex < 0 ? "" : " to point (" + pathIndex
						+ "/" + pathLength + ")";
				if (pathDistance > 1000) {
					distance_text.setTitle(MeasurementUtil.displayAsKiloMeters(
							pathDistance / 1000, "###,###,###,###.0") + distanceInfo);
				} else {
					distance_text.setTitle(MeasurementUtil.displayAsMeters(
							pathDistance, "###,###,###,###") + distanceInfo);
				}

				MenuItem direction_text = menu.findItem(R.id.direction_text);
				direction_text.setVisible(pathIndicatorVisible);
				direction_text.setTitle(MeasurementUtil.displayAsDegrees(pathBearing,
						"###"));

				MenuItem direction_indicator = menu.findItem(R.id.direction_indicator);
				direction_indicator.setVisible(pathIndicatorVisible);
				if (pathHeading != null) {
					if (tempBitmap != null) {
						tempBitmap.recycle();
					}
					if (whiteArrow == null) {
						whiteArrow = new BitmapDrawable(
								activityRef.get().getResources(),
								UnscaledBitmapLoader
										.decodeResource(
												activityRef.get().getResources(),
												au.org.intersect.faims.android.R.drawable.white_arrow));
					}
					if (greyArrow == null) {
						greyArrow = new BitmapDrawable(
								activityRef.get().getResources(),
								UnscaledBitmapLoader
										.decodeResource(
												activityRef.get().getResources(),
												au.org.intersect.faims.android.R.drawable.grey_arrow));
					}

					this.tempBitmap = BitmapUtil.rotateBitmap(
							pathValid ? whiteArrow.getBitmap() : greyArrow.getBitmap(),
							pathBearing - pathHeading);
					direction_indicator.setIcon(new BitmapDrawable(activityRef.get().getResources(),
							tempBitmap));
				} else {
					direction_indicator.setVisible(false);
				}
				
				// autosave status
				menu.findItem(R.id.action_autosave).setVisible(false);
				menu.findItem(R.id.action_autosave_inactive).setVisible(false);
				menu.findItem(R.id.action_autosave_ready).setVisible(false);
				menu.findItem(R.id.action_autosave_saving).setVisible(false);
				menu.findItem(R.id.action_autosave_error).setVisible(false);

				autoSaveImage.clearAnimation();

				switch (autoSaveManager.getStatus()) {
				case SAVING:
					MenuItem autosaveItem = menu.findItem(R.id.action_autosave_saving).setVisible(true);
					autoSaveImage.startAnimation(rotation);
					autosaveItem.setActionView(autoSaveImage);
					break;
				case READY:
					menu.findItem(R.id.action_autosave_ready).setVisible(true);
					break;
				case ACTIVE:
					menu.findItem(R.id.action_autosave).setVisible(true);
					break;
				case ERROR:
					menu.findItem(R.id.action_autosave_error).setVisible(true);
					break;
				default:
					menu.findItem(R.id.action_autosave_inactive).setVisible(true);
					break;
				}
	}

	public void setPathVisible(boolean value) {
		this.pathIndicatorVisible = value;
	}

	public void setPathDistance(float value) {
		this.pathDistance = value;
	}

	public void setPathIndex(int value, int length) {
		this.pathIndex = value;
		this.pathLength = length;
	}

	public void setPathBearing(float value) {
		this.pathBearing = value;
	}

	public void setPathHeading(Float heading) {
		this.pathHeading = heading;
	}

	public void setPathValid(boolean value) {
		this.pathValid = value;
	}
	
}

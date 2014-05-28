package au.org.intersect.faims.android.gps;

import java.lang.ref.WeakReference;

import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.BODSentence;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.util.CompassPoint;
import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.Toast;
import au.org.intersect.faims.android.data.ActivityData;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.tasks.BluetoothActionListener;
import au.org.intersect.faims.android.tasks.ExternalGPSTasks;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

import com.google.inject.Singleton;

@Singleton
public class GPSDataManager implements BluetoothActionListener, LocationListener, ActivityData {

	private static final String EXTERNAL = "external";
	private static final String INTERNAL = "internal";
	private String GGAMessage;
    private String BODMessage;
    private long externalGPSTimestamp;

	private float accuracy;
    private Location location;
    private long internalGPSTimestamp;

	private BluetoothDevice gpsDevice;
	public Handler handler;
	private ExternalGPSTasks externalGPSTasks;
	private LocationManager locationManager;

	private HandlerThread handlerThread;

	private int gpsUpdateInterval = 10;
	
	private boolean isExternalGPSStarted;
	private boolean isInternalGPSStarted;

	private boolean hasValidExternalGPSSignal;
	private boolean hasValidInternalGPSSignal;
	
	private String trackingType;
	
	private int trackingValue;
	private String trackingExec;
	private boolean isTrackingStarted;

	private WeakReference<ShowModuleActivity> activityRef;
	
	public void init(LocationManager manager, ShowModuleActivity activity){
		GGAMessage  = null;
		BODMessage = null;
		externalGPSTimestamp = 0;
		accuracy = 0;
		location = null;
		internalGPSTimestamp = 0;
		gpsDevice = null;
		handler = null;
		externalGPSTasks = null;
		locationManager = manager;
		handlerThread = null;
		gpsUpdateInterval = 10;
		isExternalGPSStarted = false;
		isInternalGPSStarted = false;
		hasValidExternalGPSSignal = false;
		hasValidInternalGPSSignal = false;
		trackingType = null;
		trackingValue = 0;
		trackingExec = null;
		isTrackingStarted = false;
		this.activityRef = new WeakReference<ShowModuleActivity>(activity);
	}
	
    @Override
	public void handleGPSUpdates(String GGAMessage, String BODMessage) {
		setGGAMessage(GGAMessage);
		setBODMessage(BODMessage);
		setExternalGPSTimestamp(System.currentTimeMillis());
		if(!hasValidExternalGPSSignal){
			if(hasValidGGAMessage()){
				setHasValidExternalGPSSignal(true);
				activityRef.get().invalidateOptionsMenu();
			}
		}else{
			if(!hasValidGGAMessage()){
				setHasValidExternalGPSSignal(false);
				activityRef.get().invalidateOptionsMenu();
			}
		}
	}
    
    @Override
    public void bluetoothOff(String message) {
    	try {
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(activityRef.get().getApplicationContext(),
					message, duration);
			toast.show();
		} catch (Exception e) {
			FLog.e("error showing toast", e);
		}
    }

    private boolean hasValidGGAMessage(){
    	GGASentence sentence = null;
        try{
	        if (this.GGAMessage != null) {
	            sentence = (GGASentence) SentenceFactory.getInstance()
	                    .createParser(this.GGAMessage);
	        }
        	return this.GGAMessage != null && sentence != null && sentence.getPosition() != null;
        } catch (Exception e){
        	FLog.e("wrong gga format sentence", e);
        	return false;
        }
    }

    @Override
	public void onLocationChanged(Location location) {
		setAccuracy(location.getAccuracy());
		setLocation(location);
		setInternalGPSTimestamp(System.currentTimeMillis());
		if(!hasValidInternalGPSSignal){
			setHasValidInternalGPSSignal(true);
			activityRef.get().invalidateOptionsMenu();
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		setAccuracy(0.0f);
		setLocation(null);
		setInternalGPSTimestamp(System.currentTimeMillis());
		if(hasValidInternalGPSSignal){
			setHasValidInternalGPSSignal(false);
			activityRef.get().invalidateOptionsMenu();
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void startInternalGPSListener(){
		try{
			destroyInternalGPSListener();
			this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, getGpsUpdateInterval() * 1000, 0, this);
			setInternalGPSStarted(true);
			activityRef.get().invalidateOptionsMenu();
		}catch(Exception e){
			FLog.e("Starting internal gps exception : " + e);
		}
	}

	public void startExternalGPSListener(){
		try{
			destroyExternalGPSListener();
			this.handlerThread = new HandlerThread("GPSHandler");
			this.handlerThread.start();
			this.handler = new Handler(this.handlerThread.getLooper());
			this.externalGPSTasks = new ExternalGPSTasks(this.gpsDevice,this.handler, this, getGpsUpdateInterval() * 1000);
			this.handler.postDelayed(externalGPSTasks, getGpsUpdateInterval() * 1000);
			setExternalGPSStarted(true);
			activityRef.get().invalidateOptionsMenu();
		}catch (Exception e){
			FLog.e("Starting external gps exception", e);
		}
	}

	private void destroyInternalGPSListener(){
		try{
			if(this.locationManager != null){
				this.locationManager.removeUpdates(this);
			}
			setHasValidInternalGPSSignal(false);
		}catch(Exception e){
			FLog.e("Stopping internal gps exception : " + e);
		}
	}
	
	private void destroyExternalGPSListener(){
		try{
			if(this.handler != null){
				this.externalGPSTasks.closeBluetoothConnection();
				this.handler.removeCallbacks(this.externalGPSTasks);
			}
			if(this.handlerThread != null){
				handlerThread.quit();
			}
			setHasValidExternalGPSSignal(false);
		}catch (Exception e) {
			FLog.e("Stopping external gps exception", e);
		}
	}
	
	public void destroyListener(){
		destroyInternalGPSListener();
		destroyExternalGPSListener();
	}

	public GPSLocation getGPSPosition(){
		try{
			if(isUsingExternalGPS()){
				GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
				double latitude = ggaSentence.getPosition().getLatitude();
				double longitude = ggaSentence.getPosition().getLongitude();
				latitude = CompassPoint.NORTH.equals(ggaSentence.getPosition().getLatHemisphere()) ? latitude : -latitude;
				longitude = CompassPoint.EAST.equals(ggaSentence.getPosition().getLonHemisphere()) ? longitude : -longitude;
				FLog.d("lat:" + latitude +", long: "+ longitude);
				return new GPSLocation(longitude, latitude, this.externalGPSTimestamp);
			}else if(isUsingInternalGPS()){
				return new GPSLocation(this.location.getLongitude(), this.location.getLatitude(), this.internalGPSTimestamp);
			}else{
				return null;
			}
		}catch(Exception e){
			FLog.e("error when getting gps position", e);
			return null;
		}
	}

	public Object getGPSEstimatedAccuracy(){
		try{
			if(isUsingExternalGPS()){
				GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
				double nmeaAccuracy = ggaSentence.getHorizontalDOP();
				return (float) nmeaAccuracy;
			}else if(isUsingInternalGPS()){
				return this.accuracy;
			}else{
				return null;
			}
		}catch(Exception e){
			FLog.e("error when getting gps accuracy", e);
			return null;
		}
	}

	public Object getGPSHeading(){
		try{
			if(isUsingExternalGPS()){
				if(this.BODMessage != null){
					BODSentence bodSentence = (BODSentence) SentenceFactory.getInstance().createParser(this.BODMessage);
					return bodSentence.getTrueBearing();
				}else{
					return null;
				}
			}else if(isUsingInternalGPS()){
				if (this.location.hasBearing()) {
					return this.location.getBearing();
				} else {
					return null;
				}
			}else{
				return null;
			}
		}catch(Exception e){
			FLog.e("error when getting gps heading", e);
			return null;
		}
	}

	public Object getGPSPosition(String gps){
		try{
			if(EXTERNAL.equals(gps) && this.GGAMessage != null){
				GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
				double latitude = ggaSentence.getPosition().getLatitude();
				double longitude = ggaSentence.getPosition().getLongitude();
				latitude = CompassPoint.NORTH.equals(ggaSentence.getPosition().getLatHemisphere()) ? latitude : -latitude;
				longitude = CompassPoint.EAST.equals(ggaSentence.getPosition().getLonHemisphere()) ? longitude : -longitude;
				return new GPSLocation(longitude, latitude, this.externalGPSTimestamp);
			}else if(INTERNAL.equals(gps) && isUsingInternalGPS()){
				return new GPSLocation(this.location.getLongitude(), this.location.getLatitude(), this.internalGPSTimestamp);
			}else{
				return null;
			}
		}catch(Exception e){
			FLog.e("error when getting gps position for " + gps, e);
			return null;
		}
	}

	public Object getGPSEstimatedAccuracy(String gps){
		try{
			if(EXTERNAL.equals(gps) && this.GGAMessage != null){
				GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
				double nmeaAccuracy = ggaSentence.getHorizontalDOP();
				return (float)nmeaAccuracy;
			}else if(INTERNAL.equals(gps) && isUsingInternalGPS()){
				return this.accuracy;
			}else{
				return null;
			}
		}catch(Exception e){
			FLog.e("error when getting gps accuracy for " + gps, e);
			return null;
		}
	}

	public Object getGPSHeading(String gps){
		try{
			if(EXTERNAL.equals(gps) && this.GGAMessage != null){
				if(this.BODMessage != null){
					BODSentence bodSentence = (BODSentence) SentenceFactory.getInstance().createParser(this.BODMessage);
					return bodSentence.getTrueBearing();
				}else{
					return null;
				}
			}else if(INTERNAL.equals(gps) && isUsingInternalGPS()){
				if (this.location.hasBearing()) {
					return this.location.getBearing();
				} else {
					return null;
				}
			}else{
				return null;
			}
		}catch(Exception e){
			FLog.e("error when getting gps heading for " + gps, e);
			return null;
		}
	}

	private boolean isUsingExternalGPS(){
		if(this.GGAMessage != null){
			try{
				GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
				double nmeaAccuracy = ggaSentence.getHorizontalDOP();
				if(this.location != null && nmeaAccuracy > accuracy){
					return false;
				}
				return true;
	        } catch (DataNotAvailableException e){
	        	return false;
	        }
		}
		return false;
	}

	private boolean isUsingInternalGPS(){
		return this.location != null && this.accuracy != 0.0;
	}

	public void setGGAMessage(String gGAMessage) {
		this.GGAMessage = gGAMessage;
	}

	public void setBODMessage(String bODMessage) {
		this.BODMessage = bODMessage;
	}

	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

    public void setExternalGPSTimestamp(long externalGPSTimestamp) {
		this.externalGPSTimestamp = externalGPSTimestamp;
	}

	public void setInternalGPSTimestamp(long internalGPSTimestamp) {
		this.internalGPSTimestamp = internalGPSTimestamp;
	}

	public void setGpsDevice(BluetoothDevice gpsDevice) {
		this.gpsDevice = gpsDevice;
	}

	public void setLocationManager(LocationManager locationManager) {
		this.locationManager = locationManager;
	}

	public boolean hasValidExternalGPSSignal() {
		return hasValidExternalGPSSignal;
	}

	public void setHasValidExternalGPSSignal(boolean hasValidExternalGPSSignal) {
		this.hasValidExternalGPSSignal = hasValidExternalGPSSignal;
	}

	public boolean hasValidInternalGPSSignal() {
		return hasValidInternalGPSSignal;
	}

	public void setHasValidInternalGPSSignal(boolean hasValidInternalGPSSignal) {
		this.hasValidInternalGPSSignal = hasValidInternalGPSSignal;
	}

	public int getGpsUpdateInterval() {
		return gpsUpdateInterval;
	}
	public void setGpsUpdateInterval(int gpsUpdateInterval) {
		this.gpsUpdateInterval = gpsUpdateInterval;
	}

	public boolean isExternalGPSStarted() {
		return isExternalGPSStarted;
	}

	public void setExternalGPSStarted(boolean isExternalGPSStarted) {
		this.isExternalGPSStarted = isExternalGPSStarted;
	}

	public boolean isInternalGPSStarted() {
		return isInternalGPSStarted;
	}

	public void setInternalGPSStarted(boolean isInternalGPSStarted) {
		this.isInternalGPSStarted = isInternalGPSStarted;
	}

	public String getTrackingType() {
		return trackingType;
	}

	public void setTrackingType(String trackingType) {
		this.trackingType = trackingType;
	}

	public int getTrackingValue() {
		return trackingValue;
	}

	public void setTrackingValue(int trackingValue) {
		this.trackingValue = trackingValue;
	}
	
	public String getTrackingExec() {
		return trackingExec;
	}

	public void setTrackingExec(String exec) {
		this.trackingExec = exec;
	}

	public boolean isTrackingStarted() {
		return isTrackingStarted;
	}

	public void setTrackingStarted(boolean isTrackingStarted) {
		this.isTrackingStarted = isTrackingStarted;
	}

	@Override
	public void saveTo(Bundle savedInstanceState) {
		savedInstanceState.putBoolean("isExternalGPSStarted", isExternalGPSStarted);
		savedInstanceState.putBoolean("isInternalGPSStarted", isInternalGPSStarted);
		savedInstanceState.putInt("gpsUpdateInterval", gpsUpdateInterval);
		savedInstanceState.putString("trackingType", trackingType);
		savedInstanceState.putInt("trackingValue", trackingValue);
		savedInstanceState.putString("trackingExec", trackingExec);
	}

	@Override
	public void restoreFrom(Bundle savedInstanceState) {
		setExternalGPSStarted(savedInstanceState.getBoolean("isExternalGPSStarted"));
		setInternalGPSStarted(savedInstanceState.getBoolean("isInternalGPSStarted"));
		setGpsUpdateInterval(savedInstanceState.getInt("gpsUpdateInterval"));
		setTrackingType(savedInstanceState.getString("trackingType"));
		setTrackingValue(savedInstanceState.getInt("trackingValue"));
		setTrackingExec(savedInstanceState.getString("trackingExec"));
	}

}

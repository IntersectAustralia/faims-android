package au.org.intersect.faims.android.gps;

import java.lang.ref.WeakReference;

import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.BODSentence;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.util.CompassPoint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import au.org.intersect.faims.android.data.ActivityData;
import au.org.intersect.faims.android.data.IFAIMSRestorable;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.BluetoothManager;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GPSDataManager implements BluetoothManager.BluetoothListener, LocationListener, ActivityData, IFAIMSRestorable {

	private static final String TAG = "gpsmanager:";
	
	@Inject
	BluetoothManager bluetoothManager;
	
	private static final String EXTERNAL = "external";
	private static final String INTERNAL = "internal";
	
	private String GGAMessage;
    private String BODMessage;
   
	private float accuracy;
    private Location location;
    private long externalGPSTimestamp;
    private long internalGPSTimestamp;

	private LocationManager locationManager;

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
		GGAMessage = null;
		BODMessage = null;
		accuracy = 0;
		location = null;
		externalGPSTimestamp = 0;
		internalGPSTimestamp = 0;
		locationManager = manager;
		gpsUpdateInterval = 10;
		isExternalGPSStarted = false;
		isInternalGPSStarted = false;
		hasValidExternalGPSSignal = false;
		hasValidInternalGPSSignal = false;
		trackingType = null;
		trackingValue = 0;
		trackingExec = null;
		isTrackingStarted = false;
		activityRef = new WeakReference<ShowModuleActivity>(activity);
	}
	
    @Override
	public void onInput(String input) {
    	String nmeaMessage = input;
        if (nmeaMessage == null) {
        	return;
        }
        
        if (nmeaMessage.startsWith("$GPGGA")) {
            setGGAMessage(nmeaMessage);
            setExternalGPSTimestamp(System.currentTimeMillis());
        } else if (nmeaMessage.startsWith("$GPBOD")) {
        	setBODMessage(nmeaMessage);
        	setExternalGPSTimestamp(System.currentTimeMillis());
        } else {
        	return;
        }
        
		checkValidExternalGPSSignal();		
		bluetoothManager.clearMessages();
	}
    
    @Override
    public void onConnect() {
    	setGGAMessage(null);
    	setBODMessage(null);
    	checkValidExternalGPSSignal();
    }
    
    @Override
    public void onDisconnect() {
    	setGGAMessage(null);
    	setBODMessage(null);
    	checkValidExternalGPSSignal();
    }
    
    private void checkValidExternalGPSSignal() {
    	if (!hasValidExternalGPSSignal()) {
			if (hasValidGGAMessage()) {
				setHasValidExternalGPSSignal(true);
			}
		} else{ 
			if (!hasValidGGAMessage()) {
				setHasValidExternalGPSSignal(false);
			}
		}
    }
    
    private boolean hasValidGGAMessage() {
    	GGASentence sentence = null;
        try {
	        if (GGAMessage != null) {
	            sentence = (GGASentence) SentenceFactory.getInstance().createParser(GGAMessage);
	        }
        	return GGAMessage != null && sentence != null && sentence.getPosition() != null;
        } catch (Exception e){
        	FLog.e("error parsing gga sentence", e);
        	return false;
        }
    }

    @Override
	public void onLocationChanged(Location location) {
		setAccuracy(location.getAccuracy());
		setLocation(location);
		setInternalGPSTimestamp(System.currentTimeMillis());
		if (!hasValidInternalGPSSignal()) {
			setHasValidInternalGPSSignal(true);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		setAccuracy(0.0f);
		setLocation(null);
		setInternalGPSTimestamp(System.currentTimeMillis());
		if(hasValidInternalGPSSignal()){
			setHasValidInternalGPSSignal(false);
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void startInternalGPSListener(){
		destroyInternalGPSListener();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, getGpsUpdateInterval() * 1000, 0, this);
		setInternalGPSStarted(true);
	}
	
	private void destroyInternalGPSListener(){
		pauseInternalGPSListener();
		setInternalGPSStarted(false);
		setHasValidInternalGPSSignal(false);
	}
	
	public void startExternalGPSListener(){
		destroyExternalGPSListener();
		bluetoothManager.createConnection(this, getGpsUpdateInterval() * 1000);
		setExternalGPSStarted(true);
	}
	
	private void destroyExternalGPSListener(){
		pauseExternalGPSListener();
		setExternalGPSStarted(false);
		setHasValidExternalGPSSignal(false);
	}
	
	public void destroyListener() {
		destroyInternalGPSListener();
		destroyExternalGPSListener();
	}
	
	public void pauseListener() {
		pauseInternalGPSListener();
		pauseExternalGPSListener();
	}
	
	public void pauseInternalGPSListener() {
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
	}
	
	public void pauseExternalGPSListener() {
		bluetoothManager.destroyConnection();
	}

	public GPSLocation getGPSPosition(String gps) {
		try {
			if ((gps == null || EXTERNAL.equals(gps)) && hasValidGGAMessage()) {
				GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
				double latitude = ggaSentence.getPosition().getLatitude();
				double longitude = ggaSentence.getPosition().getLongitude();
				latitude = CompassPoint.NORTH.equals(ggaSentence.getPosition().getLatHemisphere()) ? latitude : -latitude;
				longitude = CompassPoint.EAST.equals(ggaSentence.getPosition().getLonHemisphere()) ? longitude : -longitude;
				return new GPSLocation(longitude, latitude, this.externalGPSTimestamp);
			} else if ((gps == null || INTERNAL.equals(gps)) && isUsingInternalGPS()) {
				return new GPSLocation(this.location.getLongitude(), this.location.getLatitude(), this.internalGPSTimestamp);
			}
		}catch(Exception e){
			FLog.e("error when getting gps position for " + gps, e);
		}
		return null;
	}
	
	public GPSLocation getGPSPosition(){
		return getGPSPosition(null);
	}

	public Object getGPSEstimatedAccuracy(String gps) {
		try {
			if ((gps == null || EXTERNAL.equals(gps)) && hasValidGGAMessage()) {
				GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
				double nmeaAccuracy = ggaSentence.getHorizontalDOP();
				return (float) nmeaAccuracy;
			} else if ((gps == null || INTERNAL.equals(gps)) && isUsingInternalGPS()) {
				return this.accuracy;
			}
		} catch(Exception e) {
			FLog.e("error when getting gps accuracy for " + gps, e);
		}
		return null;
	}
	
	public Object getGPSEstimatedAccuracy(){
		return getGPSEstimatedAccuracy(null);
	}

	public Object getGPSHeading(String gps) {
		try {
			if ((gps == null || EXTERNAL.equals(gps)) && isUsingExternalGPS()) {
				if (this.BODMessage != null) {
					BODSentence bodSentence = (BODSentence) SentenceFactory.getInstance().createParser(this.BODMessage);
					return bodSentence.getTrueBearing();
				} else {
					return null;
				}
			} else if ((gps == null || INTERNAL.equals(gps)) && isUsingInternalGPS()) {
				if (this.location.hasBearing()) {
					return this.location.getBearing();
				} else {
					return null;
				}
			}
		} catch(Exception e) {
			FLog.e("error when getting gps heading for " + gps, e);
		}
		return null;
	}
	
	public Object getGPSHeading() {
		return getGPSHeading(null);
	}

	private boolean isUsingExternalGPS(){
		if (this.GGAMessage != null) {
			try {
				GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
				double nmeaAccuracy = ggaSentence.getHorizontalDOP();
				if (this.location != null && nmeaAccuracy > accuracy) {
					return false;
				}
				return true;
	        } catch (DataNotAvailableException e) {
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

	public void setLocationManager(LocationManager locationManager) {
		this.locationManager = locationManager;
	}

	public boolean hasValidExternalGPSSignal() {
		return hasValidExternalGPSSignal;
	}

	public void setHasValidExternalGPSSignal(boolean hasValidExternalGPSSignal) {
		this.hasValidExternalGPSSignal = hasValidExternalGPSSignal;
		activityRef.get().updateActionBar();
	}

	public boolean hasValidInternalGPSSignal() {
		return hasValidInternalGPSSignal;
	}

	public void setHasValidInternalGPSSignal(boolean hasValidInternalGPSSignal) {
		this.hasValidInternalGPSSignal = hasValidInternalGPSSignal;
		activityRef.get().updateActionBar();
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
		activityRef.get().updateActionBar();
	}

	public boolean isInternalGPSStarted() {
		return isInternalGPSStarted;
	}

	public void setInternalGPSStarted(boolean isInternalGPSStarted) {
		this.isInternalGPSStarted = isInternalGPSStarted;
		activityRef.get().updateActionBar();
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
		activityRef.get().updateActionBar();
	}

	@Override
	public void saveTo(Bundle savedInstanceState) {
		savedInstanceState.putBoolean(TAG + "isExternalGPSStarted", isExternalGPSStarted);
		savedInstanceState.putBoolean(TAG + "isInternalGPSStarted", isInternalGPSStarted);
		savedInstanceState.putInt(TAG + "gpsUpdateInterval", gpsUpdateInterval);
		savedInstanceState.putString(TAG + "trackingType", trackingType);
		savedInstanceState.putInt(TAG + "trackingValue", trackingValue);
		savedInstanceState.putString(TAG + "trackingExec", trackingExec);
	}

	@Override
	public void restoreFrom(Bundle savedInstanceState) {
		setExternalGPSStarted(savedInstanceState.getBoolean(TAG + "isExternalGPSStarted"));
		setInternalGPSStarted(savedInstanceState.getBoolean(TAG + "isInternalGPSStarted"));
		setGpsUpdateInterval(savedInstanceState.getInt(TAG + "gpsUpdateInterval"));
		setTrackingType(savedInstanceState.getString(TAG + "trackingType"));
		setTrackingValue(savedInstanceState.getInt(TAG + "trackingValue"));
		setTrackingExec(savedInstanceState.getString(TAG + "trackingExec"));
	}

	@Override
	public void resume() {
		if (isExternalGPSStarted()) {
			startExternalGPSListener();
		}
		
		if (isInternalGPSStarted()) {
			startInternalGPSListener();
		}
	}

	@Override
	public void pause() {
		if (!isTrackingStarted()) {
			pauseListener();
		}
	}

	@Override
	public void destroy() {
		destroyListener();
	}

}

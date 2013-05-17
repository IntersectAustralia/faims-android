package au.org.intersect.faims.android.gps;

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
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.tasks.BluetoothActionListener;
import au.org.intersect.faims.android.tasks.ExternalGPSTasks;

import com.google.inject.Singleton;

@Singleton
public class GPSDataManager implements BluetoothActionListener, LocationListener{

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

	private int gpsUpdateInterval=10000;
	
	private boolean isExternalGPSStarted;
	private boolean isInternalGPSStarted;
	
	public void init(LocationManager manager){
		this.locationManager = manager;
	}
	
    @Override
	public void handleGPSUpdates(String GGAMessage, String BODMessage) {
		setGGAMessage(GGAMessage);
		setBODMessage(BODMessage);
		setExternalGPSTimestamp(System.currentTimeMillis());
	}

	@Override
	public void onLocationChanged(Location location) {
		setAccuracy(location.getAccuracy());
		setLocation(location);
		setInternalGPSTimestamp(System.currentTimeMillis());
	}

	@Override
	public void onProviderDisabled(String provider) {
		setAccuracy(0.0f);
		setLocation(null);
		setInternalGPSTimestamp(System.currentTimeMillis());
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
			this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, getGpsUpdateInterval(), 0, this);
			setInternalGPSStarted(true);
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
			this.externalGPSTasks = new ExternalGPSTasks(this.gpsDevice,this.handler, this, getGpsUpdateInterval());
			this.handler.postDelayed(externalGPSTasks, getGpsUpdateInterval());
			setExternalGPSStarted(true);
		}catch (Exception e){
			FLog.e("Starting external gps exception", e);
		}
	}

	public void destroyInternalGPSListener(){
		try{
			if(this.locationManager != null){
				this.locationManager.removeUpdates(this);
			}
			setInternalGPSStarted(false);
		}catch(Exception e){
			FLog.e("Stopping internal gps exception : " + e);
		}
	}
	
	public void destroyExternalGPSListener(){
		try{
			if(this.handler != null){
				this.externalGPSTasks.closeBluetoothConnection();
				this.handler.removeCallbacks(this.externalGPSTasks);
			}
			if(this.handlerThread != null){
				handlerThread.quit();
			}
			setExternalGPSStarted(false);
		}catch (Exception e) {
			FLog.e("Stopping external gps exception", e);
		}
	}
	
	public void destroyListener(){
		destroyInternalGPSListener();
		destroyExternalGPSListener();
	}

	public GPSLocation getGPSPosition(){
		if(isUsingExternalGPS()){
			GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
			double latitude = ggaSentence.getPosition().getLatitude();
			double longitude = ggaSentence.getPosition().getLongitude();
			latitude = CompassPoint.NORTH.equals(ggaSentence.getPosition().getLatHemisphere()) ? latitude : -latitude;
			longitude = CompassPoint.EAST.equals(ggaSentence.getPosition().getLonHemisphere()) ? longitude : -longitude;
			return new GPSLocation(longitude, latitude, this.externalGPSTimestamp);
		}else if(isUsingInternalGPS()){
			return new GPSLocation(this.location.getLongitude(), this.location.getLatitude(), this.internalGPSTimestamp);
		}else{
			return null;
		}
	}

	public Object getGPSEstimatedAccuracy(){
		if(isUsingExternalGPS()){
			GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
			double nmeaAccuracy = ggaSentence.getHorizontalDOP();
			return (float) nmeaAccuracy;
		}else if(isUsingInternalGPS()){
			return this.accuracy;
		}else{
			return null;
		}
	}

	public Object getGPSHeading(){
		if(isUsingExternalGPS()){
			if(this.BODMessage != null){
				BODSentence bodSentence = (BODSentence) SentenceFactory.getInstance().createParser(this.BODMessage);
				return bodSentence.getTrueBearing();
			}else{
				return 0.0f;
			}
		}else if(isUsingInternalGPS()){
			return this.location.getBearing();
		}else{
			return null;
		}
	}

	public Object getGPSPosition(String gps){
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
	}

	public Object getGPSEstimatedAccuracy(String gps){
		if(EXTERNAL.equals(gps) && this.GGAMessage != null){
			GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
			double nmeaAccuracy = ggaSentence.getHorizontalDOP();
			return (float)nmeaAccuracy;
		}else if(INTERNAL.equals(gps) && isUsingInternalGPS()){
			return this.accuracy;
		}else{
			return null;
		}
	}

	public Object getGPSHeading(String gps){
		if(EXTERNAL.equals(gps) && this.GGAMessage != null){
			if(this.BODMessage != null){
				BODSentence bodSentence = (BODSentence) SentenceFactory.getInstance().createParser(this.BODMessage);
				return bodSentence.getTrueBearing();
			}else{
				return 0.0f;
			}
		}else if(INTERNAL.equals(gps) && isUsingInternalGPS()){
			return this.location.getBearing();
		}else{
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

	public int getGpsUpdateInterval() {
		return gpsUpdateInterval;
	}
	public void setGpsUpdateInterval(int gpsUpdateInterval) {
		this.gpsUpdateInterval = gpsUpdateInterval*1000;
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
	
}

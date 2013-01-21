package au.org.intersect.faims.android.util;

import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.BODSentence;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.util.CompassPoint;
import android.location.Location;

public class GPSData {

	private String GGAMessage;
    private String BODMessage;
    private long externalGPSTimestamp;

	private float accuracy;
    private Location location;
    private long internalGPSTimestamp;

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
			return nmeaAccuracy;
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
				return 0.0;
			}
		}else if(isUsingInternalGPS()){
			return this.location.getBearing();
		}else{
			return null;
		}
	}

	public Object getGPSPosition(String gps){
		if("external".equals(gps) && this.GGAMessage != null){
			GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
			double latitude = ggaSentence.getPosition().getLatitude();
			double longitude = ggaSentence.getPosition().getLongitude();
			latitude = CompassPoint.NORTH.equals(ggaSentence.getPosition().getLatHemisphere()) ? latitude : -latitude;
			longitude = CompassPoint.EAST.equals(ggaSentence.getPosition().getLonHemisphere()) ? longitude : -longitude;
			return new GPSLocation(longitude, latitude, this.externalGPSTimestamp);
		}else if("internal".equals(gps) && isUsingInternalGPS()){
			return new GPSLocation(this.location.getLongitude(), this.location.getLatitude(), this.internalGPSTimestamp);
		}else{
			return null;
		}
	}

	public Object getGPSEstimatedAccuracy(String gps){
		if("external".equals(gps) && this.GGAMessage != null){
			GGASentence ggaSentence = (GGASentence) SentenceFactory.getInstance().createParser(this.GGAMessage);
			double nmeaAccuracy = ggaSentence.getHorizontalDOP();
			return nmeaAccuracy;
		}else if("internal".equals(gps) && isUsingInternalGPS()){
			return this.accuracy;
		}else{
			return null;
		}
	}

	public Object getGPSHeading(String gps){
		if("external".equals(gps) && this.GGAMessage != null){
			if(this.BODMessage != null){
				BODSentence bodSentence = (BODSentence) SentenceFactory.getInstance().createParser(this.BODMessage);
				return bodSentence.getTrueBearing();
			}else{
				return 0.0;
			}
		}else if("internal".equals(gps) && isUsingInternalGPS()){
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

}

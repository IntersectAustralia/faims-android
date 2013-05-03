package au.org.intersect.faims.android.nutiteq;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import au.org.intersect.faims.android.log.FLog;

import com.nutiteq.components.MapPos;

public class SimpleScaleBar {

  private static final int EARTH_RADIUS=6371;
  private static final double METRIC_TO_IMPERIAL=0.621371192;

  public static final int TOP_LEFT=0;
  public static final int TOP_RIGHT=1;
  public static final int BOTTOM_LEFT=2;
  public static final int BOTTOM_RIGHT=3;

  public static final int METRIC=0;
  public static final int IMPERIAL=1;

  private static final String IMPERIAL_UNIT=" mi";
  private static final String METRIC_UNIT=" km";
  private static final String IMPERIAL_UNIT_SMALL=" ft";
  private static final String METRIC_UNIT_SMALL=" m";

  private static final int KM_TO_M=1000;
  private static final int MI_TO_FT=5280;

  private static final int BAR_SIZE = 10;
  private static final int BAR_BORDER = 5;

  private int alignment=BOTTOM_RIGHT;
  private int startx;
  private int starty;
  private int offsetx=20;
  private int offsety=20;
  private int endx;
  private int endy;
  private int mapWidthPx;
  private int mapHeightPx;
  private double mapWidthKm;
  private int barMinWidth=40;
  private int barMaxWidth=101;
  private int barWidth;
  private int unitMode = METRIC;

  private boolean visible=true;

  private double scale;
  double[] allowedScales = { 10000, 5000, 2000, 1000, 500, 200, 100, 50, 
      20, 10, 5, 2, 1, 0.500, 0.200, 0.100, 0.050, 0.020 };

  /**
   * Set distance unit used by the scale bar
   * 
   * 
   * @param unitType
   *              set the distance unit, METRIC or IMPERIAL
   *         
   */
  public void setUnitMode(int unitType){
    switch (unitType){
    case METRIC:
      unitMode=METRIC;
      break;
    case IMPERIAL:
      unitMode=IMPERIAL;
      break;
    }
    calculateScaleBar();
  }

  /**
   * Get distance unit currently used by the scale bar
   * @return
   *            IMPERIAL or METRIC
   */
  public int getUnitMode(){
    return unitMode;
  }


  private void calculateScaleBar(){
    double mapWidthInUnits=mapWidthKm;
    //FLog.d("mapWidthKM: " + mapWidthKm);
    
    if (unitMode==IMPERIAL){
      mapWidthInUnits*=METRIC_TO_IMPERIAL;
    }
    //FLog.d("mapWidthInUnits: " + mapWidthInUnits);
    

    if (mapWidthPx>0 && mapWidthInUnits>0){
    	
      double currentScale = mapWidthPx/mapWidthInUnits; // in pixels/meter 

      for (int i=0;i<allowedScales.length;i++){ 
        double tempBarWidth = allowedScales[i]*currentScale; 
        //FLog.d("barMinWidth: " + barMinWidth);
        //FLog.d("barMaxWidth: " + barMaxWidth);
        //FLog.d("tempBarWidth: " + tempBarWidth);
        
        if((tempBarWidth > barMinWidth) & (tempBarWidth <= barMaxWidth)){ 
          barWidth=(int) tempBarWidth;
          scale=allowedScales[i];
          calculatePosition();

          break; 
        } 
      }
    }
  }

  private void calculatePosition(){
    // Log.debug("CalcPos");
    switch (alignment) {
    case TOP_LEFT: 
      startx=0+offsetx;
      starty=0+offsety;
      endx=startx+barWidth;
      endy=starty;
      break;
    case TOP_RIGHT:
      startx=mapWidthPx-offsetx;
      starty=0+offsety;
      endx=startx-barWidth;
      endy=starty;
      break;
    case BOTTOM_LEFT: 
      startx=0+offsetx;
      starty=mapHeightPx-offsety;
      endx=startx+barWidth;
      endy=starty;
      break;
    case BOTTOM_RIGHT:
      endx=mapWidthPx-offsetx;
      starty=mapHeightPx-offsety;
      startx=endx-barWidth;
      endy=starty;
      break;
    }
  }

  /*
  public void mapMoved(MapPos min, MapPos max) {
    double lat1 = (min.y + max.y)/2.0;  
    double lat2 = lat1; 
    double lon1 = min.x;
    double lon2 = max.x;
    double dLat = Math.toRadians(lat2-lat1);  
    double dLon = Math.toRadians(lon2-lon1);  
    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +  
    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *  
    Math.sin(dLon/2) * Math.sin(dLon/2);  
    double c = 2 * Math.asin(Math.sqrt(a)); 
    mapWidthKm = EARTH_RADIUS * c;
    calculateScaleBar();
  }
*/
  
  public void setMapWidth(double value) {
	  mapWidthKm = value;
	  calculateScaleBar();
  }
  public void paint(Canvas g) {
	FLog.d("scale: " + scale);
	FLog.d("barWidth: " + barWidth);
    if (scale>0 && barWidth>0) {
    	
    	FLog.d("startx: " + startx);
    	FLog.d("starty: " + starty);
    	FLog.d("endx: " + endx);
    	
    	//g.drawRect(0, 0, 100, 100, getPaint(Color.BLUE));
    	
    	g.drawRect(startx, starty, startx + Math.abs(startx-endx), starty + BAR_SIZE, getPaint(Color.BLUE));

    	g.drawRect(startx+BAR_BORDER, starty+BAR_BORDER, startx+BAR_BORDER + Math.abs(startx-endx)-2*BAR_BORDER, starty+BAR_BORDER + BAR_SIZE-2*BAR_BORDER, getPaint(Color.BLACK));
     
      if (unitMode==METRIC){
        if (scale>=1.0){
          g.drawText(Double.toString(round2Places(scale))+METRIC_UNIT, (startx+endx)/2, starty-BAR_BORDER, getPaint(Color.RED));
        }else{
          g.drawText(Double.toString(round2Places(scale*KM_TO_M))+METRIC_UNIT_SMALL, (startx+endx)/2, starty-BAR_BORDER, getPaint(Color.RED));
        }
      }else if (unitMode==IMPERIAL){
        if (scale>=1.0){
          g.drawText(Double.toString(round2Places(scale))+IMPERIAL_UNIT, (startx+endx)/2, starty-BAR_BORDER, getPaint(Color.RED));
        }else{
          g.drawText(Double.toString(round2Places(scale*MI_TO_FT))+IMPERIAL_UNIT_SMALL, (startx+endx)/2, starty-BAR_BORDER, getPaint(Color.RED));
        }
      }
    }
  }
  
  private Paint getPaint(int color) {
	  	Paint p = new Paint();
  		p.setColor(color);
		p.setStrokeWidth(5.0f);
		p.setAntiAlias(true);
		return p;
  }

  private double round2Places(double a) {
    return Math.ceil(a * 100)/100;
  }


  public void reSize(int width, int height, double mapWidth) {
    this.mapWidthPx=width;
    this.mapHeightPx=height;
    //mapMoved(min, max);
    setMapWidth(mapWidth);
  }


  /**
   * Set the scale bar min and max length
   * 
   * 
   * @param barMinWidth
   *            min allowed length of the scale bar
   * @param barMaxWidth
   *            max allowed length of the scale bar
   *         
   */
  public void setBarWidthRange(int barMinWidth, int barMaxWidth) {
    this.barMinWidth=barMinWidth;
    this.barMaxWidth=barMaxWidth;
    calculateScaleBar();
  }

  /**
   * Set the alignment of the scale bar
   * 
   * 
   * @param alignment
   *            TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT or BOTTOM_RIGHT
   *         
   */
  public void setAlignment(int alignment) {
    this.alignment=alignment;
    calculatePosition();
  }

  /**
   * Set the offset from map corner
   * 
   * 
   * @param x
   *          x offset
   * @param y
   *          y offset    
   */
  public void setOffset(int x, int y) {
    this.offsetx=x;
    this.offsety=y;
    calculatePosition();
  }


  public boolean isVisible() {
    return visible;

  }


  public void setVisible(boolean visible) {
    this.visible=visible;

  }



}
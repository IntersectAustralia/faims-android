package au.org.intersect.faims.android.nutiteq;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

//Note: this is an old nutiteq library class
public class SimpleScaleBar {

	//private static final int EARTH_RADIUS = 6371;
	private static final double METRIC_TO_IMPERIAL = 0.621371192;

	public static final int TOP_LEFT = 0;
	public static final int TOP_RIGHT = 1;
	public static final int BOTTOM_LEFT = 2;
	public static final int BOTTOM_RIGHT = 3;

	public static final int METRIC = 0;
	public static final int IMPERIAL = 1;

	private static final String IMPERIAL_UNIT = " mi";
	private static final String METRIC_UNIT = " km";
	private static final String IMPERIAL_UNIT_SMALL = " ft";
	private static final String METRIC_UNIT_SMALL = " m";

	private static final int KM_TO_M = 1000;
	private static final int MI_TO_FT = 5280;

	private static final int BAR_SIZE = 6;
	private static final int BAR_BORDER = 2;

	private int alignment = BOTTOM_RIGHT;
	private int startx;
	private int starty;
	private int offsetx = 20;
	private int offsety = 20;
	private int endx;
	//private int endy;
	private int mapWidthPx;
	private int mapHeightPx;
	private double mapWidthKm;
	private int barMinWidth = 40;
	private int barMaxWidth = 101;
	private int barWidth;
	private int unitMode = METRIC;

	private boolean visible = true;

	private double scale;
	double[] allowedScales = { 10000, 5000, 2000, 1000, 500, 200, 100, 50, 20,
			10, 5, 2, 1, 0.500, 0.200, 0.100, 0.050, 0.020 };
	
	private Paint white;
	//private Paint black;
	
	public SimpleScaleBar() {
		white = getPaint(Color.WHITE);
		//black = getPaint(Color.BLACK);
	}

	/**
	 * Set distance unit used by the scale bar
	 * 
	 * 
	 * @param unitType
	 *            set the distance unit, METRIC or IMPERIAL
	 * 
	 */
	public void setUnitMode(int unitType) {
		switch (unitType) {
		case METRIC:
			unitMode = METRIC;
			break;
		case IMPERIAL:
			unitMode = IMPERIAL;
			break;
		}
		calculateScaleBar();
	}

	/**
	 * Get distance unit currently used by the scale bar
	 * 
	 * @return IMPERIAL or METRIC
	 */
	public int getUnitMode() {
		return unitMode;
	}

	private void calculateScaleBar() {
		double mapWidthInUnits = mapWidthKm;

		if (unitMode == IMPERIAL) {
			mapWidthInUnits *= METRIC_TO_IMPERIAL;
		}

		if (mapWidthPx > 0 && mapWidthInUnits > 0) {

			double currentScale = mapWidthPx / mapWidthInUnits; // in
																// pixels/meter

			for (int i = 0; i < allowedScales.length; i++) {
				double tempBarWidth = allowedScales[i] * currentScale;
				
				if ((tempBarWidth > barMinWidth)
						& (tempBarWidth <= barMaxWidth)) {
					barWidth = (int) tempBarWidth;
					scale = allowedScales[i];
					calculatePosition();

					break;
				}
			}
		}
	}

	private void calculatePosition() {
		switch (alignment) {
		case TOP_LEFT:
			startx = 0 + offsetx;
			starty = 0 + offsety;
			endx = startx + barWidth;
			//endy = starty;
			break;
		case TOP_RIGHT:
			startx = mapWidthPx - offsetx;
			starty = 0 + offsety;
			endx = startx - barWidth;
			//endy = starty;
			break;
		case BOTTOM_LEFT:
			startx = 0 + offsetx;
			starty = mapHeightPx - offsety;
			endx = startx + barWidth;
			//endy = starty;
			break;
		case BOTTOM_RIGHT:
			endx = mapWidthPx - offsetx;
			starty = mapHeightPx - offsety;
			startx = endx - barWidth;
			//endy = starty;
			break;
		}
	}

	public void setMapWidth(double value) {
		mapWidthKm = value;
		calculateScaleBar();
	}

	public void paint(Canvas g) {
		if (scale > 0 && barWidth > 0) {
			int sx = startx;
			int sy = starty;
			int ex = startx + Math.abs(startx - endx);
			int ey = starty + BAR_SIZE;
			
			//g.drawRect(sx - BAR_BORDER, sy - BAR_BORDER, ex + BAR_BORDER, ey + BAR_BORDER, black);
			int barEndHeight = 8;
			g.drawRect(sx - BAR_BORDER, sy - barEndHeight, sx, ey, white);
			g.drawRect(ex, sy - barEndHeight, ex + BAR_BORDER, ey, white);
			
			g.drawRect(sx, sy, ex, ey, white);

			if (unitMode == METRIC) {
				if (scale >= 1.0) {
					g.drawText(Double.toString(round2Places(scale))
							+ METRIC_UNIT, sx + BAR_BORDER, sy
							- BAR_BORDER, white);
				} else {
					g.drawText(Double.toString(round2Places(scale * KM_TO_M))
							+ METRIC_UNIT_SMALL, sx + BAR_BORDER, sy
							- BAR_BORDER, white);
				}
			} else if (unitMode == IMPERIAL) {
				if (scale >= 1.0) {
					g.drawText(Double.toString(round2Places(scale))
							+ IMPERIAL_UNIT, sx + BAR_BORDER, sy
							- BAR_BORDER, white);
				} else {
					g.drawText(Double.toString(round2Places(scale * MI_TO_FT))
							+ IMPERIAL_UNIT_SMALL, sx + BAR_BORDER, sy
							-  BAR_BORDER, white);
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
		return Math.ceil(a * 100) / 100;
	}

	public void reSize(int width, int height, double mapWidth) {
		this.mapWidthPx = width;
		this.mapHeightPx = height;
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
		this.barMinWidth = barMinWidth;
		this.barMaxWidth = barMaxWidth;
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
		this.alignment = alignment;
		calculatePosition();
	}

	/**
	 * Set the offset from map corner
	 * 
	 * 
	 * @param x
	 *            x offset
	 * @param y
	 *            y offset
	 */
	public void setOffset(int x, int y) {
		this.offsetx = x;
		this.offsety = y;
		calculatePosition();
	}

	public boolean isVisible() {
		return visible;

	}

	public void setVisible(boolean visible) {
		this.visible = visible;

	}

}
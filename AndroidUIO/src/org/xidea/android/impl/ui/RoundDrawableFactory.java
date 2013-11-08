package org.xidea.android.impl.ui;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import org.xidea.android.DrawableFactory.DefaultDrawableFactory;
import org.xidea.android.impl.ApplicationState;

public class RoundDrawableFactory extends DefaultDrawableFactory {
	private static final Resources RESOURCES = ApplicationState.getInstance().getApplication().getResources();

	public RoundDrawableFactory(float radiusDip) {
		this.radius = (int)dipToPixels(radiusDip);
	}

	@Override
	public Bitmap parseResource(Bitmap bitmap) {
		return createBoxBitmapAndCleanRaw(bitmap);
	}

	protected int radius = 0;
	protected int shadowRadius = 0;
	protected int shadowDx = 0;
	protected int shadowDy = 0;
	protected int borderWidth;
	protected int borderColor;
	protected int shadowColor;

	public void setShadow(float shadowRadiusDip, float shadowDipDx, float shadowDipDy,
			int shadowColor) {
		this.shadowRadius =(int) dipToPixels(shadowRadiusDip);
		this.shadowDx = (int) dipToPixels(shadowDipDx);
		this.shadowDy = (int) dipToPixels(shadowDipDy);
		this.shadowColor = shadowColor;
	}
	public static float dipToPixels(float dipValue) {
	    DisplayMetrics metrics = RESOURCES.getDisplayMetrics();
	    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}
	public void setBorder(float borderWidth, int color) {
		this.borderWidth = (int) dipToPixels(borderWidth);
		this.borderColor = color;
	}

	public Bitmap createBoxBitmapAndCleanRaw(Bitmap bitmap) {

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		int x = computeOffset(shadowDx);
		int y = computeOffset(shadowDy);

		int left = x + borderWidth;
		int top = y + borderWidth;

		int densityDpi = RESOURCES.getDisplayMetrics().densityDpi;
		int outerWidth = computeWidth(
				bitmap.getScaledWidth(densityDpi),//DisplayMetrics.DENSITY_DEFAULT),
				shadowRadius, shadowDx);// this.getIntrinsicWidth();
		int outerHeight = computeWidth(
				bitmap.getScaledWidth(densityDpi),
				shadowRadius, shadowDy);
		Config config = bitmap.getConfig();
		if (config == null) {
			config = Config.ARGB_8888;
		}

		// 这里的Bitmap没有任何回收策略
		Bitmap output = Bitmap.createBitmap(outerWidth, outerHeight, config);
		Canvas tmpCanvas = new Canvas(output);
		{
			final int color = 0xff424242;
			final Paint paint = new Paint();
			final Rect rect = new Rect(left, top, left + w, top + h);

			paint.setAntiAlias(true);
			tmpCanvas.drawARGB(0, 0, 0, 0);
			paint.setColor(color);
			tmpCanvas.drawRoundRect(new RectF(rect), radius, radius, paint);
			paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
			tmpCanvas.drawBitmap(bitmap, left, top, paint);
		}

		Paint paint2 = null;
		if (shadowRadius != 0 || shadowDx != 0 || shadowDy != 0) {
			paint2 = new Paint();
			paint2.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
		}

		if (borderWidth > 0) {
			final RectF rectF = new RectF(x, y, x + w + borderWidth * 2, y + h
					+ borderWidth * 2);
			paint2.setColor(borderColor);
			tmpCanvas.drawRoundRect(rectF, radius, radius, paint2);
			paint2 = null;
		}
		bitmap.recycle();
		return output;
	}

	int computeWidth(int w, int r, int d) {
		return w + borderWidth * 2 + Math.max(0, r + d) + Math.max(0, r - d);
	}

	int computeOffset(int offset) {
		return Math.max(0, shadowRadius - offset);
	}

}

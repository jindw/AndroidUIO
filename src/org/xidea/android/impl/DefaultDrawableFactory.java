package org.xidea.android.impl;

import org.xidea.android.DrawableFactory;
import org.xidea.android.UIO;
import org.xidea.android.impl.ui.ImageUtil;
import org.xidea.android.impl.ui.MovieDrawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

public class DefaultDrawableFactory implements DrawableFactory{
	public static class SafeBitmapDrawable extends BitmapDrawable {
		private boolean unrelease = true;
		public SafeBitmapDrawable(Bitmap bitmap) {
			super(UIO.getApplication()
					.getResources(), bitmap);
			ImageUtil.retain(bitmap);
		}
		public void draw(Canvas canvas) {
			try {
				super.draw(canvas);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public void finalize() throws Throwable{
			super.finalize();
			release();
		}
		public void release() {
			if(unrelease){
				unrelease = false;
				ImageUtil.release(this.getBitmap());
			}
		}
	}

	@Override
	public Bitmap prepare(Bitmap bitmap) {
		return bitmap;
	}

	@Override
	public Movie prepare(Movie movie) {
		return movie;
	}

	@Override
	public Drawable createDrawable(Bitmap bitmap) {
		return new SafeBitmapDrawable(bitmap);
	}

	@Override
	public Drawable createDrawable(Movie movie) {
		return new MovieDrawable(movie);
	}

	public Drawable getLoadingDrawable(View view) {
		return UIO.getApplication().getResources().getDrawable(android.R.drawable.stat_notify_sync);
	}

	public int getSize(Bitmap bm) {
		if (bm != null) {
			return bm.getRowBytes() * bm.getHeight();
		}
		return 0;
	}

	public int getSize(Movie m) {
		if (m != null) {
			return m.height() * m.width() * 3;
		}
		return 0;
	}


}

package org.xidea.android;

import org.apache.commons.logging.Log;
import org.xidea.android.impl.ui.MovieDrawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public interface DrawableFactory {
	public Bitmap parseResource(Bitmap source);

	public Movie parseResource(Movie source);

	public Drawable getLoadingDrawable();

	public Drawable createDrawable(Bitmap bitmap);

	public Drawable createDrawable(Movie movie);

	public int getSize(Movie movie);

	public int getSize(Bitmap bitmap);

	public class DefaultDrawableFactory implements DrawableFactory {

		@Override
		public Bitmap parseResource(Bitmap bitmap) {
			return bitmap;
		}

		@Override
		public Movie parseResource(Movie movie) {
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

		public Drawable getLoadingDrawable() {
			return null;
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

	static class SafeBitmapDrawable extends BitmapDrawable {
		public static Log log = CommonLog.getLog();


		public SafeBitmapDrawable(Bitmap bitmap) {
			super(UIO.getApplication()
					.getResources(), bitmap);
		}

		public void draw(Canvas canvas) {
			try {
				super.draw(canvas);
			} catch (Exception e) {
			}
		}
	}

}

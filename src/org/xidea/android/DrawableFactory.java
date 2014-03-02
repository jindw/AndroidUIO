package org.xidea.android;



import android.graphics.Bitmap;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;

public interface DrawableFactory {
	public Bitmap prepare(Bitmap source);

	public Movie prepare(Movie source);

	public Drawable getLoadingDrawable(android.view.View view);

	public Drawable createDrawable(Bitmap bitmap);

	public Drawable createDrawable(Movie movie);

	public int getSize(Movie movie);

	public int getSize(Bitmap bitmap);

}

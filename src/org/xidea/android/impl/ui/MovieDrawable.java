package org.xidea.android.impl.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Movie;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

public class MovieDrawable extends Drawable  implements Runnable, Animatable {
	private Movie movie;
	private long begin = SystemClock.uptimeMillis();
	private int duration;
	private boolean running;
	public MovieDrawable(Movie movie){
		this.movie = movie;
		this.duration = movie.duration();
	}

	@Override
	public void draw(Canvas canvas) {
		int ms = (int)(SystemClock.uptimeMillis()-begin);
		if(duration >0){
			ms = ms%duration;
		}
		movie.setTime(ms);
		movie.draw(canvas, 0, 0);
	}
    public void start() {
        if (!isRunning()) {
        	running = true;
            run();
        }
    }

    public void stop() {
        if (isRunning()) {
            unscheduleSelf(this);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void run() {
    	this.invalidateSelf();
        scheduleSelf(this, SystemClock.uptimeMillis() + 100);
    }


	@Override
	public void setAlpha(int alpha) {
		
	}
    public int getIntrinsicWidth() {
        return movie.width();
    }

    public int getIntrinsicHeight() {
        return movie.height();
    }
	public void setColorFilter(ColorFilter cf) {
	}

	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

}

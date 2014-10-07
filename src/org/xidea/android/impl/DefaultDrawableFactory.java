package org.xidea.android.impl;

import org.xidea.android.DrawableFactory;
import org.xidea.android.UIO;
import org.xidea.android.impl.ui.ImageUtil;
import org.xidea.android.impl.ui.MovieDrawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Movie;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;

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
				Bitmap bitmap = this.getBitmap();
				
				if(!bitmap.isRecycled()){
					super.draw(canvas);
				}
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
		if(view instanceof ImageView){
			ImageView image = (ImageView) view;
			return new LoadingDrawable(image);
		}
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
class LoadingDrawable extends Drawable  implements Runnable, Animatable {
	private Drawable over;
	private Drawable base;
	private boolean running;
	int width ;
	int height;
	public LoadingDrawable(ImageView view){
		this.base = view.getDrawable();
		
		while(base instanceof LoadingDrawable){
			base = ((LoadingDrawable)base).base;
		}
		this.over = UIO.getApplication().getResources().getDrawable(android.R.drawable.stat_notify_sync);
		this.width = Math.max(view.getWidth(),over.getMinimumWidth());
		this.height = Math.max(view.getHeight(),over.getMinimumHeight());

		if(base != null){
			width = Math.max(width,base.getMinimumWidth());
			height = Math.max(width,base.getMinimumHeight());
		}
	}

	@Override
	public void draw(Canvas canvas) {
		if(base!=null){
			base.draw(canvas);
		}
		float degrees = ((float)System.currentTimeMillis())/1000;
		float px = 0;
		float py = 0;

        int saveCount = canvas.save();
		//canvas.rotate(degrees,px,py);
		over.draw(canvas);
		canvas.restoreToCount(saveCount);
	}
	
	@Override
	public int getIntrinsicWidth() {
		return width;
	}

	@Override
	public int getIntrinsicHeight() {
		return height;
	}

	@Override
	public int getMinimumWidth() {
		return width;
	}

	@Override
	public int getMinimumHeight() {
		return height;
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
		if(base!=null)base.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		if(base!=null)base.setColorFilter(cf);
		
	}

	@Override
	public int getOpacity() {
		if(base!=null)return base.getOpacity();
		return 0;
	}
	
}

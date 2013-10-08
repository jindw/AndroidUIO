package org.xidea.android;

import org.apache.commons.logging.Log;
import org.xidea.android.impl.ApplicationState;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.SparseIntArray;

public interface DrawableFactory<RawType> {
	public static Log log = CommonLog.getLog();
	public Object parseResource(RawType id);
	public Drawable createDrawable(Object bitmap);
	public int getSize(Object bitmap);
	public class DefaultDrawableFactory implements DrawableFactory<Bitmap>{

		@Override
		public Object parseResource(Bitmap bitmap) {
			return bitmap;
		}

		@Override
		public Drawable createDrawable(Object bitmap) {
			return new SafeBitmapDrawable((Bitmap)bitmap);
		}

		public int getSize(Object cache) {
			if (cache instanceof Bitmap) {
				Bitmap bm = (Bitmap) cache;
				if (bm != null) {
					return bm.getRowBytes() * bm.getHeight();
				}
			}
			return 0;
		}
		
	}
	static class SafeBitmapDrawable extends BitmapDrawable {

		private static SparseIntArray bitmapRefMap = new SparseIntArray();
//		private static final Resources RESOURCES = ABFacade.getApplication().getResources();

		static void retain(Bitmap bitmap) {
			// hash 冲突无碍
			int group = bitmap.hashCode();
			synchronized (bitmapRefMap) {
				int count = bitmapRefMap.get(group);
				bitmapRefMap.put(group, count + 1);
			}
			log.info("bitmap size:"+bitmapRefMap.size());
		}

		static void release(Bitmap bitmap) {
			// hash 冲突无碍
			int group = bitmap.hashCode();
			synchronized (bitmapRefMap) {
				int index = bitmapRefMap.indexOfKey(group);
				if (index >= 0) {
					int count = bitmapRefMap.valueAt(index);
					if (count > 1) {
						bitmapRefMap.put(group, count - 1);
						return;
					}
					bitmapRefMap.removeAt(index);
				}
			}
			if (!bitmap.isRecycled()) {
				bitmap.recycle();
			}
		}
		static class BitmapRecycle {
			private Bitmap bitmap;

			BitmapRecycle(Bitmap bitmap) {
				this.bitmap = bitmap;
				retain(bitmap);
			}

			public void finalize() throws Throwable {
				try{
					super.finalize();
				}finally{
					release(bitmap);
				}
			}
		}

		final BitmapRecycle recycled;

		public SafeBitmapDrawable(Bitmap bitmap) {
			super(ApplicationState.getInstance().getApplication().getResources(), bitmap);
			this.recycled = Build.VERSION.SDK_INT <= 10 && bitmap != null ? new BitmapRecycle(
					bitmap) : null;
		}

		public void draw(Canvas canvas) {
			try {
				super.draw(canvas);
			} catch (Exception e) {
			}
		}
	}

}

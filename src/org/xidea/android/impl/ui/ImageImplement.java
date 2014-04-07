package org.xidea.android.impl.ui;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.xidea.android.Callback;
import org.xidea.android.Callback.PrepareCallback;
import org.xidea.android.DrawableFactory;
import org.xidea.android.UIO;
import org.xidea.android.impl.CommonLog;
import org.xidea.android.impl.io.HttpImplementation;
import org.xidea.android.impl.io.HttpUtil;
import org.xidea.android.impl.io.HttpInterface.AsynTask;
import org.xidea.android.impl.io.HttpInterface.CachePolicy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Movie;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ImageView;

public class ImageImplement {
	private static Log log = CommonLog.getLog();
	private LinkedHashMap<String, CacheInfo> queue = new LinkedHashMap<String, CacheInfo>(
			0, 0.75f, true);
	private Object queueLock = new Object();
	private int coreCapacity = 2 * 1024 * 1024;// 2M
	private int maxCapacity = 4 * 1024 * 1024;// 4M
	private int currentSize = 0;
	private static DrawableFactory DEFAULT_FACTORY = new DrawableFactory.DefaultDrawableFactory();

	private final static Object IMAGE_TAG_KEY = new Object();
	public static final ImageImplement INSTANCE = new ImageImplement();

	private ImageImplement() {
	}

	public void bind(final ImageView view, final String url,
			DrawableFactory factory, final int fallbackResource,
			final Callback<Drawable> callback) {
		log.info("bind:" + url + view);
		if (view == null || url == null || url.length() == 0) {
			log.fatal("invalid view(" + view + ") or url(" + url + ")");
			return;
		}
		if (factory == null) {
			factory = DEFAULT_FACTORY;
		}
		updateImageDrawable(view,factory.getLoadingDrawable());
		final Handler handler = HttpUtil.currentHandler();
		if (handler == null) {
			log.warn("图片绑定必须在ui线程执行！");
			final DrawableFactory factory2 = factory;
			view.post(new Runnable() {
				@Override
				public void run() {
					bind(view, url, factory2, fallbackResource, callback);
				}
			});
		} else {
			new ImageLoaderCallback(view, url, factory, callback,
					fallbackResource).doLoad();
		}
	}

	public void clear(Context activity) {
		if (currentSize > coreCapacity) {
			synchronized (queueLock) {
				Iterator<Entry<String, CacheInfo>> it = queue.entrySet()
						.iterator();
				while (it.hasNext()) {
					Entry<String, CacheInfo> entry = it.next();
					boolean remove = false;
					CacheInfo info = entry.getValue();
					if (info.maybeContains(activity)) {// 需要清理的Activity，只要大于核心容量，
						if (currentSize > coreCapacity) {
							remove = true;
						}
					} else {// 对于其他没有指定需要清理的Activity，如果大于最大容量， 也需要删除。
						if (currentSize > maxCapacity) {
							remove = true;
						}
					}
					if (remove) {
						info.release();
						log.warn("remove cache: currentSize:" + currentSize);
						it.remove();
						currentSize -= info.size;
						if (currentSize <= coreCapacity) {
							break;
						}
					}
				}
			}
		}
	}

	public void clearAll() {
		log.warn("clear all");
		synchronized (queueLock) {
			for(CacheInfo inf : queue.values()){
				inf.release();
			}
			queue.clear();
			currentSize = 0;
		}
	}

	public void removeCache(String path) {
		HttpImplementation.getInstance().removeCache(path);
		synchronized (queueLock) {
			CacheInfo info = queue.remove(path);
			if (info != null) {
				currentSize -= info.size;
			}
		}
	}

	private void updateImageDrawable(ImageView view,Drawable drawable) {
		Drawable old = view.getDrawable();
		if(old instanceof DrawableFactory.SafeBitmapDrawable){
			ImageUtil.release(((BitmapDrawable)old).getBitmap());
		}
		
		view.setImageDrawable(drawable);
	}

	class ImageLoaderCallback implements PrepareCallback<AsynTask, Bitmap> {
		ImageView view;
		String url;
		DrawableFactory factory;
		Callback<Drawable> callback;
		int fallbackResourceId;

		boolean callbacked;

		ImageLoaderCallback(ImageView view, String url,
				DrawableFactory factory, Callback<Drawable> callback, int resId) {
			this.view = view;
			this.factory = factory == null ? DEFAULT_FACTORY : factory;
			this.url = url;
			this.callback = callback;
			this.fallbackResourceId = resId;
		}

		private boolean cacheFailed() {
			CacheInfo info;
			synchronized (queueLock) {
				info = queue.get(url);
			}
			if (info != null && info.cache != null
					&& factory.equals(info.factory)) {
				info.attach(view);
				this.callback((Bitmap) info.cache);
				log.info("use old image!" + url + "\nqueue info:count:"
						+ queue.size() + ";memery:" + currentSize / 1024f
						/ 1024f + "M\n");
				return false;
			}
			return true;
		}

		void doLoad() {
			UIFacade.getInstance().setTag(view, IMAGE_TAG_KEY, url);
			if (cacheFailed()) {
				updateImageDrawable(view,factory.getLoadingDrawable());
				UIO.get(this, url);
			}
		}

		@Override
		public Object prepare(AsynTask task) {
			Object data = null;
			Object result = null;
			boolean cacheFailed = cacheFailed();
			try {
				if (cacheFailed) {
					data = ImageUtil.createMediaContent(url, -1, -1, 
							CachePolicy.CacheOnly, task, null);
					if (cacheFailed = cacheFailed()) {
						//只要prepare了，bitmap的控制权就自动交给了系统。
						Object cacheResult = doPrepare(data);
						// 非标准cache callback 做法
						if (cacheResult != null) {
							task.execute(cacheResult);
						}
					} else if (data instanceof Bitmap) {
						((Bitmap)data).recycle();//无主的bitmap一定要立即回收
					}
				}
			} catch (Throwable e) {
				task.error(e);
			} finally {
				if (cacheFailed) {
					// 须兼容 度缓存的时候缓存没有完成初始化，而读网络的时候发现网络数据未更新的情况
					// 为了确保，
					// 网络更新能够跟心已经缓存的bitmap，这里不能再直接利用cache中的bitmap了，必须通过prepare去更新缓存。

					CachePolicy policy = data == null ? CachePolicy.Any
							: CachePolicy.NetworkOnly;
					data = ImageUtil.createMediaContent(url, -1, -1, policy, task,
							null);
					result =  doPrepare(data);
					log.info("load new image!" + url + "\nqueue info:count:"
							+ queue.size() + ";memery:" + currentSize / 1024f
							/ 1024f + "M\n");
				}
			}
			return result;
		}
		public Object doPrepare(Object rawData){
			if(rawData instanceof Bitmap){
				return prepare((Bitmap )rawData);
			}else if(rawData instanceof Movie){
				return prepare((Movie)rawData);
			}
			return null;
		}

		public Object prepare(Bitmap rawData) {
			final Bitmap cache = factory.parseResource(rawData);
			if (cache != null) {
				synchronized (queueLock) {
					CacheInfo info = new CacheInfo(factory, cache, view);
					queue.put(url, info);
					int size = info.size;
					currentSize += size;
				}
				if (currentSize > maxCapacity) {
					log.warn("memmery clear on new image");
					clear(null);
				}
			}
			return cache;
		}
		public Object prepare(Movie rawData) {
			final Movie cache = factory.parseResource(rawData);
			if (cache != null) {
				synchronized (queueLock) {
					CacheInfo info = new CacheInfo(factory, cache, view);
					queue.put(url, info);
					int size = info.size;
					currentSize += size;
				}
				if (currentSize > maxCapacity) {
					log.warn("memmery clear on new image");
					clear(null);
				}
			}
			return cache;
		}

		public void callback(Bitmap bitmap) {
			boolean valid = url.equals(UIFacade.getInstance().getTag(view,
					IMAGE_TAG_KEY));// &&
			Drawable drawable = null; // view.isShown();
			if (valid) {
				if (bitmap == null) {
					useFallbackResource();
				} else {
					drawable = factory.createDrawable(bitmap);
					updateImageDrawable(view,drawable);
				}
			}
			if (callback != null) {
				callback.callback(drawable);
			}
			callbacked = true;
		}

		private void useFallbackResource() {
			if (!callbacked && fallbackResourceId >= 0) {
				updateImageDrawable(view, null);
				view.setImageResource(fallbackResourceId);
			}
		}

		public void error(Throwable ex, boolean callbackError) {
			if (CommonLog.isDebug()) {
				UIFacade.getInstance().shortTips("图片装载失败：" + ex);
			}
			useFallbackResource();
			if (callback != null) {
				callback.error(ex, callbackError);
			} else {
				ex.printStackTrace();
			}
		}

	}

	private static class CacheInfo {
		final DrawableFactory factory;
		final int size;
		final Object cache;
		int activityMask;

		CacheInfo(DrawableFactory factory, Object bitmap, ImageView view) {
			this.factory = factory;
			this.cache = bitmap;
			if (bitmap instanceof Bitmap) {
				this.size = factory.getSize((Bitmap) bitmap);

				if(ImageUtil.GC_IGNORED_BITMAP){
					ImageUtil.retain((Bitmap)bitmap);
				}
			} else {
				this.size = factory.getSize((Movie) bitmap);
			}
			this.activityMask = Math.abs(System.identityHashCode(view
					.getContext()));
		}

		public void release() {
			if(ImageUtil.GC_IGNORED_BITMAP && cache instanceof Bitmap){
			//  不能莽撞清理
			//	ImageUtil.release((Bitmap)cache);
			}
		}

		boolean maybeContains(Context activity) {
			int hash = Math.abs(System.identityHashCode(activity));
			return hash == (hash & activityMask);
		}

		void attach(ImageView view) {
			int hash = Math.abs(System.identityHashCode(view.getContext()));
			this.activityMask |= hash;

		}
	}

}

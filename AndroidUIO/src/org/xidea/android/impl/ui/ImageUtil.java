//package org.xidea.android.impl.ui;
//
//import java.io.BufferedInputStream;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.Iterator;
//import java.util.LinkedHashMap;
//import java.util.Map.Entry;
//import java.util.regex.Pattern;
//
//import org.apache.commons.logging.Log;
//
//import android.app.Application;
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.Bitmap.Config;
//import android.graphics.BitmapFactory;
//import android.graphics.Movie;
//import android.graphics.drawable.Drawable;
//import android.os.Handler;
//import android.util.DisplayMetrics;
//import android.widget.ImageView;
//
//import org.xidea.android.Callback;
//import org.xidea.android.CommonLog;
//import org.xidea.android.DrawableFactory;
//import org.xidea.android.impl.io.HttpInterface;
//import org.xidea.android.impl.io.HttpUtil;
//
//
//public class ImageUtil {
//	private static final int IMAGE_HEADER_BUFFER = 1024 * 16;
//	private static Log log = CommonLog.getLog();
//	private static final Config COLOR_TYPE = Bitmap.Config.ARGB_8888;
//	private static Pattern URL_PATTERN = Pattern.compile(
//			"^(?:https?|ftp|file)\\:\\/", Pattern.CASE_INSENSITIVE);
//
//	private HttpInterface asyn;
//	private LinkedHashMap<String, CacheInfo> queue = new LinkedHashMap<String, CacheInfo>(
//			0, 0.75f, true);
//	private Object queueLock = new Object();
//	private int coreCapacity = 2 * 1024 * 1024;// 2M
//	private int maxCapacity = 4 * 1024 * 1024;// 4M
//	private int currentSize = 0;
//	private int maxSide = 1024;
//	private static DrawableFactory<Bitmap> DEFAULT_FACTORY = new DrawableFactory.DefaultDrawableFactory();
//
//	private ImageUtil() {
//	}
//
//	private static ImageUtil instance;
//
//	public static ImageUtil getInstance() {
//		if (instance == null) {
//			instance = new ImageUtil();
//		}
//		return instance;
//	}
//
//	private final static Object IMAGE_TAG_KEY = new Object();
//
//	public void bind(final ImageView view, final String url,
//			@SuppressWarnings("rawtypes") final DrawableFactory factory,
//			final Callback<Drawable> callback, final int loadingDrawable,
//			final int resId) {
//		log.info("bind:"+url+view);
//		if (view == null || url == null || url.length() == 0) {
//			log.fatal("invalid view(" + view + ") or url(" + url + ")");
//			return;
//		}
//		if (loadingDrawable > 0) {
//			view.setImageResource(loadingDrawable);
//		}
//		final Handler handler = HttpUtil.currentHandler();
//		if (handler == null) {
//			log.warn("图片绑定必须在ui线程执行！");
//			view.post(new Runnable() {
//				@Override
//				public void run() {
//					bind(view, url, factory, callback, loadingDrawable,
//							resId);
//				}
//			});
//		} else {
//			if (url.endsWith(".gif")) {
//				MovieLoaderCallback callbackWrapper = new MovieLoaderCallback(
//						view, url, callback, resId);
//				callbackWrapper.doLoad(asyn);
//			} else {
//				@SuppressWarnings("unchecked")
//				ImageLoaderCallback callbackWrapper = new ImageLoaderCallback(
//						view, url, factory, callback, resId);
//				callbackWrapper.doLoad(asyn);
//			}
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	public void bind(final ImageView view, final String url,
//			@SuppressWarnings("rawtypes") final DrawableFactory factory,
//			final Callback<Drawable> callback, final Drawable loadingDrawable,
//			final int resId) {
//		log.info("bind:"+url+view);
//		if (view == null || url == null || url.length() == 0) {
//			log.fatal("invalid view(" + view + ") or url(" + url + ")");
//			return;
//		}
//
//		if (loadingDrawable != null) {
//			view.setImageDrawable(loadingDrawable);
//		}
//		final Handler handler = HttpUtil.currentHandler();
//		if (handler == null) {
//			log.warn("图片绑定必须在ui线程执行！");
//			view.post(new Runnable() {
//				@Override
//				public void run() {
//					bind(view, url, factory, callback, loadingDrawable,
//							resId);
//				}
//			});
//		} else {
//			ImageLoaderCallback callbackWrapper = new ImageLoaderCallback(view,
//					url, factory, callback, resId);
//			callbackWrapper.doLoad(asyn);
//		}
//	}
//
//	public void clear(Context activity) {
//		if (currentSize > coreCapacity) {
//			synchronized (queueLock) {
//				Iterator<Entry<String, CacheInfo>> it = queue
//						.entrySet().iterator();
//				while (it.hasNext()) {
//					Entry<String, CacheInfo> entry = it.next();
//					boolean remove = false;
//					CacheInfo info = entry.getValue();
//					if (info == null) {// context 不存在了， 需要立即删除
//						remove = true;
//					} else if (info.maybeContains(activity)) {// 需要清理的Activity，只要大于核心容量，
//						if (currentSize > coreCapacity) {
//							remove = true;
//						}
//					} else {// 对于其他没有指定需要清理的Activity，如果大于最大容量， 也需要删除。
//						if (currentSize > maxCapacity) {
//							remove = true;
//						}
//					}
//					if (remove) {
//						log.warn("remove cache: currentSize:" + currentSize);
//						it.remove();
//						currentSize -= info.size;
//						if (currentSize <= coreCapacity) {
//							break;
//						}
//					}
//				}
//			}
//		}
//	}
//
//	public void clearAll() {
//		log.warn("clear all");
//		synchronized (queueLock) {
//			queue.clear();
//			currentSize = 0;
//		}
//	}
//
//	public void removeCache(String path) {
//		asyn.removeCache(path);
//		synchronized (queueLock) {
//			CacheInfo info = queue.remove(path);
//			if (info != null) {
//				currentSize -= info.size;
//			}
//		}
//	}
//
//	public Bitmap createBitmap(String path) {
//		return createBitmap(path, -1, -1);
//	}
//
//	public Bitmap createBitmap(String path, int maxWidth, int maxHeight) {
//		return requireBitmap(path, null, maxWidth, maxHeight, false, false);
//	}
//
//	public Bitmap createBitmap(String key, InputStream in, int maxWidth,
//			int maxHeight) {
//		return requireBitmap(key, in, -1, -1, false, false);
//	}
//
//	private InputStream requareStream(Object source, boolean loadFromeCache,
//			boolean ignoreCachedContent) throws IOException {
//		if (source instanceof String) {
//			String path = (String) source;
//			if (URL_PATTERN.matcher(path).find()) {
//				source = asyn.getStream(asyn.parseURL(path), Method.GET, null,
//						null, loadFromeCache, ignoreCachedContent);
//			} else {
//				source = new FileInputStream(path);
//			}
//		}
//		return (InputStream) source;
//	}
//
//	Bitmap requireBitmap(String key, Object resource, int maxWidth,
//			int maxHeight, boolean loadFromeCache, boolean networkOnly) {
//		Bitmap bm = null;
//		if (resource == null) {
//			resource = key;
//		}
//		try {
//			BitmapFactory.Options options = null;
//			InputStream in = null;
//			try {
//				in = requareStream(resource, loadFromeCache, networkOnly);
//				if (in == null) {
//					return null;
//				}
//				if (resource instanceof InputStream) {
//					resource = key;
//				}
//				BufferedInputStream buf = new BufferedInputStream(in,
//						IMAGE_HEADER_BUFFER);
//				buf.mark(1024);
//				options = requireBoundAndInitOptions(buf, maxWidth, maxHeight);
//				if (options.outWidth <= 0) {
//					removeCache(key);
//					return null;
//				}
//
//				try {
//					buf.reset();
//					in = buf;
//				} catch (IOException e) {
//					log.error("read over:" + e);
//					sc(in);
//					in = requareStream(resource, loadFromeCache, networkOnly);
//				}
//				// log.info("info:" + key + in.read());
//
//				// long t1 = System.nanoTime();
//				bm = BitmapFactory.decodeStream(in, null, options);
//				// log.info("decode image btime:"
//				// + (System.nanoTime() - t1));
//			} catch (OutOfMemoryError e) {
//				log.warn(e);
//				options.inSampleSize++;
//				sc(in);
//				in = requareStream(resource, true, false);
//				bm = BitmapFactory.decodeStream(in, null, options);
//			} finally {
//				sc(in);
//			}
//		} catch (Exception e) {
//			log.warn(e);
//			return null;
//		}
//		if (bm == null) {
//			removeCache(key);
//		}
//		return bm;
//	}
//
//	private static void sc(InputStream in) {
//		try {
//			in.read();
//			in.close();
//		} catch (Exception e2) {
//		}
//	}
//
//	private BitmapFactory.Options requireBoundAndInitOptions(InputStream in,
//			int maxWidth, int maxHeight) {
//		if (maxWidth <= 0) {
//			maxWidth = maxSide;
//		}
//		if (maxHeight <= 0) {
//			maxHeight = maxSide;
//		}
//		BitmapFactory.Options options = new BitmapFactory.Options();
//		options.inJustDecodeBounds = true;
//		// long t1 = System.nanoTime();
//		BitmapFactory.decodeStream(in, null, options);
//		// log.info("parse bound time:" + (System.nanoTime() - t1));
//		final int width = options.outWidth;
//		final int height = options.outHeight;
//		if (CommonLog.isDebug() && (width > 200 || height > 200)) {
//			CommonUtil.longTips("正在解码大图片(" + width + ',' + height
//					+ ")，请QA确认此处是否需要换成缩略图！");
//		}
//		final float scale;
//		if (width > maxWidth || height > maxHeight) { // 防止内存溢出
//			scale = Math.max((float) width / maxWidth, (float) height
//					/ maxHeight);
//		} else {
//			scale = 1;
//		}
//		options.inJustDecodeBounds = false;
//		options.inSampleSize = (int) Math.ceil(scale);
//		options.inPreferredConfig = COLOR_TYPE;
//		options.inPurgeable = true;
//		options.inInputShareable = true;
//		return options;
//	}
//
//
//	class ImageLoaderCallback implements BackgroundCallback<Bitmap, Bitmap>,
//			CancelCallback, CacheCallback<Bitmap, Bitmap> {
//		ImageView view;
//		String url;
//		DrawableFactory<Bitmap> factory;
//		Callback<Drawable> callback;
//		int resId;
//
//		boolean canceled;
//		private boolean callbacked;
//
//		ImageLoaderCallback(ImageView view, String url,
//				DrawableFactory<Bitmap> factory, Callback<Drawable> callback,
//				int resId) {
//			this.view = view;
//			this.factory = factory == null ? DEFAULT_FACTORY : factory;
//			this.url = url;
//			this.callback = callback;
//			this.resId = resId;
//		}
//
//		public void callback(Bitmap bitmap) {
//			boolean valid = url.equals(CommonUtil.getTag(view, IMAGE_TAG_KEY));// &&
//			Drawable drawable = null; // view.isShown();
//			if (valid) {
//				if (bitmap == null) {
//					if (resId >= 0) {
//						view.setImageResource(resId);
//					}
//				} else {
//					drawable = factory.createDrawable(bitmap);
//					view.setImageDrawable(drawable);
//				}
//			}
//			if (callback != null) {
//				callback.callback(drawable);
//			}
//			callbacked = true;
//		}
//
//		@Override
//		public void updateCallback(Bitmap t) {
//			if (CommonUtil.isDebug()) {
//				if (callbacked) {
//					log.warn("image updated:" + url);
//					CommonUtil.longTips("图片有更新：" + url);
//				} else {
//					log.info("new image:" + url);
//					CommonUtil.longTips("装载新图片：" + url);
//				}
//			}
//			callback(t);
//		}
//
//		@Override
//		public boolean requireCacheCallback() {
//			return true;
//		}
//
//		@Override
//		public boolean ignoreCachedContent() {
//			return false;
//		}
//
//		public void error(Throwable ex, boolean callbackError) {
//			if (CommonUtil.isDebug()) {
//				CommonUtil.longTips("图片装载失败：" + ex);
//			}
//			if (callback instanceof ErrorCallback) {
//				((ErrorCallback) callback).error(ex, callbackError);
//			} else {
//				view.setImageResource(resId);
//			}
//		}
//
//		void doLoad(HttpAsyn async) {
//			CommonUtil.setTag(view, IMAGE_TAG_KEY, url);
//			CacheInfo info;
//			synchronized (queueLock) {
//				info = queue.get(url);
//			}
//			if (info != null && info.cache != null
//					&& factory.equals(info.factory)) {
//				info.attach(view);
//				this.callback((Bitmap)info.cache);
//				// log.warn("use old image!"+url +
//				// "#"+cache.size()+"/"+queue.size()+","+currentSize/1024f/1024f);
//			} else {
//				synchronized (queueLock) {
//					log.info("load new image!" + (info == null) + url + "#"
//							+ queue.size() + "/" + queue.size() + ","
//							+ currentSize / 1024f / 1024f + "%"
//							+ System.identityHashCode(queue));
//				}
//				async.get(this, url);
//			}
//		}
//
//		@Override
//		public Object prepare(Bitmap rawData) {
//			final Object cache = factory.parseResource(rawData);
//
//			if (cache instanceof Bitmap) {
//				synchronized (queueLock) {
//					CacheInfo info = new CacheInfo(factory,
//							cache, view);
//					queue.put(url, info);
//					int size = info.size;
//					currentSize += size;
//				}
//				if (currentSize > maxCapacity) {
//					log.warn("memmery clear on new image");
//					clear(null);
//				}
//			}
//			return cache;
//		}
//
//		@Override
//		public boolean isCanceled() {
//			return canceled;
//		}
//
//		@Override
//		public void setCacheEntry(String key, Bitmap data) {
//			
//		}
//
//	}
//
//	public void init(Application application) {
//		DisplayMetrics dm = application.getResources().getDisplayMetrics();
//		this.maxSide = Math.max(dm.widthPixels, dm.heightPixels);
//		// log.info("ms:"+ this.maxSide);
//	}
//
//	private static class CacheInfo {
//		final DrawableFactory<?> factory;
//		final int size;
//		final Object cache;
//		int activityMask;
//
//		CacheInfo(DrawableFactory<?> factory, Object drawable, ImageView view) {
//			this.factory = factory;
//			this.cache = drawable;
//			this.size = factory.getSize(drawable);
//			this.activityMask = Math.abs(System.identityHashCode(view
//					.getContext()));
//			;
//		}
//
//		boolean maybeContains(Context activity) {
//			int hash = Math.abs(System.identityHashCode(activity));
//			return hash == (hash & activityMask);
//		}
//
//		void attach(ImageView view) {
//			int hash = Math.abs(System.identityHashCode(view.getContext()));
//			this.activityMask |= hash;
//
//		}
//	}
//
//
//}

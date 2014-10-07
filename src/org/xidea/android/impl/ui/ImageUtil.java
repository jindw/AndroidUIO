package org.xidea.android.impl.ui;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Movie;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import org.xidea.android.Callback.Cancelable;
import org.xidea.android.UIO;
import org.xidea.android.impl.DebugLog;
import org.xidea.android.impl.Network.CachePolicy;
import org.xidea.android.impl.Network.HttpMethod;
import org.xidea.android.impl.http.HttpSupport;
import org.xidea.android.impl.http.HttpUtil;
import org.xidea.android.impl.io.IOUtil;

public final class ImageUtil {
	private static final int IMAGE_HEADER_BUFFER = 1024 * 16;
	public static final boolean GC_IGNORED_BITMAP = Build.VERSION.SDK_INT <= 10;
	private static SparseIntArray bitmapRefMap = new SparseIntArray();
	private static final Config COLOR_TYPE = Bitmap.Config.ARGB_8888;
	private static Pattern URL_PATTERN = Pattern.compile(
			"^(?:https?|ftp|file)\\:\\/", Pattern.CASE_INSENSITIVE);
	private static int maxSide;

	private ImageUtil() {
	}

	/**
	 * return Bitmap or Movie
	 * 
	 * @param path
	 * @param maxWidth
	 * @param maxHeight
	 * @param policy
	 * @param options
	 * @return
	 */
	public static Object createMediaContent(InputStream in, View view,
			Cancelable cancelable, Options options) {
		LayoutParams lp = view.getLayoutParams();
		int width = 0, height = 0;
		if (view != null) {
			if (lp.height != LayoutParams.WRAP_CONTENT) {
				height = view.getHeight();
			}
			if (lp.width != LayoutParams.WRAP_CONTENT) {
				width = view.getWidth();
			}
		}

		return loadMedia(in, width, height, cancelable, options, false);
	}

	/**
	 * @param resource
	 *            String path, InputStream
	 * @param maxWidth
	 * @param maxHeight
	 * @param policy
	 * @return
	 */
	private static Object loadMedia(InputStream in, int maxWidth, int maxHeight,
			Cancelable cancelable, Options options, boolean ignoreMovie) {
		Object bm = null;
		try {
			if (options == null) {
				options = new Options();
				options.inPreferredConfig = COLOR_TYPE;
			}
			in = new BufferedInputStream(in,IMAGE_HEADER_BUFFER);
			in.mark(IMAGE_HEADER_BUFFER);
			try {
				boolean isMovie = checkAndInitOptions(in, maxWidth, maxHeight,
						options, ignoreMovie);
				if (options.outWidth <= 0) {
					return null;
				}
				in.reset();
				bm = decodeStream(in, options, isMovie);
			} finally {
				in.close();
			}
		} catch (Exception e) {
			DebugLog.warn(e);
			return null;
		}
		return bm;
	}

	private static final Object decodeStream(InputStream in, Options opts,
			boolean isMovie) {
		if (isMovie) {
			return Movie.decodeStream((InputStream) in);
		}
		return BitmapFactory.decodeStream((InputStream) in, null, opts);
	}

	private static int getMaxSide() {
		if (maxSide <= 0) {
			DisplayMetrics dm = UIO.getApplication().getResources()
					.getDisplayMetrics();
			maxSide = Math.max(dm.widthPixels, dm.heightPixels);
		}
		return maxSide;
	}

	private static boolean checkAndInitOptions(InputStream in, int maxWidth,
			int maxHeight, Options options, boolean ignoreMovie)
			throws IOException {
		options.inJustDecodeBounds = true;
		try {
			if (!ignoreMovie) {
				GifDecoder gd = new GifDecoder(in);
				int width = gd.getWidth();
				if (width > 0) {// match
					if (options.inSampleSize <= 0) {
						options.inSampleSize = computeSampleSize(maxWidth,
								maxHeight, width, gd.getHeight());
						options.outHeight = gd.getHeight();
						options.outWidth = width;
					}
					return gd.isAnimate();
				} else {
					in.reset();// GIF 解析失败，尝试用android 系统自己的图像解析处理。
				}
			}
			// long t1 = System.nanoTime();
			if (options.inSampleSize <= 0) {
				decodeStream(in, options, false);
				// DebugLog.info("parse bound time:" + (System.nanoTime() -
				// t1));
				options.inSampleSize = computeSampleSize(maxWidth, maxHeight,
						options.outWidth, options.outHeight);
			}

			return false;
		} finally {

			options.inJustDecodeBounds = false;
			// 是否允许回收数据
			// options.inInputShareable = in instanceof byte[];
			options.inPurgeable = true;
		}

	}

	private static int computeSampleSize(int maxWidth, int maxHeight,
			final int width, final int height) {
		if (maxWidth <= 0) {
			maxWidth = getMaxSide();
		}
		if (maxHeight <= 0) {
			maxHeight = getMaxSide();
		}
		if (DebugLog.isDebug() && (width > 200 || height > 200)) {
			UIFacade.getInstance().shortTips(
					"正在解码大图片(" + width + ',' + height + ")，请QA确认此处是否需要换成缩略图！");
		}
		if (width > maxWidth || height > maxHeight) { // 防止内存溢出
			final float scale = Math.max((float) width / maxWidth, (float) height
					/ maxHeight);
			int intScale = (int) Math.ceil(scale);
			DebugLog.info("decoder scale："+scale+"=>"+intScale);
			return intScale;
		} else {
			return 1;
		}
	}

	/**
	 * @hide
	 * @param bitmap
	 */
	public static void retain(Bitmap bitmap) {
		// hash 冲突无碍
		int group = bitmap.hashCode();
		synchronized (bitmapRefMap) {
			int count = bitmapRefMap.get(group);
			bitmapRefMap.put(group, count + 1);
		}
		//DebugLog.info("bitmap size:" + bitmapRefMap.size());
	}

	/**
	 * @hide
	 * @param bitmap
	 */
	public static void release(Bitmap bitmap) {
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
			//bitmap.recycle();
		}
	}
}

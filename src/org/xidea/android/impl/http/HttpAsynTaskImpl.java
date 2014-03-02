package org.xidea.android.impl.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;

import org.xidea.android.Callback;
import org.xidea.android.Callback.CacheCallback;
import org.xidea.android.Callback.PrepareCallback;
import org.xidea.android.impl.AsynTask;
import org.xidea.android.impl.DebugLog;
import org.xidea.android.impl.Network.CachePolicy;
import org.xidea.android.impl.Network.HttpMethod;
import org.xidea.android.impl.io.IOUtil;
import org.xidea.android.impl.ui.ImageUtil;

import android.graphics.Bitmap;
import android.graphics.Movie;
import android.os.Handler;




class HttpAsynTaskImpl implements AsynTask {
	private static final int MAX_TASK_TIMEOUT = 1 * 60 * 1000;
	@SuppressWarnings("rawtypes")
	private final Callback callback;
	private final long createTime = System.currentTimeMillis();
	private long startedTime;
	private final String path;
	private final URL url;
	private final HttpMethod method;
	private final HttpSupport http;
	private final Handler from = HttpUtil.currentHandler();
	private final Type[] types;// [prepareType?,callbackType]

	private Thread thread;// for interrupt
	private boolean canceled;
	private Map<String, Object> postParams;

	public HttpAsynTaskImpl(HttpSupport http, String path, HttpMethod method,
			Callback<?> callback,Map<String,Object> postParams) {
		this.http = http;
		this.path = path;
		this.url = HttpUtil.parseURL(path);
		this.method = method;
		this.callback = callback;
		this.types = HttpUtil.getResultType(callback);
		this.postParams = postParams;
	}

	@Override
	public void onStart() {
		startedTime = System.currentTimeMillis();
		thread = Thread.currentThread();
		http.getStatistics().onHttpWaitDuration(url,
				System.currentTimeMillis() - getCreateTime());
	}

	@Override
	public Object load(CachePolicy cp) {
		Type rawType = types[0];
		Object result = null;
		try {
			if (rawType == AsynTask.class) {
				result = this;
			} else if (rawType == URL.class) {
				result = url;
			} else if (rawType == File.class) {
				result = loadFile(cp);
			} else if (rawType == Bitmap.class) {
				File file = loadFile(cp);
				result = ImageUtil.createMediaContent(new FileInputStream(file), null, this, null);
			} else if (rawType == InputStream.class) {
				result = loadStream(cp);
			} else if (rawType == byte[].class) {
				result = IOUtil.loadBytesAndClose(loadStream(cp));
			} else if (rawType == Movie.class) {
				byte[] data = IOUtil.loadBytesAndClose(loadStream(cp));
				result = Movie.decodeByteArray(data, 0, data.length);
			} else {
				String text = http.getString(url, this.method, this.getPost(),
						this, cp);
				if (text != null) {
					DebugLog.info("http返回：" + url + "\n" + text);
					result = HttpUtil.transform(text, rawType);
				} else {
					DebugLog.info("Http请求无数据返回");
				}
			}

			return result;
		} catch (Exception e) {
			this.error(e, false);
			return null;
		}
	}

	private InputStream loadStream(CachePolicy cp) throws IOException {
		return http.getStream(url, method, this.getPost(), this, cp);
	}

	private File loadFile(CachePolicy cp) throws IOException {
		return http.getFile(url, method, this.getPost(), this, cp);
	}

	@Override
	public boolean onCache(Object result){
		return doExecute(result, true);
	}
	@Override
	public void onCallback(Object result){
		doExecute(result, false);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean doExecute(Object result, final boolean cacheCheck) {
		if (callback instanceof PrepareCallback) {
			result = ((PrepareCallback) callback).prepare(result);
			if (result != null) {// 只有prepare
				// 结果有可能变化，因为在loadRaw中已经正确转型了。
				result = HttpUtil.transform(result, types[1]);
			}
		}
		final Object result2 = result;
		final Boolean[] lock = new Boolean[1];
		postBack(new Runnable() {
			@Override
			public void run() {
				boolean rtv = false;
				try {
					if (cacheCheck) {
						rtv = ((CacheCallback) callback).cache(result2);
					} else {
						callback.callback(result2);
					}
				} catch (Throwable th) {
					error(th, true);
				} finally {
					synchronized (lock) {
						lock[0] = rtv;
						lock.notify();
					}
				}
			}
		});
		synchronized (lock) {
			if (lock[0] == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		return Boolean.TRUE.equals(lock[0]);

	}

	@Override
	public void error(final Throwable ex, final boolean callbackError) {
		DebugLog.warn(ex);
		postBack(new Runnable() {
			@Override
			public void run() {
				callback.error(ex, callbackError);
			}
		});

	}

	@Override
	public Object requireLock() {
		return url.getPath().intern();
	}

	private Map<String, Object> getPost() {
		return postParams;
	}

	private void postBack(Runnable runner) {
		if (from == null) {
			runner.run();
		} else {
			from.post(runner);
		}
	}

	@Override
	public void onComplete() {
	}

	@Override
	public URL getURL() {
		return url;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Callback<? extends Object> getCallback() {
		return callback;
	}

	@Override
	public long getCreateTime() {
		return createTime;
	}

	@Override
	public int getTimeout() {
		return MAX_TASK_TIMEOUT;
	}

	@Override
	public void cancel() {
		if (!canceled) {
			canceled = true;
		}
		if (thread != null) {
			thread.interrupt();
		}
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public long getStartTime() {
		return startedTime;
	}

	@Override
	public Thread getThread() {
		return thread;
	}

}

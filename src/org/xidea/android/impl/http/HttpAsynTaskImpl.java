package org.xidea.android.impl.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;

import org.xidea.android.Callback;
import org.xidea.android.Callback.CacheCallback;
import org.xidea.android.Callback.Loading;
import org.xidea.android.Callback.PrepareCallback;
import org.xidea.android.impl.AsynTask;
import org.xidea.android.impl.DebugLog;
import org.xidea.android.impl.Network.CachePolicy;
import org.xidea.android.impl.Network.HttpMethod;
import org.xidea.android.impl.Network.RequestTimes;
import org.xidea.android.impl.io.IOUtil;
import org.xidea.android.impl.ui.ImageUtil;
import org.xidea.el.impl.ReflectUtil;

import android.graphics.Bitmap;
import android.graphics.Movie;
import android.os.Handler;




class HttpAsynTaskImpl implements AsynTask,RequestTimes {
	private static final int MAX_TASK_TIMEOUT = 1 * 60 * 1000;
	@SuppressWarnings("rawtypes")
	private final Callback callback;
	private final long createTime = System.currentTimeMillis();
	private long startedTime;
	private final URL url;
	private final HttpMethod method;
	private final HttpSupport http;
	private final Handler from = HttpUtil.currentHandler();
	private final Type prepareType;
	private final Type callbackType;

	private Thread thread;// for interrupt
	private boolean canceled;
	private Map<String, Object> postParams;

	final long[] requestTimes= new long[3];
	final long[] cacheTimes= new long[3];
	final long[] callbackTimes= new long[3];

	public HttpAsynTaskImpl(HttpSupport http, String path, HttpMethod method,
			Callback<?> callback,Map<String,Object> postParams) {
		this.http = http;
		this.url = HttpUtil.parseURL(path);
		this.method = method;
		this.callback = callback;
		Type[] types = HttpUtil.getPrepareCallbackType(callback);//[prepareType?,callbackType]
		prepareType = types[0];
		callbackType = types[1];
		
		this.postParams = postParams;
	}

	@Override
	public void onStart() {
		Method method;
		try {
			method = callback.getClass().getDeclaredMethod("callback", ReflectUtil.baseClass(callbackType));
			Loading loading = method.getAnnotation(Loading.class);
			LoadingImpl.showDialog(this,loading);
		} catch (NoSuchMethodException e) {
			DebugLog.error(e);
		}
		startedTime = System.currentTimeMillis();
		thread = Thread.currentThread();
	}

	@Override
	public Object load(CachePolicy cp) {
		Type rawType = prepareType == null?callbackType:prepareType;
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
	public boolean onCache(Object result,long start){
		return doExecute(result, start, true);
	}
	@Override
	public void onCallback(Object result,long start){
		doExecute(result, start,false);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean doExecute(Object result, long startTime,final boolean cacheCheck) {
		final long[] times = cacheCheck?this.cacheTimes:callbackTimes;
		times[0] = startTime;
		if (callback instanceof PrepareCallback) {
			result = ((PrepareCallback) callback).prepare(result);
			if (result != null) {// 只有prepare
				// 结果有可能变化，因为在loadRaw中已经正确转型了。
				result = HttpUtil.transform(result, callbackType);
			}
		}
		final Object result2 = result;
		final Boolean[] lock = new Boolean[1];
		postBack(new Runnable() {
			@Override
			public void run() {
				times[1] = System.currentTimeMillis();
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
					times[2] = System.currentTimeMillis();
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
		LoadingImpl.cancleUI(this);
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
	public int getTimeout() {
		return MAX_TASK_TIMEOUT;
	}

	@Override
	public void cancel() {
		LoadingImpl.cancleUI(this);
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
	public long getTaskStartTime() {
		return startedTime;
	}

	@Override
	public Thread getThread() {
		return thread;
	}

	@Override
	public long getTaskCreateTime() {
		return createTime;
	}

	@Override
	public long getRequestStartTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDownloadStartTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getRequestEndTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getPrepareStartTime(boolean isCache) {
		long[] times = isCache?this.cacheTimes:callbackTimes;
		return times[0];
	}

	@Override
	public long getCallbackStartTime(boolean isCache) {
		long[] times = isCache?this.cacheTimes:callbackTimes;
		return times[1];
	}

	@Override
	public long getCallbackEndTime(boolean isCache) {
		long[] times = isCache?this.cacheTimes:callbackTimes;
		return times[2];
	}

}

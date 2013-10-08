package org.xidea.android.impl.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.xidea.android.Callback;

public interface HttpInterface {

	/**
	 * 发起异步get请求，callback也将在发起线程执行（如果是ui线程发起的调换用，callback将在ui线程回调，可以直接操作ui）
	 * 
	 * @param callback
	 * @param url
	 */
	public CancelState get(Callback<? extends Object> callback, String url);

	/**
	 * 发起异步post请求，callback也将在发起线程执行（如果是ui线程发起的调换用，callback将在ui线程回调，可以直接操作ui）
	 * 
	 * @param callback
	 * @param url
	 * @param mutipart
	 */
	public CancelState post(Callback<? extends Object> callback, String url,
			String key, File mutipart);

	public CancelState post(Callback<? extends Object> callback, String url,
			String key, InputStream mutipart);

	public CancelState dispatchRequest(AsynTask task);

	public abstract String loadText(String url, boolean ignoreCache);

	public abstract InputStream loadStream(String url, boolean ignoreCache);

	public abstract String loadCacheText(String url);

	public abstract InputStream loadCacheStream(String url);

	public int pause(Object group);
	public int resume(Object group);
	public int cancel(Object group);

	public abstract void setRequestHeader(String key, String value);

	public abstract String getRequestHeader(String key);

	public abstract void updateCache(String url, String content);

	public abstract void removeCache(String key);

	public abstract void setStatistics(NetworkStatistics networkStatistics);

	public interface HttpRequest {
		public URLConnection init(URL url, HttpMethod method,
				Map<String, String> requestHeaders, CancelState cancelGroup)
				throws IOException;

		public void postData(HttpURLConnection conn, Map<String, Object> post,
				CancelState cancelGroup) throws IOException;
	}
	public interface HttpCache {
		public HttpCacheEntry require(URI url, HttpMethod method,
				Map<String, String> requestHeaders);

		public InputStream getInputStream(HttpCacheEntry entry)
				throws IOException;

		public String getString(HttpCacheEntry entry) throws IOException;

		public boolean useCache(HttpCacheEntry entry, URLConnection conn)
				throws IOException;

		public InputStream saveResult(HttpCacheEntry entry, URLConnection conn,
				CancelState cancelState, long timeStart) throws IOException;

		public void addCacheHeaders(HttpCacheEntry entry, URLConnection conn)
				throws IOException;
	}
	public interface CancelState{
		void cancel();
		boolean isCanceled();
	}
	public interface AsynTask extends CancelState{

		URL getURL();
		long getCreateTime();
		long getStartTime();
		int getTimeout();
		Callback<? extends Object> getCallback();
		Object requireLock();
		void start();
		Object loadResult(Type type);
		void execute(Object result);
		void error(Throwable e);
		void complete();
		boolean hitGroup(Object group);
		void interrupt();
	}

	
	public interface NetworkStatistics {

		public void onHttpWaitDuration(URL path, long time);

		public void onHttpConnectDuration(URL path, long time);

		public void onHttpHeaderDuration(URL path, long time);

		public void onHttpNetworkDuration(URL path, long time);

		public void onHttpCacheDuration(URL path, long time, boolean fromTtl);

		public void onHttpCancelDuration(URL path, long time);

		public void onHttpDownloadError(URL path, Throwable exception);

		public void onHttpParseError(URL path, Throwable exception);

		public void onHttpCallbackError(URL path, Throwable exception);

		public void onRedirectOut(String domain);

	}

	public enum HttpMethod {
		GET, POST
	}

	public enum CachePolicy {
		CacheOnly, NetworkOnly, Any
	}

}
package org.xidea.android.impl;

import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

import org.xidea.android.Callback;
import org.xidea.android.Callback.Cancelable;
import org.xidea.android.impl.http.HttpSupport;

/**
 * @see HttpSupport
 * @author jindawei
 * 
 */
public interface Network {

	/**
	 * 发起异步get请求，callback也将在发起线程执行（如果是ui线程发起的调换用，callback将在ui线程回调，可以直接操作ui）
	 * 
	 * @param callback
	 * @param url
	 */
	public Cancelable get(Callback<? extends Object> callback, String url);

	/**
	 * 发起异步post请求，callback也将在发起线程执行（如果是ui线程发起的调换用，callback将在ui线程回调，可以直接操作ui）
	 * 
	 * @param callback
	 * @param url
	 * @param postParams
	 */
	public Cancelable post(Callback<? extends Object> callback, String url,
			Map<String, Object> postParams);

	public Cancelable dispatchRequest(AsynTask task);

	public abstract String loadText(String url, boolean ignoreCache);

	public abstract InputStream loadStream(String url, boolean ignoreCache);

	public abstract String loadCacheText(String url);

	public abstract InputStream loadCacheStream(String url);

	public abstract void setRequestHeader(String key, String value);

	public abstract String getRequestHeader(String key);

	public abstract void updateCache(String url, String content);

	public abstract void removeCache(String key);

	public abstract void setStatistics(NetworkStatistics networkStatistics);

	public boolean isWifiConnected();

	public boolean isInternetConnected();

	public Proxy getProxy();

	public int getMobileGeneration();

	public void addWifiCallback(Callback<Boolean> callback);

	public boolean removeWifiCallback(Callback<Boolean> callback);

	public void addConnectedCallback(Callback<Boolean> callback);

	public boolean removeConnectedCallback(Callback<Boolean> callback);

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
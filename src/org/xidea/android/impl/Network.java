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

	public boolean isWifiConnected();

	public boolean isInternetConnected();

	public Proxy getProxy();

	public int getMobileGeneration();

	public void addWifiCallback(Callback<Boolean> callback);

	public boolean removeWifiCallback(Callback<Boolean> callback);

	public void addConnectedCallback(Callback<Boolean> callback);

	public boolean removeConnectedCallback(Callback<Boolean> callback);

	public interface RequestTimes {
		//请求url
		public URL getURL();

		//任务建立时间
		public long getTaskCreateTime();

		//任务建立时间
		public long getTaskStartTime();

		//网络请求开始时间
		public long getRequestStartTime();

		//网络接收数据开始时间（ttl请求为零）
		public long getDownloadStartTime();

		//网络请求结束时间(etag cached 请求中，与getDownloadStartTime 相等，ttl请求为0)
		public long getRequestEndTime();

		//数据预处理开始时间(isCache?缓存:网络)
		public long getPrepareStartTime(boolean isCache);

		//callback开始时间(isCache?缓存:网络)
		public long getCallbackStartTime(boolean isCache);

		//callback结束时间(isCache?缓存:网络)
		public long getCallbackEndTime(boolean isCache);
	}

	public enum HttpMethod {
		GET, POST
	}

	public enum CachePolicy {
		CacheOnly, NetworkOnly, Any
	}

}
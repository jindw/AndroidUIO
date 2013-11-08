package org.xidea.android.impl.io;


import org.xidea.android.Callback;
import org.xidea.android.Callback.Cancelable;
import org.xidea.android.Callback.Cancelable.CanceledException;
import org.xidea.android.CommonLog;
import org.xidea.android.impl.ApplicationState;
import org.xidea.android.impl.DebugProvider;

import org.apache.commons.logging.Log;

import android.app.Application;
import android.os.Build;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class HttpImplementation implements HttpInterface {

	private static final String NETWORK_UNAVALIABLE = "网络暂无法连接， 请检查网络后再尝试！";
	private final static Log log = CommonLog.getLog();
	private static final int WIFI_TRY_COUNT = 2;
	private static final int MOBILE_TRY_COUNT = 3;
	private static final long MAX_RETRY_TIME = 1000 * 25 * 10;

	static {
		String keepAlive = String.valueOf(Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1);
		System.setProperty("http.keepAlive", keepAlive);
		HttpURLConnection.setFollowRedirects(false);
	}

	private static HttpImplementation instance = new HttpImplementation();
	private File cacheDir;
	private int cacheSize;
	private HttpCacheImpl cacheImpl;
	private Object cacheLock = new Object();
	private HttpRequestImpl request = new HttpRequestImpl();
	private HttpAsynImpl asyn = new HttpAsynImpl() ;
	private NetworkStatistics networkStatistics = HttpUtil.DEFAULT_NETWORK_STATISTICS;

	Map<String, String> requestHeaders = new HashMap<String, String>();
	{
		requestHeaders.put("User-Agent", "Android Client");
		//requestHeaders.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/536.26.14 (KHTML, like Gecko) Version/6.0.1 Safari/536.26.14");
		
		requestHeaders.put("Accept", "*/*");
		requestHeaders.put("X-Wap-Proxy-Cookie", "none");
		requestHeaders.put("Accept-Encoding", "gzip");
		
	}
	public static HttpImplementation getInstance() {
		return instance;
	}
	public void init(final Application application,
			final int cacheSize) {
		File extCache = application.getExternalCacheDir();
		if(extCache == null ){
			//TODO: 监控SD卡
		}else{
			this.cacheDir = new File(extCache,"uio_http_cache");
			
		}
		this.cacheSize = cacheSize;
	}


	void initCache() {
		synchronized (cacheLock) {
			if (cacheImpl == null && cacheDir != null && (cacheDir.exists() ||cacheDir.mkdirs())) {
				try {
					cacheImpl = new HttpCacheImpl(cacheDir, cacheSize);
				} catch (Exception e) {
					cacheDir = null;
					log.error(e);
				}
			}
		}
	}

	@Override
	public void setRequestHeader(String key, String value) {
		requestHeaders.put(key, value);
	}

	@Override
	public String getRequestHeader(String key) {
		return requestHeaders.get(key);
	}


	@Override
	public InputStream loadStream(String url, boolean ignoreCache) {
		try {
			return getStream(HttpUtil.parseURL(url), HttpMethod.GET, null, null, 
					ignoreCache?CachePolicy.NetworkOnly:CachePolicy.Any);
		} catch (IOException e) {
			log.error(e);
			return null;
		}
	}
	@Override
	public InputStream loadCacheStream(String url) {
		try {
			return getStream(HttpUtil.parseURL(url), HttpMethod.GET, null, null, 
					CachePolicy.CacheOnly);
		} catch (IOException e) {
			log.error(e);
			return null;
		}
	}

	@Override
	public String loadText(String url, boolean ignoreCache) {
		try {
			return getString(HttpUtil.parseURL(url), HttpMethod.GET, null, null, 
					ignoreCache?CachePolicy.NetworkOnly:CachePolicy.Any);
		} catch (IOException e) {
			log.error(e);
			return null;
		}
	}

	@Override
	public String loadCacheText(String url) {
		try {
			return getString(HttpUtil.parseURL(url), HttpMethod.GET, null, null, CachePolicy.CacheOnly);
		} catch (IOException e) {
			log.error(e);
			return null;
		}

	}

	@Override
	public void removeCache(String key) {
		if (cacheImpl != null) {
			cacheImpl.removeCache(key);
		}
	}
	@Override
	public void updateCache(String id, String content) {
		cacheImpl.updateCache(id, content);
	}

	public NetworkStatistics getStatistics() {
		return networkStatistics;
	}

	@Override
	public void setStatistics(NetworkStatistics networkStatistics) {
		this.networkStatistics = networkStatistics;
	}

	public InputStream getStream(URL url, HttpMethod method, Map<String, Object> post,
			Cancelable cancelable, CachePolicy cache)
			throws IOException {
		return new Request(url, cancelable, cache, true)
				.doRequest(method, post, InputStream.class);
	}
	public String getString(URL url, HttpMethod method, Map<String, Object> post,
			Cancelable cancelGroup, CachePolicy cache)
			throws IOException {
		ApplicationState as = ApplicationState.getInstance();
		int tryCount = 0;
		long beginTime = System.currentTimeMillis();
		int maxCount = as.isWifiConnected() ? WIFI_TRY_COUNT
				: as.isInternetConnected() ? MOBILE_TRY_COUNT : 1;

		while (true) {
			IOException e;
			try {
				String result = new Request(url, cancelGroup, cache, true).doRequest(method, post,
						String.class);
				RedirectOutException.inc = 0;// 成功真实请求后清零
				return result;
			} catch (RedirectOutException re) {
				if (as.isWifiConnected()) {// wifi 资费页面
					// 不用重试了，直接提示返回
					HttpUtil.showNetworkTips(NETWORK_UNAVALIABLE);
					throw re;
				}else if(as.isInternetConnected()){/// wap 资费页面
					if (++RedirectOutException.inc >= 2) {// 事不过三
						HttpUtil.showNetworkTips(NETWORK_UNAVALIABLE);
						throw re;
					}
				}
				
				e = re;
			} catch (CanceledException ce) {
				throw ce;
			} catch (IOException ex) {
				e = ex;
			}
			try {

				while (true) {
					if (e != null
							&& ((System.currentTimeMillis() - beginTime) > MAX_RETRY_TIME || ++tryCount >= maxCount)) {
						HttpUtil.showNetworkTips(NETWORK_UNAVALIABLE);
						throw e;
					}
					if (tryCount <= 1) {
						Thread.sleep(100);
					} else {
						Thread.sleep(600);
					}
					if (as.isInternetConnected()) {
						break;
					}
				}
			} catch (InterruptedException ie) {
			}
		}
	}


	/**
	 * 用于生成入库的uri
	 * @param url
	 * @return
	 */
	protected URI toIdentity(URL url) {
		try {
			return url.toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @param rawURL
	 * @param method
	 * @param post
	 * @param cancelable
	 * @param toString
	 *            是否转为 string 返回，如果是缓存数据，永远只范围缓存实体
	 * @param onlyCache
	 *            是否直接从cache中取数据，只在get 请求中有效。
	 * @param ignoreCache
	 *            是否忽略cache 数据, 直连网络，如失败反回null
	 * @return
	 * @throws IOException
	 */
	class Request {
		final long start;
		final URI uri;
		final URL url;
		final Cancelable cancelable;
		final CachePolicy cache;
		final boolean saveCache;

		URLConnection conn;
		// InputStream in;
		HttpCacheEntry entry;
		Object result;// 可以是，URLConnection，InputStream，String，HttpCacheEntry

		Request(final URL rawURL, Cancelable cancelState,
				CachePolicy cache, boolean saveCache)
				throws IOException {
			this.start = System.currentTimeMillis();
			this.uri = toIdentity(rawURL);
			this.url = rawURL;
			this.cache = cache;
			this.saveCache = saveCache;
			this.cancelable = cancelState ;
		}

		@SuppressWarnings("unchecked")
		public <T> T getResult(Class<T> type) throws IOException {
			if (result instanceof URLConnection) {
				// conn = (URLConnection) result;
				conn.connect();
				networkStatistics.onHttpConnectDuration(url,
						System.currentTimeMillis() - start);
				result = new FilterInputStream(HttpUtil.getInputStream(conn)) {
					@Override
					public void close() throws IOException {
						networkStatistics.onHttpNetworkDuration(url,
								System.currentTimeMillis() - start);
						super.close();
					}
				};
			}

			if (result instanceof HttpCacheEntry) {
				if (type == String.class) {
					result = cacheImpl.getString((HttpCacheEntry) result);
				} else {
					result = cacheImpl.getInputStream((HttpCacheEntry) result);
				}
			} else {
				if (type == String.class) {
					if (result instanceof InputStream) {
						String encoding = HttpUtil.guessCharset(conn);
						result = StreamUtil.loadTextAndClose(
								(InputStream) result, encoding);
					}
				}
			}
			return (T) result;
		}

		<T> T doRequest(HttpMethod method, Map<String, Object> post, Class<T> type)
				throws IOException {
			try {
				if ("file".equalsIgnoreCase(url.getProtocol())) {
					// file:///android_asset
					result = HttpUtil.openFileStream(url);
				} else if (HttpMethod.POST == method) {
					result = conn = request.init(url, method, requestHeaders,
							cancelable);
					request.postData((HttpURLConnection) conn, post,
							cancelable);
				} else if (HttpMethod.GET == method) {
					doGet();
				} else {
					throw new UnsupportedOperationException();
				}
				return getResult(type);
			} catch (final UnknownHostException e) {
				// log.warn(e.toString());
				tryCache(false);
				return getResult(type);
			} catch (CanceledException e) {
				networkStatistics.onHttpCancelDuration(url,
						System.currentTimeMillis() - start);
				throw e;
			} finally {
				processRedirect(url, conn);
				pushToProvider(method);
			}
		}

		private void pushToProvider(HttpMethod method) {
				String text = null;
				File file = null;
				if (result instanceof String) {
					text = (String) result;
				} else if (cacheImpl != null) {
					file = cacheImpl.getCacheFile(entry);
				}
				DebugProvider.addEntry(uri, method == HttpMethod.GET?"GET":"POST", text, file);
		}

		private void doGet() throws IOException {
			initCache();
			if (cacheImpl == null) {// no cache
				if(cache != CachePolicy.CacheOnly){
					initConn(HttpMethod.GET);
				}
			} else {
				entry = cacheImpl.require(uri, HttpMethod.GET, requestHeaders);
				if (cache == CachePolicy.CacheOnly) {
					tryCache(false);
				} else if (cacheImpl.hasCache(entry)) {// has cache
					if (cacheImpl.useCache(entry, null)) {
						tryCache(true);// ttl cache
					} else {
						initConn(HttpMethod.GET);
						if (cacheImpl.hasCache(entry)) {
							cacheImpl.addCacheHeaders(entry, conn);// 304
							if (cacheImpl.useCache(entry, conn)) {
								tryCache(true);
								return;// valid cache
							}
						}
						// invalid cache !!
						initCacheSaver();
					}
				} else {// no cache
					initConn(HttpMethod.GET);
					initCacheSaver();
				}
			}

		}

		private void initConn(HttpMethod method) throws IOException {
			result = conn = (HttpURLConnection) request.init(url, method,
					requestHeaders, cancelable);
			//System.out.println(conn.getRequestProperties());
			//System.out.println(conn.getHeaderFields());
		}

		/**
		 * 尝试是否能从cache中获取内容
		 * @param checked
		 */
		private void tryCache(boolean checked) {
			if (entry != null) {
				networkStatistics.onHttpCacheDuration(url,
						System.currentTimeMillis() - start, true);
			}
			if (cache == CachePolicy.NetworkOnly) {
				result = null;
			} else {
				result = entry;
			}
		}

		/**
		 * 初始化cache 保存的包装
		 * @throws IOException
		 */
		private void initCacheSaver() throws IOException {
			if (saveCache) {
				if (entry == null) {
					entry = cacheImpl.require(uri, HttpMethod.GET, requestHeaders);
				}
				InputStream ws = cacheImpl.saveResult(entry, conn, cancelable,
						start);
				result = (ws == null ? entry : ws);
			}
		}

		private void processRedirect(final URL url, URLConnection conn)
				throws RedirectOutException {
			if (conn != null) {
				try {
					URL realURL = conn.getURL();
					if (realURL == url) {
						String location = conn.getHeaderField("Location");
						if (location != null) {
							realURL = new URL(realURL, location);
						}
					}
					if (realURL != url) {
						String host1 = url.getHost();
						String host2 = realURL.getHost();
						if (!host1.equals(host2)) {
							int p = host1.lastIndexOf('.',
									host1.lastIndexOf('.') - 1);
							if (p >= 0 && !host2.endsWith(host1.substring(p))) {
								networkStatistics.onRedirectOut(host2);
								throw new RedirectOutException(host2);
							}
						}
					}
				} catch (RedirectOutException e) {
					throw e;
				} catch (Exception e) {
				}
			}
		}


	}


	private static class RedirectOutException extends ConnectException {
		private static int inc;
		private static final long serialVersionUID = 1L;
		public RedirectOutException(String domain) {
			super("network error! maybe a provider fee page");
		}
	}


	@Override
	public Cancelable post(Callback<? extends Object> callback, String url, String key, File file) {
		return dispatchRequest(new HttpAsynImpl.TaskImpl(this,url,
				HttpInterface.HttpMethod.POST, callback, key, file));
	}

	@Override
	public Cancelable post(Callback<? extends Object> callback, String url, String key,
			InputStream in) {
		return dispatchRequest(new HttpAsynImpl.TaskImpl(this,url,
				HttpInterface.HttpMethod.POST, callback, key, in));
	}

	@Override
	public Cancelable get(Callback<? extends Object> callback, String url) {
		return dispatchRequest(new HttpAsynImpl.TaskImpl(this,url, HttpInterface.HttpMethod.GET,
				callback, null, null));
	}

	@Override
	public Cancelable dispatchRequest(HttpInterface.AsynTask task){
		asyn.dispatchRequest(task);
		return task;
	}

	@Override
	public int cancel(Object group) {
		return asyn.cancel(group);
	}
	@Override
	public int pause(Object group) {
		return asyn.pause(group);
	}
	@Override
	public int resume(Object group) {
		return asyn.resume(group);
	}

}

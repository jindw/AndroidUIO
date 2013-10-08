package org.xidea.android.impl.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.xidea.el.json.JSONEncoder;

import android.content.ContentValues;

import org.xidea.android.Callback;
import org.xidea.android.CommonLog;
import org.xidea.android.SQLiteMapper;
import org.xidea.android.impl.ApplicationState;
import org.xidea.android.impl.io.HttpInterface.CancelState;
import org.xidea.android.impl.io.HttpInterface.HttpCache;
import org.xidea.android.impl.io.HttpInterface.HttpMethod;
import org.xidea.android.impl.io.HttpInterface.NetworkStatistics;

public class HttpCacheImpl implements HttpCache {
	private static CommonLog log = CommonLog.getLog();

	private static final int VERSION = 20120316;
	private static final int MAX_COUNT = 1024 * 2;

	private DiskLruCache cache;
	private SQLiteMapper<HttpCacheEntry> mapper;
	private Object lock = new Object();

	public HttpCacheImpl(File dir, int maxCacheSize) throws IOException {
		if (!dir.exists()) {
			dir.mkdirs();
		}
		cache = DiskLruCache.open(dir, VERSION, MAX_COUNT, maxCacheSize);
		mapper = StorageImpl.INSTANCE.getSQLiteStorage(HttpCacheEntry.class,ApplicationState.getInstance().getApplication());// new
																	// SQLiteMapperImpl<CacheEntry>(context,
																	// CacheEntry.class);
	}

	@Override
	public HttpCacheEntry require(URI identityURL, HttpMethod method,
			Map<String, String> requestHeaders) {
		HttpCacheEntry entry = null;
		synchronized (lock) {
			entry = mapper.getByKey("uri", identityURL);
			if (entry == null) {
				entry = new HttpCacheEntry();
				entry.uri = identityURL;
				// entry.requestHeaders =
				// JSONEncoder.encode(requestHeaders);//没有必要存储这个，省点电
				try {
					mapper.save(entry);
				} catch (android.database.sqlite.SQLiteConstraintException e) {
					entry = mapper.getByKey("uri", identityURL);
				}
			}
		}
		return entry;
	}

	@Override
	public InputStream getInputStream(HttpCacheEntry entry) throws IOException {
		String key = key(entry);
		InputStream in = cache.get(key);
		if (in == null) {
			cache.remove(key);
			if (entry.responseBody != null) {
				return new ByteArrayInputStream(
						entry.responseBody.getBytes(entry.charset));
			}
		}
		return in;
	}

	@Override
	public String getString(HttpCacheEntry entry) throws IOException {
		if (entry.responseBody == null) {
			InputStream in = cache.get(key(entry));
			if (in != null) {
				return FileUtil.loadTextAndClose(in,
						entry.charset == null ? HttpUtil.DEFAULT_CHARSET
								: entry.charset);
			}
		}
		return entry.responseBody;
	}

	public InputStream saveResult(final HttpCacheEntry entry,
			final URLConnection conn, CancelState cancelState,
			final long timeStart) throws IOException {
		final URL url = conn.getURL();
		final NetworkStatistics networkStatistics = HttpImplementation.getInstance()
				.getStatistics();
		// log.timeStart();
		HttpUtil.assertNotCanceled(conn, cancelState);
		conn.connect();
		HttpUtil.assertNotCanceled(conn, cancelState);
		networkStatistics.onHttpConnectDuration(url, System.currentTimeMillis()
				- timeStart);
		// log.timeEnd("connect" + path);
		InputStream in = HttpUtil.getInputStream(conn);//conn.getInputStream();
		// log.timeEnd("get$" + in.available() + path);

		final ContentValues contents = new ContentValues();
		contents.put("id", entry.id);
		contents.put("contentLength", conn.getContentLength());
		String responseHeaders = JSONEncoder.encode(conn.getHeaderFields());
		contents.put("responseHeaders", responseHeaders);
		if(CommonLog.isDebug()){
			log.warn(entry.uri+"\n"+responseHeaders);
			HashMap<String, String> map = new HashMap<String,String>(HttpImplementation.getInstance().requestHeaders);
			map.put("Cookie", conn.getRequestProperty("Cookie"));
			
			contents.put("requestHeaders", JSONEncoder.encode(map));
		}
		// ttl
		// TODO: max-age,no-cache.......
		contents.put("ttl", entry.ttl = conn.getExpiration());
		contents.put("hit", entry.hit++);
		// etag
		contents.put("lastModified",
				entry.lastModified = conn.getLastModified());
		contents.put("lastSaved", entry.lastSaved = System.currentTimeMillis());
		contents.put("etag", entry.etag = conn.getHeaderField("ETag"));
		String contentType = conn.getContentType();
		contents.put("contentType", entry.contentType = contentType);
		String charset = HttpUtil.guessCharset(conn);
		contents.put("charset", entry.charset = charset);
		if (charset != null) {
			String result = FileUtil.loadTextAndClose(in, entry.charset);

			networkStatistics.onHttpNetworkDuration(url,
					System.currentTimeMillis() - timeStart);
			// log.timeEnd("read" + path);
			contents.put("responseBody", entry.responseBody = result);
			mapper.update(contents);
			return null;
		} else {
			contents.put("responseBody", entry.responseBody = null);
			return cache.edit(in, key(entry),0,new Callback<Boolean>() {
				@Override
				public void callback(Boolean success) {
						if (success) {
							networkStatistics.onHttpNetworkDuration(url,
									System.currentTimeMillis() - timeStart);
							// log.timeEnd("read" + path);
							try {
								mapper.update(contents);
							} catch (Exception e) {
								log.warn(e);
							}
						} else {
						}

				}

				@Override
				public void error(Throwable ex, boolean callbackError) {
					
				}
			});
		}
	}

	public boolean hasCache(HttpCacheEntry entry) {
		return (entry != null)
				&& (entry.responseBody != null || entry.contentType != null
						&& cache.contains(key(entry)));
	}

	@Override
	public boolean useCache(HttpCacheEntry entry, URLConnection conn) {
		try {
			if (conn == null) {
				// check ttl
				if (entry.ttl != null
						&& entry.ttl >= System.currentTimeMillis()) {
					return true;
				}
			} else if (conn instanceof HttpURLConnection) {
				// check 304
				return ((HttpURLConnection) conn).getResponseCode() == 304;
			}
		} catch (Exception e) {
			log.error(e);
		}
		return false;
	}

	public void addCacheHeaders(HttpCacheEntry entry, URLConnection conn) {
		if (conn instanceof HttpURLConnection) {
			if (entry.lastModified != null && entry.lastModified > 0) {
				// add lastModified
				// Expires
				// Cache-Control max age
				conn.setRequestProperty("If-Modified-Since",
						toGMTString(new Date(entry.lastModified)));
			}
			if (entry.etag != null) {
				// add etag
				conn.setRequestProperty("If-None-Match", entry.etag);
			}
		}
	}

	public String toGMTString(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE, dd MMM y HH:mm:ss 'GMT'", Locale.US);
		TimeZone gmtZone = TimeZone.getTimeZone("GMT");
		sdf.setTimeZone(gmtZone);
		GregorianCalendar gc = new GregorianCalendar(gmtZone);
		gc.setTimeInMillis(date.getTime());
		return sdf.format(date);
	}

	public DiskLruCache startCache() {
		return cache;
	}

	/**
	 * @internal
	 * @param entry
	 * @return
	 */
	File getCacheFile(HttpCacheEntry entry) {
		if (entry == null) {
			return null;
		}
		String key = key(entry);
		return new File(cache.getDirectory(), key);
	}

	private String key(HttpCacheEntry entry) {
		String path = entry.uri.getPath();
		return entry.id + "_" + path.substring(path.lastIndexOf('/') + 1);
	}

	public void removeCache(String uri) {
		try {
			HttpCacheEntry entry = mapper.getByKey("uri", uri);
			if (entry != null) {
				mapper.remove(entry);
				if (cache != null) {
					cache.remove(key(entry));
				}
			}
		} catch (Exception e) {
		}
	}

	public void updateCache(String id, String content) {
		HttpCacheEntry entry = mapper.getByKey("uri", id);
		if (entry != null) {
			entry.responseBody = content;
			mapper.update(entry);
		}

	}
}

package org.xidea.android.impl.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.xidea.el.json.JSONEncoder;

import android.util.Base64;

import org.xidea.android.Callback;
import org.xidea.android.SQLiteMapper;
import org.xidea.android.UIO;
import org.xidea.android.impl.DebugLog;
import org.xidea.android.Callback.Cancelable;
import org.xidea.android.impl.io.DiskLruCache;
import org.xidea.android.impl.io.StorageFactory;
import org.xidea.android.impl.io.IOUtil;



public interface HttpCache {
	public HttpCacheEntry require(URI url);

	public InputStream getStream(HttpCacheEntry entry)
			throws IOException;
	
	public String getText(HttpCacheEntry entry) throws IOException;

	public File getFile(HttpCacheEntry entry)
			throws IOException;

	/**
	 * 需要同时考虑文件被删除的情况
	 */
	public boolean hasCache(HttpCacheEntry entry) ;
	public boolean useCache(HttpCacheEntry entry, URLConnection conn)
			throws IOException;

	public void removeCache(String uri) ;
	public void updateCache(String uri,String content) ;
	
	public InputStream getWritebackStream(HttpCacheEntry entry, URLConnection conn,InputStream in,
			Cancelable cancelState) throws IOException;

	public void addCacheHeaders(HttpCacheEntry entry, URLConnection conn)
			throws IOException;
}
class HttpCacheImpl implements HttpCache {

	private static final int MAX_COUNT = 1024 * 2;

	private DiskLruCache cache;
	private SQLiteMapper<HttpCacheEntry> mapper;
	private Object lock = new Object();

	public HttpCacheImpl(File dir, long maxCacheSize) throws IOException {
		cache = StorageFactory.INSTANCE.openCache(dir,  maxCacheSize,MAX_COUNT);
		mapper = UIO.getSQLiteStorage(HttpCacheEntry.class);// new
	}

	@Override
	public HttpCacheEntry require(URI identityURL) {
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
	public String getText(HttpCacheEntry entry) throws IOException {
		if (entry.responseBody == null) {
			InputStream in = cache.get(key(entry));
			if (in != null) {
				return IOUtil.loadTextAndClose(in,
						entry.charset == null ? HttpUtil.DEFAULT_CHARSET.name()
								: entry.charset);
			}
		}
		return entry.responseBody;
	}

	@Override
	public InputStream getStream(HttpCacheEntry entry) throws IOException {
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
	public File getFile(final HttpCacheEntry entry) throws IOException {
		return cache.getCacheFile(key(entry));
	}
	
	public InputStream getWritebackStream(final HttpCacheEntry entry,
			final URLConnection conn, InputStream in,Cancelable cancelState) throws IOException {
		final String charset = HttpUtil.guessCharset(conn);
		if (charset != null) {
			String result = IOUtil.loadTextAndClose(in, entry.charset);
			try{
				initEntry(entry, conn,charset);
				entry.responseBody = result;
				mapper.update(entry);
			}catch(Exception e){
				DebugLog.warn(e);
			}
			return null;
		} else {
			entry.responseBody = null;
			return cache.getWritebackFilter(in, key(entry),0,new Callback<Boolean>() {
				@Override
				public void callback(Boolean success) {
						if (success) {
							try {
								initEntry(entry, conn,charset);
								entry.responseBody = null;
								mapper.update(entry);
							} catch (Exception e) {
								DebugLog.warn(e);
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

	private void initEntry(final HttpCacheEntry entry,
			final URLConnection conn,String charset) {
		entry.contentLength =  conn.getContentLength();
		entry.responseHeaders = JSONEncoder.encode(conn.getHeaderFields());
		if(DebugLog.isDebug()){
			DebugLog.warn(entry.uri+"\n"+entry.responseHeaders);
			HashMap<String, String> map = new HashMap<String,String>(HttpSupport.INSTANCE.requestHeaders);
			map.put("Cookie", conn.getRequestProperty("Cookie"));
			
			entry.requestHeaders = JSONEncoder.encode(map);
		}
		// ttl
		// TODO: max-age,no-cache.......
		entry.ttl = conn.getExpiration();
		entry.hit++;
		// etag
		entry.lastModified = conn.getLastModified();
		entry.lastAccess = System.currentTimeMillis();
		entry.etag = conn.getHeaderField("ETag");
		String contentType = conn.getContentType();
		entry.contentType = contentType;
		entry.charset = charset;
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
			DebugLog.error(e);
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


	private String key(HttpCacheEntry entry) {
		String path = entry.uri.toString();
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] data = md5.digest(path.getBytes("UTF-8"));
			path = Base64.encodeToString(data, Base64.NO_WRAP)+path.substring(path.lastIndexOf('/') + 1);
		} catch (Exception e) {
		}
		return path.replaceAll("[?#].*|[^\\w-\\.]", "");
	}

	public void removeCache(String uri) {
		try {
			HttpCacheEntry entry = mapper.getByKey("uri", uri);
			if (entry != null) {
				entry.responseBody = null;
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

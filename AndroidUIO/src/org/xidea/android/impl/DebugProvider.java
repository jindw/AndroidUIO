package org.xidea.android.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.commons.logging.Log;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Base64;

import org.xidea.android.CommonLog;
import org.xidea.android.SQLiteMapper;
import org.xidea.android.impl.io.FileUtil;
import org.xidea.android.impl.io.HttpCacheEntry;
import org.xidea.android.impl.io.SQLiteMapperImpl;

public class DebugProvider extends ContentProvider {
	private static final int MAX_SIZE = 40;

	private SQLiteMapper<HttpCacheEntry> sqliteData = null;
	private static Log log = CommonLog.getLog();
	private static LinkedList<HttpInfo> httpCache = new LinkedList<DebugProvider.HttpInfo>();
	private static Object httpCacheLock = new Object();

	static boolean init() {
		return CommonLog.isDebug();
	}

	static class HttpInfo {
		URI uri;
		String method;
		String loadedResult;
		long time = System.currentTimeMillis();
		File cacheFile;
	}

	public static void addEntry(URI uri, String method, String stringResult,
			File cacheFile) {
		if (init()) {
			HttpInfo info = new DebugProvider.HttpInfo();
			info.uri = uri;
			info.method = method;
			info.loadedResult = stringResult;
			info.cacheFile = cacheFile;

			synchronized (httpCacheLock) {
				httpCache.add(info);

				while (httpCache.size() > MAX_SIZE) {
					httpCache.removeFirst();
				}
			}
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		{
			{
				String sourcePath = getEntryURI(uri);
				Cursor cursor = null;
				if (sourcePath == null) {
					cursor = queryHttpList();
				} else {
					cursor = queryHttpEntry(sourcePath);

				}

				return cursor;
			}
		}
	}

	private String getEntryURI(Uri uri) {
		String sourcePath = uri.getPath();
		if (sourcePath != null) {
			sourcePath = sourcePath.replaceFirst("^\\/", "");
			if (sourcePath.length() > 0) {
				try {
					return new String(
							Base64.decode(sourcePath, Base64.URL_SAFE), "UTF-8");
				} catch (IOException e) {
					log.error(e);
				}
			}
		}
		return null;
	}

	private Cursor queryHttpEntry(String sourcePath) {
		HttpInfo info = getHttpInfo(sourcePath);
		if (info != null) {
			String[] columns = new String[] { "requestHeader",
					"responseHeader", "content", "data", "contentType",
					"charset", "lastSaved", "ttl" };
			String requestHeader = null;
			String responseHeader = null;
			String content = info.loadedResult;
			String contentType = null;
			String charset = null;
			long lastSaved = info.time;
			Long ttl = null;
			if (sqliteData == null) {
				sqliteData = new SQLiteMapperImpl<HttpCacheEntry>(getContext(),
						HttpCacheEntry.class);
			}
			HttpCacheEntry entry = sqliteData.getByKey("uri",
					Uri.parse(sourcePath));
			if (entry != null) {
				ttl = entry.ttl;
				contentType = entry.contentType;
				charset = entry.charset;
				// if (entry.lastSaved > info.time) {// entry 很可能是有效的
				requestHeader = entry.requestHeaders;
				responseHeader = entry.responseHeaders;
				if (content == null) {
					content = entry.responseBody;
				}
				// file = HttpCacheImpl.
				lastSaved = entry.lastSaved;
				// }
			}
			byte[] data = null;
			try {
				if (content == null) {
					File file = info.cacheFile;
					if (file != null && file.exists() && file.isFile()) {
						data = FileUtil.loadBytesAndClose(new FileInputStream(
								file));
						charset = null;
					}
				} else {
					charset = "UTF-8";
					data = content.getBytes(charset);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			System.out.println("content=" + content);
			System.out.println("data is " + data == null ? "null" : "not null");
			Object[] values = new Object[] { requestHeader, responseHeader,
					content, data, contentType, charset, lastSaved, ttl };
			MatrixCursor cursor = new MatrixCursor(columns);
			cursor.addRow(values);
			return cursor;
		}
		return null;
	}

	private HttpInfo getHttpInfo(String sourcePath) {
		URI uri = URI.create(sourcePath);
		synchronized (httpCacheLock) {
			// System.out.println(sourcePath);

			for (ListIterator<HttpInfo> it = httpCache.listIterator(httpCache
					.size()); it.hasPrevious();) {
				HttpInfo entry = it.previous();

				// System.out.println(entry.uri);
				if (entry.uri.equals(uri)) {
					return entry;
				}
			}
		}
		return null;
	}

	private Cursor queryHttpList() {
		MatrixCursor cursor = new MatrixCursor(new String[] { "uri", "method",
				"time" });
		synchronized (httpCacheLock) {
			for (ListIterator<HttpInfo> it = httpCache.listIterator(httpCache
					.size()); it.hasPrevious();) {
				HttpInfo entry = it.previous();
				URI uri = entry.uri;
				cursor.addRow(new Object[] { uri.toString(), entry.method,
						entry.time });
			}
		}
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		HttpCacheEntry entry = sqliteData.getByKey("uri", getEntryURI(uri));
		if (entry != null) {
			if (values.containsKey("content")) {
				entry.responseBody = values.getAsString("content");
			}
			if (values.containsKey("ttl")) {
				entry.ttl = values.getAsLong("ttl");
			}
			sqliteData.update(entry);
			return 1;
		}
		return 0;
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String entryURI = getEntryURI(uri);
		if (entryURI != null) {
			// TODO:clear http cache and image cache
			// HttpUtil.getInstance().clear(entryURI);
			return 1;
		}
		return 0;
	}

	private static boolean forceDebug = false;

	static boolean forceDebug() {
		return forceDebug;
	}

	void call(String method, String arg) {
		if (method.equals("debug_enable")) {
			forceDebug = true;
		} else if (method.equals("debug_disable")) {
			forceDebug = false;
		} else if (method.equals("request_clean")) {
			synchronized (httpCacheLock) {
				httpCache.clear();
			}
		} else if (method.equals("debug_setting")) {
			Context context = this.getContext();
			String clazz = context.getPackageName() + ".DebugActivity";
			try {
				Intent intent = new Intent(context, Class.forName(clazz));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			} catch (ClassNotFoundException e) {

			}
		}
	}
}

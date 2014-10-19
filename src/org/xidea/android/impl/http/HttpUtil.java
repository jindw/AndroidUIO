package org.xidea.android.impl.http;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.xidea.android.Callback;
import org.xidea.android.Callback.Cancelable.CanceledException;
import org.xidea.android.Callback.PrepareCallback;
import org.xidea.android.UIO;
import org.xidea.android.impl.DebugLog;
import org.xidea.android.Callback.Cancelable;
import org.xidea.android.impl.Network.NetworkStatistics;
import org.xidea.android.impl.ui.UIFacade;
import org.xidea.el.ExpressionSyntaxException;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder;

import android.os.Handler;
import android.os.Looper;

public final class HttpUtil {

	static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	private static final String ANDROID_ASSET = "/android_asset/";
	private static byte[] END_BYTES = "\r\n".getBytes();
	private static byte[] TWO_HYPENS_BYTES = "--".getBytes();
	private static byte[] BOUNDARY_PREFIX_BYTES = "--------7da3d81520810"
			.getBytes();
	// static final String DEFAULT_CHARSET = "UTF-8";
	private static final JSONDecoder JSON_DECODER = new JSONDecoder(false);
	static Pattern CHARSET = Pattern.compile(".*?;\\s*charset=([\\w\\-]+).*?");
	static Pattern UTF8_DEFAULT_CONTENT_TYPE = Pattern
			.compile("^(?:application\\/json|text\\/json)");
	static Pattern STRING_CONTENT_TYPE = Pattern
			.compile("^(?:application\\/os-stream|image\\/.*)");
	static Pattern TEXT_CONTENT_TYPE = Pattern.compile("^text\\/.*");

	private static HashMap<Type, Type[]> callbackTypeMap = new HashMap<Type, Type[]>();
	private static SSLSocketFactory sslSocketFactory;

	static void startCacheAsyn(final HttpSupport impl) {
		new Thread() {
			public void run() {
				impl.initCache();
				DebugLog.info("cache inited");
			}

		}.start();
	}

	private HttpUtil() {
	}

	static void showNetworkTips(String msg) {
		UIFacade.getInstance().shortTips(msg);
	}

	public static void trustConnection(URLConnection conn) {
		if (conn instanceof HttpsURLConnection) {
			HttpsURLConnection sonn = (HttpsURLConnection) conn;
			// Create a trust manager that does not validate certificate chains
			if (sslSocketFactory == null) {
				TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					@Override
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					@Override
					public void checkClientTrusted(X509Certificate[] certs,
							String authType) {
					}

					@Override
					public void checkServerTrusted(X509Certificate[] certs,
							String authType) {
					}
				} };
				try {
					SSLContext sslContext = SSLContext.getInstance("TLS");
					sslContext.init(null, trustAllCerts, null);
					sslSocketFactory = sslContext.getSocketFactory();
				} catch (Throwable e) {
					DebugLog.error(e.getMessage(), e);
				}
			}

			if (sslSocketFactory != null) {
				sonn.setSSLSocketFactory(sslSocketFactory);
				sonn.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			}
		}
	}

	static String guessCharset(URLConnection conn) {
		if (conn == null) {
			return null;
		}
		String contentType = conn.getContentType();
		if (contentType == null) {
			return null;
		}
		String charset = CHARSET.matcher(contentType).replaceFirst("$1");
		if (charset.equals(contentType)) {
			if (UTF8_DEFAULT_CONTENT_TYPE.matcher(contentType).find()) {
				return "UTF-8";
			} else if (TEXT_CONTENT_TYPE.matcher(contentType).find()) {
				return DEFAULT_CHARSET.name();
			} else {
				return null;
			}
		} else if (STRING_CONTENT_TYPE.matcher(contentType).find()) {
			return null;
		}
		return charset;
	}

	static final Pattern COOKIE_ENTRY = Pattern
			.compile("(?:^|;\\s*)([\\w\\.\\-\\_\\$]+)(?:=([^;]+))");

	static URL appendCookieAsQuery(URL url, String cookie)
			throws MalformedURLException {
		StringBuilder buf = new StringBuilder(url.getPath());
		String query = url.getQuery();
		if (query != null) {
			buf.append('?').append(query);
		}

		if (cookie != null) {
			char join = query == null ? '?' : '&';
			Matcher m = COOKIE_ENTRY.matcher(cookie);
			while (m.find()) {
				if (m.groupCount() >= 2) {
					buf.append(join).append(m.group(1)).append('=')
							.append(m.group(2));
				} else {
					buf.append(join).append(m.group(1));
				}
				join = '&';
			}

		}
		url = new URL(url, buf.toString());
		return url;
	}

	private static Pattern DOUBLE_SPLIT_REGEXP = Pattern
			.compile("([^:])\\/\\/+");

	// private static Pattern CLEAR_REGEXP = Pattern
	// .compile("(^|[?&])(?:bduss|timestamp)=\\w+");
	// public static URI toIdentity(URL url) {
	// try {
	// String clearURL = CLEAR_REGEXP.matcher(url.toString()).replaceAll(
	// "$1");
	// return URI.create(clearURL);
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// }
	// }

	static InputStream getInputStream(URLConnection conn) throws IOException {
		String contentEncoding = conn.getContentEncoding();
		// log.info("contentEncoding:"+contentEncoding);
		InputStream in = conn.getInputStream();
		if (contentEncoding != null && contentEncoding.indexOf("gzip") >= 0
				&& !(in instanceof GZIPInputStream)) {
			if (!in.markSupported()) {
				// we need a pushbackstream to look ahead
				in = new BufferedInputStream(in);
			}
			// check if matches standard gzip maguc number
			in.mark(2);
			if (in.read() == 0x1f && in.read() == 0x8b) {
				in.reset();
				DebugLog.info("try ungzip:" + conn.getURL());
				in = new GZIPInputStream(in);
			} else {
				in.reset();
			}
			return in;
		} else {
			// conn inputstream no buffer by default(buffered for beter
			// performance)!!
			return new BufferedInputStream(in);
		}

	}

	public static URL parseURL(String path) {
		if (path == null || path.length() == 0) {
			return null;
		}
		if (path.startsWith("/")) {
			// isFile
			path = "file://" + path;
		} else {
			Matcher m = DOUBLE_SPLIT_REGEXP.matcher(path);
			if (m.find()) {
				path = m.replaceAll("$1/");
			}
		}
		try {
			return new URL(path);
		} catch (MalformedURLException e) {
			DebugLog.warn(e);
			return null;
		}
	}

	static String buildQuery(URL url, Map<String, Object> post, String charset)
			throws IOException {
		String query = url.getQuery();
		StringBuilder buf = query == null ? new StringBuilder()
				: new StringBuilder(query);
		if (post != null) {
			for (Map.Entry<String, Object> vs : post.entrySet()) {
				String key = vs.getKey();
				Object value = vs.getValue();
				if (value instanceof Object[]) {
					for (Object v : (Object[]) value) {
						if (!appendKeyValue(buf, key, v, charset)) {
							return null;
						}
					}
				} else {
					if (!appendKeyValue(buf, key, value, charset)) {
						return null;
					}
				}
			}
		}
		return buf.toString();
	}

	private static boolean appendKeyValue(StringBuilder buf, String key,
			Object value, String charset) throws UnsupportedEncodingException {
		if (value instanceof File || value instanceof InputStream) {
			return false;
		}
		if (buf.length() > 0) {
			buf.append('&');
		}
		buf.append(URLEncoder.encode(key, charset));
		buf.append('=');
		buf.append(URLEncoder.encode(String.valueOf(value), charset));
		return true;
	}

	public static void addMultiPartPost(HttpURLConnection conn,
			Map<String, Object> params, String charset, Cancelable cancelState)
			throws IOException {
		boolean sendzip = false;
		String boundaryPostfix = Double.toHexString(Math.random() * 0xFFFF);
		byte[] boundaryPostfixBytes = boundaryPostfix.getBytes();
		conn.setRequestProperty("Content-Type",
				"multipart/form-data; boundary="
						+ new String(BOUNDARY_PREFIX_BYTES) + boundaryPostfix);
		if (sendzip) {
			conn.setRequestProperty("Content-Encoding", "gzip");
		}
		assertNotCanceled(conn, cancelState);
		conn.connect();
		assertNotCanceled(conn, cancelState);

		OutputStream out = conn.getOutputStream();
		if (sendzip) {
			out = new GZIPOutputStream(out);
		}
		for (Map.Entry<String, Object> kv : params.entrySet()) {
			String k = kv.getKey();
			Object vs = kv.getValue();
			if (vs instanceof Object[]) {
				for (Object v : (Object[]) vs) {
					assertNotCanceled(conn, cancelState);
					writeEntry(out, charset, k, v, boundaryPostfixBytes);
					assertNotCanceled(conn, cancelState);
				}
			} else {
				assertNotCanceled(conn, cancelState);
				writeEntry(out, charset, k, vs, boundaryPostfixBytes);

			}
		}
		writeLine(out, TWO_HYPENS_BYTES, BOUNDARY_PREFIX_BYTES,
				boundaryPostfixBytes, TWO_HYPENS_BYTES);
		out.flush();
		out.close();// gzip finished on close
		assertNotCanceled(conn, cancelState);
	}

	static void assertNotCanceled(URLConnection conn, Cancelable cancelState)
			throws CanceledException {
		// TODO:...
		if (cancelState != null && cancelState.isCanceled()) {
			if (conn instanceof HttpURLConnection) {
				((HttpURLConnection) conn).disconnect();
			}
			throw new CanceledException();
		}
	}

	private static void writeEntry(OutputStream to, String charset, String k,
			Object v, byte[] boundaryPostfixBytes) throws IOException {
		writeLine(to, TWO_HYPENS_BYTES, BOUNDARY_PREFIX_BYTES,
				boundaryPostfixBytes);
		if (v instanceof File) {
			File file = (File) v;
			String filename = file.getName();
			String contentType = HttpURLConnection
					.guessContentTypeFromName(filename);
			if (null == contentType) {
				contentType = "application/octet-stream";
			}
			contentType = contentType.replaceFirst("\\/jpg$", "/jpeg");
			writeLine(to, ("Content-Disposition: form-data; name=\"" + k
					+ "\"; filename=\"" + filename + "\"").getBytes());
			writeLine(to, ("Content-Type: " + contentType).getBytes(),
					END_BYTES);
			writeStreamLineAndCloseIn(new FileInputStream(file), to);
		} else {
			writeLine(to,
					("Content-Disposition: form-data; name=\"" + k + "\"")
							.getBytes(), END_BYTES);
			if (v instanceof InputStream) {
				writeStreamLineAndCloseIn((InputStream) v, to);
			} else {
				byte[] vbuffer;
				if (v instanceof byte[]) {
					vbuffer = (byte[]) v;
				} else {
					vbuffer = String.valueOf(v).getBytes(charset);
				}
				writeLine(to, vbuffer);

			}
		}
	}

	private static void writeLine(OutputStream out, byte[]... bs)
			throws IOException {
		for (byte[] b : bs) {
			out.write(b);
		}
		out.write(END_BYTES);
	}

	private static void writeStreamLineAndCloseIn(InputStream in,
			OutputStream out) throws IOException {
		byte[] buf = new byte[1024];
		int c;
		while ((c = in.read(buf)) >= 0) {
			out.write(buf, 0, c);
		}
		in.close();
		out.write(END_BYTES);
	}

	static Type[] getPrepareCallbackType(final Callback<? extends Object> cb) {
		Type cbc = cb.getClass();
		Type[] type = callbackTypeMap.get(cbc);
		if (type == null) {
			Type callbackType = ReflectUtil.getParameterizedType(cbc,
					Callback.class, 0);
			if (cb instanceof PrepareCallback) {
				type = new Type[] {
						ReflectUtil.getParameterizedType(cbc,
								PrepareCallback.class, 0),callbackType };
			} else {
				type = new Type[] { null,callbackType };
			}
			callbackTypeMap.put(cbc, type);
		}
		return type;
	}

	public static Object openFileStream(URL url) throws IOException {
		String filename = url.getPath();
		if (url.getHost().equals(
				ANDROID_ASSET.substring(1, ANDROID_ASSET.length() - 1))) {
			filename = ANDROID_ASSET + filename.substring(1);
		}
		if (filename.startsWith(ANDROID_ASSET)) {
			return UIO.getApplication().getResources().getAssets()
					.open(filename.substring(ANDROID_ASSET.length()));
		} else {
			return new FileInputStream(url.getPath());
		}
	}

	public static Handler currentHandler() {
		try {
			Looper loop = Looper.myLooper();
			return loop == null ? null : new Handler(loop);
		} catch (Exception e) {
			return null;
		}
	}

	public static NetworkStatistics DEFAULT_NETWORK_STATISTICS = new NetworkStatistics() {

		@Override
		public void onHttpWaitDuration(URL path, long time) {
			logDuration(path, "HttpWaitDuration", time);
		}

		@Override
		public void onHttpConnectDuration(URL path, long time) {
			logDuration(path, "HttpConnectDuration", time);
		}

		@Override
		public void onHttpHeaderDuration(URL path, long time) {
			logDuration(path, "HttpHeaderDuration", time);
		}

		@Override
		public void onHttpCacheDuration(URL path, long time, boolean fromTtl) {
			logDuration(path, "HttpCacheDuration( "
					+ (fromTtl ? "ttl" : "etag") + ")", time);
		}

		@Override
		public void onHttpNetworkDuration(URL path, long time) {
			logDuration(path, "HttpNetworkDuration", time);
		}

		@Override
		public void onHttpCancelDuration(URL path, long time) {
			logDuration(path, "HttpCancelDuration", time);

		}

		@Override
		public void onHttpDownloadError(URL path, Throwable exception) {
			logException(path, "HttpDownloadError", exception);
		}

		@Override
		public void onHttpParseError(URL path, Throwable exception) {

			logException(path, "HttpParseError", exception);
		}

		@Override
		public void onHttpCallbackError(URL path, Throwable exception) {
			logException(path, "HttpCallbackError", exception);
		}

		@Override
		public void onRedirectOut(String domain) {
			DebugLog.info("RedirectOut:" + domain);
		}

		private void logException(URL path, String string, Throwable exception) {
		}

		private void logDuration(URL path, String string, long time) {
		}

	};

	public static Object transform(Object source, Type requiredTye) {
		Class<? extends Object> type = source.getClass();
		if (source == null || type == requiredTye) {
			return source;
		}
		if (type == String.class) {
			String text = (String) source;
			boolean isDebug = DebugLog.isDebug();
			if (isDebug && text.startsWith("array(")) {
				text = "数据异常，请检查后端php是否在调试数据：" + text;
			} else if (isDebug && text.startsWith("<")) {
				text = text.replaceAll("(<.*)[\\s\\S]*(<title>.*)?[\\s\\S]*",
						"$1...$2...");
			} else {
				return JSON_DECODER.decodeObject(text, requiredTye);
			}
			DebugLog.warn(text);
			throw new ExpressionSyntaxException("无效JSON表达式：" + text);
		} else {
			return JSON_DECODER.transform(source, requiredTye);
		}
	}

}

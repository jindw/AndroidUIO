package org.xidea.android.impl.io;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Map;

import org.xidea.android.Callback.Cancelable;
import org.xidea.android.Callback.Cancelable.CanceledException;
import org.xidea.android.impl.ApplicationState;
import org.xidea.android.impl.CommonLog;
import org.xidea.android.impl.io.HttpInterface.HttpRequest;
import org.xidea.android.impl.io.HttpInterface.HttpMethod;


public class HttpRequestImpl implements HttpRequest {
//	private static CommonLog log = CommonLog.getLog(HttpRequestImpl.class);
	private static final int CONNECT_TIMEOUT = 30 * 1000;
	private static final int READ_TIMEOUT = 2 * 60 * 1000;//图片

	static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	@Override
	public URLConnection init(URL url, HttpMethod method,
			Map<String, String> requestHeaders, Cancelable cancelState)
			throws IOException {
		if ("file".equals(url.getAuthority())) {
			return url.openConnection();
		}

		if (CommonLog.isDebug()) {
			try {
				Thread.sleep(0);//(int) (1000 * (Math.random() * 10 + 5)/4));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Proxy proxy = ApplicationState.getInstance().getProxy();
		URLConnection conn = doInit(url, requestHeaders, cancelState, proxy);
		return conn;
	}

	private boolean isWapIp(Proxy proxy) {
		if(proxy != null){
		SocketAddress address = proxy.address();
		if (address  instanceof java.net.InetSocketAddress){
		// 联通移动:10.0.0.172
		// 电信:10.0.0.200
		String hostName = ((InetSocketAddress)address).getHostName();
		return "10.0.0.172".equals(hostName)
				|| "10.0.0.200".equals(hostName);
		}}
		return false;
	}
	private URLConnection doInit(URL url, Map<String, String> requestHeaders,
			Cancelable cancelState, Proxy proxy) throws IOException,
			CanceledException {
		HttpUtil.assertNotCanceled(null, cancelState);
		if(isWapIp(proxy)){
			String cookie = requestHeaders.get("Cookie");
			url = HttpUtil.appendCookieAsQuery(url, cookie);
		}
		URLConnection conn = proxy == null ? url.openConnection() : url.openConnection(proxy);
		HttpUtil.assertNotCanceled(conn, cancelState);
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setRequestProperty("Connection", "Keep-Alive");
		for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
			conn.setRequestProperty(entry.getKey(), entry.getValue());
		}
		return conn;
	}
	@Override
	public void postData(HttpURLConnection conn, Map<String, Object> mutiData,
			Cancelable cancelState) throws IOException {
		conn.setRequestMethod("POST");
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setUseCaches(false);

		String charset = DEFAULT_CHARSET.name();
		conn.setRequestProperty("Charset", charset);
		if (mutiData != null) {
			HttpUtil.addMultiPartPost(conn, mutiData, charset, cancelState);
			conn.setChunkedStreamingMode(0);
		} else {
			//TODO: for performance , not connect string ,simple write to output
			String query =HttpUtil. buildQuery(conn.getURL(), mutiData, charset);
			byte[] data = query == null ? null : query.getBytes();
			conn.setRequestProperty("Content-Length",
					String.valueOf(data.length));
			OutputStream out = conn.getOutputStream();
			conn.setFixedLengthStreamingMode(data.length);
			out.write(data);
			out.flush();
		}

	}
//	private Map<String, Object> lockMap = new HashMap<String, Object>();
//	private Object globalLock = new Object();
//	@Override
//	public Object requireLock(String host) {
//		synchronized (globalLock) {
//			Object lock = lockMap.get(host);
//			if (lock == null) {
//				lockMap.put(host, lock = new Object());
//			}
//			return lock;
//		}
//	}

}
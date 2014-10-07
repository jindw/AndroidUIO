package org.xidea.android.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xidea.android.UIO;
import org.xidea.android.impl.io.IOUtil;
import org.xidea.el.json.JSONDecoder;

import android.content.Context;

public class DemoUtil {
	public static class TestServer {
		public String imagePrefix;
		public String host;
		public int port;
	}

	private static TestServer server;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static String getDefaultIP() {

		return getConfig().host;
	}

	public static TestServer getConfig() {
		try {
			if (server == null) {
				InputStream in = UIO.getApplication().getAssets().open("test-server.json");
				if (in == null) {
					return null;
				}
				String text = IOUtil.loadTextAndClose(in, "utf-8");
				server = JSONDecoder.decode(text, TestServer.class);
			}
			return server;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getTestHost() {
		TestServer config = getConfig();
		return "http://" + config.host + ":"+config.port+"/";
	}

	public static List<String> getImageList() {
		String[] imgUrls = new String[] { "taiji.gif", "taiji1.jpg",
				"taiji2.jpg", "taiji3.jpg" };
		ArrayList<String> list = new ArrayList<String>();

		String urlPrefix = DemoUtil.getTestHost() + "assets/test/";
		for (int i = 0; i < 100; i++) {
			int index = i % imgUrls.length;
			String url = urlPrefix + (imgUrls[index]);
			if (index == 0) {
				url = url + "?token=" + i;
			}
			list.add(url);
		}
		return list;
	}

	public static String getUserURL() {
		return DemoUtil.getTestHost() + "api/user.json";
	}
}

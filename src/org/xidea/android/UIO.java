package org.xidea.android;

import java.io.InputStream;
import java.util.Map;

import org.xidea.android.Callback.Cancelable;
import org.xidea.android.impl.http.HttpSupport;
import org.xidea.android.impl.io.StorageFactory;
import org.xidea.android.impl.ui.ImageSupport;
import org.xidea.android.impl.ui.UIFacade;

import android.app.Application;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class UIO {

	public static Cancelable get(Callback<? extends Object> callback, String url) {
		Ext.assertInit();
		return Ext.http.get(callback, url);
	}

	public static Cancelable post(Callback<? extends Object> callback,
			String url, Map<String, Object> postParams) {
		Ext.assertInit();
		return Ext.http.post(callback, url, postParams);
	}

	public static void bind(ImageView view, String url) {
		Ext.assertInit();
		Ext.image.bind(view, url, null, 0, null);
	}

	public static void bind(ImageView view, String url, int fallbackResource) {
		Ext.assertInit();
		Ext.image.bind(view, url, null, fallbackResource, null);
	}

	public static void bind(ImageView view, String url,
			DrawableFactory factory, int fallbackResource) {
		Ext.assertInit();
		Ext.image.bind(view, url, factory, fallbackResource, null);
	}

	public static void bind(ImageView view, String url,
			DrawableFactory factory, int fallbackResource,
			Callback<Drawable> callback) {
		Ext.assertInit();
		Ext.image.bind(view, url, factory, fallbackResource, callback);
	}

	public static boolean isWifiConnected() {
		Ext.assertInit();
		return Ext.http.isWifiConnected();
	}

	public static boolean isInternetConnected() {
		Ext.assertInit();
		return Ext.http.isInternetConnected();
	}

	public static int getMobileGeneration() {
		Ext.assertInit();
		return Ext.http.getMobileGeneration();
	}

	public static void addWifiCallback(Callback<Boolean> callback) {
		Ext.assertInit();
		Ext.http.addWifiCallback(callback);
	}

	public static boolean removeWifiCallback(Callback<Boolean> callback) {
		Ext.assertInit();
		return Ext.http.removeWifiCallback(callback);
	}

	public static void addConnectedCallback(Callback<Boolean> callback) {
		Ext.assertInit();
		Ext.http.addConnectedCallback(callback);
	}

	public static boolean removeConnectedCallback(Callback<Boolean> callback) {
		Ext.assertInit();
		return Ext.http.removeConnectedCallback(callback);
	}


	public static <T extends KeyValueStorage<T>> T getKeyValueStorage(
			Class<T> type) {
		Ext.assertInit();
		return Ext.storage.getKeyValueStroage(type, Ext.app);
	}

	public static <T> SQLiteMapper<T> getSQLiteStorage(Class<T> entryType) {
		Ext.assertInit();
		return Ext.storage.getSQLiteStorage(entryType, Ext.app);
	}

	public static Cancelable showLongTips(CharSequence message) {
		Ext.assertInit();
		return UIFacade.getInstance().longTips(message);
	}

	public static Cancelable showTips(CharSequence message) {
		Ext.assertInit();
		return UIFacade.getInstance().shortTips(message);
	}

	public static Application getApplication() {
		Ext.assertInit();
		return Ext.app;
	}

	public static class Ext {
		private static Application app = null;
		private static HttpSupport http = HttpSupport.INSTANCE;
		private static ImageSupport image = ImageSupport.INSTANCE;
		private static StorageFactory storage = StorageFactory.INSTANCE;

		private Ext() {
		}

		public static void setRequestHeader(String key, String value) {
			assertInit();
			http.setRequestHeader(key, value);
		}

		public static String loadText(String url, boolean ignoreCache) {
			assertInit();
			return http.loadText(url, ignoreCache);
		}

		public static InputStream loadStream(String url, boolean ignoreCache) {
			assertInit();
			return http.loadStream(url, ignoreCache);
		}

		public static String loadCacheText(String url) {
			assertInit();
			return http.loadCacheText(url);
		}

		public static InputStream loadCacheStream(String url) {
			assertInit();
			return http.loadCacheStream(url);
		}

		public static void updateRequestCache(String url, String content) {
			assertInit();
			http.updateCache(url, content);
		}

		public static void removeRequestCache(String key) {
			assertInit();
			http.removeCache(key);
		}

		private final static void assertInit() {
			if (app == null) {
				throw new IllegalStateException(
						"UIO not inited, please add init code in your application onCreate method!!");
			}
		}

		public static void init(Application application) {
			if (application != null && app == null) {
				app = application;
				http.init(application, 1024 * 1024 * 16);
			}
		}
	}

}

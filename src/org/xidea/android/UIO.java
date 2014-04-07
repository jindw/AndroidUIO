package org.xidea.android;

import java.io.File;
import java.io.InputStream;

import org.xidea.android.Callback.Cancelable;
import org.xidea.android.impl.ApplicationState;
import org.xidea.android.impl.io.HttpImplementation;
import org.xidea.android.impl.io.StorageFactory;
import org.xidea.android.impl.ui.ImageImplement;
import org.xidea.android.impl.ui.UIFacade;

import android.app.Activity;
import android.app.Application;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class UIO {

	public static  Cancelable get(Callback<? extends Object> callback, String url) {
		Ext.assertInit();
		return Ext.http.get(callback, url);
	}
	public static Cancelable post(Callback<? extends Object> callback, String url,
			String key, File mutipart) {
		Ext.assertInit();
		return Ext.http.post(callback, url, key, mutipart);
	}
	public static Cancelable post(Callback<? extends Object> callback, String url,
			String key, InputStream mutipart) {
		Ext.assertInit();
		return Ext.http.post(callback, url, key, mutipart);
	}


	public static void bind(final ImageView view, final String url,
			final DrawableFactory factory,
			final int fallbackResource, final Callback<Drawable> callback){
		Ext.assertInit();
		Ext.image.bind(view, url, factory, fallbackResource, callback);
	}

	public static void bind(final ImageView view, final String url,
			final DrawableFactory factory, 
			final int fallbackResource){
		Ext.assertInit();
		Ext.image.bind(view, url, factory, fallbackResource, null);
	}

	public static void bind(final ImageView view, final String url){
		Ext.assertInit();
		Ext.image.bind(view, url, null, 0, null);
	}
	

	public static boolean isWifiConnected() {
		Ext.assertInit();
		return Ext.appState.isWifiConnected();
	}
	public static boolean isInternetConnected() {
		Ext.assertInit();
		return Ext.appState.isInternetConnected();
	}
	public static int getMobileGeneration() {
		Ext.assertInit();
		return Ext.appState.getMobileGeneration();
	}
	public static Application getApplication() {
		Ext.assertInit();
		return Ext.appState.getApplication();
	}
	public static String getDeviceId() {
		Ext.assertInit();
		return Ext.appState.getDeviceId();
	}
	public static String getVersionName() {
		Ext.assertInit();
		return Ext.appState.getVersionName();
	}
	public static int getVersionCode() {
		Ext.assertInit();
		return Ext.appState.getVersionCode();
	}
	public static void showLongTips(CharSequence message){
		Ext.assertInit();
		UIFacade.getInstance().longTips(message);
	}
	public static void showShortTips(CharSequence message){
		Ext.assertInit();
		UIFacade.getInstance().shortTips(message);
	}

	public static Activity getTopActivity() {
		Ext.assertInit();
		return Ext.appState.getTopActivity();
	}
	public static <T extends KeyValueStorage<T>> T getKeyValueStorage(Class<T> type){
		Ext.assertInit();
		return Ext.storage.getKVStroage(type, Ext.appState.getApplication());
	}
	public static <T> SQLiteMapper<T> getSQLiteStorage(Class<T> entryType) {
		Ext.assertInit();
		return Ext.storage.getSQLiteStorage(entryType, Ext.appState.getApplication());
	}

	public static class Ext{

		private static HttpImplementation http = HttpImplementation.getInstance();
		private static ApplicationState appState = ApplicationState.getInstance();
		private static StorageFactory storage = StorageFactory.INSTANCE;
		private static ImageImplement image = ImageImplement.INSTANCE;
		private static boolean inited;
		private Ext(){}
		public static void setTopActivity(Activity a) {
			assertInit();
			appState.setTopActivity(a);
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

		public static void addWifiCallback(Callback<Boolean> callback) {
			assertInit();
			appState.addWifiCallback(callback);
		}
		public static boolean removeWifiCallback(Callback<Boolean> callback) {
			assertInit();
			return appState.removeWifiCallback(callback);
		}
		public static void addConnectedCallback(Callback<Boolean> callback) {
			assertInit();
			appState.addConnectedCallback(callback);
		}
		public static boolean removeConnectedCallback(Callback<Boolean> callback) {
			assertInit();
			return appState.removeConnectedCallback(callback);
		}

//
//		public static Cancelable dispatchRequest(AsynTask task) {
//			assertInit();
//			return http.dispatchRequest(task);
//		}
		public static void exit(boolean systemExistAfterFinish) {
			assertInit();
			appState.exit(systemExistAfterFinish);
		}
		public static int pauseRequest(Object group) {
			assertInit();
			return http.pause(group);
		}
		public static int resumeRequest(Object group) {
			assertInit();
			return http.resume(group);
		}
		public static int cancelRequest(Object group) {
			assertInit();
			return http.cancel(group);
		}
		public static void updateRequestCache(String url, String content) {
			assertInit();
			http.updateCache(url, content);
		}
		public static void removeRequestCache(String key) {
			assertInit();
			http.removeCache(key);
		}
		private final static void assertInit(){
			if(!inited){
				throw new IllegalStateException("UIO not inited, please add init code in your application onCreate method!!");
			}
		}
		public static void init(Application application){
			if(application!= null && !inited){
				appState.init(application);
				http.init(application, 1024 * 1024 * 16);
				inited = true;
			}
		}
	}
	
}

package org.xidea.android;

import java.io.File;
import java.io.InputStream;

import org.xidea.android.Callback.Cancelable;
import org.xidea.android.impl.ApplicationState;
import org.xidea.android.impl.io.HttpImplementation;
import org.xidea.android.impl.io.HttpInterface.AsynTask;
import org.xidea.android.impl.io.StorageFactory;
import org.xidea.android.impl.ui.ImageImplement;

import android.app.Activity;
import android.app.Application;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class UIO {
	private static HttpImplementation http = HttpImplementation.getInstance();
	private static ApplicationState appState = ApplicationState.getInstance();
	private static StorageFactory storage = StorageFactory.INSTANCE;
	private static ImageImplement image = ImageImplement.INSTANCE;
	private static boolean inited;
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
	public static  Cancelable get(Callback<? extends Object> callback, String url) {
		assertInit();
		return http.get(callback, url);
	}
	public static Cancelable post(Callback<? extends Object> callback, String url,
			String key, File mutipart) {
		assertInit();
		return http.post(callback, url, key, mutipart);
	}
	public static Cancelable post(Callback<? extends Object> callback, String url,
			String key, InputStream mutipart) {
		assertInit();
		return http.post(callback, url, key, mutipart);
	}
	public static Cancelable dispatchRequest(AsynTask task) {
		assertInit();
		return http.dispatchRequest(task);
	}

	public static void bind(final ImageView view, final String url,
			final DrawableFactory factory,
			final int fallbackResource, final Callback<Drawable> callback){
		assertInit();
		image.bind(view, url, factory, fallbackResource, callback);
	}

	public static void bind(final ImageView view, final String url,
			final DrawableFactory factory, 
			final int fallbackResource){
		assertInit();
		image.bind(view, url, factory, fallbackResource, null);
	}

	public static void bind(final ImageView view, final String url){
		assertInit();
		image.bind(view, url, null, 0, null);
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
	public static void setRequestHeader(String key, String value) {
		assertInit();
		http.setRequestHeader(key, value);
	}
	public static void updateRequestCache(String url, String content) {
		assertInit();
		http.updateCache(url, content);
	}
	public static void removeRequestCache(String key) {
		assertInit();
		http.removeCache(key);
	}
	public static boolean isWifiConnected() {
		assertInit();
		return appState.isWifiConnected();
	}
	public static boolean isInternetConnected() {
		assertInit();
		return appState.isInternetConnected();
	}
	public static int getMobileGeneration() {
		assertInit();
		return appState.getMobileGeneration();
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
	public static Application getApplication() {
		assertInit();
		return appState.getApplication();
	}
	public static String getDeviceId() {
		assertInit();
		return appState.getDeviceId();
	}
	public static String getVersionName() {
		assertInit();
		return appState.getVersionName();
	}
	public static int getVersionCode() {
		assertInit();
		return appState.getVersionCode();
	}
	public static void setTopActivity(Activity a) {
		assertInit();
		appState.setTopActivity(a);
	}
	public static Activity getTopActivity() {
		assertInit();
		return appState.getTopActivity();
	}
	public static void exit(boolean systemExistAfterFinish) {
		assertInit();
		appState.exit(systemExistAfterFinish);
	}
	public static <T extends KeyValueStorage<T>> T getKeyValueStorage(Class<T> type){
		assertInit();
		return storage.getKVStroage(type, appState.getApplication());
	}
	public static <T> SQLiteMapper<T> getSQLiteStorage(Class<T> entryType) {
		assertInit();
		return storage.getSQLiteStorage(entryType, appState.getApplication());
	}

}

package org.xidea.android;

import java.io.File;
import java.io.InputStream;

import org.xidea.android.impl.ApplicationState;
import org.xidea.android.impl.io.HttpImplementation;
import org.xidea.android.impl.io.HttpInterface;
import org.xidea.android.impl.io.HttpInterface.AsynTask;
import org.xidea.android.impl.io.HttpInterface.NetworkStatistics;

import android.app.Activity;
import android.app.Application;

public class UIO {
	private static HttpInterface http = new HttpImplementation();
	private static ApplicationState appState = ApplicationState.getInstance();
	public static void init(Application application){
		appState.init(application);
	}
	public static  void get(Callback<? extends Object> callback, String url) {
		http.get(callback, url);
	}
	public static void post(Callback<? extends Object> callback, String url,
			String key, File mutipart) {
		http.post(callback, url, key, mutipart);
	}
	public static void post(Callback<? extends Object> callback, String url,
			String key, InputStream mutipart) {
		http.post(callback, url, key, mutipart);
	}
	public static void dispatchRequest(AsynTask task) {
		http.dispatchRequest(task);
	}
	public static String loadText(String url, boolean ignoreCache) {
		return http.loadText(url, ignoreCache);
	}
	public static InputStream loadStream(String url, boolean ignoreCache) {
		return http.loadStream(url, ignoreCache);
	}
	public static String loadCacheText(String url) {
		return http.loadCacheText(url);
	}
	public static InputStream loadCacheStream(String url) {
		return http.loadCacheStream(url);
	}
	public static int pause(Object group) {
		return http.pause(group);
	}
	public static int resume(Object group) {
		return http.resume(group);
	}
	public static int cancel(Object group) {
		return http.cancel(group);
	}
	public static void setRequestHeader(String key, String value) {
		http.setRequestHeader(key, value);
	}
	public static String getRequestHeader(String key) {
		return http.getRequestHeader(key);
	}
	public static void updateCache(String url, String content) {
		http.updateCache(url, content);
	}
	public static void removeCache(String key) {
		http.removeCache(key);
	}
	public static void setStatistics(NetworkStatistics networkStatistics) {
		http.setStatistics(networkStatistics);
	}
	public static boolean isWifiConnected() {
		return appState.isWifiConnected();
	}
	public static boolean isInternetConnected() {
		return appState.isInternetConnected();
	}
	public static int getMobileGeneration() {
		return appState.getMobileGeneration();
	}
	public static void addWifiCallback(Callback<Boolean> callback) {
		appState.addWifiCallback(callback);
	}
	public static boolean removeWifiCallback(Callback<Boolean> callback) {
		return appState.removeWifiCallback(callback);
	}
	public static void addConnectedCallback(Callback<Boolean> callback) {
		appState.addConnectedCallback(callback);
	}
	public static boolean removeConnectedCallback(Callback<Boolean> callback) {
		return appState.removeConnectedCallback(callback);
	}
	public static Application getApplication() {
		return appState.getApplication();
	}
	public static String getDeviceId() {
		return appState.getDeviceId();
	}
	public static String getVersionName() {
		return appState.getVersionName();
	}
	public static int getVersionCode() {
		return appState.getVersionCode();
	}
	public static void setTopActivity(Activity a) {
		appState.setTopActivity(a);
	}
	public static Activity getTopActivity() {
		return appState.getTopActivity();
	}
	public static void exit(boolean systemExistAfterFinish) {
		appState.exit(systemExistAfterFinish);
	}

}

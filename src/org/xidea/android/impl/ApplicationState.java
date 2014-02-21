package org.xidea.android.impl;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;

import org.xidea.android.Callback;

import java.net.Proxy;

public class ApplicationState {
	// private static org.xidea.android.CommonLog log =
	// org.xidea.android.CommonLog.getLog();
	private static ApplicationState instance = new ApplicationState();

	public static ApplicationState getInstance() {
		return instance;
	}

	protected Application application;
	private String versionName;
	private int versionCode;
	private String deviceId;

	public void init(final Application application) {
		this.application = application;
	}

	private NetworkState ns;
	private Object lock = new Object();

	private NetworkState ns() {
		if (ns == null) {
			synchronized (lock) {
				if (ns == null) {
					ns = new NetworkState(application);
				}
			}
		}
		return ns;
	}

	public boolean isWifiConnected() {
		if (application == null) {// for pc test
			return true;
		}
		return ns().isWifiConnected();
	}

	public boolean isInternetConnected() {
		if (application == null) {// for pc test
			return true;
		}
		return ns().isInternetConnected();
	}

	public Proxy getProxy() {
		return ns().getProxy();
	}

	public int getMobileGeneration() {
		return ns().getMobileNetworkClass();
	}

	public void addWifiCallback(Callback<Boolean> callback) {
		ns().addConnectedCallback(callback);
	}

	public boolean removeWifiCallback(Callback<Boolean> callback) {
		return ns().removeWifiCallback(callback);
	}

	public void addConnectedCallback(Callback<Boolean> callback) {
		ns().addConnectedCallback(callback);
	}

	public boolean removeConnectedCallback(Callback<Boolean> callback) {
		return ns().removeConnectedCallback(callback);
	}

	public Application getApplication() {
		return this.application;
	}

	public String getDeviceId() {
		if (deviceId == null) {
			TelephonyManager telephonyManager = (TelephonyManager) application
					.getSystemService(Context.TELEPHONY_SERVICE);
			deviceId = telephonyManager.getDeviceId();
		}
		return deviceId;
	}

	public String getVersionName() {
		initVersion();
		return versionName;
	}

	public int getVersionCode() {
		initVersion();
		return versionCode;
	}

	private void initVersion() {
		if (versionName == null) {
			try {
				PackageInfo pinfo = application.getPackageManager()
						.getPackageInfo(application.getPackageName(),
								PackageManager.GET_CONFIGURATIONS);
				versionName = pinfo.versionName;
				versionCode = pinfo.versionCode;
			} catch (NameNotFoundException e) {
				versionName = "";
			}
		}
	}

	private Activity top;

	public void setTopActivity(Activity a) {
		if (top != null) {
			// ImageUtil.getInstance().clear(top);
		}
		top = a;
	}

	public Activity getTopActivity() {
		return top;
	}

	public void exit(boolean systemExistAfterFinish) {
		// TODO: 清理所有 activity...
		System.exit(0);
	}

	// public void closeActivity(Class<? extends Activity> type) {
	// //TODO: 清理 指定类型的全部Acticity
	// }

}

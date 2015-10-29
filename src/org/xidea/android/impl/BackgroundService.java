package org.xidea.android.impl;


import java.util.ArrayList;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemClock;

public class BackgroundService extends Service {
	final static String ACTION = BackgroundService.class.getName()+".Action";
	private ServiceBroadcastReceiver testServiceReceiver = new ServiceBroadcastReceiver();
	private AlarmManager mAlarmManager;
	private PendingIntent mPendingIntent;
	static Service instance;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION);
		registerReceiver(testServiceReceiver, intentFilter);
		startAlarm();
		instance = this;
		super.onCreate();
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void startAlarm() {
		Intent intent = new Intent();
		intent.setAction(ACTION);
		mPendingIntent = PendingIntent.getBroadcast(BackgroundService.this, 1,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		long firstime = SystemClock.elapsedRealtime();
		mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				firstime, 10 * 1000, mPendingIntent); // 10秒一个周期，不停的发送广播
		
		//mAlarmManager.setWindow(type, windowStartMillis, windowLengthMillis, operation);
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(testServiceReceiver);
		super.onDestroy();
	}

	private class ServiceBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (context != null) {
				if (!isServiceRunning(context)) {
					Intent service = new Intent(context,
							BackgroundService.class);
					context.startService(service);
				}
				// Toast.makeText(context, "BackgroundService Running...",
				// Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * 在开启服务之前应该判断该服务知否已经在运行
	 * 判断自己的Service-->BackgroundService是否已经运行
	 * 
	 * @param context
	 * @return
	 */
	static boolean isServiceRunning(Context context) {
		try {
			ActivityManager myManager = (ActivityManager) context
					.getSystemService(Context.ACTIVITY_SERVICE);
			ArrayList<RunningServiceInfo> runningService = (ArrayList<RunningServiceInfo>) myManager
					.getRunningServices(30);

			for (int i = 0; i < runningService.size(); i++) {
				RunningServiceInfo rService = runningService.get(i);
				if (rService.service.getClassName().equals(
						BackgroundService.class.getName())) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
//
//	private static WeakHashMap<Module, Boolean> keepAliveModules = new WeakHashMap<Module, Boolean>();
//
//	public static void setKeepAlive(Module module, boolean keepAlive) {
//		if (keepAlive) {
//			keepAliveModules.put(module, true);
//		} else {
//			keepAliveModules.remove(module);
//		}
//		int size2 = keepAliveModules.size();
//		Application app = CC.getApplication();
//		if (size2 > 0) {
//			if (!isServiceRunning(app)) {
//				Intent service = new Intent(app,
//					BackgroundService.class);
//				app.startService(service);
//			}
//		}else if(size2 ==0 ){
//			if (isServiceRunning(app)) {
//				//TODO:stop
//				Intent service = new Intent(app,BackgroundService.class);
//				app.stopService(service);
//			}
//		}
//	}

}

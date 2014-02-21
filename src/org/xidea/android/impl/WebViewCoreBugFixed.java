package org.xidea.android.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.xidea.android.CommonLog;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.webkit.WebView;

/**
 * System Bug Fixed Utility
 * 
 * <pre>
 * Fixed 4.0.3/4.0.4 system bug:
 *  java.lang.SecurityException: No permission to modify given thread
 *      at android.os.Process.setThreadPriority(Native Method)
 * 	 at android.webkit.WebViewCore$WebCoreThread$1.handleMessage(WebViewCore.java:805)
 * 	 at android.os.Handler.dispatchMessage(Handler.java:99)
 * 	 at android.os.Looper.loop(Looper.java:137)
 * 	 at android.webkit.WebViewCore$WebCoreThread.run(WebViewCore.java:877)
 * 	 at java.lang.Thread.run(Thread.java:856)
 * </pre>
 * @author huangweigan
 * 
 */
public class WebViewCoreBugFixed {
	private static final Log log = CommonLog.getLog();
	static {
		//4.0.0 4.0.3/4.0.4 rom
		switch (Build.VERSION.SDK_INT){
		case 14://Build.VERSION_CODES.ICE_CREAM_SANDWICH) {//Android 4.0.0
		case 15://Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {//Android 4.0.3.
			fixedWebCoreHandler();
		}
	}
	public static void load(){}

	private static void fixedWebCoreHandler() {
		try {
			Class<?> clazz = Class.forName("android.webkit.WebViewCore");
			Field sWebCoreHandlerField = clazz
					.getDeclaredField("sWebCoreHandler");
			sWebCoreHandlerField.setAccessible(true);
			Handler sWebCoreHandler = (Handler) sWebCoreHandlerField.get(null);
			//TODO:... maybe null,how to init ?
			Method getIMessengerMethod = Handler.class
					.getDeclaredMethod("getIMessenger");
			getIMessengerMethod.setAccessible(true);
			Object mMessager = getIMessengerMethod.invoke(sWebCoreHandler);
			Handler sWrapHandler = new WrapHandler(sWebCoreHandler);
			if (mMessager != null) {
				Field mMessengerField = Handler.class
						.getDeclaredField("mMessenger");
				mMessengerField.setAccessible(true);
				mMessengerField.set(sWrapHandler, mMessager);
			}
			sWebCoreHandlerField.set(null, sWrapHandler);
		} catch (Exception e) {

		}
	}

	private static class WrapHandler extends Handler {
		Handler handler;

		public WrapHandler(Handler handler) {
			super(handler.getLooper());
			this.handler = handler;
		}

		@Override
		public void handleMessage(Message msg) {
			try {
				handler.handleMessage(msg);
			} catch (Exception e) {
				log.debug("java.lang.SecurityException: No permission to modify given thread");
			}
		}
	}
}

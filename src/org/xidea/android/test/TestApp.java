package org.xidea.android.test;

import java.io.PrintStream;

import org.xidea.android.UIO;

import android.app.Application;

public class TestApp extends Application {
	public void onCreate() {
		super.onCreate();
		System.setErr(new PrintStream(System.err) {
			public void print(String txt) {
				// new Exception().printStackTrace(System.out);
				super.print(txt);
			}
		});
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			Thread.UncaughtExceptionHandler defaultHandler = Thread
					.getDefaultUncaughtExceptionHandler();

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				ex.printStackTrace();
				defaultHandler.uncaughtException(thread, ex);
			}
		});
		UIO.Ext.init(this);

	}

}

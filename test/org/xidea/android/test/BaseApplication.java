package org.xidea.android.test;

import org.xidea.android.UIO;

import android.app.Application;

public class BaseApplication extends Application{

	@Override
	public void onCreate() {
		UIO.Ext.init(this);
		super.onCreate();
	}


}

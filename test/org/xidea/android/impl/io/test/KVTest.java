package org.xidea.android.impl.io.test;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;

import org.robolectric.RobolectricTestRunner;
import org.xidea.android.UIO;
import org.xidea.android.KeyValueStorage;


@RunWith(RobolectricTestRunner.class)
public class KVTest {
	public static interface TestConfig extends KeyValueStorage<TestConfig> {
		public int getInt();
		public void setInt(int v);
	}

	@Test
	public void test() {
		Application app = new Activity().getApplication();
		//UIO.init(app);
		TestConfig config = UIO.getKeyValueStorage(TestConfig.class);
		int i = config.getInt();

		long t1 = System.nanoTime();
		config.setInt(config.getInt());
		int timeUsed0 = -(int)(t1 - (t1=System.nanoTime()));
		config.getInt();
		int timeUsed1 = -(int)(t1 - (t1=System.nanoTime()));
		config.setInt(12);
		int timeUsed2 = -(int)(t1 - (t1=System.nanoTime()));

		HashMap<Object, Object> map = new HashMap<Object, Object>();
		int v = 0;
		for(i=128;i>0;i--){
			map.put(i, i);
		} 

		t1 = System.nanoTime();
		for(i=2000;i>0;i--){
			map.get("");
		}
		System.out.println("key value time test");
		int  timeUsed = (int)(System.nanoTime() - t1);
		System.out.println(timeUsed0/1000f/1000);
		System.out.println(timeUsed1/1000f/1000);
		System.out.println(timeUsed2/1000f/1000);
		System.out.println(timeUsed);
		Assert.assertTrue("第一次不能跑的太慢啊，最多给你5毫秒！！",timeUsed0<1000*1000*1000*5);
		Assert.assertTrue("后续就更不能慢了，不能跑的太慢啊！！",timeUsed1<timeUsed && timeUsed2<timeUsed);
		
		
	}

	public void test(Application app) {
		// UIO.init(app);
		SharedPreferences sp = app.getSharedPreferences("test", 0);
		sp.edit().putInt("a", 1).commit();
		//sp = app.getSharedPreferences("test", 0);
		TestConfig config = UIO.getKeyValueStorage(TestConfig.class);
		//int v = config.getInt();

		HashMap<Object, Object> map = new HashMap<Object, Object>();
		for (int k = 128; k > 0; k--) {
			map.put(k, k);
		}

		int G = 10000;
		for (int i = 10; i > 0; i--) {

			long timeUsed_get = 0, timeUsed_get_set = 0, timeUsed_set = 0,timeUsed_native_get=0,timeUsed_native_get2=0,timeUsed_native_put=0;
			
			long t1 = System.nanoTime();

for(int x= G;x>0;x--){
			config.getInt();
			config.getInt();
			config.getInt();
			config.getInt();
			config.getInt();
			config.getInt();
			config.getInt();
			config.getInt();
			config.getInt();
			config.getInt();
		}
			
			
			timeUsed_get -=  (t1 - (t1 = System.nanoTime()));

			config.setInt(config.getInt());
			timeUsed_get_set -= (int) (t1 - (t1 = System.nanoTime()));
			config.setInt(12);
			timeUsed_set -= (int) (t1 - (t1 = System.nanoTime()));

for(int x= G;x>0;x--){
			sp.getInt("a", -1);
			sp.getInt("a", -1);
			sp.getInt("a", -1);
			sp.getInt("a", -1);
			sp.getInt("a", -1);
			sp.getInt("a", -1);
			sp.getInt("a", -1);
			sp.getInt("a", -1);
			sp.getInt("a", -1);
			sp.getInt("a", -1);
}

			timeUsed_native_get -=  (t1 - (t1=System.nanoTime()));
			sp.getInt("a", -1);
			timeUsed_native_get2 -= (int)  (t1 - (t1=System.nanoTime()));
			sp.edit().putInt("a", -1).commit();
			timeUsed_native_put -= (int)  (t1 - (t1=System.nanoTime()));
			

			System.err.println("\r\n----");
			System.err.println("get_set:\t"+timeUsed_get_set);// / 1000d / 1000);
			System.err.println("get_only:\t"+timeUsed_get);// / 1000d / 1000);
			System.err.println("set_only:\t"+timeUsed_set);// / 1000d / 1000);
			System.err.println("raw get:\t"+timeUsed_native_get);// / 1000d / 1000);
			System.err.println("raw get2:\t"+timeUsed_native_get2);// / 1000d / 1000);
			System.err.println("raw put:\t"+timeUsed_native_put);// / 1000d / 1000);

			System.err.println("get rate:"+(timeUsed_get*1.0/timeUsed_native_get));
			System.err.println("put rate:"+(timeUsed_set*1.0/timeUsed_native_put));
		}

		/*
		 * ( 3973): Using default keymap: /system/usr/keychars/qwerty.kcm.bin
		 * W/System.err( 3973): 202.484135 W/System.err( 3973):
		 * 1.9226079999999999 W/System.err( 3973): 113.159178 W/System.err(
		 * 3973): 27.313233
		 */
		// assertTrue("第一次不能跑的太慢啊，最多给你5毫秒！！",timeUsed0<1000*1000*1000*5);
		// assertTrue("后续就更不能慢了，不能跑的太慢啊！！",timeUsed1<timeUsed &&
		// timeUsed2<timeUsed);

	}

}

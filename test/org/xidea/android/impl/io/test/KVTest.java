package org.xidea.android.impl.io.test;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xidea.android.KeyValueStorage;
import org.xidea.android.UIO;

import android.app.Activity;
import android.app.Application;

import org.robolectric.RobolectricTestRunner;

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
		int  timeUsed = (int)(System.nanoTime() - t1);
		System.out.println(timeUsed0/1000f/1000);
		System.out.println(timeUsed1/1000f/1000);
		System.out.println(timeUsed2/1000f/1000);
		System.out.println(timeUsed);
		Assert.assertTrue("第一次不能跑的太慢啊，最多给你5毫秒！！",timeUsed0<1000*1000*1000*5);
		Assert.assertTrue("后续就更不能慢了，不能跑的太慢啊！！",timeUsed1<timeUsed && timeUsed2<timeUsed);
		
		
	}

}

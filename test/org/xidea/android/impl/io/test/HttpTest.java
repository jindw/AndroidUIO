package org.xidea.android.impl.io.test;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.robolectric.RobolectricTestRunner;
import org.xidea.android.Callback;
import org.xidea.android.UIO;
import org.xidea.el.json.JSONDecoder;

import android.widget.ImageView;


@RunWith(HttpTest.RobolectricTestRunner2.class)
public class HttpTest {
	public static class RobolectricTestRunner2 extends RobolectricTestRunner{

		public RobolectricTestRunner2(Class<?> arg0) throws InitializationError {
			super(arg0);
		}

		@Override
		public void internalAfterTest(Method method) {
			// TODO Auto-generated method stub
			super.internalAfterTest(method);
		}

		@Override
		protected Statement methodBlock(FrameworkMethod method) {
			// TODO Auto-generated method stub
			return super.methodBlock(method);
		}
		
	}

	@Test
	public void test() throws InterruptedException {
		UIO.get(new Callback<String>(){

			@Override
			public void callback(String result) {
				System.out.println(result);
			}

			@Override
			public void error(Throwable ex, boolean callbackError) {
				ex.printStackTrace();
			}
			
		}, "http://ai-iknow-fmon00.ai01.baidu.com:8882/mobileapi/search/v2/search?query=php");
		Thread.sleep(1000);
			//Assert.assertEquals(expected, actual)
	}

}

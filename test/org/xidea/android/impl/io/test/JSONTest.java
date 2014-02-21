package org.xidea.android.impl.io.test;

import java.util.List;

import org.junit.Test;
import org.xidea.el.json.JSONDecoder;

import com.google.gson.Gson;

public class JSONTest {

	public static class User {
		List<Entry1<Entry2>> list;
	}

	public static class Entry1<T> {
		List<Entry2> list;
	}

	public static class Entry2 {
		String text = "123";
		long time;
	}

	@Test
	public void test() {

		String source = "{\"list\":[{\"list\":[{\"list\":[],\"time\":12000000000000,\"a\":1}]}]}";
		long s1 = 0;
		long s2 = 0;
		long s3 = 0;
		for (int i = 0; i < 10; i++) {
			Gson gson = new Gson();
			long t1 = System.nanoTime();
			User users = gson.fromJson(source, User.class);
			long t2 = System.nanoTime();
			s1 = s1 + (t2 - t1);
			System.out.println(s1);
			 System.out.println(users.list.get(0).list.get(0).time);

			JSONDecoder jsonDecoder = new JSONDecoder(true);
			t1 = System.nanoTime();
			users = jsonDecoder.decode(source, User.class);
			t2 = System.nanoTime();
			s2 = s2 + (t2 - t1);
			s3 = (t2 - t1);
			System.out.println(s2);
			System.out.println("--" + s1 + '/' + s2+'/'+s3);
			 System.out.println(users.list.get(0).list.get(0).time);
		}
		// JsonDeserializationContext.deserialize();
		System.out.println("###");
		System.out.println(s1);
		System.out.println(s2);
	}

}

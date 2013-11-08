package org.xidea.android.test;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Before;

public class BaseTest {
	static ByteArrayOutputStream content = new ByteArrayOutputStream();
	static {
		try {
			final PrintStream err = System.err;
			final PrintStream out = System.out;
			System.setOut(new PrintStream(new OutputStream() {
				
				@Override
				public void write(int oneByte) throws IOException {
					out.write(oneByte);
					content.write(oneByte);
				}
			}, true,"UTF-8"));
			System.setErr(new PrintStream(new OutputStream() {
				
				@Override
				public void write(int oneByte) throws IOException {
					err.write(oneByte);
					content.write(oneByte);
				}
			}, true,"UTF-8"));
		} catch (UnsupportedEncodingException e) {
		}
		
	}


	@Before
	public void prepare(){
		content.reset();
	}
	public void assertRegexp(String pattern){
		String text;
		try {
			text = content.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		Assert.assertTrue("日志匹配失败：\n"+pattern+"\n"+text,text.matches(pattern));
	}
}

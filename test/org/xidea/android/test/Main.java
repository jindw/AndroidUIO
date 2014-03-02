package org.xidea.android.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xidea.android.impl.ui.GifDecoder;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

public class Main {

	static Object lock = new Object();
	public static void test() throws Exception {
		GifDecoder gd = new GifDecoder(new FileInputStream("/Users/jinjinyun/Documents/workspace/AndroidUIO/test/org/xidea/android/impl/io/test/Gif-1.gif"));
		gd.getDelay();
		System.out.println(gd.isAnimate());
		System.exit(0);
		
		StringBuilder buf2 = new StringBuilder();
		for(int i = 10;i<=0x12;i++){
			buf2.append((char)i);
		}
	DocumentBuilder db = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
	Document d = db.parse(new ByteArrayInputStream("<xml/>".getBytes()));
	String text = "\r\n"; buf2.toString();
	System.out.println(text);
	d.getDocumentElement().setTextContent(text);//.appendChild(d.createTextNode(text));
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	Result result = new javax.xml.transform.stream.StreamResult(out );
	javax.xml.transform.TransformerFactory.newInstance().newTransformer().transform(new DOMSource(d.getDocumentElement()), result );

	System.out.println(out.toString("UTF-8"));
	Document d2 = db.parse(new ByteArrayInputStream(out.toByteArray()));
	String source2 = d2.getDocumentElement().getTextContent();
	System.out.println(source2);
	for(int i = 0;i<source2.length();i++){
		if(i != source2.charAt(i)){
			System.out.println((int)source2.charAt(i));
		}
	}
	System.exit(1);
	}
	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws Exception {
		test();
		//Looper.loop();
		try{
			//new Application();
			byte[] buf = new byte[32];
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			//URL u = new URL("http://mapdownload.autonavi.com/mobileapk/amap_android.apk");
			URL u = new URL("http://news.sina.com.cn/");
			URLConnection conn = u.openConnection();
			conn.addRequestProperty("Accept-Encoding", "gzip");
			conn.setRequestProperty("Range", "bytes=141320-141325/141325");
			System.out.println(conn.getRequestProperties());
			InputStream in = conn.getInputStream();
			int len;
			while((len= in.read(buf))>=0){
			out.write(buf,0,len);
			}
			//System.out.println(len);
			//System.out.println(new String(buf));
			System.out.println(conn.getHeaderFields());
			System.out.println(conn.getHeaderField("Content-Encoding"));
			System.out.println(conn.getHeaderField("Charset"));
			in.close();
			System.out.println(out.toString("GBK"));
			
			try{
				System.out.println(1);
				throw new RuntimeException("aaa");
			}finally{
				System.out.println(2);
				//throw new RuntimeException("bbb");
			}
		}catch(Exception e){
			e.printStackTrace(System.out);
		}
		
		Class.forName("org.xidea.android.test.Static");
	}

}
class Static{
	
	static{
		System.out.println(111111);
	}
}

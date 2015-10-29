package org.xidea.android.impl.io.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xidea.android.impl.http.HttpSupportImpl;
import org.xidea.android.impl.io.IOUtil;

public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if(true){
			try{
				try{
					throw new RuntimeException("E1");
				}finally{
					//throw new RuntimeException("E2");
					System.out.println("###");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			return;
		}
		if(true){
			
			File path = new File("/Users/jinjinyun/Documents/workspace/AndroidUIO/test/org/xidea/android/impl/io/testGif-1.gif");
			byte[] data = IOUtil.loadBytesAndClose(new FileInputStream(path));
			byte[] find = "NETSCAPE2.0".getBytes();
			for(int i=0;i<data.length-find.length;i++){
				boolean hit = true;
				for(int j=0;j<find.length;j++){
					if(data[i+j] != find[j]){
						hit = false;
						break;
					}
				}
				if(hit){
					data[i] = '.';
					System.out.println("hit");
				}
			}
			FileOutputStream out = new FileOutputStream(new File(path.getParentFile(),"out.gif"));
			out.write(data);
			out.close();
//			String text = new String(data,"ascii");
//			int p = text.indexOf("NETS");
//			System.out.println(text.substring(p,p+20));
			
			return;
		}
		HttpSupportImpl http = HttpSupportImpl.INSTANCE;
		File cacheDir = new File("bin/cache");
		System.out.println(cacheDir.toURI());
		http.init(null, 10000);
		try{
			Class.forName(" org.xidea.android.impl.http.HttpUtil").getMethod("startCacheAsyn", HttpSupportImpl.class).invoke(null, http);
		}catch(Throwable e){}
//		String content = http.loadText("http://www.sina.com.cn", true);
//		//content = http.getText("http://www.sina.com.cn", true);
//		System.out.println(1);
//		System.out.println(content);
		final Thread t1 = new Thread(){
			int i=0;
			public void run(){
				while(true){i++;
				if(this.isInterrupted()){
					System.out.println(i);
				}
				}
			}
		};
		System.out.println("000");
		t1.start();
		new Thread(){
			public void run(){
				try {
					Thread.sleep(1000 *3);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				t1.interrupt();
				
			}
		}.start();
		
	}

	
}

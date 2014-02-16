package org.xidea.android.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import org.xidea.android.UIO;


import dalvik.system.DexClassLoader;

import android.os.Bundle;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;

public class HostActivity extends FragmentActivity {

	private Resources resource;
	private AssetManager assetManager;
	private DexClassLoader dexClassLoader;
	private LayoutInflater inflater;
	public HostActivity() {
		try {
			assetManager = AssetManager.class.newInstance();
			Method addAssetPath = AssetManager.class.getDeclaredMethod(
					"addAssetPath", String.class);
			addAssetPath.setAccessible(true);

			File pluginDir = new File("/data/data/"+UIO.getApplication().getPackageName()+"/plugin");
			if (!pluginDir.exists()) {
				pluginDir.mkdirs();
			}
			File plugin = new File(pluginDir, "Assistant.apk");
			File source = new File("/mnt/sdcard/Assistant.apk");
			moveTo(plugin, source);
			addAssetPath.invoke(assetManager, plugin.toString());

			dexClassLoader  = new DexClassLoader(
					plugin.getAbsolutePath(), pluginDir.getAbsolutePath(),
					null, HostActivity.class.getClassLoader());
			

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public Resources getResources() {
		if(resource == null){
			resource = super
					.getResources();
			resource = new Resources(assetManager, resource.getDisplayMetrics(),resource
					.getConfiguration());
		}
		return resource;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		testPlugin();
	}


	public LayoutInflater getLayoutInflater() {
		if(inflater == null){
			LayoutInflater oldInflater = getWindow().getLayoutInflater();
			inflater = oldInflater.cloneInContext(this);
		}
		return inflater;
	}

	public void testPlugin() {
		try {
//			String libPath = "/data/data/com.example.ahost/lib";
			{
				Class clazz = dexClassLoader
						.loadClass("org.xidea.assistant.client.MainActivity");
				Method method = clazz.getDeclaredMethod("init",
						FragmentActivity.class, Resources.class);
				method.setAccessible(true);
				method.invoke(null, this, getResources());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void moveTo(File plugin, File source) throws FileNotFoundException,
			IOException {
		if (source.exists()) {
			System.err.println("begin move");
			byte[] buf = new byte[1024];
			FileInputStream in = new FileInputStream(source);
			FileOutputStream out = new FileOutputStream(plugin);
			int c;
			while ((c = in.read(buf)) >= 0) {
				out.write(buf, 0, c);
			}
			in.close();
			out.close();
			// boolean result = source.renameTo(plugin);
			System.err.println("move result:" + plugin.exists());
		}
	}
}

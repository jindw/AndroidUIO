package org.xidea.android.util;

import java.io.ByteArrayOutputStream;

import org.xidea.android.UIO;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Debug;
import android.util.Base64;
import android.util.Log;


public class DebugLog {

	private static Boolean debug = null;;
	private static Boolean released = null;
	private static String releaseFingerprints = "";//check if has the secure problems

	public static boolean isDebug() {
		if(debug == null){
			debug = !isRelease() || isDebugging();
		}
		return debug;
	}

	private static boolean isDebugging() {
		try{
			return Debug.isDebuggerConnected();
		}catch(Throwable e){
			return true;
		}
	}
	
	public static boolean isRelease(){
		if (released == null) {
			// TODO: 通过签名判断是否是发布状态。
			Application application = UIO.getApplication();
			String md5Fingerprint=null;
			try {
				PackageInfo info = application.getPackageManager()
						.getPackageInfo(application.getPackageName(),
								PackageManager.GET_SIGNATURES);
				ByteArrayOutputStream out = new ByteArrayOutputStream();

				if( info.signatures!=null){
				for(Signature sig : info.signatures){
					out.write(sig.toByteArray());
				}
				}
				md5Fingerprint = Base64.encodeToString(out.toByteArray(), 0);
			} catch (Exception e) {
				released = false;
				warn(e);
			} finally {
				released = releaseFingerprints .equals(md5Fingerprint);
			}
		}

		return released;
	}
	private static int logLevel = Log.VERBOSE;

	private static String generateTag(StackTraceElement caller) {
		String callerClazzName = caller.getClassName();
		String tag = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
		return tag.length() > 23 ? tag.substring(0, 20) + "..." : tag;
	}

	public static void trace(Object msg, Throwable th) {
		log(Log.VERBOSE, msg, th);
	}

	public static void trace(Object msg) {
		log(Log.VERBOSE, msg, null);
	}

	public static void debug(Object msg, Throwable th) {
		log(Log.DEBUG, msg, th);
	}

	public static void debug(Object msg) {
		log(Log.DEBUG, msg, null);
	}

	public static void info(Object msg, Throwable th) {
		log(Log.INFO, msg, th);
	}

	public static void info(Object msg) {
		log(Log.INFO, msg, null);
	}

	public static void warn(Object msg, Throwable th) {
		log(Log.WARN, msg, th);
	}

	public static void warn(Object msg) {
		log(Log.WARN, msg, null);
	}

	public static void error(Object msg, Throwable th) {
		log(Log.ERROR, msg, th);
	}

	public static void error(Object msg) {
		log(Log.ERROR, msg, null);
	}

	public static void fatal(Object msg, Throwable th) {
		log(Log.ASSERT, msg, th);
	}

	public static void fatal(Object msg) {
		log(Log.ASSERT, msg, null);
	}

	public static void timeStart() {
		latestStart = System.nanoTime();
	}

	public static void timeEnd(String label) {
		long start = System.nanoTime();
		long offset = start - latestStart;
		latestStart = start;
		System.out.println(formatTime(label, offset));
	}

	private static String formatTime(String label, long offset) {
		if (offset > NS_PER_S) {
			return "time used @" + label + ":" + (offset / (float) NS_PER_S)
				+ " s";
		} else if (offset > NS_PER_MS) {
			return "time used @" + label + ":" + (offset / (float) NS_PER_MS)
				+ " ms";
		}
		return "time used @" + label + ":" + offset + " ns";
	}

	private static long latestStart = -1;
	private static final int NS_PER_MS = 1000000;
	private static final int NS_PER_S = 1000000000;


	private static void log(int level, Object msg, Throwable th) {
		if (level < logLevel || !isDebug() ) return;
		//[3] 指向当前执行的方法 
		StackTraceElement trace = Thread.currentThread().getStackTrace()[4];
		String tag = generateTag(trace);
		try {
			if (Log.isLoggable(tag, level) || true) {
				String text;
				if (th == null) {
					if (msg instanceof Throwable) {
						text = String.valueOf(msg) + '\n'
							+ Log.getStackTraceString((Throwable) msg);
					} else {
						text = String.valueOf(msg);
					}
				} else {
					text = String.valueOf(msg) + '\n' + Log.getStackTraceString(th);
				}
				Log.println(level, tag, text);
				//System.out.println(tag + ':' + text);
			}
		} catch (Throwable e) {
			//e.printStackTrace();
			System.out.println(tag + "\t" + msg + "\t" + th);
		}
	}
}

package org.xidea.android;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CommonLog implements org.apache.commons.logging.Log {

	private static final int NS_PER_MS = 1000000;
	private static final int NS_PER_S = 1000000000;
	private static final boolean LOG_ENABLE = true;
	private static String THIS_CLASSNAME = CommonLog.class.getName();
	private Logger impl;

	private CommonLog(Class<?> clazz) {
		String tag = assertStatic(clazz);
		if (tag.length() > 23) {
			tag = tag.substring(0, 20) + "...";
		}

		this.impl = Logger.getLogger(tag);
	}

	private String assertStatic(final Class<?> fromClass) {
		if (isDebug() || fromClass == null) {
			StackTraceElement[] stacks = new Exception().getStackTrace();
			for (int i = 1; i < stacks.length; i++) {//
				StackTraceElement se = stacks[i];
				String className = se.getClassName();
				if (!THIS_CLASSNAME.equals(className)) {
					String placeClassName = se.getClassName();
					if (fromClass == null) {
						return placeClassName.substring(placeClassName.indexOf('.')+1);
					}else{
						String placeMethodName = se.getMethodName();
						if (!placeClassName.equals(fromClass.getName())) {
							fatal("CommonLog 申明在不正确的java类中:" + placeClassName
									+ "!=" + fromClass.getName());
						}
						if (!"<clinit>".equals(placeMethodName)) {
							fatal("性能考虑，CommonLog 必须在持有它的Class中申明为静态成员,而您申明在："
									+ placeClassName + "#" + placeMethodName);
						}
					}
					break;
				}

			}
		}
		return fromClass == null?"Unknow":fromClass.getSimpleName();
	}

	private void log(Level level, Object msg, Throwable e) {
		if (!LOG_ENABLE){
			return;
		}
		try {
			if (impl.isLoggable(level)) {
				String text = String.valueOf(msg);
				final Throwable pe;
				if (e == null ) {
					if(msg instanceof Throwable){
						pe = e = (Throwable) msg;
					}else{
						pe = new Throwable();
					}
				}else{
					pe = e;
				}

	            String sourceClass = impl.getName();
	            String sourceMethod = "unknown";
				StackTraceElement[] stacks = pe.getStackTrace();
					for (int i = 1; i < stacks.length; i++) {//
						StackTraceElement se = stacks[i];
						if (!THIS_CLASSNAME.equals(se.getClassName())) {
							//text = text +'\n'+ se.getLineNumber() + '@' + se.getFileName();
							sourceClass =  se.getClassName();
							sourceMethod = se.getMethodName() ;
							break;
						}
					}
	            // Hack (?) to get the stack trace.
	            Throwable dummyException = new Throwable();
	            StackTraceElement locations[] = dummyException.getStackTrace();
	            // LOGGING-132: use the provided logger name instead of the class name
	            // Caller will be the third element
	            if( locations != null && locations.length > 2 ) {
	                StackTraceElement caller = locations[2];
	                sourceMethod = caller.getMethodName();
	            }

	            if( e == null ) {
	                impl.logp( level, sourceClass, sourceMethod, text );
	            } else {
	                impl.logp( level, sourceClass, sourceMethod, text, e );
	            }
	        }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void debug(Object msg) {
		log(Level.FINE, msg, null);
	}

	@Override
	public void debug(Object msg, Throwable e) {
		log(Level.FINE, msg, e);
	}

	@Override
	public void error(Object msg) {
		log(Level.SEVERE, msg, null);
	}

	@Override
	public void error(Object msg, Throwable e) {
		log(Level.SEVERE, msg, e);
	}

	@Override
	public void fatal(Object msg) {
		log(Level.SEVERE, msg, null);
	}

	@Override
	public void fatal(Object msg, Throwable e) {
		log(Level.SEVERE, msg, e);
	}

	@Override
	public void info(Object msg) {
		log(Level.INFO, msg, null);
	}

	@Override
	public void info(Object msg, Throwable e) {
		log(Level.INFO, msg, e);
	}

	@Override
	public void trace(Object msg) {
		log(Level.FINEST, msg, null);
	}

	@Override
	public void trace(Object msg, Throwable e) {
		log(Level.FINEST, msg, e);
	}

	@Override
	public void warn(Object msg) {
		log(Level.WARNING, msg, null);
	}

	@Override
	public void warn(Object msg, Throwable e) {
		log(Level.WARNING, msg, e);
	}
    public boolean isErrorEnabled() {
        return impl.isLoggable(Level.SEVERE);
    }
    public boolean isFatalEnabled() {
        return impl.isLoggable(Level.SEVERE);
    }
    public boolean isInfoEnabled() {
        return impl.isLoggable(Level.INFO);
    }
    public boolean isDebugEnabled() {
        return impl.isLoggable(Level.FINE);
    }
    public boolean isTraceEnabled() {
        return impl.isLoggable(Level.FINEST);
    }
    public boolean isWarnEnabled() {
        return impl.isLoggable(Level.WARNING);
    }
	private long start = -1;
	public void timeStart() {
		start = System.nanoTime();
	}
	public void timeEnd(String label) {
		long start = System.nanoTime();
		long offset = start - this.start;
		this.start = start;
		info(formatTime(label, offset));
	}

	private String formatTime(String label, long offset) {
		if (offset > NS_PER_S) {
			return "time used @" + label + ":" + (offset / (float) NS_PER_S)
					+ " s";
		} else if (offset > NS_PER_MS) {
			return "time used @" + label + ":" + (offset / (float) NS_PER_MS)
					+ " ms";
		}
		return "time used @" + label + ":" + offset + " ns";
	}


	public void assertException(String msg) {
		if (isDebug()) {
			throw new RuntimeException(msg);
		}
	}

	public boolean isAssertEnabled() {
		return isDebug();
	}

	public static boolean isDebug() {
		try {
			return android.os.Debug.isDebuggerConnected();
		} catch (Throwable e) {
		}
		return false;
	}
	public static boolean isRelease() {
		return false;
	}

	public static CommonLog getLog() {
		return new CommonLog(null);
	}

}

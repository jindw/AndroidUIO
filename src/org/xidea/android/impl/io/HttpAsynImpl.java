package org.xidea.android.impl.io;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.logging.Log;

import org.xidea.android.Callback;
import org.xidea.android.Callback.Cancelable;
import org.xidea.android.Callback.PrepareCallback;
import org.xidea.android.impl.ApplicationState;
import org.xidea.android.impl.CommonLog;
import org.xidea.android.impl.io.HttpInterface.CachePolicy;
import org.xidea.android.impl.io.HttpInterface.HttpMethod;
import org.xidea.android.impl.io.HttpInterface.AsynTask;
import org.xidea.android.impl.ui.ImageUtil;

import android.graphics.Bitmap;
import android.graphics.Movie;
import android.os.Handler;

public class HttpAsynImpl {
	private static final int MAX_TASK_TIMEOUT = 1 * 60 * 1000;
	// private static final int MAX_IMAGE_TIMEOUT = 4 * 60 * 1000;

	private static Log log = CommonLog.getLog();

	private ExecutorService executorService = Executors.newFixedThreadPool(8);
	private Object taskLock = new Object();
	private List<AsynTask> taskList = new LinkedList<AsynTask>();
	//for kill timeout task
	private List<AsynTask> runningTaskList = new LinkedList<AsynTask>();
	// for pause
	private List<AsynTask> pauseTaskList = new LinkedList<HttpInterface.AsynTask>();

	public HttpAsynImpl() {

	}

	public void dispatchRequest(AsynTask task) {
		synchronized (taskLock) {
			taskList.add(task);
		}
		executorService.submit(requestComsumer);
	}

	public int cancel(Object group) {
		int hit = 0;
		synchronized (taskLock) {
			Iterator<AsynTask> it = taskList.iterator();
			while (it.hasNext()) {
				if (it.next().hitGroup(group)) {
					hit++;
					it.remove();
				}
			}
			it = pauseTaskList.iterator();
			while (it.hasNext()) {
				if (it.next().hitGroup(group)) {
					hit++;
					it.remove();
				}
			}
			it = runningTaskList.iterator();
			while (it.hasNext()) {
				AsynTask task = it.next();
				if (task.hitGroup(group)) {
					hit++;
					task.cancel();
					task.interrupt();
				}
			}
		}
		return hit;
	}

	public int resume(Object group) {
		int hit = 0;
		synchronized (taskLock) {
			Iterator<AsynTask> it = pauseTaskList.iterator();
			while (it.hasNext()) {
				AsynTask task = it.next();
				if (task.hitGroup(group)) {
					it.remove();
					hit++;
					taskList.add(task);
					executorService.submit(requestComsumer);
				}
			}
		}
		return hit;
	}

	public int pause(Object group) {
		int hit = 0;
		synchronized (taskLock) {
			Iterator<AsynTask> it = taskList.iterator();
			while (it.hasNext()) {
				AsynTask task = it.next();
				if (task.hitGroup(group)) {
					hit++;
					it.remove();
					pauseTaskList.add(task);
				}
			}
		}
		return hit;
	}

	private Runnable taskKiller = new Runnable() {

		@Override
		public void run() {
			while (true) {
				synchronized (taskLock) {
					// log.debug("try killer");
					if (runningTaskList.isEmpty()) {
						return;
					} else {
						long now = System.currentTimeMillis();
						for (Iterator<AsynTask> it = runningTaskList.iterator(); it
								.hasNext();) {
							AsynTask task = it.next();
							long timeout = task.getTimeout();// //callback
																// instanceof
																// ImageUtil.ImageLoaderCallback
																// ?
																// MAX_IMAGE_TIMEOUT
																// :
																// MAX_TASK_TIMEOUT;
							if (timeout > 0 && task.getStartTime() > 0
									&& now - task.getStartTime() > timeout) {
								it.remove();
								log.warn("task timeout :" + task.getURL());
								task.interrupt();
							}
						}
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
	};


	Runnable requestComsumer = new Runnable() {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void run() {
			// resumeTasks();
			AsynTask task;
			synchronized (taskLock) {
				if (taskList.isEmpty()) {
					return;
				} else {
					if( runningTaskList.isEmpty()){
						executorService.submit(taskKiller);
					}
					runningTaskList.add(task = taskList.remove(0));
				}
			}
			synchronized (task.requireLock()) {
				task.onStart();
				Callback<? extends Object> callback = task.getCallback();
				try {
					// requireTaskKiller(task);
					// log.info("used thread:"+runningTaskList.size());
					Object result = task.load(CachePolicy.Any);
					if (callback instanceof PrepareCallback) {
						result = ((PrepareCallback) callback).prepare(result);
					}
					task.execute(result);
				} catch (Throwable e) {// 捕捉所有异常 ,否则可能有多线程问题
					task.error(e);
				} finally {
					synchronized (taskLock) {
						runningTaskList.remove(task);
					}
					task.complete();
				}
			}
		}

	};

	public static class TaskImpl implements AsynTask {
		@SuppressWarnings("rawtypes")
		Callback callback;
		private boolean callbackErrorTime = false;
		private long createTime = System.currentTimeMillis();
		private long begin;
		private Thread thread;// for interrupt
		private String path;
		private URL url;
		private HttpMethod method;
		private Handler from = HttpUtil.currentHandler();
		private final WeakReference<Object> groupRef;
		private HttpImplementation http;
		private boolean canceled;
		private List<Cancelable> cancelables;
		private Type[] types;

		public TaskImpl(HttpImplementation http, String path, HttpMethod method,
				Callback<?> callback, String name, Object mutip) {
			this.http = http;
			this.path = path;
			this.url = HttpUtil.parseURL(path);
			this.method = method;
			this.callback = callback;
			this.types = HttpUtil.getResultType(callback);
			this.groupRef = new WeakReference<Object>(ApplicationState
					.getInstance().getTopActivity());
		}

		public void onStart() {
			begin = System.currentTimeMillis();
			thread = Thread.currentThread();
			http.getStatistics().onHttpWaitDuration(url,
					System.currentTimeMillis() - getCreateTime());
		}

		public Object load(CachePolicy cp) {
			Type rawType = types[0];
			Object result = null;
			try {
				if (rawType == AsynTask.class) {
					result = this;
				}else if (rawType == URL.class) {
					result = url;
				} else if (rawType == Bitmap.class) {
					result = ImageUtil.createBitmap(path,1,-1,cp,this,null);
				} else if (rawType == InputStream.class) {
					result = loadStream(cp);
				} else if (rawType == byte[].class) {
					result = StreamUtil.loadBytesAndClose(loadStream(cp));
				} else if (rawType == Movie.class) {
					byte[] data = StreamUtil.loadBytesAndClose(loadStream(cp));
					result = Movie.decodeByteArray(data, 0, data.length);
				} else {
					String text = http.getString(url, this.method,
							this.getPost(), this, cp);
					if (text != null) {
						log.info("http返回：" + url + "\n" + text);
						result = HttpUtil.transform(text, rawType);
					} else {
						log.info("Http请求无数据返回");
					}
				}

				return result;
			} catch (Exception e) {
				this.error(e);
				return null;
			}
		}

		private InputStream loadStream(CachePolicy cp) throws IOException {
			return http.getStream(url, method, this.getPost(), this,
					cp);
		}

		@SuppressWarnings("unchecked")
		public void execute(Object result) {
			if(types.length == 2 && result != null){//只有prepare 结果有可能变化，因为在loadRaw中已经正确转型了。
				result = HttpUtil.transform(result, types[1]);
			}
			final Object result2 = result;
			run(new Runnable() {
					@Override
					public void run() {
						callbackErrorTime = true;
						callback.callback(result2);
						callbackErrorTime = false;//不能用final
					}
				});
			
		}

		public void error(final Throwable ex) {
			ex.printStackTrace();
			final boolean callbackError = callbackErrorTime;
			callbackErrorTime = false;
			run(new Runnable() {
				@Override
				public void run() {
					callback.error(ex,callbackError );
				}
			});

		}


		public Object requireLock() {
//			System.out.println(url.getPath());
			return url.getPath().intern();
		}

		private Map<String, Object> getPost() {
			return null;
		}
		private void run(Runnable runner) {
			if (from == null) {
				runner.run();
			} else {
				from.post(runner);
			}
		}
		
		public void complete() {
		}

		@Override
		public URL getURL() {
			return url;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Callback<? extends Object> getCallback() {
			return callback;
		}

		@Override
		public long getCreateTime() {
			return createTime;
		}

		@Override
		public int getTimeout() {
			return MAX_TASK_TIMEOUT;
		}

		@Override
		public boolean hitGroup(Object group) {
			return groupRef.get() == group;
		}

		@Override
		public void cancel() {
			if (!canceled) {
				canceled = true;
				if (cancelables != null) {
					for (Cancelable c : cancelables) {
						c.cancel();
					}
				}
			}
		}

		public void add(Cancelable sub) {
			if (cancelables == null) {
				cancelables = new ArrayList<Callback.Cancelable>();
			}
			cancelables.add(sub);

		}

		@Override
		public boolean isCanceled() {
			return canceled;
		}

		@Override
		public long getStartTime() {
			return begin;
		}

		@Override
		public void interrupt() {
			thread.interrupt();
		}

	}

}

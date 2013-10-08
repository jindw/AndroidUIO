package org.xidea.android.impl.io;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.logging.Log;

import org.xidea.android.Callback;
import org.xidea.android.Callback.PrepareCallback;
import org.xidea.android.CommonLog;
import org.xidea.android.impl.ApplicationState;
import org.xidea.android.impl.io.HttpInterface.CachePolicy;
import org.xidea.android.impl.io.HttpInterface.HttpMethod;
import org.xidea.android.impl.io.HttpInterface.AsynTask;

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
	private List<AsynTask> runningTaskList = new LinkedList<AsynTask>();// for
																		// kill
																		// timeout
																		// task
	private List<AsynTask> pauseTaskList = new LinkedList<HttpInterface.AsynTask>();// for
																					// pause

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
								if (CommonLog.isDebug()) {
									HttpUtil.showTips("task timeout！："
											+ task.getURL());
								}
								it.remove();
								log.warn("do killer :" + task.getURL());
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

	private void requireTaskKiller(AsynTask task) {
		boolean empty;
		synchronized (taskLock) {
			empty = runningTaskList.isEmpty();
			runningTaskList.add(task);
		}
		if (empty) {
			executorService.submit(taskKiller);
		}
	}

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
					task = taskList.remove(0);
				}
			}

			synchronized (task.requireLock()) {
				task.start();
				requireTaskKiller(task);
				Callback<? extends Object> callback = task.getCallback();
				try {
					// requireTaskKiller(task);
					// log.info("used thread:"+runningTaskList.size());
					Type[] types = HttpUtil.getResultType(callback);
					Object result = task.loadResult(types[0]);
					if (callback instanceof PrepareCallback) {
						result = ((PrepareCallback) callback).prepare(result);
						result = HttpUtil.transform(result, types[1]);
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

	public static class TaskImpl implements AsynTask, Runnable {
		@SuppressWarnings("rawtypes")
		Callback callback;
		private boolean callbackErrorTime = false;
		private long createTime = System.currentTimeMillis();
		private long begin;
		private Thread thread;// for interrupt
		private URL url;
		private HttpMethod method;
		private Handler from = HttpUtil.currentHandler();
		private final WeakReference<Object> groupRef;
		private HttpImplementation http;
		private Object result;
		private boolean canceled;

		public TaskImpl(HttpImplementation http, String url, HttpMethod method,
				Callback<?> callback, String name, Object mutip) {
			this.http = http;
			this.url = HttpUtil.parseURL(url);
			this.method = method;
			this.callback = callback;
			this.groupRef = new WeakReference<Object>(ApplicationState
					.getInstance().getTopActivity());
		}

		public void start() {
			begin = System.currentTimeMillis();
			thread = Thread.currentThread();
			http.getStatistics().onHttpWaitDuration(url,
					System.currentTimeMillis() - getCreateTime());
		}

		public Object loadResult(Type requiredType) {
			Object result = null;
			try {
				if (requiredType == URL.class) {
					result = url;
				} else if (requiredType == URL.class) {
					result = url;
				} else if (requiredType == Bitmap.class) {
					// result =
					// ImageUtil.getInstance().requireBitmap(task.toIdentify(),
					// null, -1, -1, onlyCache, onlyNetwork);
				} else if (requiredType == InputStream.class) {
					result = loadStream();
				} else if (requiredType == byte[].class) {
					result = FileUtil.loadBytesAndClose(loadStream());
				} else if (requiredType == Movie.class) {
					byte[] data = FileUtil.loadBytesAndClose(loadStream());
					result = Movie.decodeByteArray(data, 0, data.length);
				} else {
					String text = http.getString(url, this.method,
							this.getPost(), this, CachePolicy.Any);
					if (text != null) {
						log.info("http返回：" + url + "\n" + text);
						result = HttpUtil.transform(text, requiredType);
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

		private InputStream loadStream() throws IOException {
			return http.getStream(url, method, this.getPost(),
					this, CachePolicy.Any);
		}

		private Map<String, Object> getPost() {
			return null;
		}

		public Object requireLock() {
			return url.getPath();
		}

		@SuppressWarnings("unchecked")
		public void execute(final Object result) {
			callbackErrorTime = true;
			this.result = result;
			run(this);
			if (from == null) {
				callback.callback(result);
			} else {
				from.post(new Runnable() {
					@Override
					public void run() {

					}
				});
			}

		}

		public void run(Runnable runner) {
			if (from == null) {
				runner.run();
			} else {
				from.post(runner);
			}
		}

		@SuppressWarnings("unchecked")
		public void run() {
			callback.callback(result);
		}

		public void error(final Throwable ex) {
			run(new Runnable() {
				@Override
				public void run() {
					callback.error(ex, callbackErrorTime);
				}
			});

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
		public void cancel(){
			canceled = true;
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

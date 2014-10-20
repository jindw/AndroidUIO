package org.xidea.android.impl;

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.xidea.android.Callback;
import org.xidea.android.Callback.CacheCallback;
import org.xidea.android.Callback.Cancelable;
import org.xidea.android.impl.Network.CachePolicy;
import org.xidea.android.impl.http.LoadingImpl;

public interface AsynTask extends Cancelable {
	void onStart();

	/**
	 * 处理缓存数据，返回是否信任该缓存
	 */
	boolean onCache(Object cacheData,long start);

	/**
	 * 处理缓存数据，返回是否信任该缓存
	 */
	void onCallback(Object result,long start);

	void onComplete();

	void error(Throwable e, final boolean callbackError);

	Object requireLock();

	Object load(CachePolicy cachePolicy);

	URL getURL();

	long getTaskStartTime();

	int getTimeout();

	Callback<? extends Object> getCallback();

	Thread getThread();

	public class AsynImpl implements Runnable {
		private ExecutorService executorService = Executors
				.newFixedThreadPool(8);
		private Object taskLock = new Object();
		private List<AsynTask> taskList = new LinkedList<AsynTask>();
		// for kill timeout task
		private List<AsynTask> runningTaskList = new LinkedList<AsynTask>();

		public AsynImpl() {
		}

		public void dispatchRequest(AsynTask task) {
			synchronized (taskLock) {
				taskList.add(task);
			}
			executorService.submit(this);
		}

		@Override
		public void run() {
			AsynTask task;
			synchronized (taskLock) {
				if (taskList.isEmpty()) {
					return;
				} else {
					if (runningTaskList.isEmpty()) {
						executorService.submit(taskKiller);
					}
					runningTaskList.add(task = taskList.remove(0));
				}
			}
			synchronized (task.requireLock()) {
				try {
					CachePolicy cachePolicy = CachePolicy.Any;
					boolean trustCache = false;
					try {
						task.onStart();
						Callback<? extends Object> callback = task
								.getCallback();
						if (callback instanceof CacheCallback) {
							long start = System.currentTimeMillis();
							Object result = task.load(CachePolicy.CacheOnly);
							trustCache = task.onCache(result,start);
							cachePolicy = CachePolicy.NetworkOnly;
						}
					} catch (Throwable e) {
						task.error(e, false);
					}
					long start = System.currentTimeMillis();
					Object result = trustCache ? null : task.load(cachePolicy);
					task.onCallback(result,start);
				} catch (Throwable e) {
					task.error(e, false);
				} finally {
					synchronized (taskLock) {
						runningTaskList.remove(task);
					}
				}
			}
			task.onComplete();
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
							for (Iterator<AsynTask> it = runningTaskList
									.iterator(); it.hasNext();) {
								AsynTask task = it.next();
								long timeout = task.getTimeout();
								if (timeout > 0 && task.getTaskStartTime() > 0
										&& now - task.getTaskStartTime() > timeout) {
									it.remove();
									DebugLog.warn("task timeout :"
											+ task.getURL());
									task.getThread().interrupt();
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

	}
}

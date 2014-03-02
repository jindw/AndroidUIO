package org.xidea.android;

/**
 * 通用回掉接口
 * @author jindawei
 *
 * @param <ResultType>
 */
public interface Callback<ResultType> {
	/**
	 * 在调用时所在的线程执行回调处理（比如事件发起的调用是在ui线程，回调就可以直接操作ui元素）
	 * @param result 经过自动数据转换后的对象， 如，我们要求一个JavaBean，他会自动构造该对象， 并且递归初始化他的属性（从json属性值自动转换类型并赋值），支持范型
	 */
	public void callback(ResultType result);

	/**
	 * 异常回调
	 * @param ex
	 * @param callbackError 是否是发生在callback函数中的异常
	 * @return
	 */
	public void error(Throwable ex,boolean callbackError);

	/**
	 * 取消任务接口，比如 发起一个http请求， 会返回一个该对象， 然后我们可以在任意地点尝试取消这个请求。
	 * @author jindawei
	 */
	public interface Cancelable{
		void cancel();
		boolean isCanceled();
		public class CanceledException extends RuntimeException{
			private static final long serialVersionUID = 1L;
		}
	}
	/**
	 * 允许后台执行预处理的回掉任务。
	 * 一般回掉函数会抛回主线程执行，但是如果我们需要做长时间的耗时操作，就可以用这个借口，吧耗时操作放在prepare中执行。
	 * @author jindawei
	 *
	 * @param <RawType>
	 * @param <ResultType>
	 */
	public interface PrepareCallback<RawType,ResultType> extends Callback<ResultType> {
		/**
		 * 在后台线程执行耗时的数据处理。这个过程在后台执行，不在ui线程执行，不能操作ui元素
		 * @param data
		 * @return
		 */
		public Object prepare(RawType rawData);
		
		/**
		 * @see Callback#callback(Object)
		 * @param result
		 */
		public void callback(ResultType result);
	}
	public interface CacheCallback<ResultType> extends Callback<ResultType> {
		/**
		 * 当网络请求返回后回掉，如果数据相对缓存数据没有变化，则newData为空，否则为新数据
		 * @param newData 更新数据（如果数据相对缓存没有变化，该方法依然被调用，但是其值为null）
		 */
		public boolean cache(ResultType newData);
		/**
		 * 获取缓存数据后立即回掉，如果没有有效缓存，cacheData 而我null
		 * @param cacheData
		 */
		public void callback(ResultType cacheData);
	}

	/**
	 * 支持进度的回调接口
	 *
	 * @param <ResultType>
	 */
	public interface ProgressCallback<ResultType> extends Callback<ResultType> {

		/**
		 * 进度回调方法
		 * @param total
		 * @param current
		 */
		void onLoading(long total, long current);

		/**
		 * 任务被取消时，回调该接口
		 */
		void onCancelled();

		/**
		 * 下载文件时文件保存的路径和文件名
		 * @return
		 */
		public String getSavePath();
	}

}


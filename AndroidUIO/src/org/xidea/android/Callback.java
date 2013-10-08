package org.xidea.android;

public interface Callback<ResultType> {
	public class CanceledException extends RuntimeException{
		private static final long serialVersionUID = 1L;
	}
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
	
//	public interface CacheCallback<ResultType> extends Callback<ResultType> {
//		public void onCache(ResultType data);
//		public void onNetwork(ResultType data);
//	}
}


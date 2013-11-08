package org.xidea.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 这个接口是系统自动实现的！！！！，你只要申明getter setter即可。
 * 描述一个java接口到 {@link android.content.SharedPreferences}的存储映射， 通过 {@link CommonUtil#getKeyValueStorage(Class)}自动实现。
 * 这个java接口的getter setter 会自动与 android.content.SharedPreferences 对接
 * 如果需要指定存储文件名， 可以用StorageKey 注解
 * <pre><code>
 *	===  实例代码 ====
 *	public interface GlobalsConfig extends KeyValueStorage<GlobalsConfig>{
 *		/**
 *		 * 获取一个int值，默认值为1024 
 *		 *\/
 *		@DefaultValue(1024)
 *		public int getExampleInt();
 *		/** 
 *		 * 设置一个int值，setter 可以返回 当前对象，方便连续调用， 如： 
 *		 * myStorage.beginTransaction().setExampleInt(1).setExampleBool(false).commit();
 *		 *\/
 *		public T setExampleInt(int value);
 *		/**
 *		 * 采信 jsonValue的设置，默认值为false
 *		 *\/
 *		@DefaultValue(value=1,jsonValue="false")
 *		public boolean getExampleBool();
 *	}
 * </code></pre>
 * @see org.xidea.android.impl.io.KeyValueStorageImpl
 * @author jindawei
 *
 */
public interface KeyValueStorage<T extends KeyValueStorage<T>> {

	/**
	 * 开启一个存储事务，一般不需要调用，如果未开启存储事务， 默认每个set操作为一个存储事务
	 * 但是当，批量存储大量数据的时候， 通过一个事务存储多个修改记录，可以减少io操作，改善性能
	 * @hide
	 * @return KeyValueStorage 对象本身
	 */
	public T beginTransaction();
	/**
	 * 提交一个存储事务， 一般不需要调用， 如果为开启存储事务， 默认每个set操作为一个存储事务
	 * 但是当，批量存储大量数据的时候， 通过一个事务存储多个修改记录，可以减少io操作，改善性能
	 * @hide
	 */
	public void commit();
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface StorageKey{
		String value() default "default";
	}
	
	/**
	 * 默认值注解， 因为getter方法有时候默认返回值是不固定的
	 * @author jindawei
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD,ElementType.TYPE})
	public @interface DefaultValue{
		/**
		 * 用于设置Number 的值， 对整型、浮点型和布尔型均有效
		 * 当指定了jsonValue的时候，其设置可能被覆盖
		 * @return
		 */
		double value() default 0;
		/**
		 * 如非默认值null，可具有更高优先级，其值可覆盖value设置。
		 * @return
		 */
		String jsonValue() default "null";
	}

}

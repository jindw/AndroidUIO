package org.xidea.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 这个接口是系统自动实现的！！！！，你只要申明getter setter即可。
 * 描述一个java接口到 {@link android.content.SharedPreferences}的存储映射， 通过 {@link CommonUtil#getKeyValueStorage(Class)}自动实现。
 * 这个java接口的getter setter 会自动与 android.content.SharedPreferences 对接
 * 接口实现的常量 STORAGE_KEY 表示 {@link android.content.SharedPreferences}存储的具体地址
 * @see org.xidea.android.ui.StorageImpl
 * @author jindawei
 *
 */
public interface KeyValueStorage<T> {
	String STORAGE_KEY = "default";
	
	/**
	 * 默认值注解， 因为getter方法有时候必须
	 * @author jindawei
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD,ElementType.TYPE})
	@interface DefaultValue{
		int intValue() default 0;
		long longValue() default 0;
		float floatValue() default 0;
		boolean booleanValue() default false;
		String jsonValue() default "null";
	}
	
	public T beginTransaction();
	public void commit();
}

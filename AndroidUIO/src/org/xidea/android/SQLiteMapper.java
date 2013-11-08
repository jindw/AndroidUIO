package org.xidea.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;



import android.content.ContentValues;

/**
 * 描述一个java类到sqlite的存储映射， 通过 {@link #getSQLiteStorage(Class)}自动实现。
 * 
 * @author jindawei
 * @param <T>
 */
public interface SQLiteMapper<T> {

	/**
	 * 通过主键获取对象
	 * @param id
	 * @return
	 */
	public abstract T get(Object id);
	public abstract void get(Callback<T> receiver,Object id);

	/**
	 * 
	 * 通过任意字段获取第一个匹配对象
	 * @param field
	 * @param value
	 * @return
	 */
	public abstract T getByKey(String field, Object value);
	public abstract void getByKey(Callback<T> receiver,String field, Object value);

	/**
	 * 通过where 语句 查找匹配的全部对象
	 * @see org.xidea.android.impl.io.SQLiteMapperImpl#query(String, Object...)
	 * @param where
	 * @param selectionArgs
	 * @return
	 */
	public abstract List<T> query(String where, Object... selectionArgs);
	public abstract void query(Callback<List<T>> callback,String where, Object... selectionArgs);

	public abstract T save(T t);
	public abstract void save(Callback<T> callback,T t);
	public abstract void save(Callback<List<T>> callback,List<T> t);

	public abstract boolean update(T t);
	public abstract boolean update(ContentValues contents);
	public abstract void update(Callback<Boolean> callback,T t);
	public abstract void update(Callback<Boolean> callback,ContentValues contents);

	public abstract boolean remove(Object id);
	public abstract void remove(Callback<Boolean> callback,Object id);
	
	
	
	
	
	/**
	 * 属性注解， 注解内容为对应sqlite 字段定义（字段名除外）
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD})
	public @interface SQLiteProperty {
		public String value() default "";
		public boolean index() default false;
	}
	/**
	 * 数据库版本信息注解， value 标注版本号，默认为1
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface SQLiteEntry{
		public int version() default 1;
		public String name() default "";
	}
	/**
	 * upgrade 标注数据库升级方法名， 这个方法必须为静态方法，且带有两个固定参数：（SQLiteDatabase db,  String table）
	 * @author jindawei
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface SQLiteUpdate{
		public int value();
	}

}
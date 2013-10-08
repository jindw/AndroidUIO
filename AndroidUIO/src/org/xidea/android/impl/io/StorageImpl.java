package org.xidea.android.impl.io;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.xidea.android.CommonLog;
import org.xidea.android.KeyValueStorage;
import org.xidea.android.KeyValueStorage.DefaultValue;
import org.xidea.android.SQLiteMapper;

@DefaultValue
public class StorageImpl {
	private static final JSONDecoder JSON_DECODER = new JSONDecoder(false);
	private static final org.apache.commons.logging.Log log =CommonLog.getLog();
	private static final Object[] EMPTY_OBJECTS = new Object[0];
	@SuppressWarnings("rawtypes")
	private Map cache = new HashMap();

	static DefaultValue DEFAULT = StorageImpl.class
			.getAnnotation(KeyValueStorage.DefaultValue.class);

	private StorageImpl() {
	}

	static StorageImpl INSTANCE = new StorageImpl();
	@SuppressWarnings("unchecked")
	public <T extends KeyValueStorage<?>> T getKVStroage(Class<T> type,
			Context application) {
		return (T) require(type, application, true);

	}

	@SuppressWarnings("unchecked")
	public <T> SQLiteMapper<T> getSQLiteStorage(Class<T> type,
			Context application) {
		return (SQLiteMapper<T>) require(type, application, false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object require(Class type, Context application, boolean isKvType) {
		Object impl = cache.get(type);
		if (impl == null) {
			if (isKvType != type.isInterface()) {
				log.error(
						"KvStroage  必须从接口创建，SQLite需要从具体类创建！你给出的Class 与其代价过不符！"
								+ type);
			}
			synchronized (cache) {
				impl = cache.get(type);
				if (impl == null) {
					if (isKvType) {
						String key;
						try {
							Field field = type.getDeclaredField("KEY");
							field.setAccessible(true);
							key = (String) field.get(null);
						} catch (Exception e) {
							key = type.getName();
						}
						impl = buildKVStroage(type, application, key);
					} else {
						impl = new SQLiteMapperImpl(application, type);
					}
					cache.put(type, impl);
				}
			}
		}
		return impl;
	}

	@SuppressWarnings("unchecked")
	protected <T extends KeyValueStorage<?>> T buildKVStroage(
			final Class<T> type, Context context, String name) {
		final SharedPreferences preferences = context.getSharedPreferences(
				name, Activity.MODE_PRIVATE);
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {
				type }, new InvocationHandler() {

			SharedPreferences.Editor editor = null;
			private Object writeLock = new Object();

			@Override
			public Object invoke(Object thiz, Method method, Object[] args)
					throws Throwable {
				//long t1 = System.nanoTime();
				if(args == null){//alliyun bug
					args = EMPTY_OBJECTS;
				}
				Type rt = method.getGenericReturnType();
				String name = method.getName();

				Class<?> declaringClass = method.getDeclaringClass();
				//System.out.println("init-type:"+-(t1 - (t1 = System.nanoTime()))/1000000f);
				try{
				if (declaringClass == KeyValueStorage.class) {
					synchronized (writeLock) {
						if(rt == SharedPreferences.class){
							return preferences;
						}else if (rt == Void.TYPE && args.length == 0 ){//("commit".equals(name)) {
							if (editor != null) {
								editor.commit();
								editor = null;
							}
							return null;
						} else {// if ("beginTransaction".equals(name)) {
							if (editor == null) {
								editor = preferences.edit();
							}
							return thiz;
						}
					}
				} else {
					//System.out.println("before get:"+-(t1 - (t1 = System.nanoTime()))/1000000f);
					return getset(thiz, preferences, method, args, rt, name,
							declaringClass);

				}
				}finally{
				//System.out.println("complete:"+-(t1 - (t1 = System.nanoTime()))/1000000f);
				}
			}

			private Object getset(Object thiz,
					final SharedPreferences preferences, Method method,
					Object[] args, Type rt, String name, Class<?> declaringClass) {

				//long t1 = System.nanoTime();
				String key = name.replaceAll("^(?:get|set|is)([A-Z])", "$1");
				if (!name.equals(key)) {
					name = Character.toLowerCase(key.charAt(0))
							+ key.substring(1);

					if (rt == Void.TYPE || rt == type) {//
						if (args.length == 1) {
							synchronized (writeLock) {
								setValue(preferences, name, args[0],
										normalizeType(method.getGenericParameterTypes()[0]),
										editor);
								if (rt == type) {
									return thiz;
								}
							}
						}
					} else if (args.length <= 1) {
						Object value;
						rt = normalizeType(rt);
						if (args.length == 1) {
							value = args[0];
						} else {
							value = getDefaultValue(
									method.getAnnotation(KeyValueStorage.DefaultValue.class),
									rt);
							if(value == null && rt instanceof Class<?> && ((Class<?>)rt).isPrimitive()){
								log.error("get primitive type default value failed!!"+method);
							}
						}

						//System.out.println("init default:"+-(t1 - (t1 = System.nanoTime()))/1000000f);
						try{
						return getValue(preferences, name, value, rt);
						}finally{

							//System.out.println("do get:"+-(t1 - (t1 = System.nanoTime()))/1000000f);
						}
					}
				}
				return null;
			}

			private Type normalizeType(Type rt) {
				if(rt instanceof Class<?>){
					rt = ReflectUtil.toWrapper((Class<?>)rt);
				}
				return rt;
			}

		});
	}

	private Object getDefaultValue(DefaultValue def, Type valueType) {
		if (valueType instanceof Class) {
			Class<?> clazz = (Class<?>) valueType;
			if (Enum.class.isAssignableFrom(clazz)) {
				return def == null ? null : ReflectUtil.getEnum(def.intValue(),
						clazz);
			}
		}
		if (def == null) {
			def = DEFAULT;
		}
		if (valueType == Boolean.class) {
			return def.booleanValue();
		} else if (valueType == String.class) {
			return new JSONWrapper(def.jsonValue(), valueType);
		} else if (valueType == Integer.class) {
			return def.intValue();
		} else if (valueType == Float.class) {
			return def.floatValue();
		} else if (valueType == Long.class) {
			return def.longValue();
		} else if (valueType == Double.class) {
			return Double.valueOf(def.floatValue());
		} else {
			return new JSONWrapper(def.jsonValue(), valueType);
		}
	}

	private static class JSONWrapper {
		private String value;
		private Type type;

		public JSONWrapper(String jsonValue, Type valueType) {
			this.value = jsonValue;
			this.type = valueType;
		}

		public String toString() {
			return value;
		}

		public Object getValue() {
			if (value == null || value.equals("null") || value.length() == 0) {
				return null;
			}
			return JSON_DECODER.decode(value, type);
		}

	}

	private void setValue(SharedPreferences preferences, String name,
			Object value, Type valueType, Editor transEditor) {
		Editor editor = transEditor == null ? preferences.edit() : transEditor;
		if (valueType == Boolean.class) {
			editor.putBoolean(name, (Boolean) value);
		} else if (valueType == String.class) {
			editor.putString(name, (String) value);
		} else if (valueType == Integer.class) {
			editor.putInt(name, (Integer) value);
		} else if (valueType == Float.class) {
			editor.putFloat(name, (Float) value);
		} else if (valueType == Long.class) {
			editor.putLong(name, (Long) value);
		} else if (valueType == Double.class) {
			editor.putFloat(name, ((Double) value).floatValue());
		} else if (valueType instanceof Class
				&& Enum.class.isAssignableFrom((Class<?>) valueType)) {
			if (value == null) {
				editor.remove(name);
			} else {
				editor.putInt(name, ((Enum<?>) value).ordinal());
			}
		} else {
			editor.putString(name, JSONEncoder.encode(value));
		}
		if (transEditor == null) {
			editor.commit();
		}
	}

	protected Object getValue(SharedPreferences preferences, String name,
			Object defaultValue, Type valueType) {
		if (!preferences.contains(name)) {
			String name2 = toName2(name);
			if (preferences.contains(name2)) {
				name = name2;
			} else if (defaultValue instanceof JSONWrapper) {
				return ((JSONWrapper) defaultValue).getValue();
			} else {
				return defaultValue;
			}
		}
		if (valueType == Boolean.class) {
			return preferences.getBoolean(name, (Boolean) defaultValue);
		} else if (valueType == String.class) {
			return preferences.getString(name, (String)((JSONWrapper) defaultValue).getValue());
		} else if (valueType == Integer.class) {
			return preferences.getInt(name, (Integer) defaultValue);
		} else if (valueType == Float.class) {
			return preferences.getFloat(name, (Float) defaultValue);
		} else if (valueType == Long.class) {
			return preferences.getLong(name, (Long) defaultValue);
		} else if (valueType == Double.class) {
			return Double.valueOf(preferences.getFloat(name,
					((Double) defaultValue).floatValue()));
		} else if (valueType instanceof Class
				&& Enum.class.isAssignableFrom((Class<?>) valueType)) {
			int iv = preferences.getInt(name, defaultValue == null ? -1
					: ((Enum<?>) defaultValue).ordinal());
			return iv < 0 ? null : ReflectUtil
					.getEnum(iv, (Class<?>) valueType);
		} else {
			String raw = preferences.getString(name, "");
			if (raw != null && raw.length() > 0) {
				try{
					return JSON_DECODER.decode(raw, valueType);
				}catch(Exception ex){
					log.error(ex);
				}
			}
			return null;
		}
	}

	private String toName2(String name) {
		StringBuilder buf = new StringBuilder();
		for (char c : name.toCharArray()) {
			if (Character.isUpperCase(c)) {
				buf.append('_');
				c = Character.toUpperCase(c);
			}
			buf.append(c);
		}
		String name2 = buf.toString();
		return name2;
	}

}

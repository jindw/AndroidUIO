package org.xidea.android.impl.io;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xidea.android.KeyValueStorage;
import org.xidea.android.UIO;
import org.xidea.android.KeyValueStorage.DefaultValue;
import org.xidea.android.util.DebugLog;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;

/**
 * @hide
 */
public class KeyValueImpl<T> {
	protected static final JSONDecoder JSON_DECODER = new JSONDecoder(false);
	protected Editor currentEditor;
	private HashMap<String, Object> defaultValueMap = new HashMap<String, Object>();
	private HashMap<String, Type> typeMap = new HashMap<String, Type>();
	private SharedPreferences preferences;
	private Class<?> type;

	// private static DefaultValue DEFAULT_DV =
	// KeyValueImpl.class.getMethod("toString").
	//
	KeyValueImpl(Context context, Class<?> interfaceType, String storageKey) {
		init(context, interfaceType, storageKey);
	}

	protected KeyValueImpl() {
		init(UIO.getApplication(), this.getClass(), null);
	}

	static String getStorageKey(Class<?> type) {
		String key;
		try {
			KeyValueStorage.StorageKey field = type
					.getAnnotation(KeyValueStorage.StorageKey.class);
			key = (String) field.value();
		} catch (Exception e) {
			key = type.getName();
		}
		return key;
	}

	protected void init(Context context, Class<?> type, String storageKey) {
		this.preferences = context.getSharedPreferences(storageKey,
				Activity.MODE_PRIVATE);
		if (!type.isInterface()) {
			Class<?>[] ifs = type.getInterfaces();
			for (Class<?> i : ifs) {
				// 不要多重继承
				if (i.getSuperclass() == KeyValueStorage.class) {
					type = i;
					break;
				}
			}
		}

		this.type = type;
		if (storageKey == null) {
			storageKey = getStorageKey(type);
		}
		Method[] methods = type.getMethods();

		for (Method method : methods) {
			String name = method.getName();
			String key = name.replaceAll("^(?:get|set|is)([A-Z])", "$1");
			if (!name.equals(key)) {
				if (name.startsWith("set")) {
					// TODO:
					Annotation a = method.getAnnotation(DefaultValue.class);
					if (a != null) {
						DebugLog.error("DefaultValue Anotation must place on a empty arguments getter !! but you place it on"
								+ method);
					}
				} else {
					key = Character.toLowerCase(key.charAt(0))
							+ key.substring(1);
					// Object oldValue = defaultValueMap.get(key);
					if (method.getParameterTypes().length > 0) {
						DebugLog.error("DefaultValue Anotation must place on a empty arguments getter !! but you place it on"
								+ method);
					} else {
						typeMap.put(key, method.getGenericReturnType());
						DefaultValue dva = method
								.getAnnotation(DefaultValue.class);
						if (dva == null) {
							if (method.getReturnType().isPrimitive()) {
								defaultValueMap.put(key,
										method.getDefaultValue());
							}
						} else {
							Object sharedValue = buildDefault(
									method.getReturnType(), dva);
							if (immutable(sharedValue)) {
								defaultValueMap.put(key, sharedValue);
							} else {
								defaultValueMap.put(key, dva);
							}
						}
					}
				}
			}
		}
	}
	protected Type type(String key) {
		return typeMap.get(key);
	}

	@SuppressWarnings("unchecked")
	private <E>E defaultValue(Type valueType, String key) {
		Object v = defaultValueMap.get(key);
		if (v instanceof DefaultValue) {
			Object sharedValue = buildDefault(valueType, (DefaultValue) v);
			return (E)sharedValue;
		} else {
			return (E)v;
		}
	}

	@SuppressWarnings("unchecked")
	private Object buildDefault(Type valueType, DefaultValue dv) {
		double value = dv.value();
		String jsonValue = dv.jsonValue();
		Object rtv = null;
		if (jsonValue.length() > 0 && !"null".equals(jsonValue)) {
			rtv = JSON_DECODER.decodeObject(jsonValue, valueType);
		} else {
			if (valueType == Boolean.class) {
				rtv = value != 0;
			} else if (valueType instanceof Class) {
				Class<?> type = (Class<?>) valueType;
				if (Number.class.isAssignableFrom(type)) {
					rtv = ReflectUtil.toValue(value, type);
				} else if (Enum.class.isAssignableFrom(type)) {
					rtv = ReflectUtil
							.getEnum((int) value, (Class<? extends Enum<?>>) valueType);
				}
			}
		}
		return rtv;
	}

	@DefaultValue
	@SuppressWarnings("unchecked")
	public T beginTransaction() {
		commit();
		this.currentEditor = preferences.edit();
		return (T) this;

	}

	public void commit() {
		Editor editor = this.currentEditor;
		if (editor != null) {
			apply(editor);
			this.currentEditor = null;
		}
	}

	@SuppressWarnings("unchecked")
	public T reset() {
		Editor editor = this.currentEditor;
		if (editor == null) {
			editor = preferences.edit();
		}
		editor.clear();
		if (editor != this.currentEditor) {
			apply(editor);
		}
		return (T) this;
	}

	@SuppressLint("NewApi")
	static void apply(Editor editor) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			editor.commit();
		} else {
			editor.apply();
		}
	}

	public String toString() {
		return type.getName() + "@" + preferences;
	}

	boolean immutable(Object obj) {
		boolean isImutable = obj == null || obj instanceof String
				|| obj instanceof Number || obj instanceof Boolean
				|| obj instanceof File || obj instanceof Class;
		return isImutable;
	}

	public boolean contains(String name) {
		return preferences.contains(name);
	}

	Object put(Type valueType, final String key, Object value) {
		Editor editor = currentEditor;
		if (editor == null) {
			editor = preferences.edit();
		}
		if (value == null) {
			editor.remove(key);
		} else if (valueType == Boolean.class) {
			editor.putBoolean(key, (Boolean) value);
		} else if (valueType == String.class) {
			editor.putString(key, (String) value);
		} else if (valueType == Integer.class) {
			editor.putInt(key, (Integer) value);
		} else if (valueType == Float.class) {
			editor.putFloat(key, (Float) value);
		} else if (valueType == Long.class) {
			editor.putLong(key, (Long) value);
		} else if (valueType == Double.class) {
			editor.putFloat(key, ((Double) value).floatValue());
		} else if (isStringSet(valueType)) {
			@SuppressWarnings("unchecked")
			Set<String> set = (Set<String>) value;
			editor.putStringSet(key, set);
		} else if (valueType instanceof Class
				&& Enum.class.isAssignableFrom((Class<?>) valueType)) {
			editor.putInt(key, ((Enum<?>) value).ordinal());
		} else {
			editor.putString(key, JSONEncoder.encode(value));
		}
		if (editor != currentEditor) {// new edit ,no globals transaction
			apply(editor);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	protected T put(String key,Object value){
		this.put(type(key), key, value);
		return (T)this;
	}
	@SuppressWarnings({ "hiding", "unchecked" })
	protected <T> T get(final String key, T defaultValue) {
		if (!preferences.contains(key)) {
			return defaultValue;
		} else {
			final Type valueType = type(key);
			return (T) get(valueType, key);
		}
	}

	@SuppressWarnings({ "hiding", "unchecked" })
	protected <T> T get(final String key) {
		final Type valueType = type(key);
		if (!preferences.contains(key)) {
			return (T) defaultValue(valueType,key);
		} else {
			try {
				return (T) get(valueType, key);
			} catch (Exception e) {
				DebugLog.error(
						"sharepreferences read failed(auto reset later): ", e);
				put(valueType, key, null);
				return defaultValue(valueType,key);
			}
		}
	}

	Object get(final Type valueType, final String key,Object defaultValue) {
		if (!preferences.contains(key)) {
			return defaultValue;
		} else {
			return get(valueType, key);
		}
	}
	@SuppressWarnings("unchecked")
	Object get(final Type valueType, final String key) {
		Type clazz = toWrapperType(valueType);
		if (clazz == Boolean.class) {
			return preferences.getBoolean(key, false);
		} else if (clazz == String.class) {
			return preferences.getString(key, null);
		} else if (clazz == Integer.class) {
			return preferences.getInt(key, 0);
		} else if (clazz == Float.class) {
			return preferences.getFloat(key, 0);
		} else if (clazz == Long.class) {
			return preferences.getLong(key, 0);
		} else if (clazz instanceof Class
				&& Enum.class.isAssignableFrom((Class<?>) clazz)) {
			int iv = preferences.getInt(key, -1);
			return iv < 0 ? null : ReflectUtil
					.getEnum(iv, (Class<? extends Enum<?>>) clazz);
		} else if (isStringSet(clazz)) {
			return preferences.getStringSet(key, null);
		} else if (clazz == Double.class) {
			try {
				Map<String, ?> map = preferences.getAll();
				if (map != null) {
					Object value = map.get(key);
					if (value instanceof Number) {
						return ((Number) value).doubleValue();
					} else if (value instanceof String) {
						return Double.parseDouble((String) value);
					}

				}
			} catch (Exception e) {
			}
			return Double.valueOf(preferences.getFloat(key, 0));
		} else {
			String raw = preferences.getString(key, "");
			Object rtv = null;
			if (raw != null && raw.length() > 0) {
				try {
					rtv= JSON_DECODER.decodeObject(raw, clazz);
				} catch (Exception ex) {
					DebugLog.error(ex);
				}
			}
			if(rtv == null && type.isPrimitive()){
				return JSONDecoder.transform(null, type);
			}
			return rtv;
		}
	}

	private static boolean isStringSet(final Type valueType) {
		if (valueType instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) valueType;
			if (Set.class == pt.getRawType()) {
				Type[] aas = pt.getActualTypeArguments();
				if (aas.length == 1 && aas[0] == String.class) {
					return true;
				}
			}
		}
		return false;
	}

	private static Type toWrapperType(Type rt) {
		if (rt instanceof Class<?>) {
			rt = ReflectUtil.toWrapper((Class<?>) rt);
		}
		return rt;
	}

	public Object getSharedPreferences() {
		return preferences;
	}

}

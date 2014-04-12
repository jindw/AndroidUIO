package org.xidea.android.impl.io;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.IdentityHashMap;

import org.xidea.android.KeyValueStorage;
import org.xidea.android.KeyValueStorage.DefaultValue;
import org.xidea.android.impl.CommonLog;
import org.xidea.el.Invocable;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

@DefaultValue
@KeyValueStorage.StorageKey
public class KeyValueStorageImpl {
	private static final CommonLog log = CommonLog
			.getLog();
	private static final JSONDecoder JSON_DECODER = new JSONDecoder(false);
	private static final DefaultValue DEFAULT_VALUE = KeyValueStorageImpl.class
			.getAnnotation(DefaultValue.class);

	private static final Object[] EMPTY_OBJECTS = new Object[0];

	@SuppressWarnings("unchecked")
	static <T extends KeyValueStorage<?>> T buildKVStroage(final Class<T> type,
			Context context, String name) {
		final SharedPreferences preferences = context.getSharedPreferences(
				name, Activity.MODE_PRIVATE);
		return (T) Proxy.newProxyInstance(type.getClassLoader(),
				new Class[] { type }, new InvocationHandler() {
					HashMap<String, Invocable> impls = new HashMap<String, Invocable>();
					private Editor[] editorHolder = new Editor[1];

					@Override
					public Object invoke(Object thiz, Method method,
							Object[] args) throws Throwable {
						//log.timeStart();
						if (args == null) {// alliyun bug
							args = EMPTY_OBJECTS;
						}
						String name = method.getName();
						Invocable inv = impls.get(name);
						if (inv == null) {
							inv = KeyValueStorageImpl.buildInvocable(type,
									method, preferences, editorHolder);
							//System.err.println("create:"+method+"@"+System.identityHashCode(method)+(null==inv));
							if (inv == null) {
								return null;
							} else {
								impls.put(name, inv);
							}
						}
						Object value = inv.invoke(thiz, args);
						//log.timeEnd("keyValue."+method+value);
						return value;
					}
				});
	}

	public static Invocable buildInvocable(final Class<?> type, Method method,
			final SharedPreferences sharedPreferences,
			final Editor[] editorHolder) {
		Type returnType = method.getGenericReturnType();
		String name = method.getName();

		Class<?> declaringClass = method.getDeclaringClass();
		Class<?>[] paramsTypes = method.getParameterTypes();
		if (name.equals("toString") && paramsTypes.length == 0) {
			return new Invocable() {
				String label = type.getName() + "&"
						+ sharedPreferences.toString();

				@Override
				public Object invoke(Object thiz, Object... args)
						throws Exception {
					return label;
				}
			};
		} else if (declaringClass == KeyValueStorage.class) {
			Invocable inv = buildKeyValue(sharedPreferences, editorHolder,
					returnType, paramsTypes);
			if (inv != null) {
				return inv;
			}
		} else {
			// long t1 = System.nanoTime();
			String key = name.replaceAll("^(?:get|set|is)([A-Z])", "$1");
			if (!name.equals(key)) {
				key = Character.toLowerCase(key.charAt(0)) + key.substring(1);
				if (name.startsWith("is")) {
					if (returnType == Boolean.class
							|| returnType == Boolean.TYPE) {
						name = Character.toLowerCase(name.charAt(2))
								+ name.substring(3);
						return buildGetter(sharedPreferences, method,
								returnType, paramsTypes, key);
					}
				} else if (name.startsWith("set") && paramsTypes.length == 1) {
					return buildSetter(sharedPreferences, editorHolder,
							returnType!= Void.TYPE,paramsTypes[0], key);
				} else if (name.startsWith("get")) {
					return buildGetter(sharedPreferences, method, returnType,
							paramsTypes, key);
				}
			}

		}
		return new Invocable() {
			@Override
			public Object invoke(Object thiz, Object... args) throws Exception {
				throw new UnsupportedOperationException(
						"您调用了 KeyValue 存储实现不支持的的方法。");
			}
		};
	}

	private static Invocable buildSetter(final SharedPreferences preferences,
			final Editor[] editorHolder,final boolean returnThis, Class<?> type, final String name) {
		final Type valueType = toWrapperType(type);
		return new Invocable() {
			@Override
			public Object invoke(Object thiz, Object... args) throws Exception {
				Editor editor = editorHolder[0];
				boolean selfTransaction = editor == null;
				if (selfTransaction) {
					editor = preferences.edit();
				}
				Object value = args[0];
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
				if (selfTransaction) {
					editor.commit();
				}
				if(returnThis){
					return thiz;
				}else{
					return null;
				}
			}
		};
	}

	private static Invocable buildGetter(final SharedPreferences preferences,
			final Method method, final Type type, Class<?>[] paramsTypes,
			final String name) {
		DefaultValue dv = null;
		if (paramsTypes.length == 0) {
			dv = method.getAnnotation(DefaultValue.class);
			if (dv == null) {
				dv = DEFAULT_VALUE;
			}
		}
		final Type valueType = toWrapperType(type);
		final DefaultValue defaultValue = dv;
		return new Invocable() {
			private Object sharedDefaultValue;
			private boolean hasSharedValue;

			@Override
			public Object invoke(Object thiz, Object... args) throws Exception {
				if (!preferences.contains(name)) {
					return defaultValue(args);
				}
				if (valueType == Boolean.class) {
					return preferences.getBoolean(name, false);
				} else if (valueType == String.class) {
					return preferences.getString(name, null);
				} else if (valueType == Integer.class) {
					return preferences.getInt(name, 0);
				} else if (valueType == Float.class) {
					return preferences.getFloat(name, 0);
				} else if (valueType == Long.class) {
					return preferences.getLong(name, 0);
				} else if (valueType == Double.class) {
					return Double.valueOf(preferences.getFloat(name, 0));
				} else if (valueType instanceof Class
						&& Enum.class.isAssignableFrom((Class<?>) valueType)) {
					int iv = preferences.getInt(name, -1);
					return iv < 0 ? null : ReflectUtil.getEnum(iv,
							(Class<?>) valueType);
				} else {
					String raw = preferences.getString(name, "");
					if (raw != null && raw.length() > 0) {
						try {
							return JSON_DECODER.decode(raw, valueType);
						} catch (Exception ex) {
							log.error(ex);
						}
					}
					return null;
				}
			}

			private synchronized Object defaultValue(Object[] args) {
				if (defaultValue == null) {
					return args[0];
				} else if (!hasSharedValue) {
					double value = defaultValue.value();
					hasSharedValue = true;
					if (valueType instanceof Class && Enum.class.isAssignableFrom((Class<?>) valueType)) {
						sharedDefaultValue = defaultValue == null ? null : ReflectUtil.getEnum((int)value,(Class<?>) valueType);
					} else {
						String jsonValue = defaultValue.jsonValue();
						if ("null".equals(jsonValue)) {
							if (valueType == Boolean.class) {
								sharedDefaultValue = value != 0;
							} else if (valueType == Integer.class) {
								sharedDefaultValue = (int) value;
							} else if (valueType == Long.class) {
								sharedDefaultValue = (long) value;
							} else if (valueType == Float.class) {
								sharedDefaultValue = (float) value;
							} else if (valueType == Double.class) {
								sharedDefaultValue = (double) value;
							}
						} else {
							Object obj = JSON_DECODER.decode(jsonValue,
									valueType);
							if (valueType == String.class || obj == null || obj instanceof Number || obj instanceof Boolean) {
								sharedDefaultValue = obj;
							} else {
								hasSharedValue = false;
							}
							return obj;
						}
					}
				}

				return sharedDefaultValue;
			}
		};
	}

	private static Type toWrapperType(Type rt) {
		if (rt instanceof Class<?>) {
			rt = ReflectUtil.toWrapper((Class<?>) rt);
		}
		return rt;
	}

	private static Invocable buildKeyValue(final SharedPreferences sp,
			final Editor[] editorHolder, Type rt, Class<?>[] ps) {
		synchronized (sp) {
			if (rt == SharedPreferences.class) {// getSharedPrefrences
				return new Invocable() {
					@Override
					public Object invoke(Object thiz, Object... args)
							throws Exception {
						return sp;
					}
				};
			} else if (rt == Void.TYPE && ps.length == 0) {// ("commit".equals(name))
				// {
				return new Invocable() {
					@Override
					public Object invoke(Object thiz, Object... args)
							throws Exception {
						if (editorHolder[0] != null) {
							editorHolder[0].commit();
							editorHolder[0] = null;
						}
						return null;
					}
				};
			} else {// if
					// ("beginTransaction".equals(name))
					// {
				return new Invocable() {
					@Override
					public Object invoke(Object thiz, Object... args)
							throws Exception {
						if (editorHolder[0] == null) {
							editorHolder[0] = sp.edit();
						}
						return null;
					}
				};
			}
		}
	}
}

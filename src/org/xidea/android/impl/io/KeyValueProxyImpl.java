package org.xidea.android.impl.io;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xidea.android.KeyValueStorage;
import org.xidea.android.KeyValueStorage.DefaultValue;
import org.xidea.android.impl.DebugLog;
import org.xidea.el.Invocable;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;

@DefaultValue
@KeyValueStorage.StorageKey
class KeyValueProxyImpl {
	private static final DefaultValue DEFAULT_VALUE = KeyValueProxyImpl.class
			.getAnnotation(DefaultValue.class);

	private static final Object[] EMPTY_OBJECTS = new Object[0];

	protected static final JSONDecoder JSON_DECODER = new JSONDecoder(true);

	@SuppressWarnings("unchecked")
	static <T extends KeyValueStorage<?>> T create(final Class<T> type,
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
						// log.timeStart();
						if (args == null) {// alliyun bug
							args = EMPTY_OBJECTS;
						}
						String name = method.getName();
						Invocable inv = impls.get(name);
						if (inv == null) {
							inv = KeyValueProxyImpl.buildInvocable(type,
									method, preferences, editorHolder);
							// System.err.println("create:"+method+"@"+System.identityHashCode(method)+(null==inv));
							if (inv == null) {
								return null;
							} else {
								impls.put(name, inv);
							}
						}
						Object value = inv.invoke(thiz, args);
						// log.timeEnd("keyValue."+method+value);
						return value;
					}
				});
	}

	public static Invocable buildInvocable(final Class<?> type, final Method method,
			final SharedPreferences sharedPreferences,
			final Editor[] editorHolder) {
		final Type returnType = method.getGenericReturnType();
		final String name = method.getName();

		final Class<?>[] paramsTypes = method.getParameterTypes();

		// long t1 = System.nanoTime();
		String key = name.replaceAll("^(?:get|set|is)([A-Z])", "$1");
		if (!name.equals(key)) {
			key = Character.toLowerCase(key.charAt(0)) + key.substring(1);
			switch (name.charAt(0)) {
			case 'g':// (name.startsWith("get")) {
				if (paramsTypes.length == 0 && returnType != SharedPreferences.class) {
					return buildGetter(sharedPreferences, method, returnType,
							paramsTypes, key);
				}
				break;
			case 'i':// }else if (name.startsWith("is")) {
				if (returnType == Boolean.class || returnType == Boolean.TYPE) {
					return buildGetter(sharedPreferences, method, returnType,
							paramsTypes, key);
				}
				break;
			case 's':// case 's':// } else if (name.startsWith("set")
				if (paramsTypes.length == 1) {
					return buildSetter(sharedPreferences, editorHolder,
							returnType != Void.TYPE, paramsTypes[0], key);
				}
			//case 'r'://reset
			}
		}


		return new Invocable() {
			private boolean isBaseMethod = method.getDeclaringClass() == KeyValueStorage.class;
			private boolean isReset = name.equals("reset") ;
			private boolean isToString = name.equals("toString") ;
			@Override
			public Object invoke(Object thiz, Object... args) throws Exception {
				if (isBaseMethod) {
					if (returnType == Void.TYPE) {// ("commit".equals(name))
						if(isReset){
							Editor editor = editorHolder[0];
							if(editor == null){
								editor = sharedPreferences.edit();
								editor.clear();
							}
							apply(editor);
							return  thiz ;
						}else if (editorHolder[0] != null) {
							editorHolder[0].commit();
							editorHolder[0] = null;
						}
						return null;
					} else {// if( T "beginTransaction".equals(name))
						if (editorHolder[0] == null) {
							editorHolder[0] = sharedPreferences.edit();
						}
						return thiz;
					}
				}

				if (returnType == SharedPreferences.class) {// getSharedPrefrences
					return sharedPreferences;
				} 
				if(isToString){
					String label = type.getName() + "&"+ sharedPreferences;
					return label;
				}
				throw new UnsupportedOperationException(
						"您调用了 KeyValue 存储实现不支持的的方法。");
			}
		};
	}

	@SuppressLint("NewApi")
	private static void apply(Editor editor) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			editor.commit();
		}else {
			editor.apply();
		}
	}
	private static Invocable buildSetter(final SharedPreferences preferences,
			final Editor[] editorHolder, final boolean returnThis,
			Class<?> type, final String name) {
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
				} else if(isStringSet(valueType)){
					@SuppressWarnings("unchecked")
					Set<String> set = (Set<String>)value;
					editor.putStringSet(name, set);
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
					apply(editor);
				}
				if (returnThis) {
					return thiz;
				} else {
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
//		final Class<?> baseClass = ReflectUtil.baseClass(valueType);
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
				} else if (valueType instanceof Class && Enum.class.isAssignableFrom((Class<?>)valueType)) {
					int iv = preferences.getInt(name, -1);
					return iv < 0 ? null : ReflectUtil.getEnum(iv, (Class<?>)valueType);
				} else if(isStringSet(valueType)){
					return preferences.getStringSet(name, null);
				} else if (valueType == Double.class) {
					try{
						Map<String,?> map = preferences.getAll();
						if(map != null){
							Object value = map.get(name);
							if(value instanceof Number){
								return ((Number)value).doubleValue();
							}else if(value instanceof String){
								return Double.parseDouble((String)value);
							}
							
						}
					}catch(Exception e){
					}
					return Double.valueOf(preferences.getFloat(name, 0));
				} else{
					String raw = preferences.getString(name, "");
					if (raw != null && raw.length() > 0) {
						try {
							return JSON_DECODER.decodeObject(raw, valueType);
						} catch (Exception ex) {
							DebugLog.error(ex);
						}
					}
					return null;
				}
			}

			private Object defaultValue(Object[] args) {
				if (!hasSharedValue) {
					if (defaultValue == null) {
						return args[0];
					}
					double value = defaultValue.value();
					if (valueType instanceof Class
							&& Enum.class
									.isAssignableFrom((Class<?>) valueType)) {
						sharedDefaultValue = defaultValue == null ? null
								: ReflectUtil.getEnum((int) value,
										(Class<?>) valueType);
					} else {
						String jsonValue = defaultValue.jsonValue();
						if ("null".equals(jsonValue) || jsonValue.length() ==0) {
							if (valueType == Boolean.class) {
								sharedDefaultValue = value != 0;
							} else if (valueType instanceof Class ) {
								Class<?> type = (Class<?>)valueType;
								if( Number.class.isAssignableFrom(type)){
									sharedDefaultValue = ReflectUtil.toValue(value, type);
								}
							}
						} else {
							Object obj = JSON_DECODER.decodeObject(jsonValue,
									valueType);
							if (valueType == String.class || obj == null
									|| obj instanceof Number
									|| obj instanceof Boolean) {
								sharedDefaultValue = obj;
							} else {
								return obj;
							}
							
						}
					}
					this.hasSharedValue = true;
				}
				return sharedDefaultValue;
			}
		};
	}


	private static boolean isStringSet(final Type valueType) {
		if(valueType  instanceof ParameterizedType){
			ParameterizedType pt = (ParameterizedType)valueType;
			if(Set.class == pt.getRawType()){
				Type[] aas = pt.getActualTypeArguments();
				if(aas.length == 1 && aas[0] == String.class){
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

}

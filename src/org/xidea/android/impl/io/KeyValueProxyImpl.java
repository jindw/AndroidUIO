package org.xidea.android.impl.io;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;

import org.xidea.android.KeyValueStorage;
import org.xidea.el.Invocable;

import android.content.Context;
import android.content.SharedPreferences;

@KeyValueStorage.StorageKey
class KeyValueProxyImpl {

	private static final Object[] EMPTY_OBJECTS = new Object[0];

	@SuppressWarnings("unchecked")
	static <T extends KeyValueStorage<?>> T create(final Class<T> type,
			final Context context, final String name) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(),
				new Class[] { type }, new InvocationHandler() {
					KeyValueImpl<Object> base = new KeyValueImpl<Object>(context, type, name);
					// new KeyValueBase<KeyValueBase>(type,name);
					HashMap<String, Invocable> impls = new HashMap<String, Invocable>();

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
							inv = KeyValueProxyImpl.buildInvocable(thiz, base,
									type, method);
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

	public static Invocable buildInvocable(final Object proxy,
			final KeyValueImpl<?> base, final Class<?> type, final Method method) {
		final Type returnType = method.getGenericReturnType();
		final String name = method.getName();

		final Class<?>[] paramsTypes = method.getParameterTypes();

		// long t1 = System.nanoTime();
		String key = name.replaceAll("^(?:get|set|is)([A-Z])", "$1");
		if (!name.equals(key)) {
			key = Character.toLowerCase(key.charAt(0)) + key.substring(1);
			switch (name.charAt(0)) {
			case 'g':// (name.startsWith("get")) {
				if (paramsTypes.length == 0
						&& returnType != SharedPreferences.class) {
					return buildGetter(base, method, returnType, paramsTypes,
							key);
				}
				break;
			case 'i':// }else if (name.startsWith("is")) {
				if (returnType == Boolean.class || returnType == Boolean.TYPE) {
					return buildGetter(base, method, returnType, paramsTypes,
							key);
				}
				break;
			case 's':// case 's':// } else if (name.startsWith("set")
				if (paramsTypes.length == 1) {
					return buildSetter(base, returnType == Void.TYPE ? null
							: proxy, paramsTypes[0], key);
				}
				// case 'r'://reset
			}
		}

		return new Invocable() {
			private boolean isBaseMethod = method.getDeclaringClass() == KeyValueStorage.class;
			private boolean isReset = name.equals("reset");
			private boolean isToString = name.equals("toString");

			@Override
			public Object invoke(Object thiz, Object... args) throws Exception {
				if (isBaseMethod) {
					if (returnType == Void.TYPE) {// ("commit".equals(name))
						if (isReset) {
							base.reset();
						} else {
							base.commit();
						}
						return thiz;
					} else {// if( T "beginTransaction".equals(name))
						base.beginTransaction();
						return thiz;
					}
				}

				if (returnType == SharedPreferences.class) {// getSharedPrefrences
					return base.getSharedPreferences();
				}
				if (isToString) {
					String label = type.getName() + "&"
							+ base.getSharedPreferences();
					return label;
				}
				throw new UnsupportedOperationException(
						"您调用了 KeyValue 存储实现不支持的的方法。");
			}
		};
	}

	private static Invocable buildSetter(final KeyValueImpl<?> base,
			final Object rtv, final Class<?> type, final String name) {
		return new Invocable() {
			@Override
			public Object invoke(Object thiz, Object... args) throws Exception {
				Object value = args[0];
				base.put(type, name, value);
				return rtv;
			}
		};
	}

	private static Invocable buildGetter(final KeyValueImpl<?> base,
			final Method method, final Type type, Class<?>[] paramsTypes,
			final String name) {
		return new Invocable() {
			@Override
			public Object invoke(Object thiz, Object... args) throws Exception {
				if(args.length == 1){
					return base.get(type, name,args[0]);
				}
				return base.get(type, name);
			}

		};
	}

}

package org.xidea.android.impl.io;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import org.xidea.android.KeyValueStorage;
import org.xidea.android.SQLiteMapper;
import org.xidea.android.util.DebugLog;
import org.xidea.android.util.StorageFactory;

public class StorageFactoryImpl implements StorageFactory {
	private Map<Object, Object> cache = new HashMap<Object, Object>();

	protected StorageFactoryImpl() {
	}

	/**
	 * @hide
	 */
	public static final StorageFactoryImpl INSTANCE = new StorageFactoryImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.xidea.android.impl.io.StorageFactory#getSQLiteStorage(java.lang.Class
	 * , android.content.Context)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> SQLiteMapper<T> getSQLiteStorage(Class<T> type,
			Context application) {

		SQLiteMapper<T> impl = (SQLiteMapper<T>) cache.get(type);
		if (impl == null) {
			synchronized (cache) {
				impl = (SQLiteMapper<T>) cache.get(type);
				if (impl == null) {
					impl = new SQLiteMapperImpl<T>(application, type);
					cache.put(type, impl);
				}
			}
		}
		return impl;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.xidea.android.impl.io.StorageFactory#getKeyValueStroage(java.lang
	 * .Class, android.content.Context)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends KeyValueStorage<?>> T getKeyValueStroage(Class<T> type,
			Context application) {
		T impl = (T) cache.get(type);
		if (impl == null) {
			synchronized (cache) {
				impl = (T) cache.get(type);
				Class<?>[] impls ;
				if (type.isInterface()) {
					impls = type.getClasses();
				}else{
					impls = new Class[]{type};
				}
				if (impl == null) {
					for (Class<?> sub : impls) {
						if (type.isAssignableFrom(sub)) {
							try {
								impl = (T) type.newInstance();
								break;
							} catch (Exception e) {
								DebugLog.error(e);
							}
						}
					}
					if (impl == null) {
						String key = KeyValueImpl.getStorageKey(type);
						impl = KeyValueProxyImpl.create(type, application, key);
					}
					cache.put(type, impl);
				}
			}
		}
		return impl;
	}

	public DiskLruCache openCache(File dir, long maxCacheSize, int maxCount)
			throws IOException {
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return new DiskLruCacheImpl(dir, maxCacheSize, maxCount);
	}
}

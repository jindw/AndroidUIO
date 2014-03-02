package org.xidea.android.impl.io;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import org.xidea.android.KeyValueStorage;
import org.xidea.android.SQLiteMapper;
import org.xidea.android.impl.DebugLog;

public class StorageFactory {
	private Map<Object, Object> cache = new HashMap<Object, Object>();

	protected StorageFactory() {
	}

	/**
	 * @hide
	 */
	public static final StorageFactory INSTANCE = new StorageFactory();

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
	

	@SuppressWarnings("unchecked")
	public <T extends KeyValueStorage<?>> T getKeyValueStroage(Class<T> type,
			Context application) {
		if (!type.isInterface()) {
			DebugLog.error("getKeyValueStroage  must be a interface class！"
					+ type);
		}
		T impl = (T) cache.get(type);
		if (impl == null) {
			synchronized (cache) {
				impl = (T) cache.get(type);
				if (impl == null) {
					String key;
					try {
						KeyValueStorage.StorageKey field = type
								.getAnnotation(KeyValueStorage.StorageKey.class);
						key = (String) field.value();
					} catch (Exception e) {
						key = type.getName();
					}
					impl = KeyValueProxyImpl.create(type,
							application, key);
					cache.put(type, impl);
				}
			}
		}
		return impl;
	}

	public DiskLruCache openCache(File dir, long maxCacheSize, int maxCount) throws IOException{
			if (!dir.exists()) {
				dir.mkdirs();
			}
			return new DiskLruCacheImpl(dir, maxCacheSize, maxCount);
	}
}

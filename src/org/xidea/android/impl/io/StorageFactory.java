package org.xidea.android.impl.io;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import org.xidea.android.KeyValueStorage;
import org.xidea.android.SQLiteMapper;
import org.xidea.android.impl.CommonLog;

public class StorageFactory {
	private static final org.apache.commons.logging.Log log = CommonLog
			.getLog();
	private Map<Object,Object> cache = new HashMap<Object,Object>();

	private StorageFactory() {
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
				if(impl == null){
					impl = new SQLiteMapperImpl<T>(application, type);
					cache.put(type, impl);
				}
			}
		}
		return impl;

	}

	@SuppressWarnings("unchecked")
	public <T extends KeyValueStorage<?>> T getKVStroage(Class<T> type,
			Context application) {
		if (!type.isInterface()) {
			log.error("KvStroage  必须从接口创建！"
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
						key = field == null ? "default" : (String) field
								.value();
					} catch (Exception e) {
						key = type.getName();
					}
					impl = KeyValueStorageImpl.buildKVStroage(type, application, key);
					cache.put(type, impl);
				}
			}
		}
		return impl;
	}


}

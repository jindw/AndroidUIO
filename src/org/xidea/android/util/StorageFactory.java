package org.xidea.android.util;

import org.xidea.android.KeyValueStorage;
import org.xidea.android.SQLiteMapper;

import android.content.Context;

public interface StorageFactory {

	public abstract <T> SQLiteMapper<T> getSQLiteStorage(Class<T> type,
			Context application);

	public abstract <T extends KeyValueStorage<?>> T getKeyValueStroage(
			Class<T> type, Context application);

}
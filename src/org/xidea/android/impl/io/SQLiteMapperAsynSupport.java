package org.xidea.android.impl.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import android.os.Handler;
import android.os.Looper;

import org.xidea.android.Callback;
import org.xidea.android.SQLiteMapper;

abstract class SQLiteMapperAsynSupport<T> implements SQLiteMapper<T>{

	private static ExecutorService executorService = Executors
			.newSingleThreadScheduledExecutor();

	enum COMMAND {
		SAVE, QUERY, UPDATE, 
		UPDATE_CONTENT,
		REMOVE,SAVE_BAT,GET,GET_BY_KEY
	}

	@Override
	public SQLiteMapper<T> query(final Callback<List<T>> callback, final String where,
			final Object... selectionArgs) {
		invoke(callback, where, selectionArgs, COMMAND.QUERY);
		return this;
	}
	@Override
	public SQLiteMapper<T> remove(final Callback<Boolean> callback, final Object id) {
		invoke(callback, id, null, COMMAND.REMOVE);
		return this;
	}

	@Override
	public SQLiteMapper<T> save(final Callback<T> callback, final T t) {
		invoke(callback, t, null, COMMAND.SAVE);
		return this;
	}
	@Override
    public SQLiteMapper<T> save(Callback<List<T>> callback, List<T> t) {
	    invoke(callback, t, null, COMMAND.SAVE_BAT);
	    return this;
    }
	@Override
	public SQLiteMapper<T> update(Callback<Boolean> callback, T t) {
		invoke(callback, t, null, COMMAND.UPDATE);
		return this;
	}

	@Override
	public SQLiteMapper<T> update(Callback<Boolean> callback, Map<String,Object> contents) {
		invoke(callback, contents, null, COMMAND.UPDATE_CONTENT);
		return this;
	}

	private void invoke(@SuppressWarnings("rawtypes") final Callback callback,
			final Object arg1, final Object arg2, final COMMAND m) {
		final Looper looper = Looper.myLooper();
		executorService.execute(new Runnable() {
			Object data;
			boolean loaded;

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				if (loaded) {
					callback.callback(data);
				} else {
					loaded = true;
					switch (m) {
					case SAVE:
						data = save((T) arg1);
						break;
					case UPDATE:
						data = update((T) arg1);
						break;
					case UPDATE_CONTENT:
						data = update((Map<String,Object>) arg1);
						break;
					case REMOVE:
						data = remove( arg1);
						break;
					case QUERY:
						data = query((String) arg1, (Object[]) arg2);
						break;
					case SAVE_BAT:
					    data = new ArrayList<T>();
					    List<T> os = (List<T>)arg1;
					    for(T item:os){
					        ((List<T>)data).add(save(item));
					    }
					    break;
					case GET:
						data = get( arg1);
						break;
					case GET_BY_KEY:
						data = getByKey((String) arg1,arg2);
						break;

					}
					if (looper == null) {
						this.run();
					} else {
						new Handler(looper).post(this);
					}
				}

			}
		});

	}

	@Override
	public SQLiteMapper<T> get(Callback<T> receiver, Object id) {
		invoke(receiver, id, null,COMMAND.GET );
		return this;
	}

	@Override
	public SQLiteMapper<T> getByKey(Callback<T> receiver, String field, Object value) {
		invoke(receiver, field, value,COMMAND.GET_BY_KEY );
		return this;
	}

}

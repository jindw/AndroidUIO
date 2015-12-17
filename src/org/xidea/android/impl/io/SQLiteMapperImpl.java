package org.xidea.android.impl.io;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xidea.android.Callback;
import org.xidea.android.SQLiteMapper;
import org.xidea.android.util.DebugLog;
import org.xidea.el.impl.ReflectUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class SQLiteMapperImpl<T> extends SQLiteMapperAsynSupport<T> implements
		SQLiteMapper<T> {
	private SQLiteOpenHelper helper;
	private String tableName;
	private String primaryField;
	private ArrayList<Field> fields = new ArrayList<Field>();
	private HashMap<String, Field> fieldMap = new HashMap<String, Field>();
	private final Class<T> type;
	private String autoField;
	private Object lock = new Object();
	
//	private HashMap<String, Reference<T>> idCacheMap = null;//new HashMap<String, Reference<T>>();
//	private WeakHashMap<T, Map<String, Object>> backupMap = null;//new WeakHashMap<T, Map<String, Object>>();

	protected SQLiteMapperImpl(final Context context, final Class<T> c) {
		this.type = c;
		final Object initLock = new Object();
		new Thread() {// 创建过程异步化
			public void run() {
				synchronized (lock) {
					synchronized (initLock) {
						initLock.notifyAll();
					}
					init(context);
				}
			}
		}.start();
		try {
			synchronized (initLock) {
				initLock.wait(1000);
			}
		} catch (InterruptedException e) {
			DebugLog.error(e);
		}
	}

	private void init(Context context) {
		int version = initMeta();
		helper = new SQLiteOpenHelper(context, tableName, null, version) {
			public void onDowngrade(SQLiteDatabase db, int oldVersion,
					int newVersion) {
				// update by default;
				db.execSQL("DROP TABLE IF EXISTS  " + tableName);
				db.execSQL(SQLUtils.buildCreateSQL(tableName,fields));
			}

			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion,
					int newVersion) {
				if (oldVersion < newVersion) {
					Method[] ms = type.getDeclaredMethods();
					if (ms != null) {
						outer:for (Method m : ms) {
							Class<?>[] parameterTypes = m.getParameterTypes();
							if (Modifier.isStatic(m.getModifiers())
									&& parameterTypes.length == 2
									&& parameterTypes[0] == SQLiteDatabase.class
									&& parameterTypes[1] == String.class) {
								SQLiteUpdate u = m
										.getAnnotation(SQLiteUpdate.class);
								if (u != null) {
									int[] froms = u.value();
									for (int v : froms) {
										if (oldVersion == v) {
											try {
												m.invoke(null, db, tableName);
												return;
											} catch (Exception e) {
												DebugLog.warn(e);
											}
											break outer;

										}
									}
								}
							}
						}
					}
				}
				// update by default;
				onDowngrade(db, oldVersion, newVersion);
			}

			@Override
			public void onCreate(SQLiteDatabase db) {
				db.execSQL(SQLUtils.buildCreateSQL(tableName,fields));
			}
		};
	}

	private int initMeta() {
		for (Field f : type.getDeclaredFields()) {
			SQLiteProperty prop = f.getAnnotation(SQLiteProperty.class);
			if (prop != null) {
				fields.add(f);
				String fn = f.getName();
				fieldMap.put(fn, f);
				if (this.primaryField == null
						&& prop.value().indexOf("PRIMARY") >= 0) {
					if (prop.value().indexOf("AUTOINCREMENT") >= 0) {
						this.autoField = fn;
					}
					this.primaryField = fn;
				}
				f.setAccessible(true);
			}
		}

		final SQLiteEntry entry = type.getAnnotation(SQLiteEntry.class);
		if (entry == null) {
			this.tableName = type.getSimpleName();
			return 1;
		} else {
			String name = entry.name();
			this.tableName = name.length() == 0 ? type.getSimpleName() : name;
			return entry.version();
		}

	}

	@Override
	public T get(Object id) {
		return getByKey(this.primaryField, id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xidea.android.internal.ObjectMapper#getByKey(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public T getByKey(String field, Object value) {
		synchronized (lock) {
			try {
				SQLiteDatabase db = this.helper.getReadableDatabase();
				try {
					db.beginTransaction();
					Cursor cursor = db.query(tableName, null, field + "=?",
							new String[] { String.valueOf(SQLUtils.toQueryArg(value)) },
							null, null, null, null);
					try {
						if (cursor.moveToFirst()) {
							T o = SQLUtils.toObject(type,fields,cursor);
							return o;
						}
					} finally {
						cursor.close();
					}
				} finally {
					db.endTransaction();
					db.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return null;
	}

	@Override
	public List<T> query(String where, Object... selectionArgs) {
		synchronized (lock) {
			SQLiteDatabase db = this.helper.getReadableDatabase();
			try {
				db.beginTransaction();
				String[] args = SQLUtils.toQueryArgs(selectionArgs);
				Cursor cursor = db.query(tableName, null, where, args, null,
						null, null);
				try {
					ArrayList<T> result = new ArrayList<T>(cursor.getCount());
					while (cursor.moveToNext()) {
						result.add(SQLUtils.toObject(type,fields,cursor));
					}
					return result;
				} finally {
					cursor.close();
				}
			} finally {
				db.endTransaction();
				db.close();
			}
		}
	}

	public void execSQL(String sql, Object... bindArgs) {
		synchronized (lock) {
			SQLiteDatabase db = this.helper.getReadableDatabase();
			try {
				db.beginTransaction();
				try {
					for (int i = 0; i < bindArgs.length; i++) {
						bindArgs[i] = SQLUtils.toQueryArg(bindArgs[i]);
					}
					db.execSQL(sql, bindArgs);
				} finally {
					db.endTransaction();
				}
			} finally {
				db.close();
			}
		}
	}


	public void querySQL(Callback<Cursor> callback, String sql,
			Object... bindArgs) {
		synchronized (lock) {
			SQLiteDatabase db = this.helper.getReadableDatabase();
			try {
				db.beginTransaction();
				Cursor cursor = db.rawQuery(sql, SQLUtils.toQueryArgs(bindArgs));
				try {
					callback.callback(cursor);
					db.setTransactionSuccessful();
				} finally {
					cursor.close();
				}
			} finally {
				db.endTransaction();
				db.close();
			}
		}
	}

	@Override
	public T save(T t) {
		ContentValues values = SQLUtils.buildContent(t, fields);
		return save(t, values);
	}

	@Override
	public boolean update(T t) {
		ContentValues contents = SQLUtils.buildContent(t, fields);
		return this.update(contents);
	}

	public T saveOrUpdate(T t) {
		ContentValues values = SQLUtils.buildContent(t, fields);
		boolean existed = this.update(values);
		if (!existed) {
			this.save(t, values);
		}
		return t;
	}

	private T save(T t, ContentValues values) {
		synchronized (lock) {
			SQLiteDatabase db = this.helper.getWritableDatabase();
			try {
				if (autoField != null) {
					values.remove(autoField);
				}
				try {
					db.beginTransaction();
					Object id = -1;
					id = db.insert(tableName, null, values);
					if (autoField != null) {
						ReflectUtil.setValue(t, autoField, id);
					} else {
						id = ReflectUtil.getValue(t, primaryField);
					}
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}

			} finally {
				db.close();
			}
		}
		return t;
	}

	public boolean update(Map<String,Object> contents) {
		return update(SQLUtils.toContent(contents));
	}
	private boolean update(ContentValues contents) {
		Object rid = contents.get(this.primaryField);
		if (rid == null) {
			return false;
		}
		synchronized (lock) {
			String id = String.valueOf(rid);
			SQLiteDatabase db = this.helper.getWritableDatabase();
			try {
				db.beginTransaction();
				try {
					int c = db.update(tableName, contents, this.primaryField
							+ "=?", new String[] { id });
					db.setTransactionSuccessful();
					return c > 0;
				} finally {
					db.endTransaction();
				}
			} finally {
				db.close();
			}
		}
	}

	@Override
	public boolean remove(Object id) {
		if (id == null) {
			return false;
		}
		synchronized (lock) {
			SQLiteDatabase db = this.helper.getWritableDatabase();
			try {
				db.beginTransaction();
				try {
					int c = db.delete(tableName, this.primaryField + "=?",
							new String[] { String.valueOf(id) });
					db.setTransactionSuccessful();
					if (c > 0) {
						return true;
					}
					return false;
				} finally {
					db.endTransaction();
				}
			} finally {
				db.close();
			}
		}
	}

	/** read only query */

	@Override
	public int count() {
		return count(null);
	}

	@Override
	public int count(String where, Object... selectionArgs) {
		synchronized (lock) {
			SQLiteDatabase db = this.helper.getReadableDatabase();
			try {
				db.beginTransaction();
				String[] args = SQLUtils.toQueryArgs(selectionArgs);
				Cursor cursor = db.query(tableName,
						new String[] { "count(*)" }, where, args, null, null,
						null);
				try {
					int result = 0;
					if (cursor.moveToNext()) {
						result = cursor.getInt(0);
					}
					return result;
				} finally {
					cursor.close();
				}
			} finally {
				db.endTransaction();
				db.close();
			}
		}
	}

}

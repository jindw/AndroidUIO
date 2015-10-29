package org.xidea.android.impl.io;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.xidea.android.Callback;
import org.xidea.android.SQLiteMapper;
import org.xidea.android.util.DebugLog;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

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
	private HashMap<String, Reference<T>> idCacheMap = new HashMap<String, Reference<T>>();
	private WeakHashMap<T, Map<String,Object>> backupMap = new WeakHashMap<T, Map<String,Object>>();
	private Class<T> type;
	private String autoField;

	private Object lock = new Object();

	protected SQLiteMapperImpl(Context context, Class<T> c) {
		initFields(c);
		int version = initVersion(c);
		this.helper = new SQLiteOpenHelper(context, this.tableName, null,
				version) {
			public void onDowngrade(SQLiteDatabase db, int oldVersion,
					int newVersion) {
				// update by default;
				db.execSQL("DROP TABLE IF EXISTS  " + tableName);
				db.execSQL(getCreateSQL());
			}

			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion,
					int newVersion) {
				if (oldVersion < newVersion) {
					Method[] ms = type.getDeclaredMethods();
					if (ms != null) {
						for (Method m : ms) {
							Class<?>[] parameterTypes = m.getParameterTypes();
							if (Modifier.isStatic(m.getModifiers())
									&& parameterTypes.length == 2
									&& parameterTypes[0] == SQLiteDatabase.class
									&& parameterTypes[1] == String.class) {
								SQLiteUpdate u = m
										.getAnnotation(SQLiteUpdate.class);
								if (u != null && u.value() == oldVersion) {
									try {
										m.invoke(null, db, tableName);
										return;
									} catch (Exception e) {
										DebugLog.warn(e);
									}
									break;
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
				db.execSQL(getCreateSQL());
			}
		};
	}

	private void initFields(Class<T> c) {
		for (Field f : c.getDeclaredFields()) {
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
	}

	private int initVersion(Class<T> c) {
		this.type = c;
		final SQLiteEntry entry = c.getAnnotation(SQLiteEntry.class);
		if (entry == null) {
			this.tableName = c.getSimpleName();
			return 1;
		} else {
			String name = entry.name();
			this.tableName = name.length() == 0 ? c.getSimpleName() : name;
			return entry.version();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xidea.android.internal.ObjectMapper#get(java.lang.Object)
	 */
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
			boolean isPrimary = field.equals(primaryField);
			if (isPrimary) {
				Reference<T> ref = idCacheMap.get(value);
				if (ref != null) {
					T o = ref.get();
					if (o != null) {
						return o;
					}
				}
			}
			try {
				SQLiteDatabase db = this.helper.getReadableDatabase();
				try {
					db.beginTransaction();
					Cursor cursor = db.query(tableName, null, field + "=?",
							new String[] { String.valueOf(toArg(value)) },
							null, null, null, null);
					try {
						if (cursor.moveToFirst()) {
							T o = toObject(cursor);
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
				String[] args = toQueryParams(selectionArgs);
				Cursor cursor = db.query(tableName, null, where, args, null,
						null, null);
				try {
					ArrayList<T> result = new ArrayList<T>(cursor.getCount());
					while (cursor.moveToNext()) {
						result.add(toObject(cursor));
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

	private String[] toQueryParams(Object... selectionArgs) {
		if (selectionArgs == null) {
			return null;
		}
		String[] args = new String[selectionArgs.length];
		for (int i = 0; i < args.length; i++) {
			Object value = selectionArgs[i];
			args[i] = String.valueOf(toArg(value));
		}
		return args;
	}

	@SuppressWarnings("rawtypes")
	private Object toArg(Object value) {
		return value instanceof Enum ? ((Enum) value).ordinal() : value;
	}

	public void execSQL(String sql, Object... bindArgs) {
		synchronized (lock) {
			backupMap.clear();
			idCacheMap.clear();
			SQLiteDatabase db = this.helper.getReadableDatabase();
			try {
				db.beginTransaction();
				try {
					for (int i = 0; i < bindArgs.length; i++) {
						bindArgs[i] = toArg(bindArgs[i]);
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
				Cursor cursor = db.rawQuery(sql, toQueryParams(bindArgs));
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
		Map<String, Object> backup=new HashMap<String, Object>();
		ContentValues values = buildContent(t,backup);
		return save(t, values,backup);
	}

	public T saveOrUpdate(T t) {
		Map<String, Object> mapContents=new HashMap<String, Object>();
		ContentValues values = buildContent(t,mapContents);
		boolean existed = this.update(values,t);
		if (!existed) {
			this.save(t, values,mapContents);
		}
		return t;
	}

	private T save(T t, ContentValues values,Map<String, Object> backup) {
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
					// save cache
					backupMap.put(t, backup);
					idCacheMap.put(String.valueOf(id), new WeakReference<T>(t));
				} finally {
					db.endTransaction();
				}

			} finally {
				db.close();
			}
		}
		return t;
	}

	@Override
	public boolean update(T t) {
		ContentValues values = buildContent(t,null);
		return this.update(values,t);
	}


	private boolean update(ContentValues contents, T object) {
		Object rid = contents.get(this.primaryField);
		if(rid == null){
			return false;
		}
		synchronized (lock) {
			String id = String.valueOf(rid);
			Reference<T> ref = idCacheMap.get(id);
			Map<String, Object> backupValues = backupMap.get(object);
			if (ref != null) {
				T refObject = ref.get();
				if (object != refObject) {
					// update idcachemap
					idCacheMap.remove(id);
				}
			}
			if (backupValues !=null) {//减少更新数据
				contents = new ContentValues(contents);
				Iterator<Entry<String, Object>> it = contents.valueSet().iterator();
				while (it.hasNext()) {
					Entry<String, Object> e = it.next();
					String key = e.getKey();
					Object backupValue = backupValues.get(key);
				
					if(backupValue != null && backupValue.equals(e.getValue())){
						//DebugLog.info("ignore nochange cloumn:"+key);
						it.remove();
					}else{
						backupValues.put(key,ReflectUtil.getValue(object, key));
					}
				}
				
			}
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
						idCacheMap.remove(id);
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
				String[] args = null;
				if (where != null && selectionArgs != null) {
					List<String> aList = new ArrayList<String>();
					for (Object obj : selectionArgs) {
						if (obj != null) {
							aList.add(String.valueOf(obj));
						}
					}
					if (aList.size() > 0) {
						args = new String[aList.size()];
						aList.toArray(args);
					}
				}
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

	/****/

	@SuppressWarnings("rawtypes")
	private ContentValues buildContent(T t,Map<String,Object> backup) {
		ContentValues values = new ContentValues();
		for (Field f : fields) {
			String name = f.getName();
			Object value = null;
			try {
				value = f.get(t);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			Class<?> fieldType = ReflectUtil.toWrapper(f.getType());
			if(backup != null){
				backup.put(name, value);
			}

			if (value != null) {
				if (String.class == fieldType) {
					values.put(name, (String) value);
				} else if (URL.class == fieldType || URI.class == fieldType) {
					values.put(name, String.valueOf(value));
				} else if (Integer.class.isAssignableFrom(fieldType)
						|| Byte.class.isAssignableFrom(fieldType)
						|| Short.class.isAssignableFrom(fieldType)) {
					values.put(name, ((Number) value).intValue());
				} else if (Float.class.isAssignableFrom(fieldType)) {
					values.put(name, (Float) value);
				} else if (Double.class.isAssignableFrom(fieldType)) {
					values.put(name, (Double) value);
				} else if (Long.class.isAssignableFrom(fieldType)) {
					values.put(name, (Long) value);
				} else if (Boolean.class.isAssignableFrom(fieldType)) {
					values.put(name, ((Boolean) value) ? 1 : 0);
				} else if (Date.class.isAssignableFrom(fieldType)) {
					values.put(name, ((Date) value).getTime());
				} else if (byte[].class.isAssignableFrom(fieldType)) {
					values.put(name, (byte[]) value);
				} else if (Enum.class.isAssignableFrom(fieldType)) {
					values.put(name, ((Enum) value).ordinal());
				} else {
					values.put(name, JSONEncoder.encode(value));
				}
			}

		}
		return values;
	}

	private T toObject(Cursor cursor) {
		T o;
		try {
			o = type.newInstance();
		} catch (InstantiationException e) {
			DebugLog.error("对象创建失败:" + type, e);
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			DebugLog.error("对象创建失败:" + type, e);
			throw new RuntimeException(e);
		}
		//query backup
		HashMap<String, Object> cacheMap = new HashMap<String, Object>();
		this.backupMap.put(o, cacheMap);
		
		for (Field field : this.fields) {
			String name = field.getName();
			int i = cursor.getColumnIndex(name);
			if (i < 0) {
				DebugLog.warn("缺少属性:" + type + "#" + name);
				continue;
			}
			Class<?> fieldType = ReflectUtil.toWrapper(field.getType());
			Object value = toObject(cursor, i, fieldType);
			ReflectUtil.setValue(o, name, value);
			cacheMap.put(name, value);
		}
		return o;
	}

	private Object toObject(Cursor cursor, int i, Class<?> fieldType) {
		Object value = null;
		if (String.class == fieldType) {
			value = cursor.getString(i);
		} else if (URL.class == fieldType) {
			try {
				value = new URL(cursor.getString(i));
			} catch (MalformedURLException e) {
				DebugLog.warn("无效URL:" + type, e);
			}
		} else if (URI.class == fieldType) {
			value = URI.create(cursor.getString(i));
		} else if (Number.class.isAssignableFrom(fieldType)) {
			if (Float.class == fieldType) {
				value = cursor.getFloat(i);
			} else if (Double.class == fieldType) {
				value = cursor.getDouble(i);
			} else if (Long.class == fieldType) {
				value = cursor.getLong(i);
			} else {
				value = cursor.getInt(i);
			}
		} else if (Boolean.class == fieldType) {
			value = cursor.getInt(i) != 0;
		} else if (Date.class == fieldType) {
			value = new Date(cursor.getLong(i));
		} else if (byte[].class == fieldType) {
			value = cursor.getBlob(i);
		} else if (Enum.class.isAssignableFrom(fieldType)) {
			int ordi = cursor.getInt(i);
			value = ReflectUtil.getEnum(ordi, fieldType);
		} else {
			String text = cursor.getString(i);
			try {
				if (text != null) {
					value = JSONDecoder.decode(text, fieldType);
				}
			} catch (Exception e) {
				DebugLog.error("数据转换失败:" + type, e);
			}
		}
		return value;
	}

	private String getCreateSQL() {
		StringBuilder buf = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
				.append(tableName).append('(');
		StringBuilder index = new StringBuilder();
		for (Field f : fields) {
			SQLiteProperty prop = f.getAnnotation(SQLiteProperty.class);
			Class<?> type = ReflectUtil.toWrapper(f.getType());
			String name = f.getName();
			buf.append(name).append(' ');
			if (prop.index()) {
				if (index.length() > 0) {
					index.append(',');
				}
				index.append(name);
			}
			if (String.class == type) {
				buf.append("TEXT");
			} else if (Float.class.isAssignableFrom(type)
					|| Double.class.isAssignableFrom(type)) {
				buf.append("REAL");
			} else if (Number.class.isAssignableFrom(type)
					// || Integer.class.isAssignableFrom(type)
					// || Long.class.isAssignableFrom(type)
					// || Byte.class.isAssignableFrom(type)
					// || Short.class.isAssignableFrom(type)
					|| Date.class.isAssignableFrom(type)
					|| Boolean.class.isAssignableFrom(type)
					|| Enum.class.isAssignableFrom(type)) {
				buf.append("INTEGER");
			} else if (byte[].class.isAssignableFrom(type)) {
				buf.append("BLOB");
			} else {
				buf.append("TEXT");
			}
			buf.append(' ').append(prop.value());
			buf.append(',');
		}
		buf.setCharAt(buf.length() - 1, ')');
		if (index.length() > 0) {
			buf.append("; CREATE INDEX mapper_index ON ").append(tableName)
					.append('(').append(index).append(')');
		}
		buf.append("; ");
		// System.out.println(buf);
		return buf.toString();
	}
}

package org.xidea.android.impl.io;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;

import org.xidea.android.Callback;
import org.xidea.android.SQLiteMapper;
import org.xidea.android.impl.CommonLog;

public class SQLiteMapperImpl<T> implements SQLiteMapper<T> {

	private static Log log = CommonLog.getLog();
	private SQLiteOpenHelper helper;
	private String tableName;
	private String primaryField;
	private ArrayList<Field> fields = new ArrayList<Field>();
	private HashMap<String, Field> fieldMap = new HashMap<String, Field>();
	private Class<T> type;
	private String autoField;

	private Object lock = new Object();

	public SQLiteMapperImpl(Context context, Class<T> c) {
		initFields(c);
		int version = initVersion(c);
		this.helper = new SQLiteOpenHelper(context, this.tableName, null,
				version) {
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
										log.warn(e);
									}
									break;
								}
							}
						}
					}
					// update by default;
					db.execSQL("DROP TABLE IF EXISTS  " + tableName);
					db.execSQL(getCreateSQL());
				}
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
			this.tableName = name.length() == 0?c.getSimpleName():name;
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
	 * @see
	 * org.xidea.android.internal.ObjectMapper#getByKey(java.lang.String,
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
							new String[] { String.valueOf(toArg(value)) },
							null, null, null, null);
					try {
						if (cursor.moveToFirst()) {
							return toObject(cursor);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xidea.android.internal.ObjectMapper#query(java.lang.String,
	 * java.lang.Object)
	 */
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
		synchronized (lock) {
			SQLiteDatabase db = this.helper.getWritableDatabase();
			try {
				ContentValues values = buildContent(t);
				if (autoField != null) {
					values.remove(autoField);
				}
				try {
					db.beginTransaction();
					long id = -1;
					id = db.insert(tableName, null, values);
					if (autoField != null) {
						ReflectUtil.setValue(t, autoField, id);
					}
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}

			} finally {
				db.close();
			}
			return t;
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
					return c>0;
				} finally {
					db.endTransaction();
				}
			} finally {
				db.close();
			}
		}

	}

	@Override
	public boolean update(ContentValues contents) {
		synchronized (lock) {
			SQLiteDatabase db = this.helper.getWritableDatabase();
			try {
				db.beginTransaction();
				try {
					int c = db.update(tableName, contents, this.primaryField + "=?",
							new String[] { String.valueOf(contents
									.get(this.primaryField)) });
					db.setTransactionSuccessful();
					return c>0;
				} finally {
					db.endTransaction();
				}
			} finally {
				db.close();
			}
		}
	}

	@Override
	public boolean update(T t) {
		ContentValues values = buildContent(t);
		return this.update(values);
	}

	/****/

	@SuppressWarnings("rawtypes")
	private ContentValues buildContent(T t) {
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

			if (value == null) {

			} else if (String.class == fieldType) {
				values.put(name, (String) value);
			} else if (URL.class == fieldType || URI.class == fieldType) {
				values.put(name, String.valueOf(value));
			} else if (Number.class.isAssignableFrom(fieldType)) {
				values.put(name, ((Number) value).longValue());
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
		return values;
	}

	private T toObject(Cursor cursor) {
		T o;
		try {
			o = type.newInstance();
		} catch (InstantiationException e) {
			log.error("对象创建失败:"+type,e);
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			log.error("对象创建失败:"+type,e);
			throw new RuntimeException(e);
		}
		for (Field field : this.fields) {
			String name = field.getName();
			int i = cursor.getColumnIndex(name);
			if (i < 0) {
				log.warn("缺少属性:"+type+"#"+name);
				continue;
			}
			Class<?> fieldType = ReflectUtil.toWrapper(field.getType());
			Object value = null;

			if (String.class == fieldType) {
				value = cursor.getString(i);
			} else if (URL.class == fieldType) {
				try {
					value = new URL(cursor.getString(i));
				} catch (MalformedURLException e) {
					log.warn("无效URL:"+type,e);
				}
			} else if (URI.class == fieldType) {
				value = URI.create(cursor.getString(i));
			} else if (Number.class.isAssignableFrom(fieldType)) {
				if(Float.class == fieldType ){
					value = cursor.getFloat(i);
				}else if(Double.class == fieldType){
					value = cursor.getDouble(i);
				}else if(Long.class == fieldType){
					value = cursor.getLong(i);
				}else {
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
						value = new JSONDecoder(false).decode(text, fieldType);
					}
				} catch (Exception e) {
					log.error("数据转换失败:"+type,e);
				}
			}
			ReflectUtil.setValue(o, name, value);
		}
		return o;
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
			} else if (Number.class.isAssignableFrom(type)
					|| Boolean.class.isAssignableFrom(type)
					|| Date.class.isAssignableFrom(type)
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
		//System.out.println(buf);
		return buf.toString();
	}

	private static ExecutorService executorService = Executors
			.newSingleThreadScheduledExecutor();

	enum COMMAND {
		SAVE, QUERY, UPDATE, UPDATE_CONTENT, REMOVE,SAVE_BAT,GET,GET_BY_KEY
	}

	@Override
	public void query(final Callback<List<T>> callback, final String where,
			final Object... selectionArgs) {
		invoke(callback, where, selectionArgs, COMMAND.QUERY);
	}

	@Override
	public void remove(final Callback<Boolean> callback, final Object id) {
		invoke(callback, id, null, COMMAND.REMOVE);
	}

	@Override
	public void save(final Callback<T> callback, final T t) {
		invoke(callback, t, null, COMMAND.SAVE);
	}
	@Override
    public void save(Callback<List<T>> callback, List<T> t) {
	    invoke(callback, t, null, COMMAND.SAVE_BAT);
    }
	@Override
	public void update(Callback<Boolean> callback, T t) {
		invoke(callback, t, null, COMMAND.UPDATE);

	}

	@Override
	public void update(Callback<Boolean> callback, ContentValues contents) {
		invoke(callback, contents, null, COMMAND.UPDATE_CONTENT);

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
						data = update((ContentValues) arg1);
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
	public void get(Callback<T> receiver, Object id) {
		invoke(receiver, id, null,COMMAND.GET );
	}

	@Override
	public void getByKey(Callback<T> receiver, String field, Object value) {
		invoke(receiver, field, value,COMMAND.GET_BY_KEY );
	}

}

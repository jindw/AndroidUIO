package org.xidea.android.impl.io;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xidea.android.SQLiteMapper.SQLiteProperty;
import org.xidea.android.util.DebugLog;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

import android.content.ContentValues;
import android.database.Cursor;

class SQLUtils {

	static String buildCreateSQL(String tableName, List<Field> fields) {
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

	static <T> ContentValues buildContent(T t, List<Field> fields) {
		Map<String, Object> backup = new HashMap<String, Object>();
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
			if (backup != null) {
				backup.put(name, value);
			}
			SQLUtils.putValues(values, name, value, fieldType);
		}
		return values;
	}

	static <T> T toObject(Class<T> type, List<Field> fields, Cursor cursor) {
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
		// query backup
		Map<String, Object> cacheMap = new HashMap<String, Object>();

		for (Field field : fields) {
			String name = field.getName();
			int i = cursor.getColumnIndex(name);
			if (i < 0) {
				DebugLog.warn("缺少属性:" + type + "#" + name);
				continue;
			}
			Class<?> fieldType = ReflectUtil.toWrapper(field.getType());
			Object value = SQLUtils.toObject(cursor, i, type, fieldType);
			cacheMap.put(name, value);
			ReflectUtil.setValue(o, name, value);
		}

		return o;
	}

	@SuppressWarnings("unchecked")
	static Object toObject(Cursor cursor, int i, Class<?> type,
			Class<?> fieldType) {
		Object value = null;
		if (String.class == fieldType) {
			value = cursor.getString(i);
		} else if (URL.class == fieldType) {
			if (!cursor.isNull(i)) {
				try {
					value = new URL(cursor.getString(i));
				} catch (MalformedURLException e) {
					DebugLog.warn("无效URL:" + type, e);
				}
			}
		} else if (URI.class == fieldType) {
			value = URI.create(cursor.getString(i));
		} else if (File.class == fieldType) {
			value = new File(cursor.getString(i));
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
			value = ReflectUtil.getEnum(ordi,
					(Class<? extends Enum<?>>) fieldType);
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

	static void putValues(ContentValues values, String name, Object value,
			Class<?> fieldType) {
		if (value == null) {
			values.putNull(name);
		} else if (String.class == fieldType) {
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
			values.put(name, ((Enum<?>) value).ordinal());
		} else if (value instanceof File) {
			values.put(name, ((File) value).getAbsolutePath());
		} else if (value instanceof URL || value instanceof URI
				|| value instanceof File) {
			values.put(name, (String) value.toString());
		} else {
			values.put(name, JSONEncoder.encode(value));
		}
	}

	static Object toQueryArg(Object value) {
		if (value instanceof Enum) {
			return ((Enum<?>) value).ordinal();
		} else if (value instanceof Date) {
			return ((Date) value).getTime();
		} else if (value instanceof File) {
			return ((File) value).getAbsolutePath();
		} else {
			return value;
		}
	}

	static String[] toQueryArgs(Object... selectionArgs) {
		if (selectionArgs == null) {
			return null;
		}
		String[] args = new String[selectionArgs.length];
		for (int i = 0; i < args.length; i++) {
			Object value = selectionArgs[i];
			if (value != null) {
				args[i] = String.valueOf(toQueryArg(value));
			} else {
				args[i] = "";
			}
		}
		return args;
	}

	private static Field contentsValueField;
	static {
		if (DebugLog.isRelease()) {
			try {
				contentsValueField = ContentValues.class
						.getDeclaredField("mValues");
				contentsValueField.setAccessible(true);
			} catch (Exception e) {
			}
		}
	}

	static ContentValues toContent(Map<String, Object> contents) {
		ContentValues cvs = new ContentValues();
		if (contentsValueField != null) {
			try {
				contentsValueField.set(cvs, new HashMap<String, Object>(
						contents));
				return cvs;
			} catch (Exception e) {
			}
		}
		for (Map.Entry<String, Object> entry : contents.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value == null) {
				cvs.putNull(key);
			} else if (value instanceof String) {
				cvs.put(key, (String) value);
			} else if (value instanceof Boolean) {
				cvs.put(key, (Boolean) value);
			} else if (value instanceof Byte) {
				cvs.put(key, (Byte) value);
			} else if (value instanceof Short) {
				cvs.put(key, (Short) value);
			} else if (value instanceof Integer) {
				cvs.put(key, (Integer) value);
			} else if (value instanceof Long) {
				cvs.put(key, (Long) value);
			} else if (value instanceof Double) {
				cvs.put(key, (Double) value);
			} else if (value instanceof Float) {
				cvs.put(key, (Float) value);
			} else if (value instanceof byte[]) {
				cvs.put(key, (byte[]) value);
			} else if (value instanceof File) {
				cvs.put(key, ((File) value).getAbsolutePath());
			} else if (value instanceof URL || value instanceof URI
					|| value instanceof File) {
				cvs.put(key, (String) value.toString());
			} else {
				throw new IllegalArgumentException(" unsupport arguments type:"
						+ value);
			}
		}
		return cvs;
	}

}

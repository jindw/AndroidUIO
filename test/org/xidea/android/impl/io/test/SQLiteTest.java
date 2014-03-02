package org.xidea.android.impl.io.test;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.RobolectricTestRunner;
import org.xidea.android.UIO;
import org.xidea.android.SQLiteMapper;
import org.xidea.android.SQLiteMapper.SQLiteEntry;
import org.xidea.android.SQLiteMapper.SQLiteProperty;


@RunWith(RobolectricTestRunner.class)
public class SQLiteTest {
	@SQLiteEntry
	public static class Entry {
		@SQLiteProperty("PRIMARY KEY AUTOINCREMENT")
		public int id;
		@SQLiteProperty
		public String content;
	}

	@Test
	public void testDB() {
		SQLiteMapper<Entry> mapper = UIO.getSQLiteStorage(Entry.class);
		Entry t = new Entry();
		t.content = "AAA";
		mapper.save(t);
		System.out.println(t.id);
		t = new Entry();
		t.content = "BBB";
		mapper.save(t);
		System.out.println(t.id);
	}
}

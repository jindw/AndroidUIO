package org.xidea.android.impl.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.xidea.android.Callback;

public interface DiskLruCache {

	public abstract boolean contains(String key);
	/**
	 * Returns a snapshot of the entry named {@code key}, or null if it doesn't
	 * exist is not currently readable. If a value is returned, it is moved to
	 * the head of the LRU queue.
	 */
	public abstract InputStream get(String key) throws IOException;

	/**
	 * 创建一个能自动写回缓存系统的输入刘，用于更新缓存数据，一次只能有一个缓存数据流在工作
	 * @param in 原始输入流
	 * @param key
	 * @param pos
	 * @param complete
	 * @return
	 * @throws IOException
	 */
	public abstract InputStream getWritebackFilter(InputStream in, String key, int pos,Callback<Boolean> complete) throws IOException;

	/**
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public abstract boolean remove(String key) throws IOException;


	public abstract File getCacheFile(String key) ;

	/**
	 * @return current used cache size(form index not form disk)
	 */
	public abstract long size();

	/**
	 * Closes the cache and deletes all of its stored values. This will delete
	 * all files in the cache directory including files that weren't created by
	 * the cache.
	 */
	public abstract void clear() throws IOException;
	/**
	 * Closes this cache. Stored values will remain on the filesystem.
	 */
	public abstract void close() throws IOException;

}
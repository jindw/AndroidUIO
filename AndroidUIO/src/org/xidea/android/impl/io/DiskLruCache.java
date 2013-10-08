package org.xidea.android.impl.io;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.xidea.android.Callback;
import org.xidea.android.CommonLog;

public final class DiskLruCache implements Closeable {
	private static Log log = CommonLog.getLog();

	private static final int REDUNDANT_OP_COMPACT_THRESHOLD = 2000;
	private static final String INDEX_FILE = ".index";
	private static final String MAGIC = "org.xidea.DiskLruCache:1.0";
	private static final char CLEAN = 'C';
	private static final char WRITE = 'W';
	private static final char UPDATE = 'U';
	private static final char DELETE = 'D';
	private static final char READ = 'R';

	private final File directory;
	private int appVersion;
	private final long maxSize;
	private final int maxCount;
	private long size = 0;
	private DataOutputStream indexWriter;
	private int redundantOpCount;
	private final LinkedHashMap<String, DiskLruCacheEntry> lruEntries = new LinkedHashMap<String, DiskLruCacheEntry>(
			0, 0.75f, true);


	/**
	 * Opens the cache in {@code directory}, creating a cache if none exists
	 * there.
	 * 
	 * @param directory
	 *            a writable directory
	 * @param appVersion
	 * @param valueCount
	 *            the number of values per cache entry. Must be positive.
	 * @param maxSize
	 *            the maximum number of bytes this cache should use to store
	 * @throws FileNotFoundException 
	 * @throws IOException
	 *             if reading or writing the cache directory fails
	 */
	private DiskLruCache(File directory, int appVersion, int maxCount,
			long maxSize) throws IOException {
		this.directory = directory;
		this.appVersion = appVersion;
		this.maxCount = maxCount;
		this.maxSize = maxSize;

		File indexFile = new File(directory, INDEX_FILE);
		if (indexFile.exists()) {
			try {

				this.indexWriter = new DataOutputStream(new FileOutputStream(indexFile, true));
				this.initIndex(new DataInputStream(new FileInputStream(indexFile)));
				return ;
			} catch (Exception indexIsCorrupt) {
				log.error("DiskLruCache " + directory + " is corrupt: "
						+ indexIsCorrupt.getMessage() + ", removing");
				this.delete();
			}
		}
		// create a new empty cache
		this.indexWriter = new DataOutputStream(new FileOutputStream(indexFile, true));
		directory.mkdirs();
		this.rebuildIndex();

	}

	private void initIndex(DataInputStream in) throws IOException {
		try {
			final String magic = in.readUTF();
			final int appVersionString = in.readChar();
			final char blank = in.readChar();
			if (!MAGIC.equals(magic)
					|| !Integer.toString(appVersion).equals(appVersionString)
					|| '\n' != blank) {
				throw new IOException("unexpected index header: [" + magic
						+ ", " + appVersion + ", " + appVersionString + ", "
						+ blank + "]");
			}
			initIndexContent(in);
			for (final Iterator<DiskLruCacheEntry> i = lruEntries.values().iterator(); i
					.hasNext();) {
				final DiskLruCacheEntry entry = i.next();
				if (entry.isReadable()) {
					size += entry.size;
				} else {
					entry.delete();
					i.remove();
				}
			}

		} finally {
			FileUtil.closeQuietly(in);
		}
	}

	private void initIndexContent(DataInputStream in) throws IOException {
		try {
			while (true) {
				char flag = in.readChar();
				String key = in.readUTF();
				if (flag == DELETE) {
					lruEntries.remove(key);
					return;
				}
				DiskLruCacheEntry entry = lruEntries.get(key);
				if (entry == null) {
					entry = new DiskLruCacheEntry(this,key);
					lruEntries.put(key, entry);
				}
				switch (flag) {
				case CLEAN:
					entry.size = in.readInt();
					break;

				case UPDATE:
				case WRITE:
					entry.clean();//reset to clean or deleted
					break;
				case READ:
					// this work was already done by calling lruEntries.get()
					break;
				default:
					throw new IOException("unexpected index line: " + flag
							+ ":" + key);
				}
			}
		} catch (EOFException e) {
			//end
		}
	}

	/**
	 * Creates a new index that omits redundant information. This replaces the
	 * current index if it exists.
	 */
	private synchronized void rebuildIndex() throws IOException {
		if (indexWriter != null) {
			indexWriter.close();
		}
		File indexFileTmp = new File(directory, INDEX_FILE + ".tmp");
		indexWriter = new DataOutputStream(new FileOutputStream(
				indexFileTmp));
		indexWriter.writeUTF(MAGIC);
		indexWriter.writeChar(appVersion);
		indexWriter.writeChar('\n');
		for (final DiskLruCacheEntry entry : lruEntries.values()) {
			boolean readable = entry.isReadable();
			writeIndex(entry.isWriting() ?( readable?UPDATE:WRITE):(readable?CLEAN:DELETE), entry);
		}
		indexWriter.close();
		File indexFile = new File(directory, INDEX_FILE);
		indexFileTmp.renameTo(indexFile);
		indexWriter = new DataOutputStream(new FileOutputStream(indexFile,
				true));
	}

	private final void writeIndex(char type, DiskLruCacheEntry entry) throws IOException {
		indexWriter.writeByte(type);
		indexWriter.writeUTF(entry.key);
		if (type == CLEAN) {
			indexWriter.writeInt(entry.size);
		}
	}

	public boolean contains(String key) {
		validate(key);
		final DiskLruCacheEntry entry = lruEntries.get(key);
		if (entry == null) {
			return false;
		}
		File file = entry.cleanFile;
		return file != null && file.exists() ;
	}

	/**
	 * Returns a snapshot of the entry named {@code key}, or null if it doesn't
	 * exist is not currently readable. If a value is returned, it is moved to
	 * the head of the LRU queue.
	 */
	public synchronized InputStream get(String key) throws IOException {
		validate(key);
		final DiskLruCacheEntry entry = lruEntries.get(key);
		if (entry != null) {
			InputStream in = entry.newInputStream();
			if (in != null) {
				redundantOpCount++;
				writeIndex(READ, entry);// .flush(); do not flush immediately for
											// performance
				if (indexRebuildRequired()) {
					executorService.submit(cleanupCallable);
				}

			}
			return in;
		}
		return null;
	}

	public synchronized InputStream edit(InputStream in,String key,int pos, final Callback<Boolean> complete) throws IOException {
		validate(key);
		DiskLruCacheEntry entry = lruEntries.get(key);
		if (entry == null) {
			entry = new DiskLruCacheEntry(this,key);
			lruEntries.put(key, entry);
		}
		boolean readable = entry.isReadable();
		InputStream wrapper = entry.edit(in, pos, complete);
		// flush the index before creating files to prevent file leaks
		if(wrapper != null){
			writeIndex(readable?UPDATE:WRITE, entry);
			return wrapper;//
		}
		return in;
	}

	public synchronized boolean remove(String key) throws IOException {
		validate(key);
		final DiskLruCacheEntry entry = lruEntries.get(key);
		if (entry == null) {
			return false;
		}
		int s = entry.size;
		entry.delete();
		size -= s;
		lruEntries.remove(key);

		redundantOpCount++;
		writeIndex(DELETE, entry);
		if (indexRebuildRequired()) {
			executorService.submit(cleanupCallable);
		}

		return true;
	}
	synchronized void editEnd(DiskLruCacheEntry entry, boolean success)
			throws IOException {
		redundantOpCount++;
		if (success) {
			writeIndex(CLEAN, entry);
		} else {
			lruEntries.remove(entry.key);
			writeIndex(DELETE, entry);
		}

		if (overBudget() || indexRebuildRequired()) {
			executorService.submit(cleanupCallable);
		}
	}

	private boolean overBudget() {
		return size > maxSize || lruEntries.size() > maxCount;
	}

	/**
	 * We only rebuild the index when it will halve the size of the index
	 * and eliminate at least 2000 ops.
	 */
	private boolean indexRebuildRequired() {
		return redundantOpCount >= REDUNDANT_OP_COMPACT_THRESHOLD
				&& redundantOpCount >= lruEntries.size();
	}



	/**
	 * Closes this cache. Stored values will remain on the filesystem.
	 */
	@Override
	public synchronized void close() throws IOException {
		if (indexWriter == null) {
			return; // already closed
		}
		for (final DiskLruCacheEntry entry : new ArrayList<DiskLruCacheEntry>(lruEntries.values())) {
			entry.clean();
		}
		trimToSize();
		indexWriter.close();
		indexWriter = null;
	}

	private void trimToSize() throws IOException {
		while (overBudget()) {
			final Map.Entry<String, DiskLruCacheEntry> toEvict = lruEntries.entrySet()
					.iterator().next();
			remove(toEvict.getKey());
		}
	}

	/**
	 * Closes the cache and deletes all of its stored values. This will delete
	 * all files in the cache directory including files that weren't created by
	 * the cache.
	 */
	public void delete() throws IOException {
		close();
		FileUtil.deleteRecursively(directory);
	}

	private void validate(String key) {
		if (indexWriter == null) {
			throw new IllegalStateException("cache is closed");
		}
	}

	public File getDirectory() {
		return directory;
	}

	public long size() {
		return size;
	}

	/** This cache uses a single background thread to evict entries. */
	private final ExecutorService executorService = new ThreadPoolExecutor(0,
			1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	private final Callable<Void> cleanupCallable = new Callable<Void>() {
		@Override
		public Void call() throws Exception {
			synchronized (DiskLruCache.this) {
				if (indexWriter == null) {
					return null; // closed
				}
				trimToSize();
				if (indexRebuildRequired()) {
					rebuildIndex();
					redundantOpCount = 0;
				}
			}
			return null;
		}
	};
	public static DiskLruCache open(File dir, int appVersion, int maxCount,
			int maxCacheSize) {
		try{
			return new DiskLruCache(dir, appVersion, maxCount, maxCacheSize);
		}catch(IOException e){
			return null;
		}
	}

}

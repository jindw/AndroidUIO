package org.xidea.android.impl.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.android.Callback;

class DiskLruCacheEntry {
	private static Log log = LogFactory.getLog(DiskLruCacheEntry.class);
	
	final String key;
	final File writingFile;
	final File cleanFile;
	private final DiskLruCache cache;
	private final LinkedList<PositionedInputStream> ins = new LinkedList<PositionedInputStream>();
	

	int size;
	WriteCacheInputStream writer;

	DiskLruCacheEntry(DiskLruCache cache, String key) {
		File dir = cache.getDirectory();
		this.cache = cache;
		this.writingFile = new File(dir, key + ".tmp");
		this.cleanFile = new File(dir, key);
		this.key = key;
	}

	public boolean isReadable(){
		return this.cleanFile.exists();
	}

	public boolean isWriting() {
		return this.writingFile.exists();
	}
	void clean() throws IOException{
		FileUtil.deleteIfExists(writingFile);
	}


	public void delete() {
		
	}
	public InputStream newInputStream() throws IOException {
		return new PositionedInputStream(cleanFile, ins);
	}

	public synchronized InputStream edit(InputStream in, int position,
			Callback<Boolean> complete) throws IOException {
		if (writer == null) {
			File file = writingFile;
			if (!file.exists()) {
				file.createNewFile();
			}
			if (position == 0 || position == file.length()) {
				FileOutputStream out = new FileOutputStream(file, position > 0);
				writer = new WriteCacheInputStream(this, in, out, complete);
				return writer;
			}else{
				file.delete();
				writer = null;
			}
		}
		complete.callback(false);
		return null;
	}

	private synchronized void editEnd(boolean success) {
		synchronized (ins) {
			writer = null;
			for (PositionedInputStream in : ins) {
				try {
					in.readToCache();
				} catch (IOException e) {
					log.warn(e);
				}
			}
			File writingFile = this.writingFile;
			if (writingFile.exists()) {
				if (success) {
					writingFile.renameTo(cleanFile);
				} else {
					writingFile.delete();
				}
				try {
					cache.editEnd(this,success);
				} catch (IOException e) {
					log.warn("更新索引失败",e);
				}
			}
		}
	}

	static final class WriteCacheInputStream extends FilterInputStream {
		private static final byte[] VOID_BUF = new byte[1];
		private Callback<Boolean> callback;
		private OutputStream out;
		boolean done;
		private DiskLruCacheEntry entry;

		public WriteCacheInputStream(DiskLruCacheEntry entry, InputStream in,
				OutputStream out, Callback<Boolean> complete)
				throws IOException {
			super(in);
			this.entry = entry;
			entry.writer = this;
			this.callback = complete;
			this.out = out;
		}

		private void readEnd(boolean success) {
			if (!done) {
				done = true;
				try {
					success = success && out != null;
					callback.callback(success);
					if (out == null) {
						closeNotNullOut();
					}
				} finally {
					entry.editEnd(success);
				}
			}
		}

		private final int _read(byte[] buffer, int offset, int count)
				throws IOException {// in outer sync
			int i = in.read(buffer, offset, count);
			if (i > 0 && out != null) {
				try {
					out.write(buffer, offset, i);
				} catch (Throwable e) {
					closeNotNullOut();
				}
			} else if (i < 0) {
				readEnd(true);
			}
			return i;
		}

		@Override
		public synchronized void close() throws IOException {
			try {
				try {
					if (!done) {
						this.read(VOID_BUF);
					}
				} finally {
					this.readEnd(false);
				}
			} finally {
				in.close();
			}
		}

		private final void closeNotNullOut() {
			try {
				out.close();
			} catch (Throwable e) {
				log.warn(e);
			}
			out = null;// has error
		}

		@Override
		public synchronized int read(byte[] buffer, int offset, int count)
				throws IOException {
			return _read(buffer, offset, count);
		}

		@Override
		public synchronized long skip(long byteCount) throws IOException {
			int c;
			int left = (int) byteCount;
			byte[] buf = new byte[Math.max(left, 1024)];
			while ((c = _read(buf, 0, left)) >= 0) {
				left -= c;
			}
			return byteCount - left;
		}

		@Override
		public synchronized int read() throws IOException {
			byte[] buf = VOID_BUF;
			int c;
			while ((c = _read(buf, 0, 1)) == 0)
				;
			return c > 0 ? (0xFF & buf[0]) : -1;
		}

		@Override
		public boolean markSupported() {
			return false;
		}

		@Override
		public void reset() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void mark(int readlimit) {
			throw new UnsupportedOperationException();
		}
	}

	private final static class PositionedInputStream extends FilterInputStream {
		int position;
		final File file;
		List<PositionedInputStream> list;

		protected PositionedInputStream(File file,
				List<PositionedInputStream> list) throws IOException {
			super(new FileInputStream(file));
			this.file = file;
			this.list = list;
			synchronized (list) {
				list.add(this);
			}
		}

		@Override
		public synchronized void close() throws IOException {
			_clear();
			in.close();
		}

		@Override
		public synchronized int read() throws IOException {
			position++;
			return in.read();
		}

		@Override
		public synchronized int read(byte[] buffer, int offset, int count)
				throws IOException {
			int c = in.read(buffer, offset, count);
			position += c;
			return c;
		}

		synchronized void readToCache() throws IOException {
			_clear();
			int len = (int) file.length();
			int left = len - position;
			byte[] data = new byte[left];
			int offset = 0;
			int c;
			while (offset > 0 && (c = in.read(data, offset, left)) >= 0) {
				left -= c;
			}
			in.close();
			in = new ByteArrayInputStream(data);
		}

		private void _clear() {
			if (list == null) {
				synchronized (list) {
					list.remove(this);
					list = null;
				}
			}
		}

		@Override
		public long skip(long byteCount) throws IOException {
			long c = in.skip(byteCount);
			position += c;
			return c;
		}

		@Override
		public boolean markSupported() {
			return false;
		}

	}
}

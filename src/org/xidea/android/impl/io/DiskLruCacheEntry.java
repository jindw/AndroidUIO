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

import org.xidea.android.Callback;
import org.xidea.android.impl.DebugLog;

class DiskLruCacheEntry {
	final String key;
	final File writingFile;
	final File cleanFile;
	private final DiskLruCacheImpl cache;
	private final LinkedList<PositionedInputStream> ins = new LinkedList<PositionedInputStream>();
	

	int size;
	WritebackInputStream writer;

	DiskLruCacheEntry(DiskLruCacheImpl cache, String key) {
		File dir = cache.directory;
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
		IOUtil.deleteIfExists(writingFile);
	}


	public void delete() {
		
	}
	public InputStream newInputStream() throws IOException {
		return new PositionedInputStream(cleanFile, ins);
	}

	/**
	 * 
	 * @param in
	 * @param position
	 * @param complete Callback<Boolean> success on read end!
	 * @return
	 * @throws IOException
	 */
	public synchronized InputStream getWritebackFilter(InputStream in, int position,
			Callback<Boolean> complete) throws IOException {
		if(in instanceof WritebackInputStream){
			throw new IllegalArgumentException("input stream is a writeback input stream ");
		}
		if (writer == null) {
			File file = writingFile;
			System.out.println(writingFile);
			if (!file.exists()) {
				file.createNewFile();
			}
			if (position == 0 || position == file.length()) {
				FileOutputStream out = new FileOutputStream(file, position > 0);
				writer = new WritebackInputStream(this, in, out, complete);
				return writer;
			}else{
				//TODO: 断点续传
				file.delete();
				writer = null;
			}
		}else{
			
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
					DebugLog.warn(e);
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
					DebugLog.warn("更新索引失败",e);
				}
			}
		}
	}

	static final class WritebackInputStream extends FilterInputStream {
		private Callback<Boolean> callback;
		private OutputStream out;
		boolean done;
		private DiskLruCacheEntry entry;

		public WritebackInputStream(DiskLruCacheEntry entry, InputStream in,
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
						byte[] buf = new byte[4];
						this.read(buf);
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
				DebugLog.warn(e);
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
			byte[] buf = new byte[left>1024?1024:left];
			while ((c = _read(buf, 0, Math.min(left, 1024))) >= 0) {
				left -= c;
			}
			return byteCount - left;
		}

		@Override
		public synchronized int read() throws IOException {
			byte[] buf = new byte[1];
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

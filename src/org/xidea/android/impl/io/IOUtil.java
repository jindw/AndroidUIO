package org.xidea.android.impl.io;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class IOUtil {
	public static byte[] loadBytesAndClose(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] buf = new byte[1024];
			int c;
			while ((c = in.read(buf)) >= 0) {
				out.write(buf, 0, c);
			}
		} finally {
			in.close();
		}
		return out.toByteArray();
	}

	public static String loadTextAndClose(InputStream in, String encoding) {
		if (in == null) {
			return null;
		}
		try {
			try {
				Reader r = new InputStreamReader(in, encoding == null ? "utf-8"
						: encoding);
				StringBuilder out = new StringBuilder();
				char[] buf = new char[1024];
				int c;
				while ((c = r.read(buf)) >= 0) {
					out.append(buf, 0, c);
				}
				return out.toString();
			} finally {
				in.close();
			}
		} catch (IOException e) {
			return null;
		}
	}

	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (final Exception ignored) {
			}
		}
	}

	/**
	 * Reads next 16-bit value, LSB first
	 * 
	 * @throws IOException
	 */
	public static int readShort(InputStream in) throws IOException {
		// read 16-bit value, LSB first
		return in.read() | (in.read() << 8);
	}

	/**
	 * Reads next 32-bit value, LSB first
	 * @see java.io.DataInputStream#readInt()
	 * @param in
	 * @return
	 * @throws IOException 
	 */
	public static int readInt(InputStream in) throws IOException {
		return in.read()| (in.read() << 8) | (in.read()<<16) |(in.read()<<24);
	}
	public static int skip(InputStream in, int blockSize) throws IOException {

		int total = 0;
		if (blockSize > 0) {
			int count = 0;
			while (total < blockSize) {
				count = (int) in.skip(blockSize - total);
				if (count == -1)
					break;
				total += count;
			}
		}
		return total;
	}
	public static boolean startsWith(InputStream in, byte[] bs) throws IOException {
		for(byte b : bs){
			if(in.read() != (b & 0xFF)){
				return false;
			}
		}
		return true;
	}

	public static byte[] read(InputStream in, byte[] bs, int count) throws IOException {
		int i = 0;
		while (i < count) {
			i += in.read(bs, i, count - i);
		}
		return bs;
	}

	/**
	 * Recursively delete everything in {@code dir}.
	 */
	// TODO: this should specify paths as Strings rather than as Files
	static void deleteRecursively(File dir) throws IOException {
		if (dir.isDirectory()) {
			for (final File file : dir.listFiles()) {
				deleteRecursively(file);
			}
		}
		if (dir.exists() && !dir.delete()) {
			throw new IOException("failed to delete file: " + dir);
		}
	}

	static void deleteIfExists(File... files) throws IOException {
		if (files != null) {
			for (File file : files) {
				if (file.exists() && !file.delete()) {
					throw new IOException();
				}
			}
		}
	}


}

package org.xidea.android.impl.io;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;

import org.xidea.android.CommonLog;

public abstract class FileUtil {
	private static Log log = CommonLog.getLog();

	static String loadTextAndClose(InputStream in, String encoding) {
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

	static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (final Exception ignored) {
			}
		}
	}

	// from libcore.io.IoUtils
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

	static void deleteIfExists(File file) throws IOException {
		if (file.exists() && !file.delete()) {
			throw new IOException();
		}
	}

	static void deleteIfExists(File... files) throws IOException {
		for (File file : files) {
			deleteIfExists(file);
		}
	}

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

	public static void unzip(String filePath, String outPath)
			throws IOException {
		ZipInputStream in = new ZipInputStream(new FileInputStream(filePath));
		try {
			File outDirectory = new File(outPath);
			if (!outDirectory.isDirectory()) {
				if (!outDirectory.mkdirs()) {
					throw new FileNotFoundException();
				}
			}
			ZipEntry entry;
			byte[] buffer = new byte[1024];
			while ((entry = in.getNextEntry()) != null) {
				String name = entry.getName();
				if (!entry.isDirectory()) {
					try {
						File file = new File(outPath, name);
						File folder = file.getParentFile();
						if (!folder.exists() && !folder.mkdirs()) {
							throw new FileNotFoundException("目录创建失败");
						}
						if (file.exists()) {
							file.delete();
						}
						if (!file.createNewFile()) {
							throw new FileNotFoundException("文件创建失败");
						}
						FileOutputStream out = new FileOutputStream(file);
						try {
							int len;
							while ((len = in.read(buffer)) != -1) {
								out.write(buffer, 0, len);
								out.flush();
							}
						} finally {
							out.close();
						}
					} catch (IOException e) {
						log.error(e);
					}
				}
			}
		} finally {
			in.close();
		}
	}
}

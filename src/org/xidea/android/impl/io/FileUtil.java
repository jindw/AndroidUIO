package org.xidea.android.impl.io;

import java.io.File;
import java.io.IOException;

public abstract class FileUtil {
	//private static Log log = CommonLog.getLog();

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

}

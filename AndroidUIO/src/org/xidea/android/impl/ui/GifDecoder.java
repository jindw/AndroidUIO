package org.xidea.android.impl.ui;

import java.io.*;

/**
 * Class GifDecoder - Decodes a GIF file into one or more frames. <br>
 * 
 * <pre>
 * Example:
 *    GifDecoder d = new GifDecoder();
 *    d.read("sample.gif");
 *    int n = d.getFrameCount();
 *    for (int i = 0; i < n; i++) {
 *       BufferedImage frame = d.getFrame(i);  // frame i
 *       int t = d.getDelay(i);  // display duration of frame in milliseconds
 *       // do something with frame
 *    }
 * </pre>
 * 
 * No copyright asserted on the source code of this class. May be used for any
 * purpose, however, refer to the Unisys LZW patent for any additional
 * restrictions. Please forward any corrections to questions at fmsware.com.
 * 
 * @author Kevin Weiner, FM Software; LZW decoder adapted from John Cristy's
 *         ImageMagick.
 * @version 1.03 November 2003
 * 
 */

public class GifDecoder {
	private BufferedInputStream in;

	private int width; // full image width
	private int height; // full image height

	private int[] gct;
	private int currentDelay = 0; // delay in milliseconds
	private int currentTransparentIndex; // transparent color index
	private int[] currentColorTable;
	private boolean currentTransparency;

	private boolean parseImage;

	/**
	 * Reads GIF image from stream
	 * 
	 * @param BufferedInputStream
	 *            containing GIF file.
	 * @return read status code (0 = no errors)
	 * @throws IOException
	 */
	public GifDecoder(BufferedInputStream is, boolean parseImage)
			throws IOException {
		this.parseImage = parseImage;
		if (is != null) {
			this.in = is;
			readHeader();
			readContents();
			is.close();
		}
	}

	/**
	 * Reads GIF file header information.
	 * 
	 * @throws IOException
	 */
	private boolean readHeader() throws IOException {
		byte[] bs = read(new byte[6], 6);
		String header = new String(bs);
		if (header.equals("GIF87a") || header.equals("GIF89a")) {
		}
		// Reads Logical Screen Descriptor
		width = readShort();
		height = readShort();
		// packed fields
		int packed = in.read();
		boolean hasGCT = packed >= 0x80; // 1 : global color table flag
											// (0x80:10000000)
		// 2-4 : color resolution
		// 5 : gct sort flag
		int gctSize = 2 << (packed & 7); // 6-8 : gct size()

		@SuppressWarnings("unused")
		int bgIndex = in.read(); // background color index
		@SuppressWarnings("unused")
		int pixelAspect = in.read(); // pixel aspect ratio
		if (hasGCT) {
			gct = readColorTable(gctSize);
		}
		return true;
	}

	/**
	 * Reads color table as 256 RGB integer values
	 * 
	 * @param ncolors
	 *            int number of colors to read
	 * @return int array containing 256 colors (packed ARGB with full alpha)
	 * @throws IOException
	 */
	private int[] readColorTable(int ncolors) throws IOException {
		if (parseImage) {
			int[] tab = new int[ncolors]; // max size to avoid bounds checks
			int i = 0;
			while (i < ncolors) {
				int r = in.read() & 0xff;
				int g = in.read() & 0xff;
				int b = in.read() & 0xff;
				tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
			}
			return tab;
		} else {
			in.skip(ncolors*3);
			return null;
		}
	}

	/**
	 * Main file parser. Reads GIF content blocks.
	 * 
	 * @throws IOException
	 */
	private void readContents() throws IOException {
		// read GIF file content blocks
		while (true) {
			switch (in.read()) {
			case 0x2C: // image separator
				readImage();
				break;
			case 0x21: // extension
				switch (in.read()) {
				case 0xf9: // graphics control extension
					readGraphicControlExt();
					break;
				case 0xff: // application extension
					readApplicationExt();
					skipBlocks();
					// app.startsWith("NETSCAPE2.0")
					break;
				default: // uninteresting extension
					skipBlocks();
				}
				break;

			case 0x3b: // terminator
				return;

			case 0x00: // bad byte, but keep going and see what happens
				break;
			default:
				throw new IllegalStateException("invalid gif format");
			}
		}
	}

	private String readApplicationExt() throws IOException {
		int size = in.read();
		return new String(read(new byte[size], size));
	}

	/**
	 * Reads Graphics Control Extension values
	 * 
	 * @throws IOException
	 */
	private void readGraphicControlExt() throws IOException {
		in.read(); // block size
		int packed = in.read(); // packed fields
		int dispose = (packed & 0x1c) >> 2; // disposal method
		if (dispose == 0) {
			dispose = 1; // elect to keep old image if discretionary
		}
		this.currentTransparency = (packed & 1) != 0;
		this.currentDelay = readShort() * 10; // delay in milliseconds
		this.currentTransparentIndex = in.read(); // transparent color index
		in.read(); // block terminator
	}

	private byte[] read(byte[] bs, int count) throws IOException {
		int i = 0;
		while (i < count) {
			i += in.read(bs, i, count - i);
		}
		return bs;
	}
//
//	/**
//	 * Reads next variable length block from input.
//	 * 
//	 * @return number of bytes stored in "buffer"
//	 * @throws IOException
//	 */
//	private byte[] readBlock() throws IOException {
//		int blockSize = in.read();
//		skip(blockSize);
//		return null;
//	}

	/**
	 * Skips variable length blocks up to and including next zero length block.
	 * 
	 * @throws IOException
	 */
	private void skipBlocks() throws IOException {
		int blockSize;
		while ((blockSize = in.read()) > 0) {
			skip(blockSize);
		}
	}

	private int skip(int blockSize) throws IOException {
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

	/**
	 * Reads next 16-bit value, LSB first
	 * 
	 * @throws IOException
	 */
	private int readShort() throws IOException {
		// read 16-bit value, LSB first
		return in.read() | (in.read() << 8);
	}

	/**
	 * Reads next frame image
	 * 
	 * @throws IOException
	 */
	private void readImage() throws IOException {
		// (sub)image position & size
		readShort(); // px
		readShort(); // py
		readShort(); // w
		readShort(); // h

		int packed = in.read();
		boolean lctFlag = packed >= 0x80; // 1 - local color table flag
		// interlace = (packed & 0x40) != 0; // 2 - interlace flag
		// 3 - sort flag
		// 4-5 - reserved
		int lctSize = 2 << (packed & 7); // 6-8 - local color table size

		if (lctFlag) {
			int[] lct = readColorTable(lctSize); // read table
			currentColorTable = lct; // make local table active
		} else {
			currentColorTable = gct; // make global table active
			// if (bgIndex == transIndex)
			// bgColor = 0;
		}
		int backupTransparentColor = 0;
		if (currentTransparency) {
			backupTransparentColor = currentColorTable[currentTransparentIndex];
			currentColorTable[currentTransparentIndex] = 0; // set transparent
															// color if
															// specified
		}

		decodeImageData(); // decode pixel data
		// skipBlocks();

		if (currentTransparency) {
			currentColorTable[currentTransparentIndex] = backupTransparentColor;
		}

	}

	/**
	 * Decodes LZW image data into pixel array. Adapted from John Cristy's
	 * ImageMagick.
	 * 
	 * @throws IOException
	 */
	protected void decodeImageData() throws IOException {
		skipBlocks();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getDelay() {
		return currentDelay;
	}

}
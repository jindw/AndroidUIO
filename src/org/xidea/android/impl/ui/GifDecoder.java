package org.xidea.android.impl.ui;

import java.io.*;

import org.xidea.android.impl.io.IOUtil;

public class GifDecoder {
	private static byte[] GIF87 = "GIF87a".getBytes();
	private static byte[] GIF89 = "GIF89a".getBytes();
	private static int DECODER_IMAGE = 0;
	private static int DECODER_CHECK_ANIMAL_ONLY = 1;
//	private static int DECODER_CHECK_ANIMAL_AND_DELAY = 2;
	

	private InputStream in;

	private int width; // full image width
	private int height; // full image height

	private int[] gct;
	private int gce;
	private int currentDelay = 0; // delay in milliseconds
	private int currentTransparentIndex; // transparent color index
	private int[] currentColorTable;
	private boolean currentTransparency;

	private int decodeModel = DECODER_CHECK_ANIMAL_ONLY;
	private boolean gif89;
	private boolean animate;

	/**
	 * Reads GIF image from stream
	 * 
	 * @param BufferedInputStream
	 *            containing GIF file.
	 * @return read status code (0 = no errors)
	 * @throws IOException
	 */
	public GifDecoder(InputStream in) throws IOException {
//		this.decodeModel = parseImage?DECODER_IMAGE:DECODER_CHECK_ANIMAL_ONLY;
		//long t1 = System.nanoTime();
		if (in != null) {
			this.in = in;
			if (readHeader() && this.gif89) {
				readContents();
			}
		}
		//System.out.println("time used:"+(System.nanoTime()-t1)/1000000f);
	}

	/**
	 * Reads GIF file header information.
	 * 
	 * @throws IOException
	 */
	private boolean readHeader() throws IOException {
		if(IOUtil.startsWith(in,  GIF89)) {
			gif89 = true;
		} else{
			in.reset();
			if(!IOUtil.startsWith(in, GIF87)) {
				return false;
			}
		}
		// Reads Logical Screen Descriptor
		width = IOUtil.readShort(in);
		height = IOUtil.readShort(in);
		// packed fields
		int packed = in.read();
		boolean hasGCT = packed >= 0x80; // 1 : global color table flag
											// (0x80:10000000)
		// 2-4 : color resolution
		//int colorResolution = ((packed & 0x70) >>> 4) + 1;
		//System.out.println(colorResolution + "/"+ Integer.toBinaryString(packed));

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
	private int[] readColorTable(int ncolors)
			throws IOException {
		if (decodeModel == DECODER_IMAGE) {
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
			IOUtil.skip(in,ncolors * 3);
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
			int flag = in.read();
//			System.out.println("flag1:"+Integer.toHexString(flag));
			switch (flag) {
			case 0x2C: // image separator
				readImage();
				break;
			case 0x21: // extension
				int flag2 = in.read();
//				System.out.println("flag2:"+Integer.toHexString(flag2));
				switch (flag2) {
				case 0xf9: // graphics control extension
					readGraphicControlExt();
					if((gce >1 ||animate) && decodeModel == DECODER_CHECK_ANIMAL_ONLY){
						return;
					}
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
				throw new IllegalStateException("invalid gif format:"
						+ Integer.toHexString(flag));
			}
		}
	}

	private String readApplicationExt() throws IOException {
		int size = in.read();
		if(decodeModel == DECODER_IMAGE){
			return new String(IOUtil.read(in,new byte[size], size));
		}else{
			//System.out.println(new String(IOUtil.read(in,new byte[size], size)));
			IOUtil.skip(in,size);
			return null;
		}
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
		this.currentDelay = IOUtil.readShort(in) * 10; // delay in milliseconds
		
		

		//System.out.println("delay:"+currentDelay);
		if (currentDelay > 0) {
			this.animate = true;
		}

		gce++;
		System.out.println("\ngce"+ gce+ ";delay:"+currentDelay);
		this.currentTransparentIndex = in.read(); // transparent color index
		in.read(); // block terminator
	}

	/**
	 * Skips variable length blocks up to and including next zero length block.
	 * 
	 * @throws IOException
	 */
	private void skipBlocks() throws IOException {
		int blockSize;
		while ((blockSize = in.read()) > 0) {
			IOUtil.skip(in,blockSize);
		}
	}

	/**
	 * Reads next frame image
	 * 
	 * @throws IOException
	 */
	private void readImage() throws IOException {
		// (sub)image position & size
		// readShort(); // px
		// readShort(); // py
		// readShort(); // w
		// readShort(); // h
		IOUtil.skip(in,8);
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
		if (decodeModel == DECODER_IMAGE && currentTransparency) {
			int backupTransparentColor = currentColorTable[currentTransparentIndex];
			currentColorTable[currentTransparentIndex] = 0; // set
			decodeImageData(); // decode pixel data
			currentColorTable[currentTransparentIndex] = backupTransparentColor;
		} else {
			decodeImageData();
		}

	}

	/**
	 * Decodes LZW image data into pixel array. Adapted from John Cristy's
	 * ImageMagick.
	 * 
	 * @throws IOException
	 */
	protected void decodeImageData() throws IOException {
		in.read();//LZW CodeSize
		skipBlocks();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean isAnimate() {
		return animate || gce>1;
	}

	public boolean isGif89() {
		return gif89;
	}

	public int getDelay() {
		return currentDelay;
	}

}
/*
 *  FE Editor - GBA Fire Emblem (U) ROM editor
 *
 *  Copyright (C) 2008-2011 Hextator,
 *  hextator (AIM) hextator@gmail.com (MSN)
 *
 *  Major thanks to Zahlman (AIM/MSN: zahlman@gmail.com) for optimization,
 *  organization and modularity improvements.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 3
 *  as published by the Free Software Foundation
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  <Description> This class is expected to provide an interface for converting
 *  graphical data between the standard PNG format and the 4bpp-reversed format
 *  native to the GBA
 */

package Graphics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;
import Model.Util;

// Format of GBA graphics:
// 8x8 tiles with 32 bytes per tile; each nibble of each byte is a single pixel
// Nibbles are reversed for each byte; bytes are in order
// So if the top left tile of an image has something like 10 32 54 76
// and the tile to the right of it begins with 98 BA DC FE
// then the first 16 pixels along the top row will count 0-F

public class GBAImage {
	private int width, height;

	private Palette palette;

	private byte[] image;
	// Order is top to bottom by tile, left to right by tile,
	// top to bottom by pixel, left to right by pixel (nibbles reversed)
	private BufferedImage rendered;
	// ARGB data describing the image as it would actually render.
	// Whenever the image is modified, raw is set null to indicate
	// a "dirty cache".

	// Convert ARGB to B5G5R5.
	public static short condense(int colour) {
		return (short)(
			((colour & 0x00F80000) >> 19)
			| ((colour & 0x0000F800) >> 6)
			| ((colour & 0x000000F8) << 7)
		);
	}

	// Shared code for constructors.
	private void setup(
		byte[] image, Palette palette, int width, int height
	) {
		if (palette == null) {
			throw new IllegalArgumentException("Palette is null");
		}

		this.palette = palette.copy();

		this.width = width;
		this.height = height;
		int expectedImageLength = (width * height) << 5;

		this.image = image;

		if (image == null) {
			this.image = new byte[expectedImageLength];
		}

		if (this.image.length != expectedImageLength) {
			throw new IllegalArgumentException("Unexpected image length");
		}
	}

	// Shared code for File and BufferedImage constructors.
	private void initFromImage(Palette sharedPalette) {
		int pixelWidth = rendered.getWidth();
		int pixelHeight = rendered.getHeight();
		if (((pixelWidth % 8) != 0) || ((pixelHeight % 8) != 0)) {
			throw new RuntimeException(
				"Image is not a multiple of 8 pixels in each dimension!"
			);
		}

		width = pixelWidth / 8;
		height = pixelHeight / 8;

		int[] raw = rendered.getRGB(0, 0, pixelWidth, pixelHeight, null, 0, pixelWidth);

		// Drop the least-significant bits of the colour.
		for (int i = 0; i < raw.length; ++i) {
			raw[i] &= 0xFFF8F8F8;
		}

		// If any pixels are fully transparent, make them opaque and make them match
		// the top-right pixel. If any pixels are partially transparent, make them opaque.
		int colourKey = raw[pixelWidth - 1] | 0xFF000000; // opaque.
		for (int i = 0; i < raw.length; ++i) {
			if ((raw[i] & 0xFF000000) == 0) { raw[i] = colourKey; }
			else { raw[i] |= 0xFF000000; }
		}

		// Convert each pixel to a palette index (allocating
		// new indices as new colours are seen) and write it
		// to the appropriate spot.
		image = new byte[pixelWidth * pixelHeight / 2];

		/* Hextator: Deprecated.
		//short topRight = condense(raw[pixelWidth - 1]);
		// Hextator: Crazy 3 line top right corner analysis that
		// older versions did to allow the top right corner to be used
		// for forced palette sharing. Doesn't hurt if the top right
		// corner isn't intended for this person for a given image,
		// either.
		// Hextator: Deprecated.
		short[] topRightCorner = new short[16];
		for (int i = 0; i < 16; i++) {
			// Order is left to right, then top to bottom
			topRightCorner[i] = condense(raw[
				(pixelWidth * (i < 8 ? 1 : 2))
				- (i < 8 ? i : (i - 8)) - 1
			]);
		}
		*/

		// The requiredPalette may belong to another GBAImage, in which case
		// new colours are added to the shared palette; or it may be a full
		// palette loaded from elsewhere that is used as a constraint on loading
		// (i.e. the input image must only use those colours).
		if (sharedPalette != null) {
			palette = sharedPalette;

			// For shared images read in from the user, the topRight pixel should
			// match the background colour. However, enforcing this causes major
			// problems for OAM sheets, and it's not a serious problem regardless.
		} else {
			// Hextator: Deprecated.
			// Ensure that the background colour gets index 0 in the palette.
			//palette = new Palette(topRight);
			// Hextator: Ensure that the background color gets index 0
			// in the palette AND ensure that the other 15 colors in the
			// top right corner are in the palette in the order they
			// occur from right to left then top to bottom, like with
			// old versions of FEditor
			//palette = new Palette(topRightCorner);

			// Zahlman: That doesn't work any more. Never mind the thumbnails;
			// we'll preserve that information externally.
			palette = new Palette(new short[0]);
		}
		// Hextator: Gotta read from right to left, then top to bottom
		// instead of the usual left to right, then top to bottom
		// to ensure that it's the top right corner and not the top left
		// that becomes the BG color
		for (int y = 0; y < pixelHeight; y++) {
			for (int x = pixelWidth - 1; x >= 0; x--) {
				int i = y * pixelWidth + x;
				int row = i / pixelWidth;
				int column = i % pixelWidth;
				int tile = (row / 8) * width + (column / 8);
				int positionInTile = (row % 8) * 8 + (column % 8);
				int byteToModify = tile * 32 + positionInTile / 2;
				int paletteIndex = palette.getIndex(condense(raw[i]));
				// We can be sure the bits in question are already clear.
				int shift = (positionInTile % 2) * 4;
				image[byteToModify] |= (paletteIndex << shift);
			}
		}

		// The source image may have had invalid colours with the low bits of RGB
		// components set. Therefore the cached image can't necessarily be used.
		// The easiest thing is to just throw it away and let it be calculated later.
		rendered = null;
	}

	// Shared code for File constructors.
	private void initFromFile(File file, Palette sharedPalette) {
		try {
			rendered = ImageIO.read(file);
		} catch (java.io.IOException ioe) {
			// Convert the exception so we don't have to declare the throw everywhere
			throw new RuntimeException("Couldn't read the file!");
		}
		initFromImage(sharedPalette);
	}

	private GBAImage(GBAImage source) {
		width = source.width;
		height = source.height;
		palette = source.palette.copy();
		image = source.image.clone();
		// don't need to copy 'rendered' since it's just a cache
	}

	public GBAImage copy() {
		return new GBAImage(this);
	}

	// Image constructor similar to GBAImage(File), only with the IO
	// already handled by the calling code
	public GBAImage(BufferedImage theImage) {
		rendered = theImage;
		initFromImage(null);
	}

	// Image constructor similar to GBAImage(File, GBAImage), only with
	// the IO already handled by the calling code
	public GBAImage(BufferedImage theImage, GBAImage toSharePaletteFrom) {
		rendered = theImage;
		initFromImage(
			toSharePaletteFrom == null ? null : toSharePaletteFrom.palette
		);
	}

	// Image constructor similar to GBAImage(File, Palette), only with
	// the IO already handled by the calling code
	public GBAImage(BufferedImage theImage, Palette requiredPalette) {
		rendered = theImage;
		initFromImage(requiredPalette);
	}

	// File constructor. Replaces old processImage().
	public GBAImage(File file) {
		initFromFile(file, null);
	}

	// File constructor. Causes the two GBA_Images to share
	// a palette; new colours found in the new image are added
	// to the common palette. For convenience, the image is allowed to
	// be null, in which case we simply load from file normally.
	public GBAImage(File file, GBAImage toSharePaletteFrom) {
		initFromFile(
			file,
			toSharePaletteFrom == null ? null : toSharePaletteFrom.palette
		);
	}

	// File constructor. Causes the new GBAImage to use
	// the provided palette; if colours are found from outside
	// the palette, an error will be reported. The provided
	// palette is assumed to be "full" (all 16 colours used).
	// For convenience, the palette is allowed to be null, in which case
	// we simply load from file normally.
	public GBAImage(File file, Palette requiredPalette) {
		initFromFile(file, requiredPalette);
	}

	public GBAImage(byte[] image, Palette palette, int tileWidth, int tileHeight) {
		setup(image, palette, tileWidth, tileHeight);
	}

	public GBAImage(Palette palette, int tileWidth, int tileHeight)	{
		setup(null, palette, tileWidth, tileHeight);
	}

	// Constructor from a sub-region of an existing image.
	// The new image may not be larger in either direction than the source.
	public GBAImage(GBAImage source, int from_x, int from_y, int from_w, int from_h) {
		setup(null, source.palette, from_w, from_h);
		blit(source, from_x, from_y, from_w, from_h, 0, 0, false);
	}

	// Constructor to make a "sample" image from the palette of an existing image.
	public GBAImage(Palette sample) {
		setup(null, sample, 4, 4);
		for (byte i = 0; i < 16; ++i) {
			for (int j = 0; j < 32; ++j) {
				image[i * 32 + j] = i;
			}
		}
	}

	// Constructor to "paint" a new image from an existing image used as a
	// "tile palette". Up to the first 255 tiles of the existing image (read
	// top-to-bottom, left-to-right) can be used; a 0xFF value in the
	// tileMapping means to leave that part of the new image blank.
	public GBAImage(GBAImage source, int tileWidth, int tileHeight, byte[] tileMapping) {
		if (tileMapping == null || tileMapping.length != (tileWidth * tileHeight)) {
			throw new IllegalArgumentException(
				"Tile mapping is null or of incorrect dimensions"
			);
		}
		setup(null, source.palette, tileWidth, tileHeight);

		for (int i = 0; i < tileMapping.length; ++i) {
			int id = tileMapping[i] & 0xFF; // convert to unsigned
			if (id != 0xFF) {
				System.arraycopy(source.image, id * 32, image, i * 32, 32);
			}
		}
	}

	// Overwrites if not transparent; otherwise, pixels with color 0 in the
	// tileData use the existing pixel. tileData must be 32 bytes long, giving
	// 64 pixels (8x8) of 4 bits each.
	private void blitTile(byte[] tileData, int x, int y, boolean transparent) {
		if (x < 0 || x >= width) { throw new IllegalArgumentException(); }
		if (y < 0 || y >= height) { throw new IllegalArgumentException(); }
		if (tileData == null || tileData.length != 32) {
			throw new IllegalArgumentException(
				"Tile data is null or isn't of length 32"
			);
		}

		int index = ((y * width) + x) * 32;
		for (int i = 0; i < 32; ++i) {
			byte result = tileData[i];
			if (transparent) { 
				if ((result & 0xF0) == 0) { result |= (image[i + index] & 0xF0); }
				if ((result & 0x0F) == 0) { result |= (image[i + index] & 0x0F); }
			}
			image[i + index] = result;
		}
	}

	public boolean matches(
		GBAImage from,
		int from_x, int from_y, int from_w, int from_h, 
		int to_x, int to_y
	) {
		if (to_x < 0 || to_y < 0 || (to_x + from_w) > width || (to_y + from_h) > height) {
			throw new IllegalArgumentException(
				"GBA_Image.matches: Invalid selection(s)"
			);
		}

		for (int i = 0; i < from_w; ++i) {
			for (int j = 0; j < from_h; ++j) {
				// Do the calculation locally to avoid redundant bounds checking.
				int from_index = (((from_y + j) * from.width) + (from_x + i)) * 32;
				int index = (((to_y + j) * width) + (to_x + i)) * 32;
				for (int k = 0; k < 32; ++k) {
					if (from.image[from_index + k] != image[index + k]) { return false; }
				}
			}
		}

		return true;
	}

	// Currently the blit() functions are the only way to mutate the image...
	public void blit(
		GBAImage from,
		int from_x, int from_y, int from_w, int from_h, 
		int to_x, int to_y, boolean transparent
	) {
		if (to_x < 0 || to_y < 0 || (to_x + from_w) > width || (to_y + from_h) > height) {
			throw new IllegalArgumentException("Rectangle doesn't fit within destination image");
		}

		if (from_x < 0 || from_y < 0 || (from_x + from_w) > from.width || (from_y + from_h) > from.height) {
			throw new IllegalArgumentException("Rectangle doesn't fit within source image");
		}

		for (int i = 0; i < from_w; ++i) {
			for (int j = 0; j < from_h; ++j) {
				// Do the calculation locally to avoid redundant bounds checking.
				int index = (((from_y + j) * from.width) + (from_x + i)) * 32;
				blitTile(Arrays.copyOfRange(from.image, index, index + 32), to_x + i, to_y + j, transparent);
			}
		}

		rendered = null;
	}

	public void blit(
		GBAImage from,
		int to_x, int to_y, boolean transparent
	) {
		blit(from, 0, 0, from.width, from.height, to_x, to_y, transparent);
	}

	public void wipeTiles(
		int from_x, int from_y, int from_w, int from_h
	) {
		if (
			from_x < 0 || from_y < 0 ||
			(from_x + from_w) > width || (from_y + from_h) > height
		)
			throw new IllegalArgumentException(
				"GBA_Image.wipeTiles: Invalid selection(s)"
			);

		for (int i = 0; i < from_w; i++) {
			for (int j = 0; j < from_h; j++) {
				blitTile(new byte[32], from_x + i, from_y + j, false);
			}
		}
	}

	public boolean sameImageAs(GBAImage other) {
		return Arrays.equals(image, other.image);
	}

	public boolean blankTileAt(int x, int y) {
		int index = ((y * width) + x) * 32;
		for (int end = index + 32; index != end; ++index) {
			if (image[index] != (byte)0) { return false; }
		}
		return true;
	}

	public boolean isBlank() {
		for (int i = 0; i < image.length; i++) {
			if (image[i] != (byte)0) { return false; }
		}
		return true;
	}

	public boolean samePaletteAs(GBAImage other) {
		return palette.equals(other.palette);
	}

	public int getTileWidth() {
		return width;
	}

	public int getTileHeight() {
		return height;
	}

	public byte[] getData() {
		return image;
	}

	public Palette getPalette() {
		return palette;
	}

	public BufferedImage getImageWithExtraColumn() {
		GBAImage withExtraColumn = new GBAImage(palette, width + 1, height);
		withExtraColumn.blit(this, 0, 0, false);
		int colourCount = palette.countUsed();
		for (int i = 0; i < colourCount; ++i) {
			int rowInTile = i / 8;
			int colInTile = 7 - (i % 8);
			int intToModify = (width * 32) + (rowInTile * 4) + (colInTile / 2);
			int nibbleToModify = colInTile % 2;

			// Keep the other nibble.
			withExtraColumn.image[intToModify] &= nibbleToModify == 1 ? 0x0F : 0xF0;
			withExtraColumn.image[intToModify] |= i << (nibbleToModify * 4);
		}

		return withExtraColumn.getImage();
	}

	public BufferedImage getImage() {
		if (rendered != null) { return rendered; }
		rendered = new BufferedImage(width * 8, height * 8, BufferedImage.TYPE_INT_ARGB);
		int[] raw = new int[width * height * 64];
		for (int i = 0; i < image.length; ++i) {
			// Convert each byte into two pixels.
			byte b = image[i];
			int tileRow = (i / 32) / width;
			int tileColumn = (i / 32) % width;
			int positionInTile = (i % 32) * 2;
			int row = tileRow * 8 + positionInTile / 8;
			int column = tileColumn * 8 + positionInTile % 8;
			int intToModify = row * width * 8 + column;
			raw[intToModify + 1] = palette.getARGB((b & 0xF0) >> 4);
			raw[intToModify] = palette.getARGB(b & 0x0F);
		}
		rendered.setRGB(0, 0, width * 8, height * 8, raw, 0, width * 8);
		return rendered;
	}

	public void sortPalette() {
		short[] paletteArray = palette.getShorts();
		int[] sortKey = new int[palette.countUsed()];
		short[] oldImage = new short[image.length * 2];
		for (int i = 0; i < image.length; ++i) {
			int highNibble = (image[i] >> 4) & 0x0f;
			int lowNibble = image[i] & 0x0f;
			oldImage[i * 2] = paletteArray[highNibble];
			oldImage[i * 2 + 1] = paletteArray[lowNibble];
		}

		for (int i = 0; i < sortKey.length; ++i) { sortKey[i] = i; }
		// Selection sort of the used elements, except the transparent
		// colour, which must not be changed
		for (int i = 1; i < sortKey.length; ++i) {
			int lowest = paletteArray[i];
			int lowestPos = i;
			for (int j = i + 1; j < sortKey.length; ++j) {
				if (paletteArray[j] < lowest) {
					lowest = paletteArray[j];
					lowestPos = j;
				}
			}
			// Swap
			short tmpShort = paletteArray[lowestPos];
			paletteArray[lowestPos] = paletteArray[i];
			paletteArray[i] = tmpShort;
			// Other swap
			int tmpInt = sortKey[lowestPos];
			sortKey[lowestPos] = sortKey[i];
			sortKey[i] = tmpInt;
		}

		// Invert the sort keys for translation purposes
		int[] invSortKey = new int[sortKey.length];
		for (int i = 0; i < sortKey.length; ++i) {
			invSortKey[sortKey[i]] = i;
		}

		for (int i = 0; i < image.length; ++i) {
			int highNibble = (image[i] >> 4) & 0x0f;
			int lowNibble = image[i] & 0x0f;
			image[i] = (byte)((invSortKey[highNibble] << 4) | invSortKey[lowNibble]);
		}
		for (int i = 0; i < image.length; ++i) {
			int highNibble = (image[i] >> 4) & 0x0f;
			int lowNibble = image[i] & 0x0f;
			if (oldImage[i * 2] != paletteArray[highNibble]) { System.out.println(Util.verboseReport("SEVERE: sortPalette FAIL")); }
			if (oldImage[i * 2 + 1] != paletteArray[lowNibble]) { System.out.println(Util.verboseReport("SEVERE: sortPalette FAIL")); }
		}
		palette.setShorts(paletteArray);
	}
	// Tested and working

	// Tell image to treat all of its pixels as opaque
	public void opaque() {
		String methodName = Util.methodName() + ": ";
		if (palette.countUsed() == 16)
			throw new RuntimeException(
				methodName
				+ "Image uses too many colors to be opaque"
			);
		short[] paletteArray = palette.getShorts();
		int used = palette.countUsed();
		paletteArray[used] = paletteArray[0];
		paletteArray[0] = 0;
		for (int i = 0; i < image.length;i++) {
			int highNibble = (image[i] >> 4) & 0x0f;
			int lowNibble = image[i] & 0x0f;
			if (highNibble == 0)
				highNibble = used;
			if (lowNibble == 0)
				lowNibble = used;
			image[i] = (byte)((highNibble << 4) | lowNibble);
		}
		used++;
		palette.setShorts(paletteArray);
		palette.setUsed(used);
		rendered = null;
	}

	// Recolors each pixel of each color with the color closest to the
	// original color in the palette given
	public void closestRecolor(Palette toRecolorWith) {
		// Old code
		byte[] replaceIndices = palette.closestColors(toRecolorWith);
		for (int i = 0; i < image.length; i++) {
			int highNibble = (image[i] >> 4) & 0x0f;
			int lowNibble = image[i] & 0x0f;
			highNibble = replaceIndices[highNibble];
			lowNibble = replaceIndices[lowNibble];
			image[i] = (byte)((highNibble << 4) | lowNibble);
		}
		// TODO: Get the dithering working such that it can be done
		// across an image composed of GBA_Images
		/* Experimental code
		int[] raw = getImage().getRGB(
			0, 0, 8, 8, null, 0, 8
		);
		// For creating new ColorInfo instances
		ColorReducer tempReducer = new ColorReducer();
		int length = toRecolorWith.countUsed() - 1;
		ColorReducer.ColorInfo[] paletteArray = new ColorReducer.ColorInfo[length];
		for (int i = 0; i < length; i++)
			paletteArray[i] = tempReducer.new ColorInfo(
				toRecolorWith.getARGB(i + 1)
			);
		ColorReducer.recolor(paletteArray, raw, 8, 8, false);
		for (int i = 0; i < image.length; i++) {
			int highNibble = toRecolorWith.getIndex(condense(raw[i + 1]));
			int lowNibble = toRecolorWith.getIndex(condense(raw[i]));
			image[i] = (byte)((highNibble << 4) | lowNibble);
		}
		*/
		palette.setShorts(toRecolorWith.getShorts());
		palette.setUsed(toRecolorWith.countUsed());
		rendered = null;
	}
	// XXX Needs improvement

	// Force redrawing of the image next time because some outside code has
	// modified the palette.
	public void notifyPaletteChangedExternally() { rendered = null; }
}

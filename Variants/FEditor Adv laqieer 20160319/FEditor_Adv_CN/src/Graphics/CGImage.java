/*
 *  FE Editor - GBA Fire Emblem (U) ROM editor
 *
 *  Copyright (C) 2008-2011 Hextator,
 *  hextator (AIM) hextator@gmail.com (MSN)
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
 *  <Description> This class is for formatting images to be compatible with
 *  the display settings of a full screen Fire Emblem CG
 */

package Graphics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Arrays;
//import java.util.Collection;
//import java.util.Hashtable;
import javax.imageio.ImageIO;
import Compression.LempelZiv;
import Model.Util;

public class CGImage {
	// Set to true to test making CG data given an image
	// Set to false to test rendering a CG given data
	public static boolean testWrite = true;

	// Original image after initial reduction to 120 or fewer colors
	private BufferedImage startImage = null;
	// Tiles reduced to 15 colors
	private GBAImage[] tiles;
	// Palettes to reduce to 8
	private Palette[] palettes;
	// Palette-tile associations
	private int[] paletteIndices;
	// For dumping CGs
	private BufferedImage rendered = null;
	// For debugging purposes
	//private BufferedImage test = null;

	// Class constants
	public static final int FULL_COLOR_BITS = 8;
	public static final int HARDWARE_COLOR_BITS = 5;
	public static final int TILE_COUNT = 600;
	public static final int TILE_DIM = 8;
	public static final int WIDTH = 240;
	public static final int TILE_WIDTH = WIDTH/TILE_DIM;
	public static final int HEIGHT = 160;
	public static final int TILE_HEIGHT = HEIGHT/TILE_DIM;
	// CG image data split into multiple sections will be split into
	// sections with this height (in pixels)
	public static final int SECTION_HEIGHT = 16;
	public static final int PALETTE_LIMIT = 8;
	public static final int PAL_COLOR_LIMIT = 15;
	public static final int PAL_LEN = 16;
	public static final int COLOR_LIMIT = PALETTE_LIMIT * PAL_COLOR_LIMIT;
	public static final int IMAGE_DATA_LEN = 0x5000;
	// Extra 2 bytes for width and height settings
	// 2 bytes per tile
	public static final int MAP_DATA_LEN = (TILE_WIDTH * TILE_HEIGHT * 2) + 2;
	public static final int PAL_DATA_LEN = 0x100;

	private class PaletteInfo implements Comparable {
		private Palette palette;
		//private int IDval;
		private int frequency;

		public PaletteInfo(
			Palette thePalette//,
			//int ID
		) {
			palette = thePalette;
			//IDval = ID;
			frequency = 1;
		}

		public PaletteInfo(PaletteInfo toCopy) {
			palette = new Palette(toCopy.palette);
			//IDval = toCopy.IDval;
			frequency = toCopy.frequency;
		}

		public Palette getPalette() { return palette; }

		//public int getID() { return IDval; }

		public int getFrequency() { return frequency; }

		public void setPalette(Palette thePalette) {
			palette = thePalette;
		}

		//public void setID(int ID) { IDval = ID; }

		public void incrementFreq() {
			if (frequency < Integer.MAX_VALUE)
				frequency++;
		}

		public void incrementFreq(int amount) {
			if (frequency + amount > Integer.MAX_VALUE)
				frequency = Integer.MAX_VALUE;
			else
				frequency += amount;
		}

		public int compareTo(Object o) {
			if (!(o instanceof PaletteInfo))
				return getClass().getName().compareTo(o.getClass().getName());
			return frequency - ((PaletteInfo)o).getFrequency();
		}
	}

	// Returns the index in the array of the palette that the palette
	// given is most similar to
	private int closestPalette(
		PaletteInfo[] palettesToChooseFrom, Palette toReplace, int startIndex
	) {
		int replaceIndex = startIndex < palettesToChooseFrom.length ?
			startIndex : palettesToChooseFrom.length - 1;
		BigDecimal prevDiff = new BigDecimal(-1);
		for (int i = startIndex; i < palettesToChooseFrom.length; i++) {
			Palette currPalette = palettesToChooseFrom[i].getPalette();
			BigDecimal diff = toReplace.paletteDifference(currPalette);
			if (
				prevDiff.equals(new BigDecimal(-1))
				|| diff.compareTo(prevDiff) < 0
			) {
				replaceIndex = i;
				prevDiff = diff;
			}
		}
		return replaceIndex;
	}

	private void verifyImage(BufferedImage theImage) {
		String methodName = Util.methodName() + ": ";
		if (theImage == null)
			throw new IllegalArgumentException(
				methodName
				+ "空图片"
			);
		if (theImage.getWidth() != WIDTH)
			throw new IllegalArgumentException(
				methodName
				+ "输入图片宽度不是240像素"
			);
		if (theImage.getHeight() != HEIGHT)
			throw new IllegalArgumentException(
				methodName
				+ "输入图片高度不是160像素"
			);
	}

	// All constructor/initialization methods eventually call this
	// (Except those which are for dumping ROM data rather than
	// generating it)
	private void initFromImage(BufferedImage theImage) {
		// Number of palettes the final image will use
		// Must be between 1 and 8 inclusive
		final int numPalettes = PALETTE_LIMIT;
		// Debug
		//final int numPalettes = 1;
		// Number of colors to share between palettes
		// (smooths tile transitions if this is higher, but also
		// sacrifices optimal colors)
		// Must be between 0 and 13 inclusive and should be odd
		int sharedColors = (numPalettes - 1) + 3;
		// Must be between 1 and 120 inclusive; using 120 has
		// obvious tiling issues but values that are too low
		// sacrifice quality
		final int uniqueColorLimit = COLOR_LIMIT - (sharedColors * numPalettes);
		final int initialReductionColorCount = uniqueColorLimit;
		// Debug
		//final int initialReductionColorCount = COLOR_LIMIT;
		// Debug
		//final int uniqueColorLimit = PAL_COLOR_LIMIT;
		// For evenly distributing the unique colors among the final
		// number of palettes
		final int resolution = uniqueColorLimit/numPalettes;
		// Debug
		//final int resolution = PAL_COLOR_LIMIT;
		// Assert width and height
		verifyImage(theImage);
		startImage = (new ColorReducer(
			theImage, initialReductionColorCount,
			FULL_COLOR_BITS, FULL_COLOR_BITS, FULL_COLOR_BITS, true
		)).getImage();
		// Width, height and color count asserted
		tiles = new GBAImage[TILE_COUNT];
		// This should be unnecessary
		//palettes = new Palette[TILE_COUNT];
		paletteIndices = new int[TILE_COUNT];
		int currIndex = 0;
		for (int y = 0; y < HEIGHT; y += TILE_DIM) {
			for (int x = 0; x < WIDTH; x += TILE_DIM) {
				tiles[currIndex] =
					new GBAImage(new ColorReducer(
						startImage.getSubimage(
							x, y, TILE_DIM, TILE_DIM
						),
						PAL_COLOR_LIMIT,
						HARDWARE_COLOR_BITS,
						HARDWARE_COLOR_BITS,
						HARDWARE_COLOR_BITS,
						true
					).getImage());
				tiles[currIndex].opaque();
				// This should be unnecessary
				//tiles[currIndex].sortPalette();
				// This should be unnecessary
				//palettes[currIndex] = tiles[currIndex].getPalette();
				// This should be unnecessary
				//paletteIndices[currIndex] = currIndex;
				currIndex++;
			}
		}
		/* Ditching this palette reduction algorithm
		// TODO: Make an array of all possible palettes resulting from
		// each combination of a non-full palette with another
		// non-full palette such that the combination has as close
		// to 15 colors as possible without having more
		// TODO: In the process of doing the previous, combined
		// tiles should updated to share palettes and any shared
		// palettes that don't use 15 colors should be extended
		// to do so by cloning a color
		// Renumber palettes based on the assumption that at least
		// one tile will have the same palette as another
		Hashtable<Palette, PaletteInfo> paletteRenumber =
			new Hashtable<Palette, PaletteInfo>();
		for (int i = 0; i < palettes.length; i++) {
			PaletteInfo currInfo = paletteRenumber.get(palettes[i]);
			if (currInfo == null)
				paletteRenumber.put(
					palettes[i],
					new PaletteInfo(palettes[i], i)
				);
			else
				paletteIndices[i] = currInfo.getID();
		}
		// For keeping track of palette replacements
		Hashtable<Integer, PaletteInfo> paletteReplacer =
			new Hashtable<Integer, PaletteInfo>();
		// Convert palette of palettes into an array for manageability
		Collection<PaletteInfo> values = paletteRenumber.values();
		PaletteInfo[] palettesArray = new PaletteInfo[values.size()];
		currIndex = 0;
		for (PaletteInfo curr: values)
			palettesArray[currIndex++] = curr;
		// Palette count reduced from 600 to actual number of used palettes
		// Each palette has its number of occurrences known
		int count = palettesArray.length;
		currIndex = 0;
		while (count > PALETTE_LIMIT) {
			Arrays.sort(palettesArray);
			// Palettes sorted by ascending occurrence
			PaletteInfo oldInfo = palettesArray[currIndex++];
			Palette toReplace = oldInfo.getPalette();
			int replaceIndex = closestPalette(
				palettesArray, toReplace, currIndex
			);
			PaletteInfo newInfo = new PaletteInfo(
				palettesArray[replaceIndex]
			);
			newInfo.incrementFreq(oldInfo.getFrequency());
			paletteReplacer.put(
				oldInfo.getID(),
				newInfo
			);
			currIndex++;
			count--;
		}
		*/
		// Generate 8 palettes by using the colors in each box
		// left over from using median cut to reduce the image to 8
		// colors as entire palettes
		PaletteInfo[] palettesArray = new PaletteInfo[numPalettes];
		ColorReducer tempReducer = new ColorReducer(
			startImage, numPalettes,
			HARDWARE_COLOR_BITS, HARDWARE_COLOR_BITS, HARDWARE_COLOR_BITS,
			false, resolution
		);
		currIndex = 0;
		ColorReducer.ColorInfoArray[] boxes = tempReducer.getBoxes();
		for (ColorReducer.ColorInfoArray curr: boxes) {
			ColorReducer.ColorInfo[] colors = curr.getArray();
			short[] asShorts = new short[colors.length + 1];
			for (int i = 1; i < colors.length + 1; i++)
				asShorts[i] = GBAImage.condense(
					colors[i - 1].getColor()
				);
			palettesArray[currIndex++] = new PaletteInfo(
				new Palette(asShorts)
			);
		}
		// Fill unused palette entries in each palette with colors
		// generated by averaging the average color of the palette
		// with the average colors of similar palettes to smooth
		// tile transitions (after reusing averages of other palettes
		// without averaging result with current palette until
		// there are only 7 empty slots left in the palette being
		// filled)
		if (sharedColors < 0)
			sharedColors = 0;
		// Share average colors between palettes
		if (sharedColors > numPalettes - 1) {
			int numColors = sharedColors - (numPalettes - 1);
			for (int pal = 0; pal < palettesArray.length; pal++) {
				short[] curr = palettesArray[pal].getPalette().getShorts();
				/* Debug
				int thisColor = boxes[pal]
					.averageColor().getColor();
				int oldRed = (thisColor >> 0x10) & 0xFF;
				int oldGreen = (thisColor >> 0x8) & 0xFF;
				int oldBlue = thisColor & 0xFF;
				*/
				for (
					int offset = 0,
					index = PAL_LEN - (numPalettes - 1) - numColors;
					index < PAL_LEN - (numPalettes - 1); index++
				) {
					offset = offset >= 0
						? -(offset + 1) : -offset;
					/* Debug
					int otherColor = boxes[pal + offset]
						.averageColor().getColor();
					int newRed = (otherColor >> 0x10) & 0xFF;
					int newGreen = (otherColor >> 0x8) & 0xFF;
					int newBlue = otherColor & 0xFF;
					int finalRed = (newRed + newRed + newRed)/3;
					int finalGreen = (newGreen + newGreen + newGreen)/3;
					int finalBlue = (newBlue + newBlue + newBlue)/3;
					int finalColor = 0xFF000000
						| (finalRed << 0x10)
						| (finalGreen << 0x8)
						| finalBlue;
					*/
					int temp = pal + offset;
					while (temp < 0) temp += boxes.length;
					int finalColor = boxes[temp % boxes.length]
						.averageColor().getColor();
					curr[index] = GBAImage.condense(
						finalColor
					);
				}
				palettesArray[pal].setPalette(new Palette(curr));
			}
			sharedColors = numPalettes - 1;
		}
		// Nearby averages with frequency bias
		for (int pal = 0; pal < palettesArray.length; pal++) {
			short[] curr = palettesArray[pal].getPalette().getShorts();
			ColorReducer.ColorInfo currInfo = boxes[pal].averageColor();
			int thisColor = currInfo.getColor();
			int thisFreq = currInfo.getFrequency();
			int oldRed = (thisColor >> 0x10) & 0xFF;
			int oldGreen = (thisColor >> 0x8) & 0xFF;
			int oldBlue = thisColor & 0xFF;
			for (
				int offset = 0, index = PAL_LEN - sharedColors;
				index < PAL_LEN; index++
			) {
				offset = offset >= 0
					? -(offset + 1) : -offset;
				int temp = pal + offset;
				while (temp < 0) temp += boxes.length;
				ColorReducer.ColorInfo thatInfo =
					boxes[temp % boxes.length].averageColor();
				int otherColor = thatInfo.getColor();
				int thatFreq = thatInfo.getFrequency();
				int newRed = (otherColor >> 0x10) & 0xFF;
				int newGreen = (otherColor >> 0x8) & 0xFF;
				int newBlue = otherColor & 0xFF;
				int denominator = thisFreq + thatFreq;
				int finalRed = (oldRed * thisFreq + newRed * thatFreq)/denominator;
				int finalGreen = (oldGreen * thisFreq + newGreen * thatFreq)/denominator;
				int finalBlue = (oldBlue * thisFreq + newBlue * thatFreq)/denominator;
				int finalColor = 0xFF000000
					| (finalRed << 0x10)
					| (finalGreen << 0x8)
					| finalBlue;
				curr[index] = GBAImage.condense(
					finalColor
				);
			}
			palettesArray[pal].setPalette(new Palette(curr));
		}
		/* Debug
		GBAImage[] temp = Arrays.copyOf(tiles, tiles.length);
		for (int i = 0; i < tileCount; i++)
			tiles[i] = new GBAImage(
				new byte[] {
					(byte)0x10, (byte)0x32,
					(byte)0x54, (byte)0x76,
					(byte)0x98, (byte)0xBA,
					(byte)0xDC, (byte)0xFE,
					0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0, 0, 0
				}, palettesArray[
					i % palettesArray.length
				].getPalette(), 1, 1
			);
		render();
		test = rendered;
		tiles = temp;
		// End debug
		*/
		// Update palettes field
		palettes = new Palette[PALETTE_LIMIT];
		for (int i = 0; i < PALETTE_LIMIT; i++) {
			try {
				palettes[i] = palettesArray[i].getPalette();
			} catch (Exception e) {
				palettes[i] = new Palette(new short[PAL_LEN]);
			}
		}
		// Recolor tile images with palette from palettesArray
		// closest to the palette of the image
		// Update palette indices along the way
		for (int i = 0; i < tiles.length; i++) {
			int index = closestPalette(
				palettesArray, tiles[i].getPalette(), 0
			);
			tiles[i].closestRecolor(palettesArray[index].getPalette());
			paletteIndices[i] = index;
		}
		// Invalidate rendered image
		rendered = null;
	}
	// May need improvement

	private void initFromFile(File imageFile) {
		BufferedImage theImage = null;
		try {
			theImage = ImageIO.read(imageFile);
			initFromImage(theImage);
		} catch (Exception e) {}
	}

	public CGImage(BufferedImage theImage) {
		initFromImage(theImage);
	}

	public CGImage(File imageFile) {
		initFromFile(imageFile);
	}

	public CGImage(byte[] imageData, byte[] mapData, byte[] paletteData) {
		String methodName = Util.methodName() + ": ";
		if (
			imageData == null || imageData.length != IMAGE_DATA_LEN
		)
			throw new IllegalArgumentException(
				methodName
				+ "图片数据长度错误"
			);
		if (
			mapData == null || mapData.length != MAP_DATA_LEN
		)
			throw new IllegalArgumentException(
				methodName
				+ "地图数据长度错误"
			);
		if (
			mapData[0] != TILE_WIDTH - 1
			|| mapData[1] != TILE_HEIGHT - 1
		)
			throw new IllegalArgumentException(
				methodName
				+ "地图数据指定了错误的尺寸"
			);
		if (
			paletteData == null || paletteData.length != PAL_DATA_LEN
		)
			throw new IllegalArgumentException(
				methodName
				+ "调色板数据长度错误"
			);
		final int mapTileWidth = 32;
		final int bytesPerTile = TILE_DIM * TILE_DIM/2;
		palettes = new Palette[PALETTE_LIMIT];
		paletteIndices = new int[TILE_COUNT];
		tiles = new GBAImage[TILE_COUNT];
		// Load palettes
		for (
			int i = 0, pal = 0;
			i < paletteData.length;
			i += PAL_LEN * 2, pal++
		)
			palettes[pal] = new Palette(Arrays.copyOfRange(
				paletteData, i, i + PAL_LEN * 2
			));
		// Draw tiles and update palette indices in the process
		short[] mapDataAsShorts = Util.bytesToShorts(mapData);
		for (int i = 1; i < mapDataAsShorts.length; i++) {
			short curr = mapDataAsShorts[i];
			int palIndex = (curr >> 0xC) & 0xF;
			int originalTileID = curr & 0xFFF;
			int tileID = (originalTileID % mapTileWidth)
				+ ((originalTileID/mapTileWidth) * TILE_WIDTH);
			tiles[tileID] = new GBAImage(
				Arrays.copyOfRange(
					imageData, originalTileID * bytesPerTile,
					originalTileID * bytesPerTile + bytesPerTile
				),
				palettes[palIndex],
				1, 1
			);
		}
	}

	public CGImage(
		byte[][] imageData, byte[] mapData, byte[] paletteData
	) {
		this(Util.bytesToBytes(imageData), mapData, paletteData);
	}

	public byte[] getImageData() {
		byte[] output = new byte[IMAGE_DATA_LEN];
		if (tiles == null || tiles.length != TILE_COUNT)
			return output;
		final int mapTileWidth = 32;
		final int bytesPerTile = TILE_DIM * TILE_DIM/2;
		int currTile = 0;
		int currIndex = 0;
		for (int y = 0; y < TILE_HEIGHT; y++) {
			for (int x = 0; x < mapTileWidth; x++) {
				if (x * TILE_DIM < WIDTH) {
					System.arraycopy(
						tiles[currTile].getData(), 0,
						output, currIndex * bytesPerTile,
						bytesPerTile
					);
					currTile++;
				}
				currIndex++;
			}
		}
		return output;
	}

	public byte[][] getMultipleImageData() {
		final byte[] imageData = getImageData();
		final int sections = HEIGHT/SECTION_HEIGHT;
		final int bytes_per_section = imageData.length/sections;
		byte[][] output = new byte[sections][];
		for (int i = 0; i < sections; i++) {
			output[i] = java.util.Arrays.copyOfRange(
				imageData,
				i * bytes_per_section,
				(i + 1) * bytes_per_section
			);
		}
		return output;
	}

	public byte[] getMapData() {
		byte[] output = new byte[MAP_DATA_LEN];
		if (
			paletteIndices == null
			|| paletteIndices.length != TILE_COUNT
		)
			return output;
		final int mapTileWidth = 32;
		output[0] = TILE_WIDTH - 1;
		output[1] = TILE_HEIGHT - 1;
		int currTile = 0;
		int currTileValue = 0;
		int currIndex = output.length - (TILE_WIDTH * 2);
		for (int y = TILE_HEIGHT - 1; y >= 0; y--) {
			for (int x = 0; x < TILE_WIDTH; x++) {
				short temp = (short)currTileValue;
				temp |= (paletteIndices[currTile] & 0xF) << 0xC;
				output[currIndex] = (byte)temp;
				output[currIndex + 1] = (byte)(temp >> 8);
				currIndex += 2;
				currTile++;
				currTileValue++;
			}
			for (int x = TILE_WIDTH; x < mapTileWidth; x++)
				currTileValue++;
			currIndex -= TILE_WIDTH * 4;
		}
		return output;
	}

	public byte[] getBattleBGMap() {
		byte[] output = getMapData();
		return java.util.Arrays.copyOfRange(output, 2, output.length);
	}

	public byte[] getPalettes() {
		byte[] output = new byte[PAL_DATA_LEN];
		if (palettes == null || palettes.length != PALETTE_LIMIT)
			return output;
		final int bytesPerTile = TILE_DIM * TILE_DIM/2;
		for (int i = 0; i < PALETTE_LIMIT; i++)
			System.arraycopy(
				palettes[i].getBytes(), 0,
				output, i * bytesPerTile,
				bytesPerTile
			);
		return output;
	}

	private void render() {
		if (tiles == null || tiles.length != TILE_COUNT)
			return;
		rendered = new BufferedImage(
			WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		int currIndex = 0;
		for (int y = 0; y < HEIGHT/TILE_DIM; y++) {
			for (int x = 0; x < WIDTH/TILE_DIM; x++) {
				int[] RGBdata = tiles[currIndex].getImage().getRGB(
					0, 0, TILE_DIM, TILE_DIM, null, 0, TILE_DIM
				);
				rendered.setRGB(
					x * TILE_DIM, y * TILE_DIM,
					TILE_DIM, TILE_DIM,
					RGBdata, 0, TILE_DIM
				);
				currIndex++;
			}
		}
	}

	public BufferedImage getRendered() {
		if (rendered == null) render();
		return rendered;
	}

	private static void testWrite()
	throws Exception {
		File imageFile = FEditorAdvance.CommonDialogs
			.showOpenFileDialog("240x160 px image file");
		CGImage cg = new CGImage(imageFile);
		ImageIO.write(
			cg.getRendered(), "PNG",
			FEditorAdvance.CommonDialogs.showSaveFileDialog(
				"rendered CG"
			)
		);
		FileOutputStream imageDataStream = null;
		try {
			imageDataStream = new FileOutputStream(
			FEditorAdvance.CommonDialogs.showSaveFileDialog(
				"image data dump"
			));
			imageDataStream.write(LempelZiv.compress(cg.getImageData()));
		} catch (Exception e) {}
		try {
			imageDataStream.close();
		} catch (Exception e) {}
		FileOutputStream cgMapDataStream = null;
		try {
			cgMapDataStream = new FileOutputStream(
			FEditorAdvance.CommonDialogs.showSaveFileDialog(
				"CG map data dump"
			));
			cgMapDataStream.write(cg.getMapData());
		} catch (Exception e) {}
		try {
			cgMapDataStream.close();
		} catch (Exception e) {}
		FileOutputStream paletteDataStream = null;
		try {
			paletteDataStream = new FileOutputStream(
			FEditorAdvance.CommonDialogs.showSaveFileDialog(
				"palette data dump"
			));
			paletteDataStream.write(cg.getPalettes());
		} catch (Exception e) {}
		try {
			paletteDataStream.close();
		} catch (Exception e) {}
	}

	private static void testRead()
	throws Exception {
		File imageDataFile = FEditorAdvance.CommonDialogs
			.showOpenFileDialog("CG image data file");
		File mapDataFile = FEditorAdvance.CommonDialogs
			.showOpenFileDialog("CG map data file");
		File palDataFile = FEditorAdvance.CommonDialogs
			.showOpenFileDialog("CG palette data file");
		FileInputStream imageDataStream = new FileInputStream(imageDataFile);
		FileInputStream mapDataStream = new FileInputStream(mapDataFile);
		FileInputStream palDataStream = new FileInputStream(palDataFile);
		byte[] imageData = new byte[(int)imageDataFile.length()];
		byte[] mapData = new byte[(int)mapDataFile.length()];
		byte[] paletteData = new byte[(int)palDataFile.length()];
		imageDataStream.read(imageData);
		mapDataStream.read(mapData);
		palDataStream.read(paletteData);
		CGImage cg = new CGImage(
			LempelZiv.decompress(imageData), mapData, paletteData
		);
		ImageIO.write(
			cg.getRendered(), "PNG",
			FEditorAdvance.CommonDialogs.showSaveFileDialog(
				"rendered CG"
			)
		);
		try {
			imageDataStream.close();
		} catch (Exception e) {}
		try {
			mapDataStream.close();
		} catch (Exception e) {}
		try {
			palDataStream.close();
		} catch (Exception e) {}
	}

	public static void main(String[] args) {
		int answer = javax.swing.JOptionPane.showConfirmDialog(
			null,
			"Are you creating a CG instead of dumping one?",
			"CG Maker",
			javax.swing.JOptionPane.YES_NO_OPTION
		);
		testWrite = answer == javax.swing.JOptionPane.YES_OPTION;
		try {
			if (testWrite) testWrite();
			else testRead();
		} catch (Exception e) {
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			javax.swing.JTextArea errorDisplayArea =
				new javax.swing.JTextArea(
					"Exception: "
					+ e.getMessage()
					+ "\n"
					+ result.toString()
				);
			javax.swing.JScrollPane errorPane =
				new javax.swing.JScrollPane(errorDisplayArea);
			javax.swing.JOptionPane.showMessageDialog(
				null,
				errorPane,
				"Error",
				javax.swing.JOptionPane.ERROR_MESSAGE
			);
		}
		System.exit(0);
	}
	// Methods tested and working
}

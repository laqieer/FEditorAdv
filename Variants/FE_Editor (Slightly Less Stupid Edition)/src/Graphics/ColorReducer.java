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
 *  <Description> This class is for performing color reduction of images the
 *  intended use of which requires the images to have arbitrarily fewer colors
 */

package Graphics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import javax.imageio.ImageIO;

public class ColorReducer {
	private BufferedImage image = null;
	private BufferedImage original = null;
	private int colorInfoSortMode = 0;
	private ColorInfoArray[] boxes;

	// Dithering constants
	// Type is currently Floyd-Steinberg
	private static int[] Xoffsets = new int[] { 1, -1, 0, 1 };
	private static int[] Yoffsets = new int[] { 0, 1, 1, 1 };
	private static int[] coefficients = new int[] { 7, 3, 5, 1 };
	private static int scalar = 16;

	// Median cut algorithm constant
	private int resolution = 1;

	public class ColorInfo implements Comparable {
		private int color;
		private int frequency;

		public ColorInfo(int theColor) {
			color = theColor;
			frequency = 1;
		}

		public ColorInfo(ColorInfo toCopy) {
			color = toCopy.color;
			frequency = toCopy.frequency;
		}

		public int getColor() { return color; }

		public int getFrequency() { return frequency; }

		public void setColor(int newColor) { color = newColor; }

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

		public void sortByFrequency() { colorInfoSortMode = 0; }

		public void sortByRedIntensity() { colorInfoSortMode = 1; }

		public void sortByGreenIntensity() { colorInfoSortMode = 2; }

		public void sortByBlueIntensity() { colorInfoSortMode = 3; }

		public int compareTo(Object o) {
			if (!(o instanceof ColorInfo))
				return getClass().getName().compareTo(o.getClass().getName());
			if (colorInfoSortMode == 1)
				return (color & 0xFF0000) - (((ColorInfo)o).getColor() & 0xFF0000);
			if (colorInfoSortMode == 2)
				return (color & 0xFF00) - (((ColorInfo)o).getColor() & 0xFF00);
			if (colorInfoSortMode == 3)
				return (color & 0xFF) - (((ColorInfo)o).getColor() & 0xFF);
			return frequency - ((ColorInfo)o).getFrequency();
		}
	}

	public class ColorInfoArray implements Comparable {
		private ColorInfo[] array;

		public ColorInfoArray(ColorInfo[] theArray) {
			array = theArray;
		}

		public ColorInfoArray(ColorInfoArray toCopy) {
			ColorInfo[] originalCopy = toCopy.getArray();
			ColorInfo[] arrayCopy = new ColorInfo[originalCopy.length];
			for (int i = 0; i < arrayCopy.length; i++)
				arrayCopy[i] = new ColorInfo(originalCopy[i]);
			array = arrayCopy;
		}

		public ColorInfo[] getArray() { return array; }

		public void setArray(ColorInfo[] theArray) { array = theArray; }

		private int[] setSortModeByWidestAxis(boolean retVol) {
			int redMin = 256;
			int greenMin = 256;
			int blueMin = 256;
			int redMax = -1;
			int greenMax = -1;
			int blueMax = -1;
			for (int i = 0; i < array.length; i++) {
				int color = array[i].getColor();
				int red = (color >> 0x10) & 0xFF;
				int green = (color >> 0x8) & 0xFF;
				int blue = color & 0xFF;
				if (red < redMin)
					redMin = red;
				if (red > redMax)
					redMax = red;
				if (green < greenMin)
					greenMin = green;
				if (green > greenMax)
					greenMax = green;
				if (blue < blueMin)
					blueMin = blue;
				if (blue > blueMax)
					blueMax = blue;
			}
			int redDiff = redMax - redMin;
			int greenDiff = greenMax - greenMin;
			int blueDiff = blueMax - blueMin;
			if (retVol)
				return new int[] {
					redDiff,
					greenDiff,
					blueDiff
				};
			colorInfoSortMode = 1;
			if (greenDiff > redDiff)
				colorInfoSortMode = 2;
			if (colorInfoSortMode == 2 && blueDiff > greenDiff)
				colorInfoSortMode = 3;
			if (colorInfoSortMode == 1 && blueDiff > redDiff)
				colorInfoSortMode = 3;
			return new int[] {
				redDiff,
				greenDiff,
				blueDiff
			};
		}

		private void setSortModeByWidestAxis() {
			setSortModeByWidestAxis(false);
		}

		public double volume() {
			int[] diffs = setSortModeByWidestAxis(true);
			double redDiff = diffs[0];
			double greenDiff = diffs[1];
			double blueDiff = diffs[2];
			return redDiff * greenDiff * blueDiff;
		}

		public ColorInfo averageColor() {
			int red = 0;
			int green = 0;
			int blue = 0;
			int elementCount = array.length;
			ColorInfo output = new ColorInfo(
				0xFF000000
			);
			output.incrementFreq(-1);
			for (int i = 0; i < elementCount; i++) {
				int color = array[i].getColor();
				red += (color >> 0x10) & 0xFF;
				green += (color >> 0x8) & 0xFF;
				blue += color & 0xFF;
				output.incrementFreq(array[i].getFrequency());
			}
			red /= elementCount;
			green /= elementCount;
			blue /= elementCount;
			output.setColor(
				0xFF000000
				| ((red > 0xFF ? 0xFF : red) << 0x10)
				| ((green > 0xFF ? 0xFF : green) << 0x8)
				| (blue > 0xFF ? 0xFF : blue)
			);
			return output;
		}

		public ColorInfo[] medianCut() {
			int halfSize = array.length >> 1;
			setSortModeByWidestAxis();
			Arrays.sort(array);
			ColorInfo[] output = Arrays.copyOfRange(array, halfSize, array.length);
			array = Arrays.copyOf(array, halfSize);
			return output;
		}

		public int population() {
			int output = 0;
			for (int i = 0; i < array.length; i++)
				output += array[i].getFrequency();
			return output;
		}

		public int compareTo(Object o) {
			if (!(o instanceof ColorInfoArray))
				return getClass().getName().compareTo(o.getClass().getName());
			ColorInfoArray newO = (ColorInfoArray)o;
			double thisWeight = population() * volume();
			double thatWeight = newO.population() * newO.volume();
			if (array.length <= resolution || newO.array.length <= resolution)
				return array.length - newO.array.length;
			return (int)(thisWeight - thatWeight);
		}
	}

	// Calculates the distance squared between two colors mapped in the
	// R, G, B coordinate space
	public static int colorDifference(int firstColor, int secondColor) {
		int redDiff = ((firstColor >> 0x10) & 0xFF)
			- ((secondColor >> 0x10) & 0xFF)
		;
		int greenDiff = ((firstColor >> 0x8) & 0xFF)
			- ((secondColor >> 0x8) & 0xFF)
		;
		int blueDiff = (firstColor & 0xFF)
			- (secondColor & 0xFF)
		;
		int diff = redDiff * redDiff
			+ greenDiff * greenDiff
			+ blueDiff * blueDiff
		;
		return diff;
	}

	// Returns the index in the array of the color that the XRGB color
	// given (as 8 bits ignored and 8 bits for R, G and B values) is
	// most similar to
	public static int closestColor(
		ColorInfo[] colorsToChooseFrom, int color, int startOffset
	) {
		int replaceIndex = startOffset < colorsToChooseFrom.length ?
			startOffset : colorsToChooseFrom.length - 1;
		int prevDiff = Integer.MAX_VALUE;
		for (int i = startOffset; i < colorsToChooseFrom.length; i++) {
			int currColor = colorsToChooseFrom[i].getColor();
			int diff = colorDifference(color, currColor);
			if (diff < prevDiff) {
				replaceIndex = i;
				prevDiff = diff;
			}
		}
		return replaceIndex;
	}

	private void optimizePaletteByMedianCut(
		ColorInfo[] paletteArray, int paletteSize
	) {
		if (paletteSize >= paletteArray.length) {
			boxes = new ColorInfoArray[paletteArray.length];
			for (int i = 0; i < boxes.length; i++)
				boxes[i] = new ColorInfoArray(new ColorInfo[] {
					paletteArray[i]
				});
		}
		int count = 1;
		boxes = new ColorInfoArray[paletteSize];
		for (int i = 0; i < paletteSize - 1; i++)
			boxes[i] = new ColorInfoArray(new ColorInfo[0]);
		boxes[boxes.length - 1] = new ColorInfoArray(paletteArray);
		while (count < paletteSize) {
			boxes[boxes.length - count - 1] =
				new ColorInfoArray(boxes[boxes.length - 1].medianCut());
			Arrays.sort(boxes);
			count++;
		}
	}

	// Calls optimizePaletteByMedianCut and reduces the boxes as well
	private ColorInfo[] optimizePalette(
		ColorInfo[] paletteArray, int paletteSize
	) {
		optimizePaletteByMedianCut(
			paletteArray, paletteSize
		);
		ColorInfo[] output = new ColorInfo[boxes.length];
		for (int i = 0; i < output.length; i++)
			output[i] = boxes[i].averageColor();
		return output;
	}

	// Dithering helper
	private static int recolorByError(
		int originalColor, int newColor, int colorToDiffuseTo, int coefficient
	) {
		for (int i = 0; i < 3; i++) {
			int temp = (colorToDiffuseTo >> (i * 8)) & 0xFF;
			int newCol = (newColor >> (i * 8)) & 0xFF;
			int original = (originalColor >> (i * 8)) & 0xFF;
			int quantError = original - newCol;
			colorToDiffuseTo &= 0xFFFFFFFF - (0xFF << (i * 8));
			temp += (coefficient * quantError)/scalar;
			// Clamping
			if (temp >= 0x100)
				temp = 0xFF;
			if (temp < 0)
				temp = 0;
			colorToDiffuseTo |= temp << (i * 8);
		}
		return colorToDiffuseTo;
	}

	public static void dither(
		int[] raw, int x, int y, int currIndex, int xDir,
		int width, int height, int oldpixel, int newpixel
	) {
		for (int i = 0; i < coefficients.length; i++) {
			boolean validX = false;
			boolean validY = false;
			if (x + Xoffsets[i] >= 0 && x + Xoffsets[i] < width)
				validX = true;
			if (y + Yoffsets[i] >= 0 && y + Yoffsets[i] < height)
				validY = true;
			if (validX && validY) {
				int tempIndex = currIndex + (xDir * Xoffsets[i]) + (width * Yoffsets[i]);
				raw[tempIndex] = recolorByError(
					oldpixel, newpixel,
					raw[tempIndex],
					coefficients[i]
				);
			}
		}
	}

	public static void recolor(
		ColorInfo[] paletteArray, int[] raw, int width, int height,
		boolean dither
	) {
		// Dithering processing is serpentine
		for (int y = 0; y < height; y++) {
			int xDir = y % 2 == 0 ? 1 : -1;
			for (int x = 0; x < width; x++) {
				int currIndex = y * width;
				if (xDir == 1)
					currIndex += x;
				else
					currIndex += width - x - 1;
				int oldpixel = raw[currIndex];
				int newpixel = paletteArray[closestColor(paletteArray, oldpixel, 0)].getColor();
				raw[currIndex] = newpixel;
				if (dither)
					dither(
						raw, x, y, currIndex, xDir,
						width, height, oldpixel, newpixel
					);
			}
		}
	}

	// All constructor/initialization methods eventually call this
	private void initFromImage(
		BufferedImage theImage, int paletteSize,
		final int redBits, final int greenBits, final int blueBits,
		boolean dither, int resolution
	) {
		if (resolution >= 1)
			this.resolution = resolution;
		// Collect colors into an optimized palette, later reducing
		// them into forms that are representable by the hardware
		// specified by the redBits, blueBits and greenBits variables
		// prior
		final int width = theImage.getWidth();
		final int height = theImage.getHeight();
		final int redMask = (0 - (1 << (8 - redBits))) & 0xFF;
		final int greenMask = (0 - (1 << (8 - greenBits))) & 0xFF;
		final int blueMask = (0 - (1 << (8 - blueBits))) & 0xFF;
		original = theImage.getSubimage(0, 0, width, height);
		int[] raw = original.getRGB(0, 0, width, height, null, 0, width);
		Hashtable<Integer, ColorInfo> palette = new Hashtable<Integer, ColorInfo>();
		for (int i = 0; i < raw.length; i++) {
			raw[i] |= 0xFF000000; // Screw transparency!
			if (palette.get(raw[i]) == null)
				palette.put(raw[i], new ColorInfo(raw[i]));
			else
				palette.get(raw[i]).incrementFreq();
		}
		// Palette generated; preparing output image:
		image = new BufferedImage(
				width, height,
				BufferedImage.TYPE_INT_ARGB
			);
		// Complete the output image if the color limit is already
		// satisfied
		if (paletteSize >= palette.size()) {
			image.setRGB(0, 0, width, height, raw, 0, width);
			return;
		}
		// Convert palette to an array for manageability
		Collection<ColorInfo> paletteValues = palette.values();
		ColorInfo[] paletteArray = new ColorInfo[paletteValues.size()];
		int index = 0;
		for (ColorInfo currInfo: paletteValues)
			paletteArray[index++] = currInfo;
		// Determine optimized palette
		paletteArray = optimizePalette(paletteArray, paletteSize);
		// Accommodate target architecture
		for (int i = 0; i < paletteArray.length; i++)
			paletteArray[i].setColor(
				paletteArray[i].getColor()
				& (0xFF000000
				| (redMask << 0x10)
				| (greenMask << 0x8)
				| blueMask)
			);
		// The above seems to work better here rather than later
		// Palette optimized
		// Recoloring (and dithering image, if applicable)
		recolor(
			paletteArray, raw, width, height,
			dither
		);
		/* Assert color count
		int count = 1;
		int[] testColors = new int[paletteSize];
		testColors[0] = raw[0];
		for (int i = 0; i < raw.length; i++) {
			int curr = raw[i];
			boolean found = false;
			for (int j = 0; j < count; j++) {
				if (testColors[j] == curr)
					found = true;
			}
			if (!found) {
				if (count == paletteSize)
					throw new RuntimeException("Uh-oh");
				testColors[count++] = curr;
			}
		}
		*/
		/* Accommodate target architecture
		for (int i = 0; i < raw.length; i++)
			raw[i] &= 0xFF000000
				| (redMask << 0x10)
				| (greenMask << 0x8)
				| blueMask
			;
		*/
		// Color reduction complete
		image.setRGB(0, 0, width, height, raw, 0, width);
	}

	private void initFromImage(
		BufferedImage theImage, int paletteSize,
		final int redBits, final int greenBits, final int blueBits,
		boolean dither
	) {
		initFromImage(
			theImage, paletteSize,
			redBits, greenBits, blueBits,
			dither, 1
		);
	}

	private void initFromFile(
		File imageFile, int paletteSize,
		final int redBits, final int greenBits, final int blueBits,
		boolean dither, int resolution
	) {
		BufferedImage theImage = null;
		try {
			theImage = ImageIO.read(imageFile);
			initFromImage(
				theImage, paletteSize,
				redBits, greenBits, blueBits,
				dither, resolution
			);
		} catch (Exception e) {}
	}

	private void initFromFile(
		File imageFile, int paletteSize,
		final int redBits, final int greenBits, final int blueBits,
		boolean dither
	) {
		initFromFile(
			imageFile, paletteSize,
			redBits, greenBits, blueBits,
			dither, 1
		);
	}

	public ColorReducer(
		BufferedImage theImage, int paletteSize,
		final int redBits, final int greenBits, final int blueBits,
		boolean dither
	) {
		initFromImage(
			theImage, paletteSize,
			redBits, greenBits, blueBits,
			dither
		);
	}

	public ColorReducer(
		BufferedImage theImage, int paletteSize,
		final int redBits, final int greenBits, final int blueBits,
		boolean dither, int resolution
	) {
		initFromImage(
			theImage, paletteSize,
			redBits, greenBits, blueBits,
			dither, resolution
		);
	}

	public ColorReducer(
		File imageFile, int paletteSize,
		final int redBits, final int greenBits, final int blueBits,
		boolean dither
	) {
		initFromFile(
			imageFile, paletteSize,
			redBits, greenBits, blueBits,
			dither
		);
	}

	public ColorReducer(
		File imageFile, int paletteSize,
		final int redBits, final int greenBits, final int blueBits,
		boolean dither, int resolution
	) {
		initFromFile(
			imageFile, paletteSize,
			redBits, greenBits, blueBits,
			dither, resolution
		);
	}

	// Empty constructor for accessing contained classes
	public ColorReducer() {}

	public BufferedImage getImage() { return image; }

	public BufferedImage getOriginal() { return original; }

	public ColorInfoArray[] getBoxes() {
		if (boxes == null)
			return null;
		ColorInfoArray[] boxesCopy = new ColorInfoArray[boxes.length];
		for (int i = 0; i < boxesCopy.length; i++)
			boxesCopy[i] = new ColorInfoArray(boxes[i]);
		return boxesCopy;
	}

	public static void main(String[] args) {
		File theFile = FEditorAdvance.CommonDialogs.showOpenFileDialog("image");
		Integer colorCount = null;
		try {
			colorCount = Model.Util.parseInt(
				javax.swing.JOptionPane.showInputDialog(
					"Enter the number of colors the image can have:",
					""
				)
			);
		} catch (Exception e) { System.exit(1); }
		ColorReducer theReducer = null;
		if (theFile != null && colorCount != null)
			theReducer = new ColorReducer(
				theFile, colorCount,
				5, 5, 5,
				true
			);
		if (theReducer == null)
			System.exit(1);
		BufferedImage reduced = theReducer.getImage();
		try {
			ImageIO.write(
				reduced, "PNG",
				FEditorAdvance.CommonDialogs.showSaveFileDialog("reduced image")
			);
		} catch (Exception e) {
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			javax.swing.JTextArea errorDisplayArea =
				new javax.swing.JTextArea(
				"Exception: " +
				e.getMessage() +
				"\n" +
				result.toString()
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
}

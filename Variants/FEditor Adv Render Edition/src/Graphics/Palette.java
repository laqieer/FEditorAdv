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
 *  <Description> Represents 16-colour palettes for GBA_Images.
 */

package Graphics;

import java.math.BigDecimal;
import java.util.Arrays;
import Model.Util;

public class Palette {
	private short[] entries = new short[16];
	// 2 byte R5B5G5 value for each of 16 colours

	private int used = 0;

	public int countUsed() { return used; }

	// Load an already known palette. Don't do any optimization to remove
	// duplicate colours, because it will corrupt the image.
	public Palette(short[] topRightCorner) {
		String methodName = Util.methodName() + ": ";
		if (topRightCorner == null || topRightCorner.length > 16)
			throw new IllegalArgumentException(
				methodName
				+ "topRightCorner is incorrect length"
			);
		for (short color: topRightCorner) {
			entries[used++] = (short)(color & 0x7FFF);
		}
	}

	public Palette(byte[] data, int begin, int length) {
		this(
			(begin == 0 && length == data.length)
			? data
			: java.util.Arrays.copyOfRange(data, begin, begin + length)
		);
	}

	public Palette(byte[] data) {
		this(Util.bytesToShorts(data));
	}

	// Copy constructor
	public Palette(Palette toCopy) {
		entries = toCopy.getShorts();
		used = toCopy.used;
	}

	// Find out where a colour is in the palette, if already there;
	// otherwise add it and assign an index.
	public int getIndex(short colour) {
		colour &= 0x7FFF;

		for (int i = 0; i < used; ++i) {
			if (entries[i] == colour) { return i; }
		}
		// Otherwise, we have to allocate a new colour - if possible.
		if (used == 16) {
			throw new RuntimeException("Image has more than 16 colours!");
		}
		int index = used;
		used += 1;
		entries[index] = colour;
		return index;
	}

	public short background() { return entries[0]; }

	public int getARGB(int index) {
		short colour = entries[index];
		return (
			(index == 0 ? 0 : 0xFF000000) |
			((colour & 0x7C00) >> 7) | ((colour & 0x7000) >> 12) |
			((colour & 0x03E0) << 6) | ((colour & 0x0380) << 1) |
			((colour & 0x001F) << 19) | ((colour & 0x001C) << 14)
		);
	}

	public byte[] getBytes() {
		return Util.shortsToBytes(entries);
	}

	public int getUsed() { return used; }

	public short[] getShorts() {
		// Doing it this way makes a copy
		return Util.shortsToShorts(entries);
	}

	// Used only by PaletteFrame!
	// Replace a colour.
	public void setIndex(int index, short colour) {
		if (index >= used) {
			throw new RuntimeException("setIndex called on a bad index!");
		}
		entries[index] = (short)(colour & 0x7FFF);
	}

	protected void setUsed(int usedCount) {
		if (usedCount >= 0 || usedCount <= 16)
			used = usedCount;
	}

	public void setShorts(short[] input) {
		String methodName = Util.methodName() + ": ";
		if (input == null || input.length != 16)
			throw new IllegalArgumentException(
				methodName
				+ "input is incorrect length"
			);
		entries = input;
	}

	public Palette copy() {
		return new Palette(entries);
	}

	public Palette recolored(short[] to_replace, short[] replace_with) {
		Palette result = copy();

		int replacements = to_replace.length;
		if (replacements != replace_with.length) {
			throw new RuntimeException(
				"Palettes aren't the same length!"
			);
		}

		for (int i = 0; i < used; ++i) {
			for (int j = 0; j < replacements; ++j) {
				if (result.entries[i] == to_replace[j]) {
					result.entries[i] = replace_with[j];
				}
			}
		}
		return result;
	}

	// Returns an array of indices representing the colors in this palette
	// most similar to the corresponding colors in the given palette
	public byte[] closestColors(Palette toCompare) {
		byte[] output = new byte[16];
		// BG colors are negligible and must be ignored
		for (int i = 1; i < 16; i++) {
			if (i >= used) {
				output[i] = (byte)i;
				continue;
			}
			int minDiff = Integer.MAX_VALUE;
			int minDiffIndex = -1;
			int currAsARGB = getARGB(i);
			for (int j = 1; j < 16; j++) {
				int compareAsARGB = toCompare.getARGB(j);
				int currDiff = ColorReducer.colorDifference(
					currAsARGB, compareAsARGB
				);
				if (currDiff < minDiff) {
					minDiff = currDiff;
					minDiffIndex = j;
				}
			}
			output[i] = (byte)minDiffIndex;
		}
		return output;
	}

	public BigDecimal paletteDifference(Palette toCompare) {
		BigDecimal output = new BigDecimal(0);
		byte[] replaceIndices = closestColors(toCompare);
		// BG colors are negligible and must be ignored
		for (int i = 1; i < used; i++) {
			int currOriginalColor = getARGB(i);
			int currNewColor = toCompare.getARGB(replaceIndices[i]);
			output = output.add(new BigDecimal(ColorReducer.colorDifference(
				currOriginalColor, currNewColor
			)));
		}
		return output;
	}

	// There used to be sorting code, but it doesn't accomplish what it was
	// planned for, and causes problems if palettes are shared.
	// The code has been archived and improved in GBA_Image, however.

	@Override
	public String toString() {
		String result = "";
		for (int i = 0; i < used; ++i) {
			if (i != 0) { result += " "; }
			result += String.format("%08X", getARGB(i));
		}
		return result;
	}

	// We overload Object.equals(Object) and Object.hashCode() in order to make
	// this behave like a value type.
	@Override
	public boolean equals(Object other) {
		try {
			Palette rhs = (Palette)other;
			return Arrays.equals(entries, rhs.entries) && used == rhs.used;
		} catch (ClassCastException e) { return false; }
	}

	// This is slow. It's only provided as a matter of style; FEditor doesn't
	// currently use it.
	@Override
	public int hashCode() {
		int result = used;
		for (int i = 0; i < used; ++i) { result ^= entries[i]; }
		return result;
	}
}

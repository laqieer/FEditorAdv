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
 *  <Description> Wraps the pointer array for battle palettes that are stored
 *  separately from those in the class animation array and intended for
 *  characters who override the palettes for their classes
 */

package Model;

import Compression.LempelZiv;
import Graphics.Palette;

// XXX This model isn't even used!
public class CharacterPaletteArray extends PointerArray {
	public CharacterPaletteArray(
		Game owner,
		ROM.Pointer handle, ROM.Pointer default_base,
		int default_size, int ints_per_record
	) {
		super(owner, handle, default_base, default_size, ints_per_record);
	}

	// Done
	@Override
	protected void moveToEndOfData(ROM.Pointer p, int index) {
		if (index == 3)
			LempelZiv.decompress(p);
	}

	// Done
	@Override
	protected byte[] prepare(byte[] newData, int index) {
		if (index == 3)
			return LempelZiv.compress(newData);
		else
			return newData;
	}

	// Done
	@Override
	protected boolean markNewPointer() { return false; }

	// Done
	@Override
	protected boolean isPointer(int index) { return index == 3; }

	// Tested and working
	public String getName() {
		int[] name = new int[3];
		for (int i = 0; i < 3; i++)
			name[i] = readValue(i);
		byte[] nameBytes = Util.intsToBytes(name);
		try {
			return new String(nameBytes).replace("\000", "");
		} catch (Exception e) { return null; }
	}

	// Needs testing
	public Palette[] getEntry() {
		byte[] quinPalette = LempelZiv.decompress(getPointer(3));
		Palette[] output = new Palette[5];
		for (int i = 0; i < 5; i++)
			output[i] = new Palette(quinPalette, i * 32, (i + 1) * 32);
		return output;
	}

	// Tested and working
	public void setName(String input) {
		if (input == null)
			return;
		if (input.length() >= 12)
			input = input.substring(0, 11);
		byte[] name = new byte[12];
		for (int i = 0; i < input.length(); i++)
			name[i] = (byte)input.charAt(i);
		int[] nameInts = Util.bytesToInts(name);
		for (int i = 0; i < 3; i++)
			writeValue(i, nameInts[i]);
	}

	// Needs testing
	public void setEntry(Palette[] input) {
		if (input == null || input.length < 5)
			throw new IllegalArgumentException(
				"Attempt to set character palette of insufficient size"
			);

		byte[][] palettes = new byte[5][32];
		for (int i = 0; i < 5; i++)
			palettes[i] = input[i].getBytes();

		setData(3, LempelZiv.compress(Util.bytesToBytes(palettes)));
	}
}

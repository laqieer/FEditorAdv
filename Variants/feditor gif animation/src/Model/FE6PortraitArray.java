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
 *  <Description> For representing Fire Emblem 6's portrait's unique attributes.
 */

package Model;

import Graphics.GBAImage;

public class FE6PortraitArray extends PortraitArray {
	private ROM.Pointer[] garbageChibis;

	protected boolean checkIfCard() {
		return getPointer(CHIBI) == null;
	}

	public boolean eyesSupported() { return false; }

	// XXX Hextator's doc suggests that there should be one more byte
	// read from this word which is an "unknown boolean".
	// Hextator: See if that boolean has anything to do with invalid
	// chibi refs

	public void setMouthAndEyeData(int[] data) {
		int composed = data[0] | (data[1] << 8);
		writeValue(3, composed); // 3 is the index for mouth data
		System.out.println("Writing done by " + Util.methodName());
	}

	public int[] getMouthAndEyeData() {
		int composed = readValue(3);
		return new int[] {  composed & 0xff, (composed & 0xff00) >> 8 };
	}

	public FE6PortraitArray(Game owner) {
		super(
			new byte[] {
				  0,  8, 17,  1, // mug - weird in FE6
				  1,  4,  4,  0, // chibi
				 -1,  0,  0,  0, // mouth - absent in FE6 (mouths are on mug sheet)
				  0, 10,  9,  1, // card - reuses mug slot in FE6
				  2,  1,  1,  0  // palette
			},
			owner, owner.getPointer(0x08007FD8), owner.getPointer(0x0866074C),
			0xE7, 4
		);

		garbageChibis = new ROM.Pointer[] {
			owner.getPointer(0x0863FED0), // soldier
			owner.getPointer(0x0864505C), // villagers
			owner.getPointer(0x0864579C),
			owner.getPointer(0x08645E20),
			owner.getPointer(0x08646498),
			owner.getPointer(0x08646B84),
			owner.getPointer(0x086472B4),
			owner.getPointer(0x08647A28),
			owner.getPointer(0x086480FC),
			owner.getPointer(0x086487AC),
			owner.getPointer(0x08648E58),
			owner.getPointer(0x08649494),
			owner.getPointer(0x08649AC8)
		};
	}

	// Take in data from the ROM (not really images; there's no
	// width/height data - but we fake it) and create a single image
	// in the format that the editor expects for portrait sheets.
	protected GBAImage synthesize(
		GBAImage mugsheet, GBAImage chibisheet, GBAImage mouthsheet
	) {
		if (mouthsheet != null) { throw new RuntimeException("How did we load a mouth sheet from FE6 sprite data when FE6 doesn't have any mouth sheets?"); }

		// Use the "painting" constructor with the mug sheet as a source.
		GBAImage result = new GBAImage(
			mugsheet, 16, 14, new byte[] {
				// P.S. Java, I really hate you for making "possible loss of precision" an error.
				// To keep everything lined up I have to byte-cast EVERYTHING now.
				// TODO: put this data into resources or something.
				(byte)  -1, (byte)  -1, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05,
				(byte)0x06, (byte)0x07, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)  -1, (byte)  -1, (byte)0x20, (byte)0x21, (byte)0x22, (byte)0x23, (byte)0x24, (byte)0x25,
				(byte)0x26, (byte)0x27, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)  -1, (byte)  -1, (byte)0x40, (byte)0x41, (byte)0x42, (byte)0x43, (byte)0x44, (byte)0x45,
				(byte)0x46, (byte)0x47, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)  -1, (byte)  -1, (byte)0x60, (byte)0x61, (byte)0x62, (byte)0x63, (byte)0x64, (byte)0x65,
				(byte)0x66, (byte)0x67, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)  -1, (byte)  -1, (byte)0x08, (byte)0x09, (byte)0x0A, (byte)0x0B, (byte)0x0C, (byte)0x0D,
				(byte)0x0E, (byte)0x0F, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)  -1, (byte)  -1, (byte)0x28, (byte)0x29, (byte)0x2A, (byte)0x2B, (byte)0x2C, (byte)0x2D,
				(byte)0x2E, (byte)0x2F, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)0x14, (byte)0x15, (byte)0x48, (byte)0x49, (byte)0x4A, (byte)0x4B, (byte)0x4C, (byte)0x4D,
				(byte)0x4E, (byte)0x4F, (byte)0x16, (byte)0x17, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)0x34, (byte)0x35, (byte)0x68, (byte)0x69, (byte)0x6A, (byte)0x6B, (byte)0x6C, (byte)0x6D,
				(byte)0x6E, (byte)0x6F, (byte)0x36, (byte)0x37, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)0x54, (byte)0x55, (byte)0x10, (byte)0x11, (byte)0x12, (byte)0x13, (byte)0x50, (byte)0x51,
				(byte)0x52, (byte)0x53, (byte)0x56, (byte)0x57, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)0x74, (byte)0x75, (byte)0x30, (byte)0x31, (byte)0x32, (byte)0x33, (byte)0x70, (byte)0x71,
				(byte)0x72, (byte)0x73, (byte)0x76, (byte)0x77, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)0x18, (byte)0x19, (byte)0x1A, (byte)0x1B, (byte)0x1C, (byte)0x1D, (byte)0x1E, (byte)0x1F,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)0x80, (byte)0x81, (byte)0x82, (byte)0x83,
				(byte)0x38, (byte)0x39, (byte)0x3A, (byte)0x3B, (byte)0x3C, (byte)0x3D, (byte)0x3E, (byte)0x3F,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)0x84, (byte)0x85, (byte)0x86, (byte)0x87,
				(byte)0x58, (byte)0x59, (byte)0x5A, (byte)0x5B, (byte)0x5C, (byte)0x5D, (byte)0x5E, (byte)0x5F,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)0x78, (byte)0x79, (byte)0x7A, (byte)0x7B, (byte)0x7C, (byte)0x7D, (byte)0x7E, (byte)0x7F,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1
			}
		);

		// Place chibi in the top-right.
		if (chibisheet != null) {
			result.blit(chibisheet, 0, 0, 4, 4, 12, 2, false);
		}

		return result;
	}

	// Break up the synthesized image into mug, chibi and mouth sheets.
	// (Actually, FE6 doesn't return a mouth sheet.)
	// FIXME: There should be some kind of "tileset" class separate from the image.
	protected GBAImage[] decompose(GBAImage synthesized) {
		return new GBAImage[] {
			new GBAImage(
				synthesized, 8, 17, new byte[] {
					// Conceptually this image is 32 tiles wide (it makes more sense that way in an editor),
					// but it has an extra 8 tiles at the end for an extra mouth.
					// But we can determine the right data by just taking the inverse of the map above.
					(byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09,
					(byte)0x42, (byte)0x43, (byte)0x44, (byte)0x45, (byte)0x46, (byte)0x47, (byte)0x48, (byte)0x49,
					(byte)0x82, (byte)0x83, (byte)0x84, (byte)0x85, (byte)0x60, (byte)0x61, (byte)0x6A, (byte)0x6B,
					(byte)0xA0, (byte)0xA1, (byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5, (byte)0xA6, (byte)0xA7,
					(byte)0x12, (byte)0x13, (byte)0x14, (byte)0x15, (byte)0x16, (byte)0x17, (byte)0x18, (byte)0x19,
					(byte)0x52, (byte)0x53, (byte)0x54, (byte)0x55, (byte)0x56, (byte)0x57, (byte)0x58, (byte)0x59,
					(byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x70, (byte)0x71, (byte)0x7A, (byte)0x7B,
					(byte)0xB0, (byte)0xB1, (byte)0xB2, (byte)0xB3, (byte)0xB4, (byte)0xB5, (byte)0xB6, (byte)0xB7,
					(byte)0x22, (byte)0x23, (byte)0x24, (byte)0x25, (byte)0x26, (byte)0x27, (byte)0x28, (byte)0x29,
					(byte)0x62, (byte)0x63, (byte)0x64, (byte)0x65, (byte)0x66, (byte)0x67, (byte)0x68, (byte)0x69,
					(byte)0x86, (byte)0x87, (byte)0x88, (byte)0x89, (byte)0x80, (byte)0x81, (byte)0x8A, (byte)0x8B,
					(byte)0xC0, (byte)0xC1, (byte)0xC2, (byte)0xC3, (byte)0xC4, (byte)0xC5, (byte)0xC6, (byte)0xC7,
					(byte)0x32, (byte)0x33, (byte)0x34, (byte)0x35, (byte)0x36, (byte)0x37, (byte)0x38, (byte)0x39,
					(byte)0x72, (byte)0x73, (byte)0x74, (byte)0x75, (byte)0x76, (byte)0x77, (byte)0x78, (byte)0x79,
					(byte)0x96, (byte)0x97, (byte)0x98, (byte)0x99, (byte)0x90, (byte)0x91, (byte)0x9A, (byte)0x9B,
					(byte)0xD0, (byte)0xD1, (byte)0xD2, (byte)0xD3, (byte)0xD4, (byte)0xD5, (byte)0xD6, (byte)0xD7,
					(byte)0xAC, (byte)0xAD, (byte)0xAE, (byte)0xAF, (byte)0xBC, (byte)0xBD, (byte)0xBE, (byte)0xBF
				}
			),
			new GBAImage(
				synthesized, 4, 4, new byte[] {
					// Yay, chibis are easy
					(byte)0x2C, (byte)0x2D, (byte)0x2E, (byte)0x2F,
					(byte)0x3C, (byte)0x3D, (byte)0x3E, (byte)0x3F,
					(byte)0x4C, (byte)0x4D, (byte)0x4E, (byte)0x4F,
					(byte)0x5C, (byte)0x5D, (byte)0x5E, (byte)0x5F
				}
			)
		};
	}

	// The same, but using the old format for sheets.
	protected GBAImage[] decompose_legacy(GBAImage synthesized) {
		// Create a mapping to "paint" the mug sheet from tiles on the
		// legacy spritesheet - they are in order, but there is an extra
		// column on the right which must be ignored.
		byte[] sequence = new byte[8 * 17];
		for (int i = 0, j = 0; i < 8 * 17; ++i, ++j) {
			// Skip the last column
			if ((j % 33) == 32) { ++j; }
			sequence[i] = (byte)j;
		}
		return new GBAImage[] {
			new GBAImage(
				synthesized, 8, 17, sequence
			),
			new GBAImage(
				synthesized, 4, 4, new byte[] {
					(byte)(4 * 33 + 24), (byte)(4 * 33 + 25), (byte)(4 * 33 + 26), (byte)(4 * 33 + 27),
					(byte)(5 * 33 + 24), (byte)(5 * 33 + 25), (byte)(5 * 33 + 26), (byte)(5 * 33 + 27),
					(byte)(4 * 33 + 28), (byte)(4 * 33 + 29), (byte)(4 * 33 + 30), (byte)(4 * 33 + 31),
					(byte)(5 * 33 + 28), (byte)(5 * 33 + 29), (byte)(5 * 33 + 30), (byte)(5 * 33 + 31)
				}
			)
		};
	}

	@Override protected boolean isGarbageChibi(ROM.Pointer p) {
		if (p.equals(getPointer(getImageData(PALETTE, INDEX)))) {
			return true;
		}
		for (ROM.Pointer garbage: garbageChibis) {
			if (p.equals(garbage)) { return true; }
		}
		return false;
	}
}

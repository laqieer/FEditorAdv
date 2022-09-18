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
 *  <Description> For representing Fire Emblem 8's portraits' unique attributes.
 */

package Model;

import Graphics.GBAImage;

public class FE8PortraitArray extends PortraitArray {
	protected boolean checkIfCard() {
		return getPointer(MUG) == null;
	}

	public boolean eyesSupported() { return true; }

	public void setMouthAndEyeData(int[] data) {
		int composed = data[0] | (data[1] << 8) | (data[2] << 16) | (data[3] << 24);
		writeValue(5, composed); // 5 is the index for mouth + eye position data
		writeValue(6, data[4]); // 6 is the index for eye open/close data
		System.out.println("Writing done by " + Util.methodName());
	}

	public int[] getMouthAndEyeData() {
		int composed = readValue(5);
		return new int[] {
			composed & 0xff,
			(composed & 0xff00) >> 8,
			(composed & 0xff0000) >> 16,
			(composed & 0xff000000) >> 24,
			readValue(6)
		};
	}

	public FE8PortraitArray(Game owner) {
		super(
			new byte[] {
				  0, 32,  4,  0, // mug
				  1,  4,  4,  1, // chibi
				  3,  4, 12,  0, // mouth
				  4, 10,  9,  1, // card
				  2,  1,  1,  0  // palette
			},
			owner, owner.getPointer(0x0800542C), owner.getPointer(0x0890111C),
			0xAD, 7
		);
	}

	// Overrides to handle the 4-byte header on mug sheets.
	@Override
	protected void skipMugHeader(ROM.Pointer p, int index) {
		if (indexToDataType(index) == MUG) {
			/**
			 * I guess we didn't like this?
			byte[] expected = new byte[] { 0x00, 0x04, 0x10, 0x00 };
			for (int i = 0; i < 4; ++i)
			if p.nextByte() != expected[i]
			throw new IllegalStateException("Corrupt mug sheet header");
			**/
			p.advance(4);
		}
	}

	@Override
	protected byte[] prepare(byte[] data, int index) {
		byte[] result = super.prepare(data, index);
		if (indexToDataType(index) == MUG) {
			// Attach the "mug header" (purpose unknown) to mug sheets.
			byte[] with_header = new byte[result.length + 4];
			with_header[0] = 0x00;
			with_header[1] = 0x04;
			with_header[2] = 0x10;
			with_header[3] = 0x00;
			System.arraycopy(result, 0, with_header, 4, result.length);
			result = with_header;
		}
		return result;
	}

	// FIXME: currently synthesize and decompose are exactly the same as for FE7.
	// Also, there is duplication of data between the two methods. Actually,
	// there is a lot of common FE7-FE8 stuff; should make another level of
	// inheritance.

	// Take in data from the ROM (not really images; there's no
	// width/height data - but we fake it) and create a single image
	// in the format that the editor expects for portrait sheets.
	protected GBAImage synthesize(
		GBAImage mugsheet, GBAImage chibisheet, GBAImage mouthsheet
	) {
		// Use the "painting" constructor with the mug sheet as a source.
		// Differences from FE6: There are eyes and the "special" mouth
		// where the open and half-open mouths used to be (and 8 garbage
		// tiles). There are closed mouths in addition to open and half-open
		// ones, and they are all on a separate sheet. Finally, the mug
		// sheet is 8 tiles shorter.
		GBAImage result = new GBAImage(
			mugsheet, 16, 14,
			new byte[] {
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
				(byte)0x4E, (byte)0x4F, (byte)0x16, (byte)0x17, (byte)0x18, (byte)0x19, (byte)0x1A, (byte)0x1B,
				(byte)0x34, (byte)0x35, (byte)0x68, (byte)0x69, (byte)0x6A, (byte)0x6B, (byte)0x6C, (byte)0x6D,
				(byte)0x6E, (byte)0x6F, (byte)0x36, (byte)0x37, (byte)0x38, (byte)0x39, (byte)0x3A, (byte)0x3B,
				(byte)0x54, (byte)0x55, (byte)0x10, (byte)0x11, (byte)0x12, (byte)0x13, (byte)0x50, (byte)0x51,
				(byte)0x52, (byte)0x53, (byte)0x56, (byte)0x57, (byte)0x58, (byte)0x59, (byte)0x5A, (byte)0x5B,
				(byte)0x74, (byte)0x75, (byte)0x30, (byte)0x31, (byte)0x32, (byte)0x33, (byte)0x70, (byte)0x71,
				(byte)0x72, (byte)0x73, (byte)0x76, (byte)0x77, (byte)0x78, (byte)0x79, (byte)0x7A, (byte)0x7B,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)0x1C, (byte)0x1D, (byte)0x1E, (byte)0x1F,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)0x3C, (byte)0x3D, (byte)0x3E, (byte)0x3F,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
				(byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1
			}
		);

		// Place chibi in the top-right.
		if (chibisheet != null) {
			result.blit(chibisheet, 0, 0, 4, 4, 12, 2, false);
		}

		// Place mouths along the bottom, transforming from a 1x6 layout to a 3x2 layout.
		for (int mood = 0; mood < 2; ++mood) {
			for (int openness = 0; openness < 3; ++openness) {
				result.blit(mouthsheet, 0, 6 * mood + 2 * openness, 4, 2, 4 * openness, 10 + 2 * mood, false);
			}
		}

		return result;
	}

	// Break up the synthesized image into mug, chibi and mouth sheets.
	// (Actually, FE6 doesn't return a mouth sheet.)
	// FIXME: There should be some kind of "tileset" class separate from the image.
	protected GBAImage[] decompose(GBAImage synthesized) {
		return new GBAImage[] {
			new GBAImage(
				// The ROM data "looks right" if interpreted as an image 32 tiles wide, but there is no
				// height/width information in the ROM. The data is 8 per line here so it's not obscenely
				// wide in a text editor (although it's still too wide for 80 chars).
				synthesized, 32, 4, new byte[] {
					(byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09,
					(byte)0x42, (byte)0x43, (byte)0x44, (byte)0x45, (byte)0x46, (byte)0x47, (byte)0x48, (byte)0x49,
					(byte)0x82, (byte)0x83, (byte)0x84, (byte)0x85, (byte)0x60, (byte)0x61, (byte)0x6A, (byte)0x6B,
					(byte)0x6C, (byte)0x6D, (byte)0x6E, (byte)0x6F, (byte)0xAC, (byte)0xAD, (byte)0xAE, (byte)0xAF,
					(byte)0x12, (byte)0x13, (byte)0x14, (byte)0x15, (byte)0x16, (byte)0x17, (byte)0x18, (byte)0x19,
					(byte)0x52, (byte)0x53, (byte)0x54, (byte)0x55, (byte)0x56, (byte)0x57, (byte)0x58, (byte)0x59,
					(byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x70, (byte)0x71, (byte)0x7A, (byte)0x7B,
					(byte)0x7C, (byte)0x7D, (byte)0x7E, (byte)0x7F, (byte)0xBC, (byte)0xBD, (byte)0xBE, (byte)0xBF,
					(byte)0x22, (byte)0x23, (byte)0x24, (byte)0x25, (byte)0x26, (byte)0x27, (byte)0x28, (byte)0x29,
					(byte)0x62, (byte)0x63, (byte)0x64, (byte)0x65, (byte)0x66, (byte)0x67, (byte)0x68, (byte)0x69,
					(byte)0x86, (byte)0x87, (byte)0x88, (byte)0x89, (byte)0x80, (byte)0x81, (byte)0x8A, (byte)0x8B,
					(byte)0x8C, (byte)0x8D, (byte)0x8E, (byte)0x8F, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1,
					(byte)0x32, (byte)0x33, (byte)0x34, (byte)0x35, (byte)0x36, (byte)0x37, (byte)0x38, (byte)0x39,
					(byte)0x72, (byte)0x73, (byte)0x74, (byte)0x75, (byte)0x76, (byte)0x77, (byte)0x78, (byte)0x79,
					(byte)0x96, (byte)0x97, (byte)0x98, (byte)0x99, (byte)0x90, (byte)0x91, (byte)0x9A, (byte)0x9B,
					(byte)0x9C, (byte)0x9D, (byte)0x9E, (byte)0x9F, (byte)  -1, (byte)  -1, (byte)  -1, (byte)  -1
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
			),
			new GBAImage(
				synthesized, 4, 12, new byte[] {
					// Mouths are pretty easy too. There's a bit of interlacing, though.
					(byte)0xA0, (byte)0xA1, (byte)0xA2, (byte)0xA3,
					(byte)0xB0, (byte)0xB1, (byte)0xB2, (byte)0xB3,
					(byte)0xA4, (byte)0xA5, (byte)0xA6, (byte)0xA7,
					(byte)0xB4, (byte)0xB5, (byte)0xB6, (byte)0xB7,
					(byte)0xA8, (byte)0xA9, (byte)0xAA, (byte)0xAB,
					(byte)0xB8, (byte)0xB9, (byte)0xBA, (byte)0xBB,
					(byte)0xC0, (byte)0xC1, (byte)0xC2, (byte)0xC3,
					(byte)0xD0, (byte)0xD1, (byte)0xD2, (byte)0xD3,
					(byte)0xC4, (byte)0xC5, (byte)0xC6, (byte)0xC7,
					(byte)0xD4, (byte)0xD5, (byte)0xD6, (byte)0xD7,
					(byte)0xC8, (byte)0xC9, (byte)0xCA, (byte)0xCB,
					(byte)0xD8, (byte)0xD9, (byte)0xDA, (byte)0xDB,
				}
			)
		};
	}

	// The same, but using the old format for sheets.
	protected GBAImage[] decompose_legacy(GBAImage synthesized) {
		// Create a mapping to "paint" the mug sheet from tiles on the
		// legacy spritesheet - they are in order, but there is an extra
		// column on the right which must be ignored.
		byte[] sequence = new byte[8 * 16];
		for (int i = 0, j = 0; i < 8 * 16; ++i, ++j) {
			// Skip the last column
			if ((j % 33) == 32) { ++j; }
			sequence[i] = (byte)j;
		}
		// Blank out 2x4 tiles in the bottom right of the mug sheet.
		for (int i = 28; i < 32; ++i) {
			for (int j = 2; j < 3; ++j) {
				sequence[j * 32 + i] = (byte)-1;
			}
		}

		// Another mapping to "paint" the mouth sheet.
		byte[] mouth_sequence = new byte[8 * 6];
		for (int i = 0, j = 4 * 33; i < 8 * 6; ++i, ++j) {
			// Skip the last 8 columns
			if ((j % 33) == 25) { j += 8; }
			// Skip the first column
			if ((j % 33) == 0) { ++j; }
			mouth_sequence[i] = (byte)j;
		}

		return new GBAImage[] {
			new GBAImage(
				synthesized, 8, 16, sequence
			),
			new GBAImage(
				synthesized, 4, 4, new byte[] {
					(byte)(2 * 33 + 28), (byte)(2 * 33 + 29), (byte)(2 * 33 + 30), (byte)(2 * 33 + 31),
					(byte)(3 * 33 + 28), (byte)(3 * 33 + 29), (byte)(3 * 33 + 30), (byte)(3 * 33 + 31),
					(byte)(4 * 33 + 28), (byte)(4 * 33 + 29), (byte)(4 * 33 + 30), (byte)(4 * 33 + 31),
					(byte)(5 * 33 + 28), (byte)(5 * 33 + 29), (byte)(5 * 33 + 30), (byte)(5 * 33 + 31)
				}
			),
			new GBAImage(
				synthesized, 4, 12, mouth_sequence
			)
		};
	}
}

/*
 *  FE Editor - GBA Fire Emblem (U) ROM editor
 *
 *  Copyright (C) 2008-2011 Hextator,
 *  hextator (AIM) hextator@gmail.com (MSN)
 *
 *  Major thanks to Zahlman (AIM/MSN: zahlman@gmail.com) for optimization,
 *  organization and modularity improvements.
 *
 *  Contributions by others in this file
 *  - Nintenlord made some suggestions to help start off optimization
 *    coding
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
 *  <Description> This class is expected to provide an interface for editing
 *  graphical data relating to class specific battle animations
 */

package Model;

import java.awt.image.BufferedImage;
import Compression.LempelZiv;
import Graphics.GBAImage;
//import Graphics.Palette;

public class ClassAnimationArray extends PointerArray {
	private PortableClassAnimation animation;
	//private int cachePosition;

	// Fine
	public ClassAnimationArray(
		Game owner,
		ROM.Pointer handle, ROM.Pointer default_base,
		int default_size, int ints_per_record
	) {
		super(owner, handle, default_base, default_size, ints_per_record);
	}

	// Fine
	protected boolean markNewPointer() { return false; }

	// Fine
	protected byte[] prepare(byte[] data, int index) {
		if (index == 3) { return data; } // section data is uncompressed
		if (index > 3 && index < 8) {
			// frame data, OAM data or palettes
			return LempelZiv.compress(data);
		}
		throw new IllegalArgumentException();
	}

	// The structure is:
	// 00 char[12] name of animation
	// 0C Section data
	// 10 frame data
	// 14 OAM (right to left)
	// 18 OAM (left to right)
	// 1C palettes

	// Index 0 is invalid.

	// Hextator: Modified to have full bounds checking. Maybe it didn't
	// matter, but it won't hurt.
	@Override
	protected boolean isPointer(int index) { return (index >= 3) && (index < 8); }

	// Done
	@Override
	protected boolean isDoublePointer(int index) { return index == 4; }

	// Section data is a constant number of ints (MAX_MODE_COUNT ints)
	// Frame data is LZ77 compressed (not deleted if the flag is set... ?)
	// and may contain pointers to Graphics that must be freed.
	// (If any word has a MSB of 0x86, the next word is a pointer.)
	// OAM data is compressed (both)
	// Palettes may or may not be compressed; expected size is RAW_PAL_SIZE
	// Graphics may or may not be compressed; expected size is RAW_GFX_SIZE

	protected void moveToEndOfData(ROM.Pointer p, int index) {
		switch (index) {
			case 3:
			p.offsetBy(AnimationBuilder.MAX_MODE_COUNT * 4);
			break;

			case 4:
			// XXX: We delete graphics data via pointers hidden in the
			// compressed frame data here.
			int[] data = Util.bytesToInts(LempelZiv.decompress(p));
			// Iterate over the MSBs of ints, except for the last
			for (int i = 0; i < data.length; i++) {
				if (((data[i] >> 24) & 0xFF) == 0x86) {
					ROM.Pointer graphic = owner.getPointer(data[i + 1]);
					ROM.Pointer endOfGraphic = graphic.offsetBy(0);
					LempelZiv.decompress(endOfGraphic);
					owner.markFreeSpace(graphic, endOfGraphic);
				}
			}
			break;

			case 5:
			case 6:
			LempelZiv.decompress(p);
			break;

			case 7:
			LempelZiv.decompress(p, AnimationBuilder.RAW_PAL_SIZE);
			break;

			default:
			throw new IllegalArgumentException();
		}
	}

	// Insert PortableClassAnimation into the ROM.
	// The spritesheet data was already written by the
	// PortableClassAnimation while it was resolving
	// pointers in the frameData.
	public void setEntry(
		int[] name, byte[] sectionData, byte[] frameData,
		byte[] rightToLeftOAM, byte[] leftToRightOAM, byte[] palettes
	) {
		writeValue(0, name[0]);
		writeValue(1, name[1]);
		writeValue(2, name[2]);
		setData(3, sectionData);
		setData(4, frameData);
		setData(5, rightToLeftOAM);
		setData(6, leftToRightOAM);
		setData(7, palettes);
		System.out.println("Writing done by " + Util.methodName());
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
		System.out.println("Writing done by " + Util.methodName());
	}

        // BwdYeti: wahh
        // Split this into two methods, one for actual work and one for rendering
	public PortableClassAnimation getEntry() throws Exception {
		return getEntry(8);
	}
	public PortableClassAnimation getEntryForRendering() throws Exception {
		return getEntry(1);
	}
	private PortableClassAnimation getEntry(int tileScale) throws Exception {
		// Hextator: This was causing GUI refreshes to fail when
		// the ArrayPanel spinner wasn't changed
		//if (animation == null || getPosition() != cachePosition) {
			animation = new PortableClassAnimation(
				owner,
				new int[] { readValue(0), readValue(1), readValue(2) },
                                tileScale,
				getPointer(3),
				getPointer(4),
				getPointer(5),
				getPointer(6),
				getPointer(7)
			);
			//cachePosition = getPosition();
		//}
		return animation;
	}

	// Tested and working
	public String getName() {
		try { return getEntry().getName(); }
		catch (Exception e) { return null; }
	}

	// Tested and working
	public BufferedImage getFrame(int id) {
		try {
                    // BwdYeti: uses the rendering system instead of the logical one
			return getEntryForRendering().getFrame(id);
		} catch (Exception e) { return null; }
	}

	// Tested and working
	public GBAImage[] getSheets() {
		try {
                    // BwdYeti: uses the rendering system instead of the logical one
			return getEntryForRendering().getSheets();
		} catch (Exception e) 
		{
			e.printStackTrace(); 
			return null; 
		}
	}


	// This function search the data count
	@Override
	public int searchDataCount(int startPointer)
	{
		for(int i = 0 ; i< 0xFFFF;i++)
		{
			int pos =startPointer+(i*32)+ROM.GBA_HARDWARE_OFFSET;
			ROM.Pointer p = owner.getPointer(pos+8+4); //using pointer1
			int a = p.nextInt();
			if(!p.isPointer(a))
			{
				return i;
			}
		}
		return maxSize();
	}
}

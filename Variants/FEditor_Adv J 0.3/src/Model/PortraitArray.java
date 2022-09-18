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
 *  <Description> This class is expected to provide an interface for editing
 *  graphical data relating to character portraits and mini portraits
 */

package Model;

import Compression.LempelZiv;
import Graphics.GBAImage;
import Graphics.Palette;

public abstract class PortraitArray extends PointerArray {
	protected byte[] imageData;
	// Each row contains: pointer array index, width, height, isCompressed info
	// for each image type.
	protected static final int MUG = 0;
	protected static final int CHIBI = 1;
	protected static final int MOUTH = 2;
	protected static final int CARD = 3;
	protected static final int PALETTE = 4;

	// It just works out that the palette uses the same
	// amount of data as a single tile of an image.
	// It doesn't actually represent image data, so the
	// width and height are actually fakes to make getImageSize() work.
	protected static final int INDEX = 0;
	protected static final int WIDTH = 1; // 1 for palette
	protected static final int HEIGHT = 2; // 1 for palette
	protected static final int COMPRESSED = 3;
	protected static final int ROWSIZE = 4;

	protected PortraitArray(
		byte[] imageData,
		Game owner, ROM.Pointer handle, ROM.Pointer default_base,
		int default_size, int ints_per_record
	) {
		// The subclass constructor tells us how many indices are in the array
		// referred to by the handle and default_base, but the zeroth index of
		// this array is invalid (in all three games). So we decrement that count,
		// and set up an offset the side of one entry.
		super(
			owner, handle, default_base,
			default_size - 1, ints_per_record,
			ints_per_record * 4
		);

		this.imageData = imageData;
	}

	@Override
	public void moveTo(int newPosition) {
		super.moveTo(newPosition);
		isCard = checkIfCard();
	}

	@Override
	public void next() {
		super.next();
		isCard = checkIfCard();
	}

	protected int getImageData(int imageType, int attribute) {
		return imageData[imageType * ROWSIZE + attribute];
	}

	private int getImageSize(int type) {
		// 64 pixels per tile, 4 bits = 1/2 byte per pixel
		return getImageData(type, WIDTH) * getImageData(type, HEIGHT) * 32;
	}

	protected int indexToDataType(int index) {
		if (index == getImageData(PALETTE, INDEX)) { return PALETTE; }
		if (isCard) {
			if (index == getImageData(CARD, INDEX)) { return CARD; }
		} else {
			if (index == getImageData(MUG, INDEX)) { return MUG; }
			if (index == getImageData(CHIBI, INDEX)) { return CHIBI; }
			if (index == getImageData(MOUTH, INDEX)) { return MOUTH; }
		}
		throw new RuntimeException(String.valueOf(index));
	}

	@Override
	protected boolean isPointer(int index) {
		try {
			indexToDataType(index);
			return true;
		} catch (RuntimeException e) { return false; }
	}

	// Hook to offset the mug sheet pointer on FE8 to skip the header.
	protected void skipMugHeader(ROM.Pointer p, int index) { }

	// Used for deallocation.
	protected void moveToEndOfData(ROM.Pointer p, int index) {
		// Don't deallocate garbage chibis, because the pointer either points to
		// a palette that we may want to keep for something else, or to something
		// external that's probably important.
		if (index == getImageData(CHIBI, INDEX) && isGarbageChibi(p)) { return; }

		// Hax for FE 8.
		skipMugHeader(p, index);

		if (getImageData(indexToDataType(index), COMPRESSED) != 0) {
			LempelZiv.decompress(p);
		} else {
			// Otherwise, data is raw; use the known constant size
			p.advance(getImageSize(indexToDataType(index)));
		}
	}

	protected boolean isGarbageChibi(ROM.Pointer p) { return false; }

	private GBAImage readImageData(Palette palette, int type) {
		int index = getImageData(type, INDEX);
		if (index == -1) { return null; } // image type not supported in this ROM
		ROM.Pointer p = getPointer(index);
		if (p == null) { return null; } // null pointer -> null image

		if (type == CHIBI && isGarbageChibi(p)) { return null; }

		skipMugHeader(p, index);

		byte[] raw = (getImageData(type, COMPRESSED) != 0)
		             ? LempelZiv.decompress(p)
		             : p.getBytes(getImageSize(type)); // 4 bits per pixel

		return new GBAImage(
			raw, palette, getImageData(type, WIDTH), getImageData(type, HEIGHT)
		);
	}

	protected byte[] prepare(byte[] data, int index) {
		return (getImageData(indexToDataType(index), COMPRESSED) != 0)
		       ? LempelZiv.compress(data)
		       : data;
	}

	public void setImage(int type, GBAImage image) {
		// Palette is ignored and assumed to match. Write the image data.
		setData(getImageData(type, INDEX), image == null ? null : image.getData());
		System.out.println("Writing done by " + Util.methodName());
	}

	protected boolean markNewPointer() { return false; }

	/**
	 * Deprecated
	public GBAImage getPaletteAsImage() {
		// Load palette
		ROM.Pointer p = getPointer(getImageData(PALETTE, INDEX));
		if (p == null) { return null; }

		// FIXME: Stock palettes might not be sorted!
		short[] palette = p.getShorts(16);
		// Make the palette test image.
		return new GBAImage(palette);
	}
	**/

	public GBAImage getPortraitData() {
		// Load palette
		ROM.Pointer p = getPointer(getImageData(PALETTE, INDEX));
		if (p == null) { return null; }

		Palette palette = new Palette(p.getBytes(32));

		try {
			if (isCard) {
				// Add a column on the right for background color identification.
				GBAImage temp = readImageData(palette, CARD);
				GBAImage result = new GBAImage(temp.getPalette(), 11, 9);
				result.blit(temp, 0, 0, false);
				return result;
			} else {
				return synthesize(
					readImageData(palette, MUG),
					readImageData(palette, CHIBI),
					readImageData(palette, MOUTH)
				);
			}
		} catch (Exception e) {
			// This should never happen, but if it somehow happens, this will
			// ensure that we get a nice user-friendly "image failed to load" type
			// message instead of a crash.
			// While this SHOULD never happen, it DOES happen when ROM data has been
			// corrupted. A common source of this corruption is... older versions of
			// FEditor: when they relocated the array, they would mark the whole old
			// array as freespace with 0xF0 bytes, but in that version, index 0 was
			// an invalid index that actually belonged to index 1's portrait data.
			return null;
		}
	}

	public boolean isCardSized(GBAImage input) {
		return (
			// We expect one extra column for background color identification.
			input.getTileWidth() == getImageData(CARD, WIDTH) + 1 &&
			input.getTileHeight() == getImageData(CARD, HEIGHT)
		);
	}

	public void setPortraitData(GBAImage input) {
		// This happens if the save button is pressed on an uninitialized or
		// failed-to-load image slot. Probably the best thing is to just ignore
		// the request...
		if (input == null) { return; }

		// Hextator: Why are we doing this? It's just going to be
		// overwritten anyway!
		//clear(); // XXX watch out for shared palettes!

		// We have to set this first so that setImage() calls will work.
		isCard = isCardSized(input);

		// Detect input cards by the image dimensions.
		if (isCard) {
			// Remove the extra column on the right-hand side.
			setImage(CARD, new GBAImage(input, 0, 0, 10, 9));
		} else {
			GBAImage[] decomposed = decompose(input);
			setImage(MUG, decomposed[0]);
			setImage(CHIBI, decomposed[1]);
			if (decomposed.length > 2) {
				setImage(MOUTH, decomposed[2]); // not provided by FE6.
			}
		}

		setData(getImageData(PALETTE, INDEX), input.getPalette().getBytes());
		System.out.println("Writing done by " + Util.methodName());

		// Post-condition
		if (isCard != checkIfCard()) { throw new RuntimeException(); }
	}

	// Take in data from the ROM (not really images; there's no
	// width/height data - but we fake it) and create a single image
	// in the format that the editor expects for portrait sheets.
	protected abstract GBAImage synthesize(GBAImage mugsheet, GBAImage chibisheet, GBAImage mouthsheet);

	// Break up the synthesized image into mug, chibi and mouth sheets.
	// (Actually, FE6 doesn't return a mouth sheet.)
	protected abstract GBAImage[] decompose(GBAImage synthesized);

	// A helper function for rearrange_legacy. Decomposes a legacy spritesheet.
	protected abstract GBAImage[] decompose_legacy(GBAImage synthesized);

	// Convert a spritesheet in the old format to the current format, by
	// cutting it up and putting it back together.
	public GBAImage rearrange_legacy(GBAImage original) {
		GBAImage[] decomposition = decompose_legacy(original);
		return synthesize(
			decomposition[0],
			decomposition[1],
			decomposition.length > 2 ? decomposition[2] : null
		);
	}

	// Determine if the current pointer array index describes a card.
	// This is needed to clean up properly for FE6, which reuses the
	// mug slot for card pointers.
	abstract protected boolean checkIfCard();
	private boolean isCard;

	// Lets the GUI know whether to show the relevant widgets.
	abstract public boolean eyesSupported();
	
	abstract public void setMouthAndEyeData(int[] data);

	abstract public int[] getMouthAndEyeData();

	// Old versions of the editor would actually allocate 0x100 entries, but
	// allow index 0 to be invalid.
	@Override
	public int maxSize() { return 0xFF; }
}

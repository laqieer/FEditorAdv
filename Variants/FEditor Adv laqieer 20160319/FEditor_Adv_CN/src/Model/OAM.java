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
 *  <Description> For representing a GBA sprite, its source location within
 *  loaded image data and its destination location on the GBA screen
 */

package Model;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import Graphics.GBAImage;
import Graphics.GBASpritesheet;
import java.util.ArrayList;

class OAM {
	// FIXME: Stop using 'null' for terminators. Do subclassing instead, like
	// the AnimationCommand hierarchy. It's cleaner.

	private static final byte square = 0;
	private static final byte horizontal = (byte)(1 << 6);
	private static final byte vertical = (byte)(2 << 6);
	private static final byte times1 = 0;
	private static final byte times2 = (byte)(1 << 6);
	private static final byte times4 = (byte)(2 << 6);
	private static final byte times8 = (byte)(3 << 6);

	private int width, height;
	// Position of the image data within the source image.
	private int image_x, image_y;
	// Position of the image data on an OAM spritesheet.
	private int sheet_x, sheet_y;
	// Position of the image data in VRAM. FIXME: We don't need to store this
	// I think; it can be calculated in serialize().
	private int vram_x, vram_y;
	private boolean is_flipped;

	public static int serialize(OAM oam, byte[] buffer, int index) {
		// buffer is assumed to have at least 12 zero bytes following index.
		if (oam == null) {
			buffer[index] = 1;
			return index + 12;
		}
		
		return oam.do_serialization(buffer, index);
	}

	private int do_serialization(byte[] buffer, int index) {
		if (width > height) {
			buffer[index + 1] = horizontal;
		} else if (height > width) {
			buffer[index + 1] = vertical;
		} else 	{
			buffer[index + 1] = square;
		}

		switch (width * height) {
			// area of region
			case 64:
			case 32:
			buffer[index + 3] = times8;
			break;

			case 16:
			case 8:
			buffer[index + 3] = times4;
			break;

			case 4:
			buffer[index + 3] = times2;
			break;

			case 2:
			case 1:
			buffer[index + 3] = times1;
			break;

			default:
			throw new RuntimeException();
		}

		if (is_flipped) { buffer[index + 3] |= 0x10; }

		buffer[index + 4] = (byte)((sheet_y << 5) | sheet_x);

		buffer[index + 6] = (byte)vram_x;
		buffer[index + 7] = (byte)(vram_x >> 8);

		buffer[index + 8] = (byte)vram_y;
		buffer[index + 9] = (byte)(vram_y >> 8);

		return index + 12;
	}

	public static OAM deserialize(byte[] buffer, int index, boolean spell) {
		if (buffer[index] == 1) { return null; }

		int width = 0, height = 0;
		byte area = buffer[index + 3];
		boolean flipped = (area & 0x10) == 0x10;
		area &= 0xC0; // What do the other bits indicate?
		byte align = buffer[index + 1];
		align &= 0xC0; // What do the other bits indicate?

		switch (area) {
			case times8: // area 64 or 32
			switch (align) {
				case vertical: width = 4; height = 8; break;
				case horizontal: width = 8; height = 4; break;
				case square: width = 8; height = 8; break;
			}
			break;

			case times4: // area 16 or 8
			switch (align) {
				case vertical: width = 2; height = 4; break;
				case horizontal: width = 4; height = 2; break;
				case square: width = 4; height = 4; break;
			}
			break;

			case times2: // area 4
			switch (align) {
				case vertical: width = 1; height = 4; break;
				case horizontal: width = 4; height = 1; break;
				case square: width = 2; height = 2; break;
			}
			break;

			case times1: // area 2 or 1
			switch (align) {
				case vertical: width = 1; height = 2; break;
				case horizontal: width = 2; height = 1; break;
				case square: width = 1; height = 1; break;
			}
			break;
		}
		if ((width == 0) || (height == 0)) {
			throw new RuntimeException(String.format(
				"Bad OAM sprite size: area code %d, align code %d", area, align
			));
		}

		int sheet_x = buffer[index + 4] & 0x1F;
		int sheet_y = ((buffer[index + 4]) & 0xE0) >> 5;

		short[] tmp = Util.bytesToShorts(buffer);
		int vram_x = tmp[index / 2 + 3];
		int vram_y = tmp[index / 2 + 4];

		int image_x = (vram_x + (spell ? 0xAC : 0x94)) / 8;
		int image_y = (vram_y + 0x58) / 8;

		OAM result = new OAM(new int[] { image_x, image_y, width, height });
		result.setVRAMLocation(spell);
		result.setSheetLocation(sheet_x, sheet_y);
		result.is_flipped = flipped;

		return result;
	}

	public OAM(int[] input) {
		image_x = input[0];
		image_y = input[1];

		width = input[2];
		height = input[3];
	}

	public GBAImage regionOfSourceImage(GBAImage image) {
		return new GBAImage(image, image_x, image_y, width, height);
	}

	private static AffineTransform transform(int x, int y, int degrees) {
		AffineTransform result = new AffineTransform();
		result.rotate(degrees * Math.PI / 180);
		result.translate(x, y);
		return result;
	}

	public void blitToScreen(BufferedImage sheet, Graphics2D screen, boolean spell) {
		// FIXME: Handle flipping and rotation
		try {
			screen.drawImage(
				sheet.getSubimage(sheet_x * 8, sheet_y * 8, width * 8, height * 8),
				transform(vram_x + (spell ? 0xAC : 0x94), vram_y + 0x58, 0), null
			);
		} catch (Exception e) {
			System.out.println(Util.verboseReport(String.format(
				"SEVERE: blit failure - %d %d %d %d %d %d",
				sheet_x, sheet_y, width, height, image_x, image_y
			)));
		}
	}

	// Convert locations on the source image into locations in VRAM.
	// NOTE: the spell animation code uses a wider window than the GBA
	// screen; thus a different x offset is used (24 pixels greater).
	public void setVRAMLocation(boolean spell) {
		vram_x = (image_x * 8) - (spell ? 0xAC : 0x94);
		vram_y = (image_y * 8) - 0x58;
	}

	public void setSheetLocation(int x, int y) {
		sheet_x = x;
		sheet_y = y;
	}

	@Override
	public boolean equals(Object other) {
		try {
			OAM o = (OAM)(other);
			return width == o.width && height == o.height &&
			image_x == o.image_x && vram_x == o.vram_x &&
			image_y == o.image_y && vram_y == o.vram_y &&
			sheet_x == o.sheet_x && sheet_y == o.sheet_y &&
			is_flipped == o.is_flipped;
		}
		catch (ClassCastException e) { return false; }
	}

	// We don't currently use this, but as a matter of style, .equals()
	// overloads should be accompanied by .hashCode() overloads.
	@Override
	public int hashCode()
	{
		return width ^ height ^ image_x ^ image_y ^
		       sheet_x ^ sheet_y ^ vram_x ^ vram_y ^ (is_flipped ? 1 : 0);
	}

	public OAM flipped() {
		if (is_flipped) { throw new RuntimeException(); }

		OAM result = new OAM(new int[] { image_x, image_y, width, height });
		result.vram_x = -(width * 8) - vram_x;
		result.vram_y = vram_y;
		result.sheet_x = sheet_x;
		result.sheet_y = sheet_y;
		result.is_flipped = true;

		return result;
	}

	// Create a set of OAM objects that represent all the marked
	// tiles of the tileMap, cut up in an optimal way. The OAMs
	// have uninitialized vram locations for now.
	public static ArrayList<OAM> calculateOptimumOAM(TileMap tileMap) {
		ArrayList<OAM> optimumOAMData = new ArrayList<OAM>();

		int[][] searchSizes = new int[][] {
			// For spell animations, the first two sizes will always fail,
			// but this does not cause a problem.
			new int[] { 8, 8 },
			new int[] { 4, 8 },
			new int[] { 8, 4 },
			new int[] { 4, 4 },
			new int[] { 2, 4 },
			new int[] { 4, 2 },
			new int[] { 2, 2 },
			new int[] { 1, 4 },
			new int[] { 4, 1 },
			new int[] { 2, 1 },
			new int[] { 1, 2 },
			new int[] { 1, 1 }
		};

		for (int[] searchSize: searchSizes) {
			while (true) {
				// As long as a region of this size can be extracted, convert and add it.
				int[] region = tileMap.extractMarkedRegion(searchSize[0], searchSize[1]);
				if (region == null) { break; }

				optimumOAMData.add(new OAM(region));
			}
		}

		// Add a terminator and return.
		optimumOAMData.add(null);
		return optimumOAMData;
	}
	// Fine

	private static boolean attemptToFitOAMOn(
		GBAImage frameImage,
		GBAImage BGframeImage,
		ArrayList<OAM> OAMdimensions,
		GBASpritesheet original,
		boolean spell
	) {
		// Make a temporary copy in case we are unsuccessful
		GBASpritesheet sheet = original.copy();
		boolean BGframe = false;

		for (OAM oam: OAMdimensions) {
			if (oam == null) { // terminator.
				BGframe = true;
				continue;
			}
			// Create a sprite from the appropriate frame and attempt to fit it
			GBAImage sprite = oam.regionOfSourceImage(
				BGframe ? BGframeImage : frameImage
			);

			int[] insertionLocation = sheet.findInsertionLocation(sprite);
			if (insertionLocation == null) { return false; }

			// Record the position in the OAM data
			// If fitting fails, there is no need to reset this data,
			// because it is guaranteed to be overwritten anyway
			int x = insertionLocation[0];
			int y = insertionLocation[1];
			// Hextator: Added/modded the lines below to more
			// accurately reflect what should be going on
			oam.setSheetLocation(x, y);
			oam.setVRAMLocation(spell);
			// Small optimization: check if the sprite was already on the sheet,
			// by checking if the sheet is occupied there (if the sprite isn't
			// on the sheet, the sheet would return empty space).
			if (!sheet.usedTileAt(x, y)) {
				sheet.blit(sprite, x, y);
			}
		}
		// Successfully blitted everything.
		// Commit the changes to the original sheet.
		original.swap(sheet);
		return true;
	}
	// Fine

	// For choosing a sheet that a frame image may fit in
	// Also fills the sheet with the tile data
	public static int selectSheet(
		GBAImage frameImage,
		GBAImage BGframeImage,
		ArrayList<OAM> OAMdimensions,
		ArrayList<GBASpritesheet> OAMgraphics,
		boolean spell
	) {
		// NOTE: There used to be code disabled with a constant boolean
		// that would, if space couldn't be found for the OAM entry,
		// split it in half and try again.
		// Hextator: I don't think we need it.
		int index = 0;
		for (; index < OAMgraphics.size(); ++index) {
			if (attemptToFitOAMOn(
				frameImage,
				BGframeImage,
				OAMdimensions,
				OAMgraphics.get(index),
				spell
			)) {
				break;
			}
		}

		// Create new sheet if previous sheets are too full
		if (index == OAMgraphics.size()) {
			GBASpritesheet to_add = new GBASpritesheet(
				frameImage.getPalette(), 32, spell ? 4 : 8
			);
			if (!attemptToFitOAMOn(
				frameImage, BGframeImage, OAMdimensions, to_add, spell
			)) {
				throw new RuntimeException("Couldn't fit on a blank sheet!");
			}
			OAMgraphics.add(to_add);
		} // Otherwise, the necessary changes are already made.

		return index;
	}
	// Fine
}

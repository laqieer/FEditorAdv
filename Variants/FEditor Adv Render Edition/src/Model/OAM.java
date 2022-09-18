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
import java.awt.geom.Dimension2D;
import java.awt.image.AffineTransformOp;
import Graphics.GBAImage;
import Graphics.GBASpritesheet;
import java.util.ArrayList;
import java.lang.Math;

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
	// Temp: flags for vertical, horizontal, rotation & double-sizing of a tile
	private boolean is_vflipped;
	private boolean is_hflipped;
	private boolean is_rotated;
	private boolean is_doubled;
	// Temp: Used to store the rotation angle calculated from the OAM matrix
	private static double[] rotate_angle;	// array to store all angles for a frame with rotating OBJs
	private static double[] scale;			// array to store all (x,y) scaling factors for scaled OBJs
	private static int r_index;				// index used to store each angle and scale set for each OBJ
	private int entry;						// index to store which angle & scaling factors to blit with
        // BwdYeti: whoops I have to store this here too, because passing it around would be convoluted
        // The size of the tiles to use for the OAM data
        // 8 for all functional purposes as custom animations have tile
        // placement spaced by 8s, but 1 for rendering
        private int tile_scale = 8;
	
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

	// Temp: added angle & scaling to pass down through recursive function
	public static OAM deserialize(byte[] buffer, int index, boolean spell, double[] rotateAngle, double[] scaling, int tile_scale) {
		short [] tmp = Util.bytesToShorts(buffer);
		// check if OAM line is a matrix line (0xFFFF)
		if (tmp[index / 2 + 1] == -1) {
			// function to return an angle and store it as an int
			double[] transform = analyzeMatrix(tmp, index);
			// store angle into a blank OAM
			OAM result = new OAM(new int[] { 0, 0, 0, 0 });
			if (r_index >= tmp[index/2]) {r_index = 0;}		// limits the # of rotated/scaled OBJs based on OAM
			result.rotate_angle[r_index] = transform[0];	// store angle into angle array
			result.scale[2*r_index] = transform[1];			// store xScaling into scale array
			result.scale[2*r_index+1] = transform[2];		// store yScaling into scale array
			r_index++;
			return result;
		}
		// check for end of OAM list for frame, return if reached
		if (buffer[index] == 1) { return null; }

		int width = 0, height = 0;
		byte area = buffer[index + 3];
		boolean flipped = (area & 0x10) == 0x10;
		boolean v_flipped = (area & 0x20) == 0x20;	// checks for OBJ flip vertically
		boolean h_flipped = (area & 0x10) == 0x10;	// checks for OBJ flip horizontally
		int entry_flag = 0;
		// Temp: probably could use a loop instead of this mess
		if ((area & 0x02) == 0x02) {entry_flag = 1;}
		if ((area & 0x04) == 0x04) {entry_flag = 2;}
		if ((area & 0x06) == 0x06) {entry_flag = 3;}
		if ((area & 0x08) == 0x08) {entry_flag = 4;}
		if ((area & 0x0A) == 0x0A) {entry_flag = 5;}
		area &= 0xC0; // What do the other bits indicate?
		byte align = buffer[index + 1];
		boolean rotate = (align & 0x01) == 0x01;		// checks for OBJ rotation
		boolean sizeDouble = (align & 0x02) == 0x02;	// checks for double size on rotation
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

		//short[] tmp = Util.bytesToShorts(buffer);		// handled earlier (line 135)
		int vram_x = tmp[index / 2 + 3];
		int vram_y = tmp[index / 2 + 4];

		int image_x = ((vram_x + (spell ? 0xAC : 0x94)) / tile_scale);
		int image_y = ((vram_y + 0x58) / tile_scale);

		OAM result = new OAM(new int[] { image_x, image_y, width, height });
                result.tile_scale = tile_scale;
		result.setVRAMLocation(spell);
		result.setSheetLocation(sheet_x, sheet_y);
		result.is_vflipped = v_flipped;		// stores vertical flip for blit on recursion
		result.is_hflipped = h_flipped;		// stores horizontal flip for blit on recursion
		result.is_flipped = flipped;
		result.is_doubled = sizeDouble;		// stores if this object needs a double-sized render area
		result.is_rotated = rotate;			// stores rotation state for blit on recursion
		result.rotate_angle = rotateAngle;	// stores angle for blit on recursion
		result.scale = scaling;				// stores xy scale factor for blit on recursion
		result.entry = entry_flag;			// stores angle/scale array entry to use
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

	// Temp: setter used for initialization
	public static void setRotation() {
		int[] rotate_angle = {0,0,0};
	}
	
	// Temp: getter for rotation array
	public double[] getRotation() {
		return rotate_angle;
	}
	
	// Temp: getter for scale array
	public double[] getScale() {
		return scale;
	}
	
	// Temp: Resetter for r_index
	public static void clearR_index() {
		r_index = 0;
	}
	
	// Temp: Used to break down LTD Matrices into a rotation angle and scaling factors
	private static double[] analyzeMatrix( short[] ar, int index ) {
		double [] calc = new double [4];
		for (int i = 0; i < 4; i++){ calc[i] = (double) ar[index/2 + i + 2] / 256; }	// pull matrix values and divide by 256
		double angle = Math.toDegrees(Math.atan(calc[1]/calc[0]));						// calculate the arctan of the cos & sin vaules of the matrix
	/*	Debug output	
		System.out.println(angle);*/
		
		// Angle sign correction
		if (ar[index/2+2] < 0 && ar[index/2+3] < 0) { angle -= 180; }
		if (ar[index/2+2] < 0 && ar[index/2+3] > 0) { angle += 180; }
		// Calculate the scaling factors
		double xmag = Math.cos(Math.toRadians(angle))/calc[0];							// calc for x_scaling
		if (xmag >= 10) { xmag = Math.abs(Math.sin(Math.toRadians(angle))/calc[1]); }	// used if angle is invalid over cos
		double ymag = Math.cos(Math.toRadians(angle))/calc[3];							// calc for y_scaling
		if (ymag >= 10) { ymag = Math.abs(Math.sin(Math.toRadians(angle))/calc[2]); }	// used if angle is invalid over cos
	/*	debug output	
		System.out.format("x: %.4f - y: %.4f\n",xmag,ymag);*/
		
		// Store values for return
		double[] result = new double[3];
		result[0] = angle;
		result[1] = xmag;
		result[2] = ymag;
		return result;
	}

	/* Temp: Handle different transformations for render, such as:
	 * OBJ flipping(horizontal, vertical, or both), 
	 * Translations(along xy)
	 * Rotation(degrees)
	 * Scaling(given x & y scaling factors)
	 * 
	 * Returns an AffineTransform to use on OBJs in blitting
	 */
	private static AffineTransform renderTransform(
		int x,				// OBJ vertical shift
		int y,				// OBJ horizontal shift
		int slot,			// array slot value
		double[] degrees,	// OBJ rotation angle array
		double[] magnify,	// OBJ scale array
		boolean doubled,	// double flag
		boolean rotation,	// rotation flag
		boolean vert_flip,	// vertical flip flag
		boolean hori_flip,	// horizontal flip flag
		int width,			// OBJ width
		int height			// OBJ height
		) {
		AffineTransform result = new AffineTransform();
		// Check all 3 possible flip combinations, flip & translate accordingly
		if (hori_flip == false && vert_flip == true) { result = AffineTransform.getScaleInstance(1,-1); result.translate(x,-y-height);}
		else if (hori_flip == true && vert_flip == false) { result = AffineTransform.getScaleInstance(-1,1); result.translate(-x-width, y); }
		else if (hori_flip == true && vert_flip == true) { result = AffineTransform.getScaleInstance(-1,-1);  result.translate(-x-width,-y-height); }
		else {result.translate(x, y);}
		
		// Rotate/Scale OBJ based on angle
		if (rotation == true) { 
			// Temp: Correct placement for double-sized elements (Dark Druid and Archsage come to mind)
			if (doubled) {result.translate(width/2, height/2);}
		
		// add rotation about the center of an object
		result.rotate(Math.toRadians(degrees[slot]), width/2, height/2);
		// add adjustment to position for scaling
		result.translate(width*(1-magnify[2*slot])/2,height*(1-magnify[2*slot+1])/2);
		// add scaling to transform
		result.scale(magnify[2*slot],magnify[2*slot+1]);
		}
		return result;
	}

	public void blitToScreen(BufferedImage sheet, Graphics2D screen, boolean spell) {
		try {
			if (width != 0 || height != 0) {
				screen.drawImage(
					sheet.getSubimage(sheet_x * 8, sheet_y * 8, width * 8, height * 8),
					renderTransform(vram_x + (spell ? 0xAC : 0x94), vram_y + 0x58, entry, rotate_angle, scale, is_doubled, is_rotated, is_vflipped, is_hflipped, width * 8, height * 8),null
				);
			}
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
		vram_x = (image_x * tile_scale) - (spell ? 0xAC : 0x94);
		vram_y = (image_y * tile_scale) - 0x58;
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
		       sheet_x ^ sheet_y ^ vram_x ^ vram_y ^ (is_flipped ? 1 : 0) ^ (is_vflipped ? 1 : 0) ^ (is_hflipped ? 1 : 0);
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

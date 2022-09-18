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
 *  <Description> An image that keeps track of empty space and allows for
 *  blitting other images into its empty space.
 */

package Graphics;

import Model.TileMap;

public class GBASpritesheet {
	GBAImage image;
	TileMap map;

	private GBASpritesheet(GBASpritesheet toCopy) {
		image = toCopy.image.copy();
		map = toCopy.map.copy();
	}

	public GBASpritesheet(Palette palette, int width, int height) {
		image = new GBAImage(palette, width, height);
		map = new TileMap(width, height);
	}

	public GBAImage getImage() { return image; }

	public byte[] getData() { return image.getData(); }

	public boolean samePaletteAs(GBAImage image) {
		return this.image.samePaletteAs(image);
	}

	// Find a spot on the sheet large enough to hold the sprite.
	public int[] findInsertionLocation(GBAImage sprite) {
		int rows = sprite.getTileHeight();
		int columns = sprite.getTileWidth();
		int max_row = image.getTileHeight() - rows;
		int max_col = image.getTileWidth() - columns;

		// First check if the sprite is already on the sheet.
		for (int row = 0; row <= max_row; ++row) {
			for (int column = 0; column <= max_col; ++column) {
				if (image.matches(sprite, 0, 0, columns, rows, column, row)) {
					return new int[] { column, row };
				}
			}
		}

		// Look for blank space.
		for (int row = 0; row <= max_row; ++row) {
			for (int column = 0; column <= max_col; ++column) {
				if (map.allMarkedAs(column, row, columns, rows, false)) {
					return new int[] { column, row };
				}
			}
		}

		// If neither of those worked, the sprite simply won't fit.
		return null;
	}

	// Image being blitted should be opaque in all tiles
	public void blit(
		GBAImage from,
		int to_x, int to_y
	) {
		image.blit(from, to_x, to_y, false);
		map.setMarks(to_x, to_y, from.getTileWidth(), from.getTileHeight(), true);
	}

	public boolean usedTileAt(int x, int y) {
		return map.allMarkedAs(x, y, 1, 1, true);
	}

	public GBASpritesheet copy() { return new GBASpritesheet(this); }

	public void swap(GBASpritesheet other) {
		GBAImage x = other.image;
		image = x;
		other.image = image;
		TileMap y = other.map;
		map = y;
		other.map = map;
	}
}

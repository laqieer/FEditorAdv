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
 *  <Description> Class for representing tile maps for GBA sprite sheets, which
 *  are GBA Images that GBA sprites get their graphics from
 */

package Model;

import Graphics.GBAImage;

public class TileMap
{
	private boolean[][] map;
	private int markCount = 0;

	private TileMap(TileMap source) {
		map = source.map.clone();
		markCount = source.markCount;
	}

	public TileMap copy() {
		return new TileMap(this);
	}

	public TileMap(GBAImage source) {
		int width = source.getTileWidth();
		int height = source.getTileHeight();
		map = new boolean[height][width];

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				boolean marked = !source.blankTileAt(j, i);
				map[i][j] = marked;
				if (marked) { markCount++; }
			}
		}
	}

	public TileMap(int width, int height) {
		map = new boolean[height][width];
	}

	public int getCount() { return markCount; }

	// If there exists an x by y block of marked cells,
	// unmark them, and return a 'rect' indicating where they were.
	// Otherwise, return null.
	public int[] extractMarkedRegion(int columns, int rows) {
		// optimization
		if ((columns * rows) > markCount) { return null; }

		int max_row = map.length - rows;
		int max_column = map[0].length - columns;

		for (int row = 0; row < max_row; row++) {
			for (int column = 0; column < max_column; column++) {
				if (allMarkedAs(column, row, columns, rows, true)) {
					setMarks(column, row, columns, rows, false);
					return new int[] { column, row, columns, rows };
				}
			}
		}
		return null; // not found
	}

	public boolean allMarkedAs(
		int column, int row, int columns, int rows, boolean mark
	) {
		int max_column = column + columns;
		int max_row = row + rows;

		for (int r = row; r < max_row; r++) {
			for (int c = column; c < max_column; c++) {
				if (map[r][c] != mark) { return false; }
			}
		}
		return true; // all are marked.
	}

	public void setMarks(int column, int row, int columns, int rows, boolean sense) {
		int max_column = column + columns;
		int max_row = row + rows;
		for (int r = row; r < max_row; r++) {
			for (int c = column; c < max_column; c++) {
				// Decrease or increase the count accordingly
				// if the existing mark doesn't match the new one.
				if (map[r][c] && !sense) { --markCount; }
				if (!map[r][c] && sense) { ++markCount; }
				map[r][c] = sense;
			}
		}
	}
}

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
 *  <Description> When executed, outputs a text file detailing a map
 *  of the contents of the specified ROM
 */

package Model;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import FEditorAdvance.CommonDialogs;

public class ROMMapper {
	public static class RangeWrapper implements Comparable {
		private int[] range;
		private String description;

		public RangeWrapper(int[] _range, String _description) {
			range = _range;
			description = _description;
		}

		public int[] getRange() { return range; }

		public String getDescription() { return description; }

		public int compareTo(Object other) {
			try {
				RangeWrapper otherRangeWrapper =
					(RangeWrapper)other;
				return range[0] - otherRangeWrapper.range[0];
			} catch (Exception ex) { return 0; }
		}
	}

	// Tested and working
	public static int[][] patchSpaceUsed(int[][][] patches) {
		List<int[]> output = new ArrayList<int[]>();
		for (int[][] patch: patches) {
			boolean firstBlock = true;
			for (int[] block: patch) {
				if (firstBlock){
					firstBlock = false;
					continue;
				}
				boolean firstInt = true;
				boolean firstShort = false;
				boolean lastShort = false;
				int[] entry = new int[2];
				entry[1] = 0;
				for (int curr: block) {
					if (firstInt) {
						firstShort = (curr & Game.FIRST_SHORT) != 0;
						lastShort = (curr & Game.LAST_SHORT) != 0;
						entry[0] = (curr & (~0x3)) + (firstShort ? 2 : 0);
						firstInt = false;
					}
					else
						entry[1] += 4;
				}
				entry[1] -= firstShort ? 2 : 0;
				entry[1] -= lastShort ? 2 : 0;
				entry[0] -= ROM.GBA_HARDWARE_OFFSET;
				entry[1] += entry[0];
				output.add(entry);
			}
		}
		return Util.intArrayListToIntArrayArray(output);
	}

	// Tested and working
	public static int[][] fixList(int[][] input, Game owner) {
		if (input == null || input.length == 0)
			return null;
		List<int[]> output = new ArrayList<int[]>();
		// Temporarily clear free space
		int[][] original = owner.getFreeSpace();
		owner.unmarkFreeSpace(
			owner.getPointer(ROM.GBA_HARDWARE_OFFSET),
			owner.getPointer(
				owner.r.size()
				+ ROM.GBA_HARDWARE_OFFSET
			)
		);
		// Process given ranges
		for (int[] curr: input) {
			owner.markFreeSpace(
				owner.getPointer(
					curr[0] + ROM.GBA_HARDWARE_OFFSET
				),
				owner.getPointer(
					curr[1] + ROM.GBA_HARDWARE_OFFSET
				),
				false
			);
		}
		int[][] overwritten = owner.getFreeSpace();
		owner.unmarkFreeSpace(
			owner.getPointer(ROM.GBA_HARDWARE_OFFSET),
			owner.getPointer(
				owner.r.size()
				+ ROM.GBA_HARDWARE_OFFSET
			)
		);
		// Put old free space back
		for (int[] curr: original) {
			owner.markFreeSpace(
				owner.getPointer(
					curr[0] + ROM.GBA_HARDWARE_OFFSET
				),
				owner.getPointer(
					curr[1] + ROM.GBA_HARDWARE_OFFSET
				)
			);
		}
		// Add freed space to list of used space
		for (int[] curr: overwritten) {
			output.add(new int[] {
				curr[0] + ROM.GBA_HARDWARE_OFFSET,
				curr[1] + ROM.GBA_HARDWARE_OFFSET
			});
		}
		return Util.intArrayListToIntArrayArray(output);
	}

	// Tested and working
	// Expects a list of int pairs "start" and "end" of a memory range
	public static List<RangeWrapper> labelRanges(
		int[][] list, Game owner, String description
	) {
		if (list == null || list.length == 0) return null;
		List<RangeWrapper> output = new ArrayList<RangeWrapper>(list.length);
		list = fixList(list, owner);
		for (int[] pair: list) {
			output.add(new RangeWrapper(
				new int[] { pair[0], pair[1] }, description
			));
		}
		return output;
	}

	public static String mapToString(List<RangeWrapper> map) {
		StringBuilder output = new StringBuilder("");
		Collections.sort(map);
		for (RangeWrapper curr: map) {
			int[] range = curr.getRange();
			output.append(String.format(
				"0x%08X to 0x%08X - %s%s",
				range[0], range[1],
				curr.getDescription(), Util.newline
			));
		}
		return output.toString();
	}

	private static void addHelper(
		List<RangeWrapper> toAddTo,
		List<RangeWrapper> toAdd
	) {
		if (toAdd == null || toAdd.isEmpty()) return;
		toAddTo.addAll(toAdd);
	}

	// Tested and working
	public static void main(String[] args) {
		try {
			Game owner = Game.open(null);
			ClassAnimationArray caa = owner.classAnimationArray();
			PortraitArray pa = owner.portraitArray();
			SpellAnimationArray saa = owner.spellAnimationArray();
			SpellProgramCounterArray spca = owner.spellProgramCounterArray();
			TextArray ta = owner.textArray();
			int[][] patchSpaceUsed = patchSpaceUsed(owner.getPatches());
			int[][] freeSpace = owner.getFreeSpace();
			List<RangeWrapper> output = new ArrayList<RangeWrapper>();
			addHelper(output, labelRanges(caa.usedSpace(), owner, "Class Animation"));
			addHelper(output, labelRanges(caa.usedArraySpace(), owner, "Class Animation Array"));
			addHelper(output, labelRanges(pa.usedSpace(), owner, "Portrait"));
			addHelper(output, labelRanges(pa.usedArraySpace(), owner, "Portrait Array"));
			addHelper(output, labelRanges(saa.usedSpace(), owner, "Spell Animation"));
			addHelper(output, labelRanges(saa.usedArraySpace(), owner, "Spell Animation Array"));
			addHelper(output, labelRanges(spca.usedSpace(), owner, "Spell Animation Program Counter"));
			addHelper(output, labelRanges(spca.usedArraySpace(), owner, "Spell Animation Program Counter Array"));
			addHelper(output, labelRanges(ta.usedSpace(), owner, "Text Entry"));
			addHelper(output, labelRanges(ta.usedArraySpace(), owner, "Text Array"));
			addHelper(output, labelRanges(patchSpaceUsed, owner, "Auto Patch"));
			addHelper(output, labelRanges(freeSpace, owner, "Free Space"));
			File file = CommonDialogs.showSaveFileDialog("ROM map");
			if (file == null) return;
			FileWriter out = null;
			try {
				out = new FileWriter(file);
				out.write(mapToString(output));
			} catch (Exception e) {
				throw new RuntimeException(
					"Stream error when dumping ROM map"
				);
			}
			try { out.close(); } catch (Exception e) {}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		System.exit(0);
	}
}

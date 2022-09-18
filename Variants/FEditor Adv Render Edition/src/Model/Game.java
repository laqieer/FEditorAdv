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
 *  <Description> For representing the aspects of a Fire Emblem game that
 *  are unique between them.
 */

package Model;

import java.io.File;
//import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import FEditorAdvance.CommonDialogs;

public abstract class Game {
	protected ROM r;
	protected int type;

	protected Metadata metadata;

	// Constants describing what state the ROM is in.
	// The original unmodified ROM
	public static final int CLEAN = 0;
	// Worked on with FEditor
	public static final int NEW_VERSIONS = 1;
	// Worked on with old versions of FEditor
	public static final int OLD_VERSIONS = 2;
	// Worked on with something else unknown
	public static final int HACKED = 3;

	// Constants for the patching system.
	protected static final int FIRST_SHORT = 0x2;
	protected static final int LAST_SHORT = 0x1;
	// The patches for a game are held in an int[][][]. Each int[][] is a patch,
	// consisting of several int[]s as follows:
	// 0 - Patch signature. int 0 is the location to check for the signature,
	// and int 1 is the value to look for. If we read the expected value, we infer
	// that the patch has already been applied.
	// 1..n - Patch blocks. int 0 is a pointer to the first quad of the ROM to be
	// patched (note: Java int is guaranteed to be exactly 4 bytes, unlike C++),
	// with a bit of evilness: since the pointer must be a multiple of 4, the
	// lower 2 bits are used as flags. Bit 1 means "only patch the high two bytes
	// of the first quad (remember the ROM is little-endian!) and bit 0 means
	// "only patch the low two bytes of the last quad". (We can't just check for
	// these values being <= 0xffff, because you might want to overwrite the other
	// two bytes with zeros explicitly and you might not.) So the bytes to patch
	// are always contiguous. Then ints 1 through N are the values to use to patch
	// sequential quads of the ROM.

	// Free space information is just a list of pairs of (start, length).
	// Hextator: Actually, we might do a differential set up to halve the
	// size of the free space list.

	// Obtain patching info.
	public abstract int[][] getDefaultFreespace();
	public abstract int[][][] getPatches();

	// Define game-specific constants and functionality.
	public abstract String condenseControlCodes(String input);
	public abstract String expandControlCodes(String input);
	public abstract HashMap<String, Integer> getTextWidthMap();
	public abstract int getMaxTextWidth();

	public BinaryModel binaryModel() { return new BinaryModel(this); }
	public abstract PortraitArray portraitArray();
	public abstract ClassAnimationArray classAnimationArray();
	public abstract TextArray textArray();
	public abstract SpellAnimationArray spellAnimationArray();
	public abstract SpellProgramCounterArray spellProgramCounterArray();

	public abstract int CLEAN_CHECKSUM();

	/*
	For correcting 0x85 commands in exported/imported class animations
	public abstract byte[][] getClassCommandsToCommon();
	public abstract byte[][] getClassCommandsFromCommon();
	*/

	// Extract the footer from the ROM, break it into chunks, and
	// process each chunk. As a side effect, the ROM is truncated
	// to remove the footer. The checksum will have been already
	// removed.
	private boolean setupMetadata(ROM r) {
		System.out.println("SET UP METADATA");
		// Get a pointer to signature field
		ROM.Pointer end = r.new Pointer(r.size() - 4 + ROM.GBA_HARDWARE_OFFSET);

		if (end.currentInt() == Metadata.SIGNATURE) {
			end.advance(-4); // step onto footer length

			// Number of words in chunks, times bytes per word, plus version length
			int footerBytes = end.currentInt() * 4 + 12;

			// We leave 'end' pointing to the footer length count, i.e. the end
			// of the footer data. Then we create 'begin' pointing to the start
			// of the data.
			ROM.Pointer begin = end.offsetBy(-footerBytes);

			// Read version, for use in interpreting chunks.
			String version = new String(begin.getBytes(10));
			System.out.println("VERSION: " + version);
			// 2 padding bytes on version
			begin.advance(2);

			// Iterate over chunks.
			while (!(begin.equals(end))) {
				int chunkType = begin.nextInt();
				int size = begin.nextInt() * 4;
				byte[] data = begin.getBytes(size);

				metadata.handleChunk(r, chunkType, data);
			}

			// Strip the footer.
			r.removeMetadata(footerBytes + 8);
			return true;
		} else {
			System.out.println("NO FOOTER FOUND");
			// We leave the spellArrayAddress as null.
			return false;
		}
	}

	public int getType() { return type; }

	private boolean inBounds(ROM.Pointer start, ROM.Pointer end) {
		if (
			start.getLocation() < 0
			|| start.getLocation() > r.size()
			|| end.getLocation() < 0
			|| end.getLocation() < start.getLocation()
			|| end.getLocation() > r.size()
		)
			return false;
		return true;
	}

	public int markFreeSpace(ROM.Pointer begin, ROM.Pointer end) {
		return markFreeSpace(begin, end, true);
	}

	public int markFreeSpace(
		ROM.Pointer begin, ROM.Pointer end, boolean conserve
	) {
		if (begin.rom() != r || end.rom() != r) {
			throw new RuntimeException(
				"Game expected to mark freespace"
				+ "in a ROM that doesn't belong to it!"
			);
		}
		if (!inBounds(begin, end))
			throw new RuntimeException(
				"Invalid range for marking free space!"
			);
		return metadata.addFreeSpace(
			begin.getLocation(), end.getLocation(),
			conserve
		);
	}

	public int unmarkFreeSpace(ROM.Pointer begin, ROM.Pointer end) {
		if (begin.rom() != r || end.rom() != r) {
			throw new RuntimeException(
				"Game expected to mark space as used"
				+ "in a ROM that doesn't belong to it!"
			);
		}
		if (!inBounds(begin, end))
			throw new RuntimeException(
				"Invalid range for marking used space!"
			);
		return metadata.removeFreeSpace(begin.getLocation(), end.getLocation());
	}

	protected void init(ROM r) {
		this.r = r;
		metadata = new Metadata();
		int position = r.size() - 4;

		int full_checksum = r.checksum();
		int checksum_except_last = r.checksum(0, position);
		int last_word = r.new Pointer(
			position + ROM.GBA_HARDWARE_OFFSET
		).currentInt();

		// Check for unmodified ROMs using the known checksums.
		if (full_checksum == CLEAN_CHECKSUM()) {
			type = CLEAN;
			for (int[] freechunk: getDefaultFreespace()) {
				metadata.addFreeSpace(
					freechunk[0], freechunk[0] + freechunk[1],
					true
				);
			}
			patch();
			type = NEW_VERSIONS;
		}

		// Check for ROMs edited with FEditor.
		else if (checksum_except_last == last_word) {
			System.out.println("HAS A CHECKSUM");
			r.removeMetadata(4);
			// It had a checksum; now see if it has a footer.
			type = setupMetadata(r) ? NEW_VERSIONS : OLD_VERSIONS;

			// Allow for updates to patches.
			// FIXME: Check versions or something.
			patch();
			if (type == OLD_VERSIONS) {
				// Do reverse-compatibility fixes on pointer arrays.
				// We do this by just opening them; if they're unrelocated, they'll
				// just load normally, and if they're relocated, the constructor will
				// do some patching.
				System.out.println("FIXING OLD POINTER ARRAYS");
				portraitArray();
				classAnimationArray();
				textArray();
				try { spellAnimationArray(); }
				// If we edit an OLD_VERSIONS FE6/8 ROM, nothing needs to be done
				// about the spell animation array (because it doesn't exist, because
				// there was no CSA patch available in the OLD_VERSIONS), so any
				// exception thrown here is of no concern.
				catch (UnsupportedOperationException uoe) {}

				// Look for freespace marked with 0xF0.
				// We do this AFTER marking up the arrays, so that new array padding
				// doesn't get identified as freespace. (This works because the new
				// Array padding uses 00 bytes instead of F0.)
				r.findFreespace_legacy(metadata);

				// Phew. Everything is fixed, so continue on as if nothing happened.
				type = NEW_VERSIONS;
			}
		}

		// Someone else edited this ROM. To be safe, don't assume any freespace,
		// spell array address, etc.
		// Should we try to patch?
		else {
			System.out.println(String.format(
				"HACKED: CHECKSUM ISN'T \"%08X\"",
				checksum_except_last
			));
			type = HACKED;
		}
	}

	public void save(String filename, String version, boolean include_metadata) {
		System.out.println("ATTEMPTING TO SAVE ROM");
		System.out.println("PATH: " + filename);
		int checksum = r.save(filename, include_metadata ? metadata : null, version);
		System.out.println(
			"SAVED TO DISK: "
			+ String.format("ROM with checksum 0x%08X", checksum)
		);
		
	}

	// This may get called manually on HACKED instances.
	public void patch() {
		for (int[][] patch: getPatches()) {
			// Check if already patched.
			if (r.new Pointer(patch[0][0]).currentInt() == patch[0][1]) { continue; }
			
			for (int i = 1; i < patch.length; ++i) {
				int[] chunk = patch[i];

				int base_address = chunk[0];
				// Check the flags in the low-order two bits
				boolean first_short = (base_address & FIRST_SHORT) != 0;
				boolean last_short = (base_address & LAST_SHORT) != 0;
				base_address &= ~(FIRST_SHORT | LAST_SHORT);

				int last = chunk.length - 1;
				if (last < 1) { throw new RuntimeException("patch too short"); }
				int first_value = chunk[1];
				int last_value = chunk[last];

				ROM.Pointer p = r.new Pointer(base_address);
				if (first_short) { p.nextShort(); p.writeShort((short)first_value); }
				else if (last != 1) { p.writeInt(first_value); }
				if (last != 1) { p.writeInts(Arrays.copyOfRange(chunk, 2, last)); }
				if (last_short) { p.writeShort((short)last_value); }
				else { p.writeInt(last_value); }
			}
		}
	}

	public boolean isSaved() { return r.isSaved(); }

	public ROM.Pointer getPointer(int ptr) {
		// Legacy support note: -1 values were used for "uninitialized" entries
		// in resized pointer arrays. It seems like the easiest thing is to convert
		// them into null pointers.
		return (ptr == 0 || ptr == -1) ? null : r.new Pointer(ptr);
	}

	public int currentROMsize() { return r.size(); }

	public ROM.Pointer write(int[] data, boolean marked) {
		int originalSize = currentROMsize();
		// Get a Pointer to a writing location, expanding the ROM if need be.
		int byteCount = data.length * 4;
		int location = metadata.allocate(byteCount);
		ROM.Pointer p = (location < 0)
			? r.expand(byteCount)
			: r.new Pointer(location + ROM.GBA_HARDWARE_OFFSET);

		// Remember the start location.
		ROM.Pointer result = p.offsetBy(0);
		result.marked = marked;

		// Write the data.
		p.writeInts(data);
		System.out.println(
			"0x"
			+ Integer.toString(data.length * 4, 16).toUpperCase()
			+ " bytes written to "
			+ String.format("0x%08X", result.getLocation() + ROM.GBA_HARDWARE_OFFSET)
		);
		int newSize = currentROMsize();
		if (originalSize != newSize)
			System.out.println(String.format(
				"New ROM size: 0x%08X",
				newSize
			));

		return result;
	}

	public ROM.Pointer write(byte[] data, boolean marked) {
		return write(Util.bytesToInts(data), marked);
	}

	// Find a space to write the given data, and then repoint the indicated
	// handle at an offset from whereever the data was written. If there is no
	// freespace big enough, the ROM will be expanded.
	public ROM.Pointer writeAndRepoint(
		int[] data, int handle_position, boolean marked, int offset
	) {
		int dest = write(data, marked).offsetBy(offset).toInt();
		r.new Pointer(
			handle_position + ROM.GBA_HARDWARE_OFFSET
		).writeInt(dest);
		return r.new Pointer(dest);
	}

	public ROM.Pointer writeAndRepoint(
		byte[] data, int handle_position, boolean marked, int offset
	) {
		int dest = write(data, marked).offsetBy(offset).toInt();
		r.new Pointer(
			handle_position + ROM.GBA_HARDWARE_OFFSET
		).writeInt(dest);
		return r.new Pointer(dest);
	}

	public void listFreeSpace() {
		if (metadata != null)
			metadata.listFreeSpace();
	}

	public String printFreeSpace() {
		if (metadata != null)
			return metadata.printFreeSpace();
		else
			return "";
	}

	public int[][] getFreeSpace() {
		if (metadata == null)
			return new int[][] { { } };
		return metadata.getFreeSpace();
	}

	// Parses free space written to a file in a similar fashion as the
	// output to printFreeSpace()
	public void parseFreeSpace(File toParse) {
		if (toParse == null)
			return;
		if (metadata == null)
			metadata = new Metadata();
		Scanner freeSpaceScanner = null;
		try {
			freeSpaceScanner = new Scanner(toParse);
		} catch (Exception e) {
			throw new RuntimeException(
				"Stream error reading free space file"
			);
		}
		String read = "-1";
		final boolean deallocate;
		if (freeSpaceScanner.hasNextLine())
			read = freeSpaceScanner.nextLine();
		if (read.equals("[Exhaustive]"))
			unmarkFreeSpace(
				r.new Pointer(ROM.GBA_HARDWARE_OFFSET),
				r.new Pointer(ROM.GBA_HARDWARE_OFFSET + r.size())
			);
		deallocate = read.equals("[Deallocate]");
		int start = -1;
		try {
			start = Integer.parseInt(read, 16);
		} catch (Exception e) {}
		int length = -1;
		while (freeSpaceScanner.hasNextLine()) {
			read = freeSpaceScanner.nextLine();
			if (start != -1)
				try {
					length = Integer.parseInt(read, 16);
				} catch (Exception e) {}
			else
				try {
					start = Integer.parseInt(read, 16);
				} catch (Exception e) {}
			if (start != -1 && length != -1) {
				ROM.Pointer startPtr =
					r.new Pointer(ROM.GBA_HARDWARE_OFFSET + start);
				ROM.Pointer endPtr =
					r.new Pointer(ROM.GBA_HARDWARE_OFFSET + start + length);
				if (!deallocate) markFreeSpace(startPtr, endPtr);
				else unmarkFreeSpace(startPtr, endPtr);
				start = -1;
				length = -1;
			}
		}
		try { freeSpaceScanner.close(); } catch (Exception e) {}
	}

	// Static interface to stuff which opens dialogs and stuff.
	// FIXME: The dialog handling code should be in the view classes,
	// not here.

	public static Game open(String fileName) {
		if (fileName == null) {
			// XXX
			java.awt.FileDialog chooser = new java.awt.FileDialog(
				new java.awt.Frame(), 
				"Select FE ROM to open"
			);
			chooser.setVisible(true);
			chooser.setLocationRelativeTo(null);
			if (chooser.getDirectory() == null) {
				return null;
			} else if (chooser.getFile() == null) {
				return null;
			}
			fileName = chooser.getDirectory() + chooser.getFile();
		}

		Game result = null;
		try {
			ROM rom = new ROM(fileName);
			final int GAME_ID_ADDRESS = 0x080000A0;
			String id = new String(rom.new Pointer(GAME_ID_ADDRESS).getBytes(0x12), "ASCII");
			if (id.equals(FE6.GAME_ID())) { result = new FE6(); }
			if (id.equals(FE7.GAME_ID())) { result = new FE7(); }
			if (id.equals(FE8.GAME_ID())) { result = new FE8(); }
			if (result != null) { result.init(rom); }
		} catch (Exception e) {
			e.printStackTrace();
			// XXX
			CommonDialogs.showStreamErrorDialog();
		}
		return result;
	}
        
        public String baseGameName()
        {
            if (this instanceof Model.FE6)
                return "FE6";
            else if (this instanceof Model.FE8)
                return "FE8";
            else
                return "FE7";
        }
}

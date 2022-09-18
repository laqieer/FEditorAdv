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
 *  <Description> Represents metadata for a ROM edited with FEditor - e.g. the
 *  version of FEditor used for the last edit, where free space is, etc.
 */

package Model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

public class Metadata {
	// Chunk identifiers and magic words. 
	// All of these are endian-swapped.
	public static final int SIGNATURE = 0x1270D1FE;
	// Endian-swapped version of 0xFED17012, which looks like "FEditor" in l33t
	// Common data chunks. Derived classes may define more.
	public static final int FREE_SPACE_CHUNK = 0x45455246; // "FREE"
	public static final int SPELL_ARRAY_CHUNK = 0x4C455053; // "SPEL"

	// Footer format:
	// Version string YYYYMMDD.B (10 bytes), plus 2 bytes padding
	// These are lexicographically comparable.
	// Build ID can be '0' through '9' or 'A' through 'F' ('0' is unusual).
	// Chunks, in any order.
	// Total length of chunks, in words.
	// Signature.
	// Checksum for entire ROM, including rest of footer.

	// Old ROMS lacking a footer (signature absent) will get all data set null.

	// Chunk format:
	// Chunk identifier (4 bytes ASCII; interpreted as a little-endian int for
	// ease of comparison).
	// Length of chunk (4 bytes little-endian), in words.
	// Data.

	// The free space list. A list of begin-end pairs, sorted by
	// location in ascending order. (begin, end] is free.
	private List<int[]> freeSpaceList = new ArrayList<int[]>();

	// Hextator: GBA_HARDWARE_OFFSET means there's NOT an array yet,
	// but null means there's not even a reference to the potential
	// array
	private ROM.Pointer spellArrayAddress;

	private void initSpellArrayAddress(ROM rom, byte[] data) {
		if (data.length != 4) { throw new RuntimeException("Invalid chunk!"); }
		// Convert the 4 bytes to an int, little-endian
		int result = 0;
		for (int i = 0; i < 4; ++i) {
			result |= data[i] << (i * 8);
		}
		spellArrayAddress = rom.new Pointer(result);
	}

	private void initFreeSpace(byte[] data) {
		int position = 0;
		for (int i = 0; i < data.length; ) {
			if (data[i] == 0) { break; } // We hit padding.
			int positionDeltaBytes = data[i] & 0x03;
			int positionDelta = data[i] & 0xFC;
			for (int j = 0; j < positionDeltaBytes; ++j) {
				positionDelta |= (data[i + j + 1] & 0xFF) << ((j + 1) * 8);
			}
			position += positionDelta;
			i += positionDeltaBytes + 1;

			int sizeBytes = data[i] & 0x03;
			int size = data[i] & 0xFC;
			for (int j = 0; j < sizeBytes; ++j) {
				size |= (data[i + j + 1] & 0xFF) << ((j + 1) * 8);
			}
			i += sizeBytes + 1;

			// Old versions might have added zero-length freespace blocks to the
			// list. Filter them out; they're useless.
			if (size != 0) {
				System.out.println(
					String.format(
						"INITIAL FREESPACE BLOCK FOUND: %08X %08X",
						position, position + size
					)
				);
				freeSpaceList.add(new int[] { position, position + size });
			}
			position += size;
		}
	}

	// TODO: optimize
	public int addFreeSpace(int begin, int end, boolean conserve) {
		// Align the added space conservatively.
		if (conserve) {
			begin = (begin + 3) & ~3;
			end &= ~3;
		}

		// Don't add an entry for a zero-length block.
		if (begin >= end) { return 0; }

		System.out.println(String.format(
			"MARK FREE SPACE: %08X TO %08X", begin, end
		));
		final int output = end - begin;

		boolean added = false;
		ArrayList<int[]> newFreeSpace = new ArrayList<int[]>();
		for (int[] entry: freeSpaceList) {
			if (entry[1] < begin) {
				newFreeSpace.add(entry); // entirely before.
			} else if (entry[0] > end) {
				// entirely after. Now is the time to write the
				// added chunk, if it hasn't been added yet.
				if (!added) {
					newFreeSpace.add(new int[] { begin, end });
					added = true;
				}
				newFreeSpace.add(entry);
			} else {
				// Chunks overlap; perform a union.
				if (entry[0] < begin) { begin = entry[0]; }
				if (entry[1] > end) { end = entry[1]; }
			}
		}

		if (!added) {
			// No freespace chunks were before the addition,
			// so we must add now.
			newFreeSpace.add(new int[] { begin, end });
		}

		freeSpaceList = newFreeSpace;
		return output;
	}

	// TODO: optimize
	public int removeFreeSpace(int begin, int end) {
		// Align the removed space conservatively.
		begin &= ~3;
		end = (end + 3) & ~3;

		System.out.println(String.format(
			"MARK USED SPACE: %08X TO %08X", begin, end
		));
		final int output = end - begin;

		ArrayList<int[]> newFreeSpace = new ArrayList<int[]>();
		for (int[] entry: freeSpaceList) {
			if (entry[1] <= begin) { // Switched from "<"
				newFreeSpace.add(entry); // entirely before.
			} else if (entry[0] >= end) { // Switched from ">"
				newFreeSpace.add(entry); // entirely after.
			} else {
				// Chunk overlaps the region; truncate it.
				// Hextator: This was mad broken before
				//entry[1] = begin;
				//entry[0] = end;
				int[] leftEntry = new int[] {
					entry[0],
					begin > entry[0] ? begin : entry[0]
				};
				int[] rightEntry = new int[] {
					end < entry[1] ? end : entry[1],
					entry[1]
				};
				if (leftEntry[0] < leftEntry[1]) {
					// Some space still remains; add that.
					newFreeSpace.add(leftEntry);
				}
				if (rightEntry[0] < rightEntry[1]) {
					// Some space still remains; add that.
					newFreeSpace.add(rightEntry);
				}
			}
		}

		freeSpaceList = newFreeSpace;
		return output;
	}

	// Find freespace of at least length bytes, remove it from the freespace,
	// and return the address where it was.
	// Zahlman: It makes more sense for padding to be requested explicitly,
	// because in addFreeSpace() we can't assume that there is any padding
	// (we could be freeing a stock game resource), but it also doesn't make
	// sense for the calling code to account for padding added here when it
	// calls addFreeSpace().
	public int allocate(int amount) {
		if (amount < 0) {
			throw new RuntimeException("Can't allocate a negative size!");
		}

		for (int[] entry: freeSpaceList) {
			if (entry[1] - entry[0] >= amount) {
				// We can allocate from this chunk. Do so.
				int result = entry[0];
				entry[0] += amount;
				return result;
			}
		}

		return -1;
	}

	public void listFreeSpace() {
		System.out.println("~~~ FREE SPACE LIST ~~~");
		for (int[] entry: freeSpaceList) {
			final int start = entry[0];
			final int end = entry[1];
			final int size = end - start;
			if (size != 0) {
				System.out.println(String.format(
					"%08X %08X", start, end
				));
			}
		}
		System.out.println("~~~ END OF FREE SPACE LIST ~~~");
	}

	// Returns a string representing the free space entries in a format
	// that is easily parsed
	public String printFreeSpace() {
		StringBuilder output = new StringBuilder("[Exhaustive]" + Util.newline);
		for (int[] entry: freeSpaceList) {
			final int start = entry[0];
			final int end = entry[1];
			final int size = end - start;
			// NOTE: Printing "size" instead of "end" this time
			if (size != 0) {
				output.append(String.format(
					"%08X%s%08X%s",
					start, Util.newline,
					size, Util.newline
				));
			}
		}
		return output.toString();
	}

	public int[][] getFreeSpace() {
		return Util.intArrayListToIntArrayArray(freeSpaceList);
	}

	public void handleChunk(ROM r, int type, byte[] data) {
		switch (type) {
			case FREE_SPACE_CHUNK:
			initFreeSpace(data);
			break;

			case SPELL_ARRAY_CHUNK:
			initSpellArrayAddress(r, data);
			break;

			default:
			handleCustomChunk(type, data);
			break;
		}
	}
	
	// Subclasses should override.
	protected void handleCustomChunk(int type, byte[] data) {}

	protected byte[][] serializedCustomChunks() { return new byte[0][]; }

	private int writeChunk(
		byte[] chunk, FileOutputStream stream, CRC32 crc, int count
	) throws IOException {
		int size = chunk.length;
		if ((size % 4) != 0) { throw new RuntimeException("bad chunk"); }
		stream.write(chunk);
		crc.update(chunk, 0, size);
		return count + (size / 4);
	}

	protected final byte[] chunkFromData(int type, byte[] data) {
		// Number of words needed to represent the data
		int size = (data.length + 3) / 4;
		byte[] result = new byte[size * 4 + 8];
		// Write size and type as little-endian.
		for (int i = 0; i < 4; ++i) {
			result[i] = (byte)(type & 0xFF);
			result[i + 4] = (byte)(size & 0xFF);
			type >>= 8;
			size >>= 8;
		}
		System.arraycopy(data, 0, result, 8, data.length);
		return result;
	}

	private void writeEncodedValue(ArrayList<Byte> dest, int src) {
		// Must be a word-aligned, positive value
		if ((src % 4) != 0) { throw new RuntimeException(); }
		if (src < 0) { throw new RuntimeException(); }

		// Figure out how many bytes are needed to represent src - dumbly
		int count = 0;
		if (src >= (1 << 8)) { count = 1; }
		if (src >= (1 << 16)) { count = 2; }
		if (src >= (1 << 24)) { count = 3; }

		// Bitwise OR the count into the LSB (for decoding), and write
		// count-many bytes, little-endian
		src |= count;
		for (int i = 0; i < (count + 1); ++i) {
			dest.add((byte)(src & 0xFF));
			src >>= 8;
		}
	}

	private byte[] serializeFreespaceList() {
		ArrayList<Byte> buffer = new ArrayList<Byte>();
		int iterator = 0;

		// Serialize each entry
		for (int[] entry: freeSpaceList) {
			int positionDelta = entry[0] - iterator;
			int size = entry[1] - entry[0];
			iterator = entry[1];
			writeEncodedValue(buffer, positionDelta);
			writeEncodedValue(buffer, size);
		}

		// Convert to byte[]
		int size = buffer.size();
		byte[] result = new byte[size];
		for (int i = 0; i < size; ++i) {
			result[i] = buffer.get(i);
		}
		return result;
	}

	private void writeLE(FileOutputStream stream, CRC32 crc, int value)
	throws IOException {
		byte[] data = new byte[4];
		for (int i = 0; i < 4; ++i) {
			data[i] = (byte)(value & 0xFF);
			value >>= 8;
		}
		writeChunk(data, stream, crc, 0);
	}

	// Given the checksum of a ROM that was just written, and the
	// stream to which it was written, write the footer, update
	// the checksum with the footer, and write the checksum.
	// The calling code will catch exceptions and clean up the stream.
	// Returns the checksum
	public int save(
		FileOutputStream stream, CRC32 crc, String version
	) throws IOException {
		// Convert version string to byte[12] including padding
		// This is the current editor version, not the one that was loaded
		byte[] versionBytes = Arrays.copyOf(version.getBytes(), 12);
		writeChunk(versionBytes, stream, crc, 0);

		int totalChunkWords = 0;

		if (spellArrayAddress != null) {
			// Convert it to a byte[4] and make a chunk for it.
			int STA_Int = spellArrayAddress.toInt();
			byte[] STA_Bytes = new byte[4];
			// We have to make sure this is little-endian...
			for (int i = 0; i < 4; ++i) {
				STA_Bytes[i] = (byte)(STA_Int & 0xFF);
				STA_Int >>= 8;
			}
			totalChunkWords = writeChunk(
				chunkFromData(SPELL_ARRAY_CHUNK, STA_Bytes),
				stream, crc, totalChunkWords
			);
		}

		// Create a freespace chunk.
		totalChunkWords = writeChunk(
			chunkFromData(FREE_SPACE_CHUNK, serializeFreespaceList()),
			stream, crc, totalChunkWords
		);

		// Create all custom chunks.
		for (byte[] chunk: serializedCustomChunks()) {
			totalChunkWords = writeChunk(chunk, stream, crc, totalChunkWords);
		}

		// Write the footer length, signature and checksum, all little-endian.
		// 0x7FFFFFFF was being used to keep the value from being negative.
		// Values that were positive and greater would indicate a scenario
		// that is infeasible.
		// However, this is silly so it has been removed.
		writeLE(stream, crc, totalChunkWords /*& 0x7FFFFFFF*/);
		writeLE(stream, crc, SIGNATURE);
		int output = (int)(crc.getValue());
		writeLE(stream, crc, output);
		return output;
	}
}

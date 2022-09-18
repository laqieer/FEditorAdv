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
 *  <Description> Interface to ROMs, with wrappers to describe FE games
 *  specifically.
 */

package Model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;

public class ROM {
	private String pathName;
	private byte[] data;
	private boolean saved = true;

	public static final int GBA_HARDWARE_OFFSET = 0x08000000;
	public static final int MARK = 0x80000000;

	class AlignmentException extends RuntimeException {}

	// A pseudo-iterator over the ROM.
	public class Pointer {
		private int location;
		public boolean marked;

		public Pointer(int address) {
			int pointer = address - GBA_HARDWARE_OFFSET;
			location = pointer & ~MARK;
			marked = (pointer & MARK) != 0;
			if (location < 0 || location > data.length) {
				throw new IllegalArgumentException(
					Util.methodName() + ": "
					+ "address out of bounds - "
					+ String.format("0x%08X", address)
				);
			}
		}

		public Pointer aligned() { return new Pointer((location & ~3) + GBA_HARDWARE_OFFSET); }

		public ROM rom() { return ROM.this; }

		public int getLocation() { return location; }

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Pointer)) return false;
			Pointer p = (Pointer)other;
			return (
				rom() == p.rom() && location == p.location && marked == p.marked
			);
		}

		@Override
		public int hashCode() {
			return rom().hashCode() ^ location ^ (marked ? 1 : 0); 
		}

		@Override
		public String toString() {
			return String.format("<%s to byte 0x%X in byte[] %s>", marked ? "Marked pointer" : "Pointer", location, data);
		}

		public int toInt() {
			int result = location + GBA_HARDWARE_OFFSET;
			if (marked) result |= MARK;
			return result;
		}

		public int asInt() {
			return location + GBA_HARDWARE_OFFSET;
		}

		public boolean atEnd() {
			return location == data.length;
		}

		public byte nextByte() {
			return data[location++];
		}
		public byte currentByte() {
			return data[location];
		}
		public void writeByte(byte value) {
			data[location++] = value;
			saved = false;
		}
		public byte[] getBytes(int count) {
			byte[] result = new byte[count];
			for (int i = 0; i < count; ++i) { result[i] = nextByte(); }
			return result;
		}
		public void writeBytes(byte[] data) {
			for (int i = 0; i < data.length; ++i) { writeByte(data[i]); }
		}

		public short nextShort() {
			if ((location % 2) != 0) { throw new AlignmentException(); }
			short result = 0;
			for (int shift = 0; shift < 16; shift += 8) {
				result |= (data[location++] & 0xff) << shift;
			}
			return result;
		}
		public short currentShort() {
			short result = nextShort();
			location -= 2;
			return result;
		}
		public void writeShort(short value) {
			if ((location % 2) != 0) { throw new AlignmentException(); }
			for (int i = 0; i < 2; ++i) {
				writeByte((byte)value);
				value >>= 8;
			}
		}
		public short[] getShorts(int count) {
			short[] result = new short[count];
			for (int i = 0; i < count; ++i) { result[i] = nextShort(); }
			return result;
		}
		public void writeShorts(short[] data) {
			for (int i = 0; i < data.length; ++i) { writeShort(data[i]); }
		}

		public int nextInt() {
			if ((location % 4) != 0) { throw new AlignmentException(); }
			int result = 0;
			for (int shift = 0; shift < 32; shift += 8) {
				result |= (data[location++] & 0xff) << shift;
			}
			return result;
		}
		public int currentInt() {
			int result = nextInt();
			location -= 4;
			return result;
		}
		public void writeInt(int value) {
			if ((location % 4) != 0) { throw new AlignmentException(); }
			for (int i = 0; i < 4; ++i) {
				writeByte((byte)value);
				value >>= 8;
			}
		}
		public int[] getInts(int count) {
			int[] result = new int[count];
			for (int i = 0; i < count; ++i) {
				result[i] = nextInt();
			}
			return result;
		}
		public void writeInts(int[] data) {
			for (int i = 0; i < data.length; ++i) { writeInt(data[i]); }
		}

		public Pointer deref() {
			return new Pointer(currentInt());
		}

		public void advance(int distance) {
			location += distance;
		}

		public Pointer offsetBy(int distance) {
			return new Pointer(location + distance + GBA_HARDWARE_OFFSET);
		}

		public boolean isPointer(int address)
		{
			int pointer = address - GBA_HARDWARE_OFFSET;
			int location = pointer & ~MARK;
			if (address < GBA_HARDWARE_OFFSET || address >= 0x0A000000) {
				return false;
			}
			else
			{
				return true;
			}
		}
	}

	private ROM(File file, String pathName) throws IOException {
		FileInputStream stream = null;
		try {
			data = new byte[(int)(file.length())];
			stream = new FileInputStream(pathName);
			stream.read(data);
			this.pathName = pathName;
		} finally {
			try { stream.close(); }
			catch (Exception e) {}
		}
	}

	public ROM(String pathName) throws IOException {
		this(new File(pathName), pathName);
	}

	public ROM(File file) throws IOException {
		this(file, file.getPath());
	}

	public boolean isSaved() { return saved; }

	public int checksum(int begin, int length) {
		CRC32 crc = new CRC32();
		crc.update(data, begin, length);
		return (int)crc.getValue();
	}

	public int checksum() { return checksum(0, data.length); }

	// Returns the checksum
	public int save(String pathName, Metadata metadata, String version) {
		int output = -1;

		// Check whether this is a "save" or a "save as"
		if (pathName == null) { pathName = this.pathName; }

		FileOutputStream outputFile = null;
		try {
			outputFile = new FileOutputStream(pathName);
			outputFile.write(data);
			CRC32 crc = new CRC32();
			crc.update(data, 0, data.length);
			
			if (metadata != null) {
//7743 killmetadata!!
//				output = metadata.save(outputFile, crc, version);
			}

			saved = true;
			// If this was a "save as", ensure that future "save" requests use the
			// new name
			this.pathName = pathName;
		} catch (IOException e) {
			// Couldn't write the file? Oh Well. Nothing we can do to recover.
		} finally {
			// It should be impossible for closing a file to fail, and there's
			// certainly nothing we can do about a failure if it does happen. But
			// that's how Java defined the API...
			try { outputFile.close(); }
			catch (IOException e) {}
		}
		return output;
	}

	public byte[] getBytesAt(int location, int count) {
		return new Pointer(location).getBytes(count);
	}
	public void writeBytesAt(int location, byte[] data) {
		new Pointer(location).writeBytes(data);
	}

	public short[] getShortsAt(int location, int count) {
		return new Pointer(location).getShorts(count);
	}
	public void writeShortsAt(int location, short[] data) {
		new Pointer(location).writeShorts(data);
	}

	public int[] getIntsAt(int location, int count) {
		return new Pointer(location).getInts(count);
	}
	public void writeIntsAt(int location, int[] data) {
		new Pointer(location).writeInts(data);
	}

	public int size() { return data.length; }

	private void doExpansion(int byteCount) {
		// Round up to nearest word
		data = java.util.Arrays.copyOf(data, data.length + ((byteCount + 3) & ~3));
	}

	// Remove some bytes from the end that represent a checksum or footer.
	// Don't count this as a change to the ROM.
	public void removeMetadata(int byteCount) {
		doExpansion(-byteCount);
	}

	// Add padding to the end of the ROM. Return a Pointer to
	// where the ROM used to end, if we expanded the ROM.
	public Pointer expand(int byteCount) {
		if (byteCount < 0) { throw new RuntimeException("Negative expansion amount"); }
		int oldEnd = data.length;
		doExpansion(byteCount);
		saved = false;
		return new Pointer(oldEnd + GBA_HARDWARE_OFFSET);
	}

	// Number of 0xf0f0f0f0 ints expected on either side of a freespace block.
	public static final int LEGACY_PADDING_REQUIREMENT = 4;
	public static final int LEGACY_FREESPACE_WORD = 0xF0F0F0F0;

	// Look for long blocks of 0xF0 bytes and add them to the metadata's freelist.
	public void findFreespace_legacy(Metadata metadata) {
		Pointer p = new Pointer(GBA_HARDWARE_OFFSET);

		int wordsFound = 0;
		while (!p.atEnd()) {
			if (p.nextInt() != LEGACY_FREESPACE_WORD) {
				findFreespace_helper(metadata, wordsFound, p.offsetBy(-4));
				wordsFound = 0;
			} else {
				wordsFound++; 
			}
		}
		findFreespace_helper(metadata, wordsFound, p);
	}

	private void findFreespace_helper(
		Metadata metadata, int wordsFound, ROM.Pointer p
	) {
		if (wordsFound > 2 * LEGACY_PADDING_REQUIREMENT) {
			// The padding is considered a part of freespace.
			metadata.addFreeSpace(
				p.location - wordsFound * 4, p.location, true
			);
		}
	}

	// NOTE: There were some utility functions here for repointing pointer
	// array handles, but they didn't really belong here. The functionality is
	// now provided by Game.
}

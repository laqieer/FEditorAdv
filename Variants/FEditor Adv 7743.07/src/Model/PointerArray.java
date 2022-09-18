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
 *  <Description> Parent class for most models.
 */

package Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class PointerArray {
	protected Game owner;
	private ROM.Pointer handle, defaultBase, currentBase;
	private int intsPerRecord;
	private int originalSize;
	// This ought to be a Vector to get automatic resizing,
	// but it's just so inconvenient converting the data back and forth.
	private int[] arrayData;
	private int arrayLength;
	private int position = 0;
	private int offset;

	private boolean saved = true;

	// Pointer arrays can be in either an "original" or "relocated" state.
	// When the array is in the original position in the ROM, its size is known;
	// it's the default_size. When it has been relocated, the pointer to the
	// beginning of the array is actually offset an int from the allocation; this
	// int holds the allocation size (number of array entries that can be held).

	// FIXME: Use ints here instead of ROM.Pointers?

	// FIXME: This might not be a good idea
	protected PointerArray(Game owner) { this.owner = owner; }

	public PointerArray(
		Game owner,
		ROM.Pointer handle, ROM.Pointer default_base,
		int default_size, int ints_per_record,
		int offset // added to base to get actual array position
	) {
		this.offset = offset;

		this.owner = owner;
		this.handle = handle;
		this.defaultBase = default_base;
		this.intsPerRecord = ints_per_record;
		currentBase = handle.deref();
		ROM.Pointer p;
		//int current_size;

		//Redesign to make it easier to import data from other remodeling.
		//by 7743
		//We change the algorithm.

		if (false && !relocated()) {
			System.out.println(
				String.format("%s: not relocated currentBase:%08X defaultBase:%08X ",getClass(),currentBase.toInt(),defaultBase.toInt() )
			);

			// This is the original array, so its size is known.
			p = default_base.offsetBy(offset);
			arrayLength = default_size;
		} else {
			// The array has been relocated. Apply reverse-compatibility
			// fixes if needed.
			switch (owner.getType()) {
				case Game.NEW_VERSIONS:
				// Read the array length that was prepended to the array.
				p = currentBase.offsetBy(offset - 4);
				arrayLength = p.nextInt();
				if (arrayLength != ROM.LEGACY_FREESPACE_WORD)
					break;
				// Oops!

				case Game.OLD_VERSIONS:
				System.out.println(getClass() + ": FIXING RELOCATED ARRAY");
				// We get here via a patching routine called by Game.
				// We fix up ROM contents (and mark a couple extra bits of freespace)
				// so that later, we can carry on as if this were a NEW_VERSIONS ROM.

				// The array size isn't recorded in the ROM, but we know the size
				// because expanded sizes were hard-coded in old versions.
				arrayLength = expandedSize_legacy();

				// If this is an offset array, the space up until the offset is
				// garbage and can be safely marked as freespace, thanks to how the old
				// array handling worked. Whether or not that's true, there should be 16
				// bytes of padding (not yet recognized as free space, but known to be
				// free) in front of the array.
				
				// First, mark the free space in front of the array that we know about,
				// except for the last 3 words (which the NEW_VERSIONS array assumes
				// ownership of).
				p = currentBase.offsetBy(offset - 12);
				owner.markFreeSpace(currentBase.offsetBy(-16), p);

				// There are also 4 extra words at the end of the array that are free.
				// However, these can be expected to be picked up either by
				// ROM.markFreespace_legacy() (if there's more free space following),
				// or by the next pointer array's adjustments (if the old ROM had
				// another pointer array immediately after this one). We do lose
				// the space if the old ROM wrote something other than a pointer
				// array immediately after the current pointer array.
				
				// To make it look just as if a NEW_VERSIONS ROM had written the data,
				// put 0 words in the space used by NEW_VERSIONS as padding at the start.
				p.writeInt(0);
				p.writeInt(0);

				// Then, write the last word in place
				System.out.println(String.format(
					"WRITING ARRAY SIZE %08X AT %08X",
					arrayLength, p.toInt()
				));
				p.writeInt(arrayLength);
				break;

				case Game.HACKED:
// We are NEVER give up!!
//					// Give up for now
//					throw new RuntimeException(
//						"This ROM appears to have been hacked outside FEditor (wrong " +
//						"checksum) and the pointer array has been relocated... FEditor " +
//						"can't handle this properly since the array size is unknown."
//					);
				
				System.out.println("This ROM appears to have been hacked outside FEditor!! We are follow the pointer!");
				
				p = currentBase.offsetBy(offset - 4);
				arrayLength = p.nextInt();

				if (arrayLength <= 0 || arrayLength > 0xFFFF)
				{
					System.out.println(String.format(
						"ARRAY SIZE %d is BAD. We are follow the pointer!",
						arrayLength
					));
					arrayLength = default_size;
				}
				System.out.println(String.format(
					"ARRAY SIZE %08X AT %08X",
					arrayLength, p.toInt()
				));
				break;

				default: // Game.CLEAN
				throw new RuntimeException(
					"ROM recognized as clean but a pointer array has been relocated - " +
					"this should be impossible"
				);
			}
		}

		//We also look at measured values.
		//We implement the length properly so that other remodeling data can be seen.
		//by 7743
		int realLength = searchDataCount(p.getLocation());
		if (realLength > arrayLength)
		{//If the measured value is larger, the measured value is adopted.
			System.out.println(String.format(
				"MAX(arrayLength: %d,realLength:%d) Since the real measured value is larger, the data is accessed with the actual measurement length.",
				arrayLength, realLength
			));
			arrayLength = realLength;
		}
		
		int amount_to_read = ints_per_record * arrayLength;
		arrayData = p.getInts(amount_to_read);
		resize(arrayLength);

		originalSize = arrayLength;
	}

	public PointerArray(
		Game owner,
		ROM.Pointer handle, ROM.Pointer default_base,
		int default_size, int ints_per_record
	) {
		this(owner, handle, default_base, default_size, ints_per_record, 0);
	}

	public boolean isSaved() { return saved; }

	public int getOriginalSize() { return originalSize; }

	public int getCurrentSize() { return arrayLength; }

	public void resize(int size) {
		arrayLength = size;
		int allocation_size = Integer.highestOneBit(arrayLength) * 2 * intsPerRecord;
		if (allocation_size != arrayData.length) {
			arrayData = Arrays.copyOf(arrayData, allocation_size);
		}
		// FIXME: When the array shrinks, zero out entries beyond arraySize,
		// and deallocate resources.
	}

	protected void fixPointers(ROM.Pointer oldP, ROM.Pointer newP) {
		int oldPtrVal = oldP.toInt();
		int newPtrVal = newP.toInt();
		ROM.Pointer traverse = owner.getPointer(ROM.GBA_HARDWARE_OFFSET);
		try {
			while (!traverse.atEnd()) {
				int curr = traverse.currentInt();
				if (curr == oldPtrVal)
					traverse.writeInt(newPtrVal);
				else
					traverse.nextInt();
			}
		} catch (Exception e) {}
	}

	public void save() {
		int array_ints = arrayLength * intsPerRecord;
		int original_ints = originalSize * intsPerRecord;

		ROM.Pointer array_start = currentBase.offsetBy(offset);

		System.out.println(String.format(
			"Saving pointer array: old size - %s, new size - %s",
			Integer.toString(originalSize, 16).toUpperCase(),
			Integer.toString(arrayLength, 16).toUpperCase()
		));

		// If the array size hasn't changed, write the array
		// data itself in place.
		// FIXME: It would probably be okay when the array length gets *smaller* to
		// just write the ints and return the excess space to the freespace list...
		if (arrayLength == originalSize) {
			array_start.writeInts(Arrays.copyOf(arrayData, array_ints));
			saved = true;
			return;
		}

		// Otherwise, we need to find a new chunk for the data.
		final int words_before = 3; // 2 of padding plus a length count

		// First, clean up the old allocation.
		owner.markFreeSpace(
			array_start.offsetBy(relocated() ? (words_before * -4) : 0),
			array_start.offsetBy(4 * original_ints)
		);

		// Prepare the data to be written: prepend a size count,
		// and put padding on either side. This matches the padding done
		// by slightly older versions, so there should be no compatibility
		// issue.
		int[] prefixedData = new int[array_ints + words_before];
		// Indices 0, 1, array_ints + 3 and array_ints + 4 are padding.
		prefixedData[words_before - 1] = arrayLength;
		System.arraycopy(arrayData, 0, prefixedData, words_before, array_ints);

		// Write the prefixed and padded array to freespace, and set the
		// handle to point to the beginning of the actual array, subtracting
		// the offset as necessary.
		ROM.Pointer oldP = handle.deref();
		ROM.Pointer newP = owner.writeAndRepoint(
			prefixedData, handle.getLocation(), false, 4 * words_before - offset
		);
		// Hextator sez: This should not have been removed. The
		// pointers used as handles for potentially moved arrays
		// were chosen arbitrarily; all pointers, not just the
		// chosen one, must be replaced. This seems dangerous, but
		// it is the safer assumption of how to handle this ambiguous
		// situation than leaving so many potential pointers non-updated
		// and potentially risking invalid dereferences in game.
		fixPointers(oldP, newP);
		saved = true; // How did this say 'false' before? Clearly a bug.
	}

	public boolean relocated() {
		return !currentBase.equals(defaultBase);
	}

	public void moveTo(int newPosition) {
		if (newPosition < minIndex() || newPosition >= arrayLength) {
			throw new IllegalArgumentException();
		}
		position = newPosition;
	}

	public void next() {
		position += 1;
		if (position >= arrayLength) { position = minIndex(); }
	}

	public void prev() {
		position -= 1;
		if (position < minIndex()) { position = arrayLength; }
	}

	public int getPosition() { return position; }

	// Hextator: Returns a reference to the index'th element of the array
	public ROM.Pointer getElementRef(int index) {
		if (index < minIndex() || index >= arrayLength) { return null; }
		return currentBase.offsetBy(index * intsPerRecord * 4);
	}

	// FIXME: This all needs refactoring.

	// Take a pointer to the beginning of a resource, and advance it to
	// just past the end of the same resource. We use this to mark
	// [begin, end) as freespace, passing a copy of begin to find end.
	// The 'index' indicates which pointer in an array entry is being used;
	// this information helps determine what kind of compression is involved.
	abstract protected void moveToEndOfData(ROM.Pointer p, int index);

	// Take the raw byte[] data for a resource, compress it if necessary
	// (or make any other modifications - e.g. adding a mug header for FE8)
	// and return the final data.
	abstract protected byte[] prepare(byte[] newData, int index);

	// Determines whether or not to set a flag on pointers that are pointed at
	// new data created by FEditor. Currently, the text module sets this to true
	// since new text is not Huffman-compressed while original game text is.
	// Other modules set it false since they respect the game's original
	// compression schemes.
	// TODO: (way in the future) set up ASM code that interprets a few flags
	// and determines a compression method, and hook everything in the existing
	// code into that instead of having it make hard-coded assumptions about
	// compression methods.
	abstract protected boolean markNewPointer();

	public ROM.Pointer getPointer(int index) {
		if (index < 0 || index >= intsPerRecord) { return null; }
		return owner.getPointer(arrayData[position * intsPerRecord + index]);
	}

	protected boolean isPointer(int index) { return true; }

	protected boolean isDoublePointer(int index) { return false; }

	public int getRecordSize() { return intsPerRecord; }

	// Intended for non-pointers!
	protected void writeValue(int index, int value) {
		saved = false;
		arrayData[position * intsPerRecord + index] = value;
	}

	// Intended for non-pointers!
	protected int readValue(int index) {
		return arrayData[position * intsPerRecord + index];
	}

	// XXX This shouldn't be used. It's unnecessary and dangerous.
	protected void clear() {
		for (int i = 0; i < intsPerRecord; ++i) {
			if (isPointer(i)) { setData(i, null); }
			else { writeValue(i, 0); }
		}
	}

	private boolean refCollision(int index) {
		if (!handleCollisions())
			return false;
		ROM.Pointer ref = getPointer(index);
		if (ref == null) return false;
		int currPos = position;
		int occurrences = 0;
		for (position = minIndex(); position < arrayLength; position++) {
			try {
				int compare = getPointer(index).toInt();
				if (compare == ref.toInt())
					occurrences++;
			} catch (Exception e) { continue; }
		}
		position = currPos;
		return occurrences > 1;
	}

	// Set a pointer in the index'th element of the current array entry
	// to point at the new data. This method should only be used for indices
	// that actually represent a pointer.
	public void setData(int index, byte[] newData) {
		// XXX The word 'index' means two things in this code, and that's Bad.
		// The new setup will sidestep this by introducing moar objectz.
		if (index < 0 || index >= intsPerRecord) {
			throw new IllegalArgumentException();
		}
		System.out.println("setData1");
		// Free existing data referred to by the pointer.
		ROM.Pointer begin = getPointer(index);
		if (begin != null && !refCollision(index)) {
			System.out.println("setData2");
			ROM.Pointer end = getPointer(index);
			moveToEndOfData(end, index);
			owner.markFreeSpace(begin, end);
		}
		System.out.println("setData3");

		if (newData != null) {
			System.out.println("setData4");
			ROM.Pointer writeLocation = owner.write(
				prepare(newData, index), markNewPointer()
			);
			writeValue(index, writeLocation.toInt());
		} else {
			System.out.println("setData5");
			// There is nothing to write; blank out the array entry.
			writeValue(index, 0);
		}
	}

	// Tested and working
	public int[][] usedSpace() {
		List<int[]> output = new ArrayList<int[]>();
		for (int i = minIndex(); i < getCurrentSize(); i++) {
			moveTo(i);
			for (int j = 0; j < getRecordSize(); j++) {
				if (!isPointer(j)) continue;
				ROM.Pointer start = getPointer(j);
				// This might occur when processing
				// spell animation entries because the array
				// for custom spell animations was not
				// originally in the game
				if (start == null) continue;
				ROM.Pointer end = start.offsetBy(0);
				if (isDoublePointer(j)) {
					// Temporarily clear free space
					int[][] original = owner.getFreeSpace();
					owner.unmarkFreeSpace(
						owner.getPointer(ROM.GBA_HARDWARE_OFFSET),
						owner.getPointer(
							owner.r.size()
							+ ROM.GBA_HARDWARE_OFFSET
						)
					);
					moveToEndOfData(end, j);
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
							curr[0], curr[1]
						});
					}
				}
				else {
					moveToEndOfData(end, j);
				}
				int startAsInt = start.asInt() - ROM.GBA_HARDWARE_OFFSET;
				int endAsInt = end.asInt() - ROM.GBA_HARDWARE_OFFSET;
				output.add(new int[] { startAsInt, endAsInt });
			}
		}
		return Util.intArrayListToIntArrayArray(output);
	}

	// Tested and working
	public int[][] usedArraySpace() {
		int asInt = currentBase.asInt();
		 asInt -= ROM.GBA_HARDWARE_OFFSET;
		return new int[][] { {
			asInt,
			asInt + (arrayLength * intsPerRecord)
		} };
	}

	// Maximum size for array expansion. Overridden in some cases.
	public int maxSize() { return 0xFF; }

	// The size that FEditor would expand arrays to. The aliasing here is
	// deliberate so that functions calls have a name appropriate to the purpose.
	private int expandedSize_legacy() { return maxSize(); }

	// Minimum index for array access. In some cases, index 0 is
	// "valid" but prohibited from editing. This is done for spell animations
	// (since spell 0 is an important safeguard - a dummy spell) and text
	// (since the empty string in index 0 probably shouldn't be modified)
	// but NOT for portrait arrays (since portrait index should not be
	// moved with the array; it belongs to the dummy portrait data).
	public int minIndex() { return 0; }

	// This should be overridden for pointer arrays that don't have
	// entries that share resources to return false
	public boolean handleCollisions() { return true; }

	// This function search the data count
	// default is the 0
	// Please make a search to extends
	public int searchDataCount(int startPointer)
	{
		return 0;
	}
}

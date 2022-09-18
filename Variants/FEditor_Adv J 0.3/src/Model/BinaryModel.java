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
 *  <Description> This class details an interface for working with raw binary
 *  data going into or coming out of the ROM as byte arrays with free space
 *  management options provided
 */

package Model;

import java.io.File;
import Compression.LempelZiv;
import Model.ROM.Pointer;

public class BinaryModel extends PointerArray {
	public BinaryModel(Game owner) { super(owner); }

	@Override
	protected void moveToEndOfData(Pointer p, int index) {}

	@Override
	protected byte[] prepare(byte[] newData, int index) {
		return null;
	}

	@Override
	protected boolean markNewPointer() { return false; }

	// Decompression type is Lempel Ziv
	// Decompression is attempted if and only if "length == null"
	public byte[] getData(int start, Integer length, boolean free) {
		byte[] output;
		ROM.Pointer startPointer = owner.getPointer(start);
		if (length == null) {
			ROM.Pointer endPointer = startPointer.offsetBy(0);
			try {
				output = LempelZiv.decompress(endPointer);
				if (output == null)
					throw new Exception();
			} catch (Exception e) {
				throw new RuntimeException(
					"データが読み込めません"
				);
			}
			if (free)
				owner.markFreeSpace(startPointer, endPointer);
			return output;
		}
		ROM.Pointer endPointer = startPointer.offsetBy(length);
		if (free)
			owner.markFreeSpace(startPointer, endPointer);
		return startPointer.getBytes(length);
	}

	public void listFreeSpace() { owner.listFreeSpace(); }

	public String printFreeSpace() {
		return owner.printFreeSpace();
	}

	public void parseFreeSpace(File toParse) {
		owner.parseFreeSpace(toParse);
	}

	// Compression type is Lempel Ziv
	// Returns address that data was written to
	public int setData(
		Integer refAddr, byte[] data,
		boolean all, boolean compress
	) throws Exception {
		if (compress)
			data = LempelZiv.compress(data);
		ROM.Pointer dest = owner.write(data, false);
		if (refAddr != null) {
			ROM.Pointer refPtr;
			try {
				refPtr = owner.getPointer(refAddr);
				refPtr.currentInt();
				if (all)
					fixPointers(refPtr.deref(), dest);
				else
					refPtr.writeInt(dest.toInt());
			} catch (Exception e) {
				int length = ((data.length + 3)/4) * 4;
				ROM.Pointer end = dest.offsetBy(length);
				owner.markFreeSpace(dest, end);
				throw new RuntimeException(
					"無効な参照アドレス; "
					+ "データの書き込みは中止されました"
				);
			}
		}
		System.out.println("Writing done by " + Util.methodName());
		return dest.toInt();
	}

	// Decompression type is Lempel Ziv
	// Decompression is attempted if and only if "end == null"
	private int allocationHelper(int start, Integer end, boolean free) {
		ROM.Pointer startPointer = owner.getPointer(start);
		ROM.Pointer endPointer = startPointer.offsetBy(0);
		if (end == null) {
			endPointer = startPointer.offsetBy(0);
			try {
				LempelZiv.decompress(endPointer);
				if (endPointer.toInt() == startPointer.toInt())
					throw new Exception();
			} catch (Exception e) {
				if (free)
					throw new RuntimeException("割り当てに失敗しました");
				else
					throw new RuntimeException("割り当て解除に失敗しました");
			}
		}
		else
			endPointer = owner.getPointer(end);
		try {
			if (free)
				return owner.markFreeSpace(startPointer, endPointer);
			else
				return owner.unmarkFreeSpace(startPointer, endPointer);
		} catch (Exception e) {
			if (free)
				throw new RuntimeException("割り当てに失敗しました.");
			else
				throw new RuntimeException("割り当て解除に失敗しました.");
		}
	}

	public int allocate(int start, Integer end) {
		return allocationHelper(start, end, true);
	}

	public int deallocate(int start, Integer end) {
		return allocationHelper(start, end, false);
	}

	public int getInt(int start) {
		ROM.Pointer ref = owner.getPointer(start + ROM.GBA_HARDWARE_OFFSET);
		return ref.currentInt();
	}

	public void putInt(int start, int val) {
		ROM.Pointer ref = owner.getPointer(start + ROM.GBA_HARDWARE_OFFSET);
		ref.writeInt(val);
	}

	@Override
	public int[][] usedSpace() {
		// This should be empty because this class wraps all data
		// anyway and has no certainty of what is "used"
		return new int[][] { { } };
	}
}

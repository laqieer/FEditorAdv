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
 *  <Description> Huffman compression/decompression interface.
 */

package Compression;

import java.util.Arrays;
import Model.ROM;

public class Huffman {
	// As a side effect, 'pointer' is advanced to the end of the compressed text.
	public static byte[] decompress(
		ROM.Pointer pointer,
		ROM.Pointer huffman_tree, ROM.Pointer root_node
	) {
		// The game reserves a buffer of this size so we copy that here for
		// compatibility.
		byte[] result = new byte[0x1000];
		int i = 0;

		if (pointer.marked) { // uncompressed; copy to null terminator
			while (pointer.currentByte() != 0) {
				result[i++] = pointer.nextByte();
			}
			// Include the null terminator in output!
			result[i++] = pointer.nextByte();
		} else {
			int bit = 0;
			ROM.Pointer node = root_node.offsetBy(0);
			byte current = pointer.nextByte();
			while (i < 0x1000) {
				short left = node.nextShort();
				short right = node.nextShort();
				if (right < 0) { // reached leaf.
					node = root_node.offsetBy(0);
					result[i++] = (byte)left;
					if ((left & 0xff00) != 0) { // two characters
						if (i != 0x1000) {
							result[i++] = (byte)(left >> 8);
						}
					} else if (left == 0) {
						break;
					}
				} else { // still in the tree; advance a bit.
					// NOTE: The streaming-in of the next byte must occur *here*.
					// Otherwise, if the input text uses a whole number of bytes, the
					// pointer will be advanced one extra time, which in turn means that
					// the length of the compressed data is misreported. That's bad
					// because it means that bytes can be marked as freespace when they
					// shouldn't be, which corrupts everything.
					if (bit == 8) {
						bit = 0;
						current = pointer.nextByte();
					}
					short offset = ((current & 1) == 0) ? left : right;
					node = huffman_tree.offsetBy(4 * offset);
					current >>= 1;
					bit++;
				}
			}
		}
		return Arrays.copyOf(result, i);
	}
}

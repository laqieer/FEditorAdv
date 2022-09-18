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
 *  <Description> This class provides an interface for interacting with
 *  the text data of the game; its main method is a text editor
 */

package Model;

import Compression.Huffman;

public class TextArray extends PointerArray {
	// The maximum length that FE allows for text items.
	public static final int TEXT_HEAP_SIZE = 0x00001000;

	private ROM.Pointer array_start, array_root;

	public TextArray(
		ROM.Pointer ptr_array_start, ROM.Pointer ptr_ptr_array_end,
		Game owner,
		ROM.Pointer handle, ROM.Pointer default_base,
		int default_size, int record_size
	) {
		super(owner, handle, default_base, default_size, record_size);

		array_start = ptr_array_start.deref();
		array_root = ptr_ptr_array_end.deref().deref();
	}

	// Turns FE formatted ASCII encoded text in a byte array into a
	// String with expanded control codes
	public String ASCIIToUnicode(byte[] data)
	{
		String output = "";

		for (int i = 0; i < data.length; i++)
		{
			switch (data[i])
			{
				case 0:
					output += "[X]";
					break;
				case 1:
					output += "\n";
					break;
				case 2:
					output += "[0x02]";
					break;
				case 3:
					output += "[A]";
					break;
				case 0x10:
					if (data.length >= i + 2 && data[i + 2] == (byte) 0xFF)
					{
						output += "[LoadFace]" + String.format("[0x%02X\\][0xFF]", data[i + 1]);
						i += 2;
					}
					else
					{
						output += "[LoadFace]" + String.format("[0x%02X\\][0x01]", data[i + 1]);
						i += 2;
					}
					break;
				case (byte) '[':
					output += "\\[\\";
					break;
				case (byte) 0x80:
					output += "[0x80" + String.format("%02X]", data[i + 1]);
					i++;
					break;
				default:
					if (data[i] < 0x20)
						output += "[0x" + String.format("%02X]", data[i]);
					else
						output += (char) data[i];
					break;
			}
		}
		return owner.expandControlCodes(output);
	}
	// ASCIIToUnicode method; tested and working!

	// Turns a String with expanded or condensed control codes into
	// an FE formatted ASCII encoded byte array of the same text
	public byte[] UnicodeToASCII(String input)
	{
		input = owner.condenseControlCodes(input);
		int byteCount = 0;
		int tempInt;
		short tempShort = 0;
		byte tempOutArray[] = new byte[input.length()];
		String tempString;

		for (int i = 0; i < input.length(); i++)
		{
			tempShort = (short) input.charAt(i);
			if (tempShort == (short) '[')
			{
				tempInt = input.indexOf("]", i);
				if (tempInt == -1)
				{
					tempOutArray[byteCount] = (byte) (tempShort & 0xFF);
					byteCount++;
				}
				else if (input.substring(i, tempInt + 1).indexOf("0x") != 1)
				{
					i = tempInt;
					continue;
				}
				else
				{
					tempString = input.substring(i + 3, tempInt);
					try
					{
						tempShort = (short) Integer.parseInt(tempString, 16);
					}
					catch (Exception e)
					{
						i = tempInt;
						continue;
					}
					if ((tempShort & 0xFF00) == 0x8000 || (tempShort & 0xFF00) == 0x1000)
					{
						tempOutArray[byteCount] = (byte) ((tempShort >> 8) & 0xFF);
						tempOutArray[byteCount + 1] = (byte) (tempShort & 0xFF);
						i += 7;
						byteCount += 2;
						if (tempOutArray[byteCount - 1] == (byte) 0)
							tempOutArray[byteCount - 1] = (byte) 0xFF;
						if (i + 1 >= input.length())
							tempOutArray[byteCount++] = (byte) 0;
					}
					else
					{
						tempOutArray[byteCount] = (byte) (tempShort & 0xFF);
						i += 5;
						byteCount++;
					}
				}
			}
			else if (tempShort == (short) '\\')
			{
				if (i + 1 < input.length())
				{
					i++;
					tempShort = (short) input.charAt(i);
					if (tempShort == (short) '[')
					{
						
						if (i + 1 < input.length())
						{
							i++;
							tempShort = (short) input.charAt(i);
							if (tempShort != (short) '\\')
							{
								tempInt = input.indexOf("]", i);
								if (tempInt != -1)
								{
									tempInt -= i;
									tempOutArray[byteCount] = (byte) '[';
									byteCount++;
									for (int j = 0; j < tempInt; j++)
									{
										tempOutArray[byteCount] = (byte) (tempShort & 0xFF);
										i++;
										byteCount++;
										tempShort = (short) input.charAt(i);
									}
									tempOutArray[byteCount] = (byte) ']';
									byteCount++;
								}
							}
							else
							{
								tempOutArray[byteCount] = (byte) '[';
								byteCount++;
							}
						}
					}
					else
					{
						tempOutArray[byteCount] = (byte) (tempShort & 0xFF);
						byteCount++;
					}
				}
			}
			else
			{
				tempOutArray[byteCount] = (byte) (tempShort & 0xFF);
				byteCount++;
			}
		}

		byte output[] = new byte[byteCount + 1];
		System.arraycopy(tempOutArray, 0, output, 0, byteCount);
		if (output.length > 1 && output[output.length - 2] == (byte) 0)
			output = java.util.Arrays.copyOf(output, output.length - 1);
		if (output.length > TEXT_HEAP_SIZE)
			output = java.util.Arrays.copyOf(output, TEXT_HEAP_SIZE);
		output[output.length - 1] = (byte) 0;
		return output;
	}
	// UnicodeToASCII method; tested and working!

	public String getText() {
		ROM.Pointer p = getPointer(0);
		return (p == null) ? null : ASCIIToUnicode(
			Huffman.decompress(p, array_start, array_root)
		);
	}
	// getText accessor; tested and working!

	@Override
	protected void moveToEndOfData(ROM.Pointer p, int index) {
		if (index != 0)
			throw new IllegalArgumentException(
				"invalid index " + index
			);

		if (p.marked) {
			// Data is raw; look for a null terminator
			while (p.nextByte() != 0) {}
			// The null terminator itself can also be included in free space.
			// So we advance the pointer one more time, to point just past that.
			// This is analogous to doing things like malloc(strlen(foo) + 1) in C.
			// Hextator sez: If you follow the logic you will see that
			// the "\0" already IS accounted for, and the line below
			// breaks both free space marking (it causes a byte
			// of unknown data to be invalidly marked free!) to be
			// broken as well as writing of text to the end of
			// "filled ROMs" without enough free space to be impossible.
			//p.nextByte();
		} else {
			Huffman.decompress(p, array_start, array_root);
		}
		// XXX The pointer must be moved to the next 32 bit
		// aligned address, but this class "doesn't know why", so
		// this is essentially magic
		while (p.marked && p.getLocation() % 4 != 0)
			p.nextByte();
	}

	// TODO: compress the data
	@Override
	protected byte[] prepare(byte[] newData, int index) {
		return newData;
	}

	@Override
	protected boolean markNewPointer() { return true; }

	// Convenience method for overwriting or inserting text quickly
	public void setText(String input) {
		/* Hextator: Deprecating this; will clear all occurrences
		// of terminator and manually append to ensure no data is
		// written that can't be freed
		if (input.indexOf("[X]") != input.length() - 3) {
			input += "[X]";
		}
		*/
		input = input.replace("[X]", "");
		input += "[X]";
		setData(0, UnicodeToASCII(input));
		System.out.println("Writing done by " + Util.methodName());
	}

	@Override
	public int maxSize() { return 0x10000; }

	@Override
	public int minIndex() { return 1; }

	@Override
	public boolean handleCollisions() { return false; }
}

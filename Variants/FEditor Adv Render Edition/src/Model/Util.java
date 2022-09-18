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
 *  <Description> Unclassified utility functions are held in this class
 */

package Model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Util {
	public static String newline = System.getProperty("line.separator");

	public static int parseInt(String input) {
		input = input.trim();
		int base = 10;
		if (input.indexOf("0x") == 0) {
			base = 16;
			input = input.substring(2);
		}

		return Integer.parseInt(input, base);
	}

	public static byte[] readAllBytes(File file)
	throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		if (length > Integer.MAX_VALUE) {
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int)length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (
			offset < bytes.length
			&& (numRead = is.read(
				bytes, offset, bytes.length-offset
			)) >= 0
		) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException(
				"Could not completely read file "
				+ file.getName()
			);
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	public static void writeAllBytes(File toWrite, byte[] bytes)
	throws IOException {
		try {
			FileOutputStream fos = new FileOutputStream(toWrite);
			fos.write(bytes);
			fos.close();
		} catch(FileNotFoundException ex) {
			// XXX What should be done here?
		}
	}

	public static String methodName() {
		StackTraceElement lastElement =
			(new Exception()).getStackTrace()[1];
		return lastElement.getClassName()
			+ "." + lastElement.getMethodName();
	}

	public static String previousMethodName() {
		StackTraceElement elementBeforeLast =
			(new Exception()).getStackTrace()[2];
		return elementBeforeLast.getClassName()
			+ "." + elementBeforeLast.getMethodName();
	}

	public static String verboseReport(String report) {
		return previousMethodName() + ":\n\t" + report.replaceAll("\n", "\n\t");
	}

	public static /*<E extends Exception>*/ String verboseException(
		Exception e //E e
	) {
		if (e == null) return null;
		StackTraceElement[] stackTrace = e.getStackTrace();
		StackTraceElement exceptSource = stackTrace[0];
		String methodName = exceptSource.getClassName()
			+ "." + exceptSource.getMethodName();
		String exceptMessage = e.getMessage();
		if (exceptMessage == null || e.getMessage().equals(""))
			exceptMessage = "Unknown cause";
		String className = e.getClass().getSimpleName();
		if (className == null || className.equals(""))
			className = "(Anonymous Exception Type)";
		exceptMessage = "\t" + className + "\n"
			+ "\t" + exceptMessage.replaceAll("\n", "\n\t");
		String message = methodName + ":\n" + exceptMessage;
		return message;
	}

	// Some routines for repacking arrays. Sadly, templates cannot be applied here.
	// I tried. Many approaches. -Zahlman
	// Streams won't work here, either, because we want to interpret the data
	// as little-endian.

	public static byte[] bytesToBytes(byte[]... arrays) {
		int size = 0;
		for (byte[] array: arrays) { size += array.length; }
		byte[] result = new byte[size];

		int position = 0;
		for (byte[] array: arrays) {
			System.arraycopy(array, 0, result, position, array.length);
			position += array.length;
		}

		return result;
	}

	public static byte[] shortsToBytes(short[]... arrays) {
		int size = 0;
		for (short[] array: arrays) { size += array.length; }
		byte[] result = new byte[size * 2];

		int position = 0;
		for (short[] array: arrays) {
			for (short value: array) {
				result[position++] = (byte)(value);
				result[position++] = (byte)(value >> 8);
			}
		}

		return result;
	}

	public static byte[] intsToBytes(int[]... arrays) {
		int size = 0;
		for (int[] array: arrays) { size += array.length; }
		byte[] result = new byte[size * 4];

		int position = 0;
		for (int[] array: arrays) {
			for (int value: array) {
				result[position++] = (byte)(value);
				result[position++] = (byte)(value >> 8);
				result[position++] = (byte)(value >> 16);
				result[position++] = (byte)(value >> 24);
			}
		}

		return result;
	}

	public static short[] bytesToShorts(byte[]... arrays) {
		int size = 0;
		for (byte[] array: arrays) { size += array.length; }
		short[] result = new short[(size + 1) / 2];

		int position = 0;
		int sub_position = 0;
		for (byte[] array: arrays) {
			for (byte value: array) {
				result[position] |= (value & 0xFF) << (sub_position * 8);
				sub_position++;
				if (sub_position == 2) { sub_position = 0; position++; }
			}
		}

		return result;
	}

	public static short[] shortsToShorts(short[]... arrays) {
		int size = 0;
		for (short[] array: arrays) { size += array.length; }
		short[] result = new short[size];

		int position = 0;
		for (short[] array: arrays) {
			System.arraycopy(array, 0, result, position, array.length);
			position += array.length;
		}

		return result;
	}

	public static short[] intsToShorts(int[]... arrays) {
		int size = 0;
		for (int[] array: arrays) { size += array.length; }
		short[] result = new short[size * 2];

		int position = 0;
		for (int[] array: arrays) {
			for (int value: array) {
				result[position++] = (short)(value);
				result[position++] = (short)(value >> 16);
			}
		}

		return result;
	}

	public static int[] bytesToInts(byte[]... arrays) {
		int size = 0;
		for (byte[] array: arrays) { size += array.length; }
		int[] result = new int[(size + 3) / 4];

		int position = 0;
		int sub_position = 0;
		for (byte[] array: arrays) {
			for (byte value: array) {
				result[position] |= (value & 0xFF) << (sub_position * 8);
				sub_position++;
				if (sub_position == 4) { sub_position = 0; position++; }
			}
		}

		return result;
	}

	public static int[] shortsToInts(short[]... arrays) {
		int size = 0;
		for (short[] array: arrays) { size += array.length; }
		int[] result = new int[(size + 1) / 2];

		int position = 0;
		int sub_position = 0;
		for (short[] array: arrays) {
			for (short value: array) {
				result[position] |= (value & 0xFFFF) << (sub_position * 16);
				sub_position++;
				if (sub_position == 2) { sub_position = 0; position++; }
			}
		}

		return result;
	}

	public static int[] intsToInts(int[]... arrays) {
		int size = 0;
		for (int[] array: arrays) { size += array.length; }
		int[] result = new int[size];

		int position = 0;
		for (int[] array: arrays) {
			System.arraycopy(array, 0, result, position, array.length);
			position += array.length;
		}

		return result;
	}

	// XXX Ugly
	public static int[][] intArrayListToIntArrayArray(List<int[]> input) {
		int[][] output = new int[input.size()][];
		int i = 0;
		for (int[] curr: input) output[i++] = curr;
		return output;
	}

	public static void main(String[] args) {
		byte[] byte1 = new byte[] { 0x01 };
		byte[] byte2 = new byte[] { 0x02, 0x03 };
		byte[] byte3 = new byte[] { 0x04, 0x05, 0x06, 0x07 };

		short[] short1 = new short[] { 0x7F01 };
		short[] short2 = new short[] { 0x7E02, 0x7D03 };
		short[] short3 = new short[] { 0x7C04, 0x7B05, 0x7A06, 0x7907 };

		int[] int1 = new int[] { 0x7F000001 };
		int[] int2 = new int[] { 0x7E000002, 0x7D000003 };
		int[] int3 = new int[] { 0x7C000004, 0x7B000005, 0x7A000006, 0x79000007 };

		for (byte v: bytesToBytes(byte1, byte2, byte3)) {
			System.out.print(String.format("%02X ", v));
		}
		System.out.println();
		for (short v: bytesToShorts(byte1, byte2, byte3)) {
			System.out.print(String.format("%04X ", v));
		}
		System.out.println();
		for (int v: bytesToInts(byte1, byte2, byte3)) {
			System.out.print(String.format("%08X ", v));
		}
		System.out.println();
		for (byte v: shortsToBytes(short1, short2, short3)) {
			System.out.print(String.format("%02X ", v));
		}
		System.out.println();
		for (short v: shortsToShorts(short1, short2, short3)) {
			System.out.print(String.format("%04X ", v));
		}
		System.out.println();
		for (int v: shortsToInts(short1, short2, short3)) {
			System.out.print(String.format("%08X ", v));
		}
		System.out.println();
		for (byte v: intsToBytes(int1, int2, int3)) {
			System.out.print(String.format("%02X ", v));
		}
		System.out.println();
		for (short v: intsToShorts(int1, int2, int3)) {
			System.out.print(String.format("%04X ", v));
		}
		System.out.println();
		for (int v: intsToInts(int1, int2, int3)) {
			System.out.print(String.format("%08X ", v));
		}
	}
}

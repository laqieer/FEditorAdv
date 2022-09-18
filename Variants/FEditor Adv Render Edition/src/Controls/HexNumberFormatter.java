/*
 *  FE Editor - GBA Fire Emblem (U) ROM editor
 *
 *  Copyright (C) 2008-2011 Hextator,
 *  hextator (AIM) hextator@gmail.com (MSN)
 *
 *  Major thanks to Zahlman (AIM/MSN: zahlman@gmail.com) for optimization,
 *  organization and modularity improvements.
 *
 *  Contributions by others in this file
 *  - Roedy Green of Canadian Mind Products and
 *  Thomas Fritsch provided/inspired this software.
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
 *  <Description> For having spinners function in base 16
 */

package Controls;

import java.text.ParseException;
import javax.swing.text.DefaultFormatter;

@SuppressWarnings({"WeakerAccess"})
public class HexNumberFormatter extends DefaultFormatter {
	private int width;

	public HexNumberFormatter(int width) {
		if (width > 16)
			throw new IllegalArgumentException("HexFormat width > 16");
		this.width = width;
	}

	@SuppressWarnings({"QuestionableName"})
	@Override
	public Object stringToValue(String string) throws ParseException {
		try {
			if (string.length() > width) {
				throw new ParseException(
					"Max "
					+ width
					+ " digits allowed.",
					0
				);
			}
			Object value = getFormattedTextField().getValue();
			if (value instanceof Byte)
				return Long.valueOf(string, 16).byteValue();
			else if (value instanceof Short)
				return Long.valueOf(string, 16).shortValue();
			else if (value instanceof Integer)
				return Long.valueOf(string, 16).intValue();
			else if (value instanceof Long) {
				if (string.length() == 16) {
					String topDigit = string.substring(0, 1);
					String theRest = string.substring(1);
					boolean negative = false;
					int topDigitVal = Integer.valueOf(topDigit, 16);
					if (topDigitVal > 7) {
						negative = true;
						topDigitVal -= 8;
					}
					String newS = Integer.toString(topDigitVal, 16);
					newS += theRest;
					long out = Long.valueOf(newS, 16);
					return negative ? -out : out;
				}
				return Long.valueOf(string, 16);
			}
			else
				throw new IllegalArgumentException(
					"HexNumberFormatter only works with "
					+ "wrappers of primitive numeric types"
				);
		}
		catch (NumberFormatException nfe) {
			throw new ParseException(string, 0);
		}
	}

	@Override
	public String valueToString(Object value) {
		//Treat as unsigned
		long asLong = ((Number) value).longValue();
		String valString = Long.toHexString(asLong);
		//Add leading zeroes as needed.
		int leadingZeroes = width - valString.length();
		return (
			leadingZeroes <= 0
			? valString.substring(valString.length() - width)
			: "0000000000000000".substring(0, leadingZeroes) + valString
		).toUpperCase();
	}
}

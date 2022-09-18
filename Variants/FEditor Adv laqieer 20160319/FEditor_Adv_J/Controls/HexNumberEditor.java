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

import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.text.DefaultFormatterFactory;

public class HexNumberEditor extends JSpinner.NumberEditor {
	public HexNumberEditor(JSpinner spinner, int width) {
		super(spinner);
		JFormattedTextField tempField = getTextField();
		tempField.setEditable(true);
		tempField.setFormatterFactory(
			new DefaultFormatterFactory(new HexNumberFormatter(width))
		);
	}
}

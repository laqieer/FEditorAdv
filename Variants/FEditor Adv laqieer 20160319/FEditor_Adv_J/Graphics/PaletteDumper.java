/*
 *  (Untitled side application) - Graphic editing utility for GBA format(s)
 *
 *  Copyright (C) 2009 Hextator,
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
 *  <Description> This class is expected to provide an interface for inserting
 *  dumping palette information from GBA 4BPP formatted images resulting from
 *  formatting images of common formats into the GBA 4BPP format
 */

package Graphics;

import java.io.File;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import FEditorAdvance.CommonDialogs;

public class PaletteDumper {
	private static void display(String title, String message) {
		JOptionPane.showMessageDialog(
			null, new JTextArea(message),
			title, JOptionPane.INFORMATION_MESSAGE
		);
	}

	public static void main(String[] args) {
		try {
			File loadedFile = CommonDialogs.showOpenFileDialog(
				"イメージをダンプする"
			);
			short[] output = new GBAImage(loadedFile).getPalette().getShorts();
			String outputString = "Your palette is:\n";
			for (int i = 0; i < 16; ++i) {
				byte top = (byte)(output[i] >> 8);
				byte bottom = (byte)(output[i] & 0xff);
				String pad = " ";
				if (i == 7) { pad = "\n"; }
				if (i == 15) { pad = ""; }
				outputString += String.format("%02X %02X%s", bottom, top, pad);
			}
			display("Palette", outputString);
		} catch (Exception e) {
			display("Error", e.getMessage());
		}
		System.exit(0); // Swing requires this
	}
}

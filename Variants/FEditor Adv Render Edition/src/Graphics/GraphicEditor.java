/*
 *  (Untitled side application) - Graphic editing utility for GBA format(s)
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
 *  <Description> This class is expected to provide an interface for inserting
 *  graphical data into .GBA file with a robust selection of insertion options
 */

package Graphics;

import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.UIManager;
import Compression.LempelZiv;
import Model.ROM;
import FEditorAdvance.CommonDialogs;

/**
 * This class is expected to provide an interface for inserting
 * graphical data into .GBA file with a robust selection of insertion options
 * @author Hextator
 */
public class GraphicEditor extends JFrame {
	// FIXME: Integrate into FEditor. This is broken as a standalone because
	// it doesn't know how to handle metadata or get the current version.
	// Also needs refactoring to come up to current standards.

	//Fields

	private ROM rom = null;
	private boolean checksumOnEnd;

	// The loaded image to be written
	private GBAImage currentImage = null;

	// TODO: support 8bpp and 16bpp editing modes.

	// Keeps the dialog open until closed by the window decoration for
	// closing
	private boolean operationComplete = false;

	// To keep the application from interrupting writing procedures
	private boolean write = false;

	//Dialog controls

	// For choosing the image to insert
	private JButton selectImageButton = new JButton("Select image");

	// For choosing the ROM to insert the image into should the user
	// choose the ROM as the image's destination (as opposed to a binary
	// file)
	private JButton selectROMButton = new JButton("Open ROM");

	// Clicking this should let the user choose where to save the open ROM
	// and then save it accordingly
	private JButton saveROMButton = new JButton("Save ROM");

	// Compress image data if this is set
	private JCheckBox compressImageCheckbox = new JCheckBox("Compress image?");

	// Compress palette data if this is set
	private JCheckBox compressPaletteCheckbox = new JCheckBox("Compress palette?");

	// Inserts the loaded image into the open ROM (if one is open >.>)
	private JButton writeToROMButton = new JButton("Write to ROM");

	// Dumps the loaded image as binary data
	private JButton writeToFileButton = new JButton("Write to file");

	// Helpers.
	private byte[] getOutput(byte[] input, boolean compress) {
		return compress ? LempelZiv.compress(input) : input;
	}

	private byte[] getPalette() {
		return getOutput(
			currentImage.getPalette().getBytes(),
			compressPaletteCheckbox.isSelected()
		);
	}

	private byte[] getImageData() {
		return getOutput(
			currentImage.getData(),
			compressImageCheckbox.isSelected()
		);
	}

	private void save_helper(String type, byte[] data) {
		File file = CommonDialogs.showSaveFileDialog(type);
		if (file == null) { return; }

		write = true;
		FileOutputStream imageWriter = null;
		try {
			imageWriter = new FileOutputStream(file);
			imageWriter.write(data);
		} catch (Exception e) {
		} finally {
			try { imageWriter.close(); }
			catch (Exception e) {}
		}
		write = false;
	}

	private void write_helper(String type, byte[] output) {
		int size = rom.size();
		Integer address = CommonDialogs.promptForInteger(
			String.format("Enter the address to write the %s data to:", type),
			0, size
		);
		if (address == null) { return; }

		Integer pointer = CommonDialogs.promptForInteger(
			String.format("Enter the address of the %s pointer to update:", type),
			0, size - 4
		);
		//if (pointer == null) { return; }

		write = true;
		try {
			int difference = address + output.length - size;
			if (difference > 0) { rom.expand(difference); }
			address += ROM.GBA_HARDWARE_OFFSET;
			if (pointer != null)
				pointer += ROM.GBA_HARDWARE_OFFSET;
			rom.writeBytesAt(address, output);
			if (pointer != null)
				rom.new Pointer(pointer).writeInt(address);
		} catch (Exception e) {
			CommonDialogs.showGenericErrorDialog("Writing error: check your input!");
		}
		write = false;
	}

	private GraphicEditor() {
		super();

		setTitle("Graphic Editor");

		Container contentPane = getContentPane();

		contentPane.setLayout(
			new BoxLayout(contentPane, BoxLayout.PAGE_AXIS)
		);

		writeToROMButton.setEnabled(false);
		writeToFileButton.setEnabled(false);

		// Image selection button drawing code

		selectImageButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					try {
						currentImage = new GBAImage(
							CommonDialogs.showOpenFileDialog("image")
						);
						currentImage = new GBAImage(
							currentImage,
							0, 0, currentImage.getTileWidth() - 1, currentImage.getTileHeight()
						);
					} catch (Exception e) {}

					writeToFileButton.setEnabled(currentImage != null);
					writeToROMButton.setEnabled(currentImage != null && rom != null);
				}
			}
		);

		//ROM selection button drawing code

		selectROMButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					if (rom != null) {
						if (CommonDialogs.showSaveDialog()) { rom.save(null, null, null); }
						rom = null;
					}

					File romFile = CommonDialogs.showOpenFileDialog("ROM");
					if (romFile != null) {
						try { rom = new ROM(romFile); }
						catch (IOException ioe) { CommonDialogs.showStreamErrorDialog(); }
					}

					// Check if there's a checksum at the end
					if (
						rom.checksum(0, rom.size() - 4) ==
						rom.new Pointer(rom.size() - 4 + ROM.GBA_HARDWARE_OFFSET).currentInt()
					)
						checksumOnEnd = true;
					else
						checksumOnEnd = false;

					writeToROMButton.setEnabled(currentImage != null && rom != null);
				}
			}
		);

		//ROM saving button drawing code

		saveROMButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					if (!checksumOnEnd)
						rom.expand(4);
					rom.new Pointer(
						rom.size() - 4 + ROM.GBA_HARDWARE_OFFSET
					).writeInt(
						rom.checksum(0, rom.size() - 4)
					);
					if (rom != null) { rom.save(CommonDialogs.showSaveAsDialog(), null, null); }
				}
			}
		);

		//Add recently created components to panel and then add the
		//panel to the content pane

		JPanel openPanel = new JPanel();
		openPanel.add(selectImageButton);
		openPanel.add(selectROMButton);
		openPanel.add(saveROMButton);
		contentPane.add(openPanel);

		//Add compression checkbox components to panel and then add the
		//panel to the content pane

		JPanel compressionPanel = new JPanel();
		compressionPanel.add(compressImageCheckbox);
		compressionPanel.add(compressPaletteCheckbox);
		contentPane.add(compressionPanel);

		//Write to ROM button drawing code

		writeToROMButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					write_helper("image", getImageData());
					write_helper("palette", getPalette());
				}
			}
		);

		//Write to file button drawing code

		writeToFileButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					save_helper("image data", getImageData());
					save_helper("palette data", getPalette());
				}
			}
		);

		//Add recently created components to panel and then add the
		//panel to the content pane

		JPanel writePanel = new JPanel();
		writePanel.add(writeToROMButton);
		writePanel.add(writeToFileButton);
		contentPane.add(writePanel);

		//Exit button drawing code

		JButton exitButton = new JButton("Exit");
		exitButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					operationComplete = true;
				}
			}
		);
		JPanel exitPanel = new JPanel();
		exitPanel.add(exitButton);
		contentPane.add(exitPanel);

		//Display code

		pack();
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(
			new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent evt) {
					operationComplete = true;
				}
			}
		);
		setVisible(true);
		while (!operationComplete) {}
		while (write) {}
		setVisible(false);
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(
				UIManager.getSystemLookAndFeelClassName()
			);
			new GraphicEditor();
		} catch (Exception e) {}

		System.exit(0);
	}
}

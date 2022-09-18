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
 *  <Description> This class provides an interface for managing free space
 *  as well as moving arrays of bytes in and out of the ROM with automatic
 *  free sapce management
 */

package FEditorAdvance;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import Controls.LabelledHexSpinner;
import Controls.IntValueListener;
import Model.BinaryModel;
import Model.Game;
import Model.ROM;

public class BinaryEditor extends Editor<BinaryModel> {
	private Game game;
	private BinaryModel binaryModel;

	private JPanel statusPanel;
	private JLabel statusLabel;

	private Mode mode = Mode.DELETE;
	private JRadioButton deleteButton = new JRadioButton("Delete");
	private JRadioButton cutButton = new JRadioButton("Cut");
	private JRadioButton copyButton = new JRadioButton("Copy");
	private JRadioButton pasteButton = new JRadioButton("Paste");
	private JRadioButton deallocateButton = new JRadioButton("Deallocate");
	private ButtonGroup modeButtons = new ButtonGroup();

	private JCheckBox updatePtrCheckBox = new JCheckBox("Update pointer?");
	private JCheckBox pointerHandleCheckBox = new JCheckBox("Fix other pointers?");
	private JCheckBox compressionCheckBox = new JCheckBox("Treat data as LZ77 compressed?");

	private LabelledHexSpinner ptrSpinner;
	private LabelledHexSpinner startSpinner;
	private LabelledHexSpinner lengthSpinner;

	private File targetPath;
	private JTextField pathField;

	private JPanel hexLabelPanel;
	private JLabel hexLabel;

	private LabelledHexSpinner addrSpinner;
	private LabelledHexSpinner valSpinner;

	// Options for usage of this interface
	public enum Mode {
		// Free space
		DELETE,
		// Free space and save to file
		CUT,
		// Save to file
		COPY,
		// Read into ROM from file and label space as used
		PASTE,
		// Label space as used without writing to it
		DEALLOCATE
	}

	public BinaryEditor(View view) {
		super(view);

		statusPanel = new JPanel();
		statusLabel = new JLabel("Memory and Free Space Manager");
		statusPanel.add(statusLabel);
		statusPanel.setMaximumSize(statusPanel.getPreferredSize());
		add(statusPanel);

		final JPanel optionsPanel1 = new JPanel();
		modeButtons.add(deleteButton);
		modeButtons.add(cutButton);
		modeButtons.add(copyButton);
		modeButtons.add(pasteButton);
		modeButtons.add(deallocateButton);
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mode = Mode.DELETE;
			}
		});
		cutButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mode = Mode.CUT;
			}
		});
		copyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mode = Mode.COPY;
			}
		});
		pasteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mode = Mode.PASTE;
			}
		});
		deallocateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mode = Mode.DEALLOCATE;
			}
		});
		deleteButton.setSelected(true);
		optionsPanel1.add(deleteButton);
		optionsPanel1.add(cutButton);
		optionsPanel1.add(copyButton);
		optionsPanel1.add(pasteButton);
		optionsPanel1.add(deallocateButton);
		optionsPanel1.setMaximumSize(optionsPanel1.getPreferredSize());
		add(optionsPanel1);

		final JPanel optionsPanel2 = new JPanel();
		updatePtrCheckBox.setSelected(false);
		pointerHandleCheckBox.setSelected(false);
		ptrSpinner = new LabelledHexSpinner(
			null, "Addr. of ptr. to update:", 8
		);
		compressionCheckBox.setSelected(false);
		optionsPanel2.add(updatePtrCheckBox);
		optionsPanel2.add(pointerHandleCheckBox);
		optionsPanel2.add(ptrSpinner);
		optionsPanel2.setMaximumSize(optionsPanel2.getPreferredSize());
		add(optionsPanel2);

		final JPanel rangePanel = new JPanel();
		startSpinner = new LabelledHexSpinner(
			null, "Start address:", 8
		);
		lengthSpinner = new LabelledHexSpinner(
			null, "Length:", 8
		);
		rangePanel.add(startSpinner);
		rangePanel.add(lengthSpinner);
		rangePanel.setMaximumSize(rangePanel.getPreferredSize());
		add(rangePanel);

		final JPanel compressionPanel = new JPanel();
		compressionPanel.add(compressionCheckBox);
		compressionPanel.setMaximumSize(compressionPanel.getPreferredSize());
		add(compressionPanel);

		final JPanel pathPanel = new JPanel();
		addButtonToPanel(pathPanel, "File...", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				boolean save = false;
				if (mode == Mode.CUT || mode == Mode.COPY)
					save = true;
				if (save)
					targetPath = CommonDialogs.showSaveFileDialog(
						"binary dump"
					);
				else
					targetPath = CommonDialogs.showOpenFileDialog(
						"binary dump"
					);
				pathField.setText(targetPath.getName());
			}
		});
		pathField = new JTextField(32);
		pathField.setEditable(false);
		pathField.setText("No file selected.");
		pathPanel.add(pathField);
		pathPanel.setMaximumSize(pathPanel.getPreferredSize());
		add(pathPanel);

		final JPanel doPanel = new JPanel();
		addButtonToPanel(doPanel, "Manage Memory", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				try {
					int bytes = 0;
					Integer length;
					boolean free = false;
					String operation = "";
					if (
						(mode == Mode.CUT || mode == Mode.COPY)
						&& targetPath == null
					)
						throw new RuntimeException(
							"No file selected!"
						);
					if (mode == Mode.PASTE && (targetPath == null || !targetPath.exists()))
						throw new RuntimeException(
							"No file to read!"
						);
					switch (mode) {
						case DELETE:
						free = true;
						operation = "allocated";
						case DEALLOCATE:
						if (operation.equals(""))
							operation = "deallocated";
						int startVal = startSpinner.getValue() + ROM.GBA_HARDWARE_OFFSET;
						Integer endVal = compressionCheckBox.isSelected()
							? null
							: startSpinner.getValue()
							+ lengthSpinner.getValue()
							+ ROM.GBA_HARDWARE_OFFSET;

						if (free)
							bytes = binaryModel.allocate(startVal, endVal);
						else
							bytes = binaryModel.deallocate(startVal, endVal);
						statusLabel.setText(
							"0x"
							+ Integer.toString(bytes, 16).toUpperCase()
							+ String.format(
								" bytes %s.",
								operation
							)
						);
						break;
						case CUT:
						free = true;
						operation = "cut";
						case COPY:
						if (operation.equals(""))
							operation = "copied";
						length = compressionCheckBox.isSelected()
							? null
							: lengthSpinner.getValue();
						byte[] output = binaryModel.getData(
							startSpinner.getValue() + ROM.GBA_HARDWARE_OFFSET,
							length,
							free
						);
						FileOutputStream outStream = new FileOutputStream(targetPath);
						outStream.write(output);
						try {
							outStream.close();
						} catch (Exception e) {}
						statusLabel.setText(
							"0x"
							+ Integer.toString(output.length, 16).toUpperCase()
							+ String.format(
								" bytes have been %s.",
								operation
							)
						);
						break;
						case PASTE:
						byte[] input = new byte[(int)targetPath.length()];
						length = input.length;
						FileInputStream inputStream = new FileInputStream(targetPath);
						inputStream.read(input);
						try {
							inputStream.close();
						} catch (Exception e) {}
						int dest = binaryModel.setData(
							updatePtrCheckBox.isSelected()
							? ptrSpinner.getValue() + ROM.GBA_HARDWARE_OFFSET
							: null,
							input,
							pointerHandleCheckBox.isSelected(),
							compressionCheckBox.isSelected()
						);
						statusLabel.setText(
							"0x"
							+ Integer.toString(length, 16).toUpperCase()
							+ " bytes have been pasted to the address "
							+ String.format("0x%08X", dest)
							+ "."
						);
						ptrSpinner.setMax(game.currentROMsize());
						startSpinner.setMax(game.currentROMsize());
						lengthSpinner.setMax(game.currentROMsize());
						break;
						default:
						statusLabel.setText("Apparently nothing happened.");
						break;
					}
				} catch (Exception e) {
					statusLabel.setText(e.getMessage());
				}
				statusPanel.setMaximumSize(statusPanel.getPreferredSize());
				statusLabel.setSize(statusLabel.getPreferredSize());
			}
		});
		addButtonToPanel(doPanel, "List Free Space", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				binaryModel.listFreeSpace();
			}
		});
		doPanel.setMaximumSize(doPanel.getPreferredSize());
		add(doPanel);

		hexLabelPanel = new JPanel();
		hexLabel = new JLabel("Integer editor (only works with 4 byte alignment)");
		hexLabelPanel.add(hexLabel);
		hexLabelPanel.setMaximumSize(hexLabelPanel.getPreferredSize());
		add(hexLabelPanel);

		final JPanel hexPanel = new JPanel();
		addrSpinner = new LabelledHexSpinner(
			new IntValueListener() {
				public void newValue(int value) {
					valSpinner.setValue(
						binaryModel.getInt(value)
					);
				}
			}, "Address:", 8
		);
		valSpinner = new LabelledHexSpinner(
			new IntValueListener() {
				public void newValue(int value) {
					binaryModel.putInt(
						addrSpinner.getValue(),
						value
					);
				}
			}, "Value:", 8
		);
		addrSpinner.setValue(0);
		hexPanel.add(addrSpinner);
		hexPanel.add(valSpinner);
		addButtonToPanel(hexPanel, "Dereference", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				try {
					int supposedPointer = valSpinner.getValue()
						- ROM.GBA_HARDWARE_OFFSET;
					valSpinner.setValue(binaryModel.getInt(supposedPointer));
					addrSpinner.setValue(supposedPointer);
				} catch (Exception e) {}
			}
		});
		hexPanel.setMaximumSize(hexPanel.getPreferredSize());
		add(hexPanel);

		final JPanel freeSpacePanel = new JPanel();
		addButtonToPanel(freeSpacePanel, "Dump free space...", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				File file = CommonDialogs.showSaveFileDialog(
					"free space dump"
				);
				if (file == null) return;
				FileWriter out = null;
				try {
					out = new FileWriter(file);
					out.write(binaryModel.printFreeSpace());
				} catch (Exception e) {
					throw new RuntimeException(
						"Stream error when dumping free space"
					);
				}
				try { out.close(); } catch (Exception e) {}
			}
		});
		addButtonToPanel(freeSpacePanel, "Import free space...", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				binaryModel.parseFreeSpace(
				CommonDialogs.showOpenFileDialog(
					"free space dump"
				));
			}
		});
		freeSpacePanel.setMaximumSize(freeSpacePanel.getPreferredSize());
		add(freeSpacePanel);

		setMinimumSize(getSize());
	}

	@Override
	public void setup(Game game) {
		this.game = game;
		binaryModel = game.binaryModel();
		ptrSpinner.setMin(0);
		ptrSpinner.setValue(0);
		ptrSpinner.setMax(game.currentROMsize());
		startSpinner.setMin(0);
		startSpinner.setValue(0);
		startSpinner.setMax(game.currentROMsize());
		lengthSpinner.setMin(0);
		lengthSpinner.setValue(0);
		lengthSpinner.setMax(game.currentROMsize());
		addrSpinner.setValue(0);
		addrSpinner.changeStep(4);
		valSpinner.setValue(binaryModel.getInt(0));
	}
}

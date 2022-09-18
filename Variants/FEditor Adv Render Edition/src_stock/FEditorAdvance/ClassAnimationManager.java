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
 *  <Description> This class provides a dialog for importing and exporting
 *  custom battle animations for classes
 */

package FEditorAdvance;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
//import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import Controls.IntValueListener;
import Controls.LabelledHexSpinner;
import Controls.Process;
import Controls.ProcessComponent;
import Controls.ArrayPanel;
import Model.Game;
import Model.ClassAnimationArray;
import Model.PortableClassAnimation;
import Controls.PaletteFrame;
import Graphics.GBAImage;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class ClassAnimationManager extends Editor<ClassAnimationArray> {
	// Clipboard
	private static PortableClassAnimation dumpedAnimation = null;

	private ArrayPanel<ClassAnimationArray> arrayPanel;
	private Game game;
	private JPanel framePanel = new JPanel();
	private JPanel sheetPanel = new JPanel();
	private JLabel frameLabel = new JLabel();
	private JLabel sheetLabel = new JLabel();
	private JTextField nameField = new JTextField(16);
	private GBAImage[] currentSheets;
	private LabelledHexSpinner frameSpinner;
	private LabelledHexSpinner sheetSpinner;

	private PortableClassAnimation pca;

	private PaletteFrame palette_editor = new PaletteFrame(
		new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				//System.out.println(Util.verboseReport("PF state changed"));
				for (GBAImage image: currentSheets) {
					image.notifyPaletteChangedExternally();
				}
				//System.out.println(Util.verboseReport("did nPCE"));
				pca.setPalette(palette_editor.getPalette());
				//System.out.println(Util.verboseReport("did PCA palette"));
				updatePreview(); updateSheetPreview();
				//System.out.println(Util.verboseReport("did updates"));
			}
		}
	);

	public ClassAnimationManager(View view) {
		super(view);

		this.view = view;

		// FIXME: This should use ClassAnimationArray.maxSize(), but there is
		// no instance available yet.
		arrayPanel = new ArrayPanel<ClassAnimationArray>(this, 0x1, 0xFF, 1);
		arrayPanel.setMaximumSize(arrayPanel.getPreferredSize());
		add(arrayPanel);

		JPanel clipboardPanel = new JPanel();
		addButtonToPanel(clipboardPanel, "Dump to Clipboard", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				dumpToClipboard();
			}
		});
		addButtonToPanel(clipboardPanel, "Paste from Clipboard", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				pasteFromClipboard();
			}
		});
		addButtonToPanel(clipboardPanel, "Clear Clipboard", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				clearClipboard();
			}
		});
		clipboardPanel.setMaximumSize(clipboardPanel.getPreferredSize());
		add(clipboardPanel);

		JPanel operationPanel = new JPanel();
		addButtonToPanel(operationPanel, "Set Name", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				setName();
			}
		});
		addButtonToPanel(operationPanel, "Dump...", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				dump();
			}
		});
		addButtonToPanel(operationPanel, "Insert...", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				insert();
			}
		});
		addButtonToPanel(operationPanel, "Quit", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				quit();
			}
		});
		operationPanel.setMaximumSize(operationPanel.getPreferredSize());
		add(operationPanel);

		JPanel namePanel = new JPanel();
		nameField.setHorizontalAlignment(JTextField.RIGHT);
		namePanel.add(nameField);
		nameField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				setName();
			}
		});
		frameSpinner = new LabelledHexSpinner(
			new IntValueListener() {
				public void newValue(int value) {
					updatePreview();
				}
			},
			"Frame:", 2
		);
		namePanel.add(frameSpinner);
		sheetSpinner = new LabelledHexSpinner(
			new IntValueListener() {
				public void newValue(int value) {
					updateSheetPreview();
				}
			},
			"Sheet:", 2
		);
		namePanel.add(sheetSpinner);
		namePanel.setMinimumSize(namePanel.getPreferredSize());
		namePanel.setMaximumSize(namePanel.getPreferredSize());
		add(namePanel);

		framePanel.add(frameLabel);
		// Don't limit the size of this!
		add(framePanel);

		sheetPanel.add(sheetLabel);
		// Don't limit the size of this!
		add(sheetPanel);

		add(palette_editor);

		setMinimumSize(getSize());
	}

	@Override
	public void setup(Game game) {
		arrayHandle = game.classAnimationArray();
		this.game = game;
		// The ArrayPanel will call back to refresh().
		arrayPanel.setArray(arrayHandle);
	}

	/*
	@Override
	public void cleanup() {
		// Hextator sez: Review "teardown" method comments in Editor
		//arrayPanel.setArray(null); // so it can be GC'd
	}
	*/

	@Override
	public void refresh() {
		try {
			pca = arrayHandle.getEntry();
		} catch (Exception e) { pca = null; }

		frameSpinner.setMax(0xFF);
		frameSpinner.setValue(0);
		currentSheets = arrayHandle.getSheets();
		sheetSpinner.setMax(currentSheets.length - 1);
		sheetSpinner.setValue(0);

		ImageIcon icon = null;
		try {
			icon = new ImageIcon(arrayHandle.getFrame(0));
		} catch (Exception ex) {}
		frameLabel.setIcon(icon);
		frameLabel.setText(
			icon == null ? "[Failed to load]" : ""
		);

		icon = null;
		try {
			icon = new ImageIcon(currentSheets[0].getImage());
		} catch (Exception ex) { }
		palette_editor.setPalette(currentSheets[0].getPalette());
		sheetLabel.setIcon(icon);
		sheetLabel.setText(
			icon == null ? "[Failed to load]" : ""
		);
		nameField.setText(arrayHandle.getName());
		framePanel.setMinimumSize(framePanel.getPreferredSize());
		sheetPanel.setMinimumSize(sheetPanel.getPreferredSize());
	}

	// Called when either the frame ID changes or the palette does. In either
	// case, the frame preview needs to be redone from scratch.
	public void updatePreview() {
		ImageIcon icon = null;
		try {
			// Redraw the frame from scratch in case either the palette or
			// frame ID changed.
			icon = new ImageIcon(arrayHandle.getFrame(frameSpinner.getValue()));
		} catch (Exception e) {
			//e.printStackTrace();
		}
		frameLabel.setIcon(icon);
		frameLabel.setText(
			icon == null ? "[Failed to load]" : ""
		);
		framePanel.setMinimumSize(framePanel.getPreferredSize());
	}

	public void updateSheetPreview() {
		ImageIcon icon = null;
		try {
			icon = new ImageIcon(currentSheets[sheetSpinner.getValue()].getImage());
		} catch (Exception e) {}
		sheetLabel.setIcon(icon);
		sheetLabel.setText(
			icon == null ? "[Failed to load]" : ""
		);
		sheetPanel.setMinimumSize(sheetPanel.getPreferredSize());
	}

	// FIXME: Extract this class; it's used elsewhere
	class PositionRestoringProcess extends Process {
		private int originalPosition;
		public PositionRestoringProcess(ProcessComponent component) {
			super(false, component); // can't cancel
			originalPosition = arrayPanel.getIndex();
		}

		@Override
		public void finish() {
			arrayPanel.setIndex(originalPosition);
		}
	}

	public void dump() {
		final File selectedFile = CommonDialogs.showSaveFileDialog("class animation dump");
		if (selectedFile == null) { return; }

		view.process(
			false, // Don't show dialogs
			new PositionRestoringProcess(
				new ProcessComponent(1, 1, "Dumping animation...") {
					protected boolean iterate(int i) {
						try {
							arrayHandle.getEntry().writeToFile(selectedFile);
						} catch (Exception e) {
							CommonDialogs.showCatchErrorDialog(e);
						}
						return false;
					}
				}
			)
		);
	}
	// Tested and working

	public void dumpToClipboard() {
		view.process(
			false, // Don't show dialogs
			new PositionRestoringProcess(
				new ProcessComponent(1, 1, "Dumping animation...") {
					protected boolean iterate(int i) {
						try {
							dumpedAnimation = arrayHandle.getEntry();
							CommonDialogs.showGenericMessageDialog(
								"The animation has been dumped to the clipboard.",
								"Success", true
							);
						} catch (Exception e) {
							CommonDialogs.showCatchErrorDialog(e);
						}
						return false;
					}
				}
			)
		);
	}
	// Tested and working

	public void insert() {
		final File selectedFile = CommonDialogs.showOpenFileDialog("class animation dump");
		if (selectedFile == null) { return; }

		view.process(
			false, // Don't show dialogs
			new PositionRestoringProcess(
				new ProcessComponent(1, 1, "Importing animation...") {
					protected boolean iterate(int i) {
						try {
							new PortableClassAnimation(selectedFile).writeToArray(game, arrayHandle);
							javax.swing.JOptionPane.showMessageDialog(
								null,
								"Animation inserted!",
								"Operation complete",
								javax.swing.JOptionPane.INFORMATION_MESSAGE
							);
						} catch (Exception e) {
							CommonDialogs.showCatchErrorDialog(e);
						}
						return false;
					}
				}
			)
		);
	}
	// Tested and working

	public void pasteFromClipboard() {
		view.process(
			false, // Don't show dialogs
			new PositionRestoringProcess(
				new ProcessComponent(1, 1, "Importing animation...") {
					protected boolean iterate(int i) {
						try {
							if (dumpedAnimation != null) {
								dumpedAnimation.writeToArray(game, arrayHandle);
								javax.swing.JOptionPane.showMessageDialog(
									null,
									"Animation inserted!",
									"Operation complete",
									javax.swing.JOptionPane.INFORMATION_MESSAGE
								);
							}
							else {
								CommonDialogs.showGenericErrorDialog(
									"The clipboard is empty."
								);
							}
						} catch (Exception e) {
							CommonDialogs.showCatchErrorDialog(e);
						}
						return false;
					}
				}
			)
		);
	}
	// Tested and working

	public void clearClipboard() {
		view.process(
			false, // Don't show dialogs
			new PositionRestoringProcess(
				new ProcessComponent(1, 1, "Importing animation...") {
					protected boolean iterate(int i) {
						dumpedAnimation = null;
						return false;
					}
				}
			)
		);
	}
	// Tested and working

	public void setName() {
		arrayHandle.setName(nameField.getText());
		nameField.setText(arrayHandle.getName());
	}
	// Tested and working
}

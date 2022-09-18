/*
 *  FE Editor - GBA Fire Emblem (U) ROM editor
 *
 *  Copyright (C) 2008-2011 Hextator,
 *  hextator (AIM) hextator@gmail.com (MSN)
 *
 *  Major thanks to Zahlman (AIM/MSN: zahlman@gmail.com) for optimization,
 *  organization and modularity improvements.
 * 
 *  Thanks to Camtech for some contributions to this file.
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
 *  <Description> This class provides a dialog with a text area and other
 *  appropriate controls for easy modification of the games' scripts
 */

package FEditorAdvance;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.File;
import java.util.Scanner;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JPanel;
//import javax.swing.JButton;
import Controls.ArrayPanel;
import Controls.Process;
import Controls.ProcessComponent;
import Model.Game;
import Model.TextArray;
import Model.Util;

public class TextEditor extends Editor<TextArray> {
	private ArrayPanel<TextArray> arrayPanel;

	private JTextArea inputArea;
	private JCheckBox regExChkBox;

	public TextEditor(View view) {
		super(view);

		inputArea = new JTextArea(8, 16);
		inputArea.setLineWrap(true);
		inputArea.setWrapStyleWord(true);
		JScrollPane inputPane = new JScrollPane(inputArea);
		inputPane.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
		// Don't limit the size of this!
		add(inputPane);

		// FIXME: This should use TextArray.maxSize(), but there is
		// no instance available yet.
		arrayPanel = new ArrayPanel<TextArray>(this, 0x1, 0xFFFF, 0);
		arrayPanel.setMaximumSize(arrayPanel.getPreferredSize());
		add(arrayPanel);

		JPanel findPanel = new JPanel();
		findPanel.add(new JLabel("Input text to find:"));
		final JTextField findField = new JTextField(32);
		findPanel.add(findField);
		findPanel.setMaximumSize(findPanel.getPreferredSize());
		add(findPanel);

		JPanel replacePanel = new JPanel();
		replacePanel.add(new JLabel("Input text to replace:"));
		final JTextField replaceField = new JTextField(30);
		replacePanel.add(replaceField);
		replacePanel.setMaximumSize(replacePanel.getPreferredSize());
		add(replacePanel);

		JPanel panelA = new JPanel();
		addButtonToPanel(panelA, "Find", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) {
				arrayHandle.moveTo(1); find(findField.getText(), true);
			}
		});
		addButtonToPanel(panelA, "Find Next", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) {
				arrayHandle.next(); find(findField.getText(), true);
			}
		});
		addButtonToPanel(panelA, "Find Previous", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) {
				arrayHandle.prev(); find(findField.getText(), false);
			}
		});
		addButtonToPanel(panelA, "Replace All", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) {
				replaceAll(findField.getText(), replaceField.getText());
			}
		});
		regExChkBox = new JCheckBox("Use Reg Ex?");
		regExChkBox.setSelected(false);
		panelA.add(regExChkBox);
		panelA.setMaximumSize(panelA.getPreferredSize());
		add(panelA);

		JPanel panelB = new JPanel();
		
		addButtonToPanel(panelB, "Apply", new ActionListener() {
			@Override
		 	public void actionPerformed(ActionEvent a) {
		 		applyChanges();
		 	}
		 });
		 addButtonToPanel(panelB, "Revert", new ActionListener() {
			@Override
		 	public void actionPerformed(ActionEvent a) {
		 		inputArea.setText(arrayHandle.getText());
		 	}
		 });
		addButtonToPanel(panelB, "Dump...", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) {
				dump();
			}
		});
		addButtonToPanel(panelB, "Insert...", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) {
				insert();
			}
		});
		addButtonToPanel(panelB, "Quit", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) {
				quit();
			}
		});
		panelB.setMaximumSize(panelB.getPreferredSize());
		add(panelB);

		setMinimumSize(getSize());
	}

	@Override
	public void setup(Game game) {
		arrayHandle = game.textArray();
		// The ArrayPanel will call back to refresh().
		arrayPanel.setArray(arrayHandle);
	}

	/**
	 * Hextator sez: This override is redundant and unsafe
	@Override
	public void cleanup() {
		arrayPanel.setArray(null); // so it can be GC'd
	}
	**/

	@Override
	public void refresh() {
		inputArea.setText(arrayHandle.getText());
	}
	
	// (Camtech) FIXME: Try to save all changed text into a
	// collection, and then check that to see if changes are saved.
	// That way, it doesn't just blindly save changes between spinner
	// changes, you have to manually apply (or just tear this down).
	// Hextator: I'm thinking something like this would be appropriate:
	// Hashtable<int, String> changes [etc.]
	// However, that's too specific; this style of caching should be
	// prevalent for all editors and implemented with a template or
	// similar
	@Override
	public boolean changesSaved() {
		return inputArea.getText().equals(arrayHandle.getText());
	}

	private void find(final String toFind, final boolean forward) {
		if (toFind.equals("")) {
			java.awt.Toolkit.getDefaultToolkit().beep();	
			return;
		}

		final boolean useRegularExpressions = regExChkBox.isSelected();

		class Finder extends ProcessComponent {
			public String found = null;
			public boolean regExError = false;

			public Finder(int iterations, int weight, String description) {
				super(iterations, weight, description);
			}

			@Override
			protected boolean iterate(int i) {
				//System.out.println(Util.verboseReport("find iteration"));
				String entry = arrayHandle.getText();
				boolean foundMatch;
				if (!useRegularExpressions)
					foundMatch = entry.indexOf(toFind) != -1;
				else {
					try {
						String[] temp = entry.split(toFind);
						foundMatch =
							temp != null
							&& (
								temp.length > 1
								|| temp.length == 0
							)
						;
					} catch (Exception e) {
						regExError = true;
						return true;
					}
				}
				if (entry != null && foundMatch) {
					found = entry;
					return true;
				}

				if (forward) { arrayHandle.next(); }
				else { arrayHandle.prev(); }
				return false;
			}
		}

		final Finder f = new Finder(
			arrayHandle.getCurrentSize(), 1, "Searching for text..."
		);

		view.process(
			false, // don't show dialogs
			new Process(true, f) { // can cancel
				@Override
				public void finish() {
					if (f.found == null) {
						if (!f.regExError)
							CommonDialogs.showGenericErrorDialog(
								"String not found!"
							);
						else
							CommonDialogs.showGenericErrorDialog(
								"It seems your regular expression is invalid."
							);
					} else {
						// Synchronize the spinner with the position that was moved to
						// behind the scenes.
						arrayPanel.setIndex(arrayHandle.getPosition());
						// In turn, the arrayPanel will call refresh().
					}
				}
			}
		);
	}
	
	class PositionRestoringProcess extends Process {
		private int originalPosition;
		public PositionRestoringProcess(ProcessComponent component) {
			super(false, component); // can't cancel
			originalPosition = arrayPanel.getIndex();
		}

		@Override
		public void finish() {
			// The spinner hasn't changed, but the array position needs to be
			// reset, and text should be updated to match.
			arrayPanel.setIndex(originalPosition);
		}
	}

	private void replaceAll(final String toFind, final String replaceWith) {
		if (toFind.equals("")) {
			java.awt.Toolkit.getDefaultToolkit().beep();	
			return;
		}

		final boolean useRegularExpressions = regExChkBox.isSelected();

		view.process(
			true, // show dialogs
			new PositionRestoringProcess(
				new ProcessComponent(
					arrayHandle.getCurrentSize(), 1, "Replacing occurrences of text..."
				) {
					@Override
					protected boolean iterate(int i) {
						if (i == 0) { return false; }
						arrayHandle.moveTo(i);
						String text = arrayHandle.getText();
						if (text != null) {
							if (!useRegularExpressions)
								arrayHandle.setText(text.replace(toFind, replaceWith));
							else
								arrayHandle.setText(text.replaceAll(toFind, replaceWith));
						}
						return false;
					}
				}
			)
		);	
	}
	
	@Override
	public void applyChanges() {
		arrayHandle.setText(inputArea.getText());
		// Refresh the input area in case the text
		// was modified in the process of any
		// last second formatting done prior to insertion
		inputArea.setText(arrayHandle.getText());
	}
	// Camtech: I'm mostly doing things in the safest way I can think of,
	// as I don't understand half of this stuff anyway. And still, I'm sure
	// I messed something up somewhere or other.

	private void dump() {
		final FileWriter scriptToDump;
		File selectedFile = CommonDialogs.showSaveFileDialog("script dump");
		if (selectedFile == null) { return; }

		try { scriptToDump = new FileWriter(selectedFile); }
		catch (Exception e) {
			CommonDialogs.showStreamErrorDialog();
			return;
		}

		int size = arrayHandle.getCurrentSize();
		view.process(
			true, // show dialogs
			new PositionRestoringProcess(
				new ProcessComponent(size, 1, "Dumping script...") {
					@Override
					protected boolean iterate(int i) {
						if (i == 0) { return false; }
						arrayHandle.moveTo(i);
						String text = arrayHandle.getText();
						if (text != null) { // should be impossible for text to be null?
							try {
								scriptToDump.write(String.format(
									"Text of ID 0x%04X\n%s\n\n",
									i, arrayHandle.getText()
								).replaceAll("\\n", Util.newline));
							} catch (Exception e) {}
						}
						return false;
					}

					@Override
					protected void cleanup() {
						try { scriptToDump.close(); }
						catch (Exception e) {}
					}
				}
			)
		);
	}

	private void insert() {
		final Scanner scriptToInsert;
		File fileSelected = CommonDialogs.showOpenFileDialog("script dump");
		if (fileSelected == null) { return; }

		try { scriptToInsert = new Scanner(fileSelected); }
		catch (Exception e) {
			CommonDialogs.showStreamErrorDialog();
			return;
		}

		//int writeIndex;
		//String indexLine;

		view.process(
			true, // show dialogs
			new PositionRestoringProcess(
				// We don't know how many iterations there will be, so we set up
				// an indeterminate process step and do the loop manually.
				new ProcessComponent(1, 0, "Reading script...") {
					@Override
					protected boolean iterate(int i) {
						int index = 1;
						while (writeEntry(index++, scriptToInsert)) {}
						return false;
					}

					@Override
					protected void cleanup() {
						try { scriptToInsert.close(); }
						catch (Exception e) {}
					}
				}
			)
		);
	}

	// Return false when we run out of data.
	@SuppressWarnings("empty-statement")
	private boolean writeEntry(int index, Scanner source) {
		String indexLine = "";
		while (indexLine.equals("")) {
			if (!source.hasNextLine()) { return false; }
			indexLine = source.nextLine();
		};

		try {
			index = Util.parseInt(
				indexLine.substring(indexLine.lastIndexOf(" "))
			) & 0xFFFF;
		} catch (Exception e) { /* Use the default counter value. */ }

		String outString = "";
		String line;
		do {
			if (!source.hasNextLine()) { return false; }
			line = source.nextLine();
			outString += line + "\n";
		} while (!line.endsWith("[X]"));
		outString = outString.trim();
		arrayHandle.moveTo(index);
		arrayHandle.setText(outString);
		return true;
	}
}

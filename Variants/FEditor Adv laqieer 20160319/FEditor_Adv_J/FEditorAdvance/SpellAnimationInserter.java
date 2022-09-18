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
 *  <Description> This class provides a dialog for inserting custom spell
 *  animations
 */

package FEditorAdvance;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import Controls.IntValueListener;
import Controls.LabelledHexSpinner;
import Controls.ArrayPanel;
import Graphics.GBAImage;
import Model.Game;
import Model.SpellAnimationArray;
import Model.SpellProgramCounterArray;

public class SpellAnimationInserter extends Editor<SpellAnimationArray> {
	private String lastPath = "N/A";
	private JTextArea diagnosticLabel = new JTextArea();
	private JScrollPane diagnosticPane;
	private JLabel dimLabel = new JLabel("Dim screen:");
	private JCheckBox dimCheckBox = new JCheckBox();
	private JButton terminateButton;
	private ArrayPanel<SpellAnimationArray> arrayPanel;
	private LabelledHexSpinner commandSpinner;
	private LabelledHexSpinner durationSpinner;

	private SpellProgramCounterArray spellPCarrayInstance;

	public SpellAnimationInserter(View view) {
		super(view);

		JPanel commandSpinnerPanel = new JPanel();
		commandSpinner = new LabelledHexSpinner(
			new IntValueListener() {
				public void newValue(int value) {}
			}, "コマンド入力:", 6,
			0, 0, 0xFFFFFF
		);
		commandSpinnerPanel.add(commandSpinner);
		addButtonToPanel(commandSpinnerPanel, "コマンド追加", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				int commandVal = commandSpinner.getValue();
				arrayHandle.addCommand(
					(byte)commandVal,
					(byte)(commandVal >> 0x10),
					(byte)(commandVal >> 8)
				);
				update();
			}
		});
		commandSpinnerPanel.setMaximumSize(commandSpinnerPanel.getPreferredSize());
		add(commandSpinnerPanel);

		JPanel durationSpinnerPanel = new JPanel();
		durationSpinner = new LabelledHexSpinner(
			new IntValueListener() {
				public void newValue(int value) {}
			}, "入力フレームの持続時間:", 4,
			0, 0, 0xFFFF
		);
		durationSpinnerPanel.add(durationSpinner);
		addButtonToPanel(durationSpinnerPanel, "フレームの挿入", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				try {
					arrayHandle.addFrame(((Integer) durationSpinner.getValue()).shortValue());
				} catch (Exception exc) {
					CommonDialogs.showCatchErrorDialog(exc);
				}
				update();
			}
		});
		durationSpinnerPanel.setMaximumSize(durationSpinnerPanel.getPreferredSize());
		add(durationSpinnerPanel);

		JPanel insertFramePanel = new JPanel();
		addButtonToPanel(insertFramePanel, "Queue Sprites", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				// Prompt user for a bitmap
				File loadedFile = CommonDialogs.showOpenFileDialog("アニメーションフレームの画像");
				try {
					GBAImage testConverter = processImage(loadedFile);
					arrayHandle.queueSprites(testConverter);
				} catch (Exception e2) {
					CommonDialogs.showCatchErrorDialog(e2);
				}
				update();
			}
		});
		addButtonToPanel(insertFramePanel, "Queue Background", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				File loadedFile = CommonDialogs.showOpenFileDialog("アニメーションフレームの背景画像");
				try {
					GBAImage testConverter = processImage(loadedFile);
					arrayHandle.queueBackground(testConverter);
				} catch (Exception e2) {
					CommonDialogs.showCatchErrorDialog(e2);
				}
			}
		});
		insertFramePanel.setMaximumSize(insertFramePanel.getPreferredSize());
		add(insertFramePanel);

		JPanel buttonPanel = new JPanel();
		addButtonToPanel(buttonPanel, "Add Miss Terminator", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				arrayHandle.addModeTerminator(true);
				update();
			}
		});
		terminateButton = addButtonToPanel(buttonPanel, "エフェクト挿入", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				arrayHandle.addModeTerminator(false);

				try {
					arrayHandle.setSpellAnimation();
					spellPCarrayInstance.setPC(dimCheckBox.isSelected());
					javax.swing.JOptionPane.showMessageDialog(
						null,
						"エフェクトを挿入しました!",
						"操作が完了しました",
						javax.swing.JOptionPane.INFORMATION_MESSAGE
					);
				} catch (Exception e) {
					CommonDialogs.showCatchErrorDialog(e);
				}
			}
		});
		buttonPanel.add(dimLabel);
		dimCheckBox.setSelected(true);
		buttonPanel.add(dimCheckBox);
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		add(buttonPanel);

		arrayPanel = new ArrayPanel<SpellAnimationArray>(this, 0x1, 0xFF, 0);
		arrayPanel.setMaximumSize(arrayPanel.getPreferredSize());
		add(arrayPanel);

		JPanel scriptPanel = new JPanel();
		addButtonToPanel(scriptPanel, "スクリプトからロード", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				loadFromScript();
				update();
			}
		});
		addButtonToPanel(scriptPanel, "リセット", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				arrayHandle.reset();
				lastPath = "N/A";
				update();
			}
		});
		addButtonToPanel(scriptPanel, "終了", new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				quit();
			}
		});
		scriptPanel.setMaximumSize(scriptPanel.getPreferredSize());
		add(scriptPanel);

		JPanel diagnosticPanel = new JPanel();
		diagnosticLabel.setEditable(false);
		diagnosticLabel.setColumns(48);
		diagnosticLabel.setRows(4);
		diagnosticPane = new JScrollPane(diagnosticLabel);
		diagnosticPanel.add(diagnosticPane);
		diagnosticPanel.setMaximumSize(diagnosticPanel.getPreferredSize());
		add(diagnosticPanel);
	}

	@Override
	public void setup(Game game) {
		arrayHandle = game.spellAnimationArray();
		spellPCarrayInstance = game.spellProgramCounterArray();
		arrayPanel.setArray(arrayHandle);

		commandSpinner.setValue(0);
		durationSpinner.setValue(0);

		update();
	}

	@Override
	public void refresh() {
		// Keep the array indices in sync.
		spellPCarrayInstance.moveTo(arrayHandle.getPosition());
	}

	@Override
	public void cleanup() {
		// The spell animation array will be saved by the superclass
		// Editor logic, but the PC array needs to be saved explicitly.
		spellPCarrayInstance.resize(arrayHandle.getCurrentSize());
		spellPCarrayInstance.save();
		// Hextator sez: Review "teardown" method comments in Editor
		//arrayPanel.setArray(null); // so it can be GC'd
	}

	private void update() {
		terminateButton.setEnabled(
			arrayHandle.getCommandCount() > 0 && arrayHandle.getFrameCount() > 0
		);
		diagnosticLabel.setText(
			String.format(
				"この魔法の0x85コマンドの数: %d\n" +
				"この魔法エフェクトのフレーム数: %d\n" +
				"Last path: %s",
				arrayHandle.getCommandCount(),
				arrayHandle.getFrameCount(),
				lastPath
			)
		);
	}

	// Convenience method
	private GBAImage processImage(File loadedFile) {
		if (loadedFile != null) { lastPath = loadedFile.getPath(); }
		return new GBAImage(loadedFile);
	}
	// processImage method; tested and working!

	class SyntaxException extends Exception {
		int line;
		String info;

		public SyntaxException(int line) {
			this.line = line;
			info = "!";
		}

		public SyntaxException(int line, String info) {
			this.line = line;
			this.info = ":\n" + info;
		}

		@Override
		public String getMessage() {
			return String.format("Error parsing line %d%s", line, info);
		}
	}

	private void loadFromScript() {
		Scanner scriptFileReader = null;

		try {
			File scriptFile = CommonDialogs.showOpenFileDialog("animation script");
			scriptFileReader = new Scanner(scriptFile);
			loadFromScript_helper(scriptFile, scriptFileReader);
		} catch (SyntaxException se) {
			CommonDialogs.showCatchErrorDialog(se);
		} catch (IOException ioe) {
			CommonDialogs.showStreamErrorDialog();
		} catch (Exception e) {
			CommonDialogs.showGenericErrorDialog("Unexpected error loading or reading the script");
		} finally {
			try { scriptFileReader.close(); }
			catch (Exception e) {}
		}
	}

	// Convenience method
	private void loadFromScript_helper(File scriptFile, Scanner scriptFileReader)
	throws Exception {
		int line = 0;
		int tempInt = 0;
		boolean blockComment = false;

		while (scriptFileReader.hasNextLine()) {
			String loadedScriptLine = scriptFileReader.nextLine();
			line++;

			if (loadedScriptLine.length() == 0) { continue; }

			if (blockComment) {
				if (loadedScriptLine.charAt(0) == '*') { blockComment = false; }
				continue;
			}

			update();

			int i = 0;
			switch (loadedScriptLine.charAt(0)) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				for (i = 0; i < loadedScriptLine.length(); i++) {
					try {
						tempInt = Integer.parseInt(loadedScriptLine.substring(i, i + 1));
					} catch (Exception e) { break; }
				}
				try { tempInt = Integer.parseInt(loadedScriptLine.substring(0, i)); }
				catch (Exception e) { throw new SyntaxException(line); }

				// Hextator: Had to cast to short instead of byte
				// (0x80 was becoming -0x80 and making animations
				// insanely long D:)
				try {
					arrayHandle.addFrame((short)tempInt);
				} catch (Exception e) {
					CommonDialogs.showCatchErrorDialog(e);
				}
				break;

				case '/':
				if (
					loadedScriptLine.length() >= 2 && loadedScriptLine.charAt(1) == '*'
				) { blockComment = true; }
				break;

				case 'D':
				if (loadedScriptLine.length() < 2) { throw new SyntaxException(line); }
				dimCheckBox.setSelected(loadedScriptLine.charAt(1) != '0');
				break; // missing from original?!
				
				case 'C':
				try { 
					tempInt = Integer.parseInt(loadedScriptLine.substring(1, 7), 16);
				} catch (Exception e) {
					try {
						tempInt = Integer.parseInt(loadedScriptLine.substring(1, 3), 16);
					} catch (Exception e2) { throw new SyntaxException(line); }
				}
				arrayHandle.addCommand(
					(byte) tempInt, (byte)(tempInt >> 0x10), (byte)(tempInt >> 8)
				);
				// Is that right? Some weird mixed-endian thing?
				// Hextator says: Big endian view is
				// 0x85
				// Param 1
				// Param 2
				// Command ID
				// and the CXXYYZZ command format is:
				// XX - Param 1
				// YY - Param 2
				// ZZ - Command ID
				// and the code interprets the parameters by shifting the value
				// left 8 bits and then right 16, yielding a short of the middle 16
				// bits. Or, if the parameters are to be interpreted separately, it
				// just right shifts and uses "strb" to only store the bottom 8 bits
				// at the location of the variable the parameter is replacing.
				// After doing a word load on the command the appropriate register will
				// hold a value that appears in the big endian format above (at least,
				// in a common debugger view)
				break;

				case '~':
				arrayHandle.addModeTerminator(true);
				break;

				default:
				if (loadedScriptLine.indexOf("-") == -1) {
					throw new SyntaxException(line);
				}

				boolean usePath = loadedScriptLine.indexOf("p-") != -1;

				final int addFrame = 0;
				final int addBG = 1;
				int type = addFrame;
				if (loadedScriptLine.charAt(0) == 'O') { type = addFrame; }
				else if (loadedScriptLine.charAt(0) == 'B') { type = addBG; }
				else { throw new SyntaxException(line); }

				i = loadedScriptLine.indexOf("-");
				if (++i >= loadedScriptLine.length()) { loadedScriptLine = "a frame"; }
				else {
					for (; i < loadedScriptLine.length(); i++) {
						if (loadedScriptLine.charAt(i) != ' ') { break; }
					}
					loadedScriptLine = loadedScriptLine.substring(i);
				}

				File loadedFile;
				if (!usePath) {
					// Prompt user for a bitmap
					loadedFile = CommonDialogs.showOpenFileDialog(loadedScriptLine);
				} else {
					loadedFile = new File(
						scriptFile.getPath().substring(
							0, scriptFile.getParent().length() + 1
						) + loadedScriptLine
					);
				}
				if (loadedFile == null) { throw new IOException(); }

				lastPath = loadedFile.getPath();
				GBAImage testConverter = processImage(loadedFile);

				if (type == addFrame) { arrayHandle.queueSprites(testConverter); }
				else if (type == addBG) { arrayHandle.queueBackground(testConverter); }
				break;
			}
		}
	}
	// loadFromScript method; tested and working!
}

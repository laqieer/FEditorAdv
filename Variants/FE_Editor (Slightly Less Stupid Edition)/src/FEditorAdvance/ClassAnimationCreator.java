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
 *  <Description> This class provides a dialog for inserting custom battle
 *  animations for classes
 */

package FEditorAdvance;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JButton;
//import javax.swing.JLabel;
import Controls.LabelledHexSpinner;
import Graphics.GBAImage;
import Model.Game;
import Model.AnimationBuilder;
import Model.PointerArray;

// FIXME: Extract common code between weapon and spell animation inserters.

// XXX Doesn't actually use a pointer array. Instead, it works with an
// AnimationBuilder to create an animation.
public class ClassAnimationCreator
extends Editor<PointerArray>
implements ActionListener {
	private JPanel buttonPanel = new JPanel();
	//private JPanel soundSpinnerPanel = new JPanel();
	//private JPanel durationSpinnerPanel = new JPanel();

	private LabelledHexSpinner commandValue = new LabelledHexSpinner(
		null, "Input command:", 2
	);

	private LabelledHexSpinner durationValue = new LabelledHexSpinner(
		null, "Input frame duration:", 2 // based on AnimationBuilder.MAX_DURATION_ALLOWED
	);

	private LabelledHexSpinner soundValue = new LabelledHexSpinner(
		null, "Input music ID:", 4
	);

	private JTextArea diagnosticLabel = new JTextArea();
	private JScrollPane diagnosticPane;
	private JPanel diagnosticPanel = new JPanel();
	private JPanel scriptPanel = new JPanel();
	private JButton insertButton = new JButton("Insert Frame");
	private JButton insertThrowableButton = new JButton("Insert Throwable Sprite");
	private JPanel insertThrowablePanel = new JPanel();
	private JButton commandButton = new JButton("Add Command");
	private JButton soundButton = new JButton("Add Sound");
	private JButton modeTerminatorButton = new JButton("Add Mode Terminator");
	private JButton loopButton = new JButton("Add Loop Marker");
	private JButton applyButton = new JButton("Apply Animation");
	private JButton scriptButton = new JButton("Load From Script");
	private JButton resetButton = new JButton("Reset Animation");
	private JButton quitButton = new JButton("Quit");
	private JButton saveButton = new JButton("Save To File...");

	private static boolean busy = false;

	private AnimationBuilder builder;

	// FIXME Make script reading a process. Make processes interruptible?
	private String lastPath = "N/A";

	public ClassAnimationCreator(View view) {
		super(view);

		this.view = view;

		insertThrowableButton.addActionListener(this);
		insertThrowablePanel.add(insertThrowableButton);
		insertThrowablePanel.setMaximumSize(insertThrowablePanel.getPreferredSize());
		add(insertThrowablePanel);

		JPanel commandSpinnerPanel = new JPanel();
		commandSpinnerPanel.add(commandValue);
		commandButton.addActionListener(this);
		commandSpinnerPanel.add(commandButton);
		commandSpinnerPanel.setMaximumSize(commandSpinnerPanel.getPreferredSize());
		add(commandSpinnerPanel);

		JPanel soundSpinnerPanel = new JPanel();
		soundSpinnerPanel.add(soundValue);
		soundButton.addActionListener(this);
		soundSpinnerPanel.add(soundButton);
		soundSpinnerPanel.setMaximumSize(soundSpinnerPanel.getPreferredSize());
		add(soundSpinnerPanel);

		JPanel durationSpinnerPanel = new JPanel();
		durationSpinnerPanel.add(durationValue);
		insertButton.addActionListener(this);
		durationSpinnerPanel.add(insertButton);
		durationSpinnerPanel.setMaximumSize(durationSpinnerPanel.getPreferredSize());
		add(durationSpinnerPanel);

		modeTerminatorButton.addActionListener(this);
		modeTerminatorButton.setEnabled(false);
		buttonPanel.add(modeTerminatorButton);
		loopButton.addActionListener(this);
		loopButton.setEnabled(false);
		buttonPanel.add(loopButton);
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		add(buttonPanel);

		scriptButton.addActionListener(this);
		scriptPanel.add(scriptButton);
		resetButton.addActionListener(this);
		scriptPanel.add(resetButton);
		saveButton.addActionListener(this);
		scriptPanel.add(saveButton);
		quitButton.addActionListener(this);
		scriptPanel.add(quitButton);
		scriptPanel.setMaximumSize(scriptPanel.getPreferredSize());
		add(scriptPanel);

		diagnosticLabel.setEditable(false);
		diagnosticLabel.setColumns(48);
		diagnosticLabel.setRows(5);
		diagnosticPane = new JScrollPane(diagnosticLabel);
		diagnosticPanel.add(diagnosticPane);
		diagnosticPanel.setMaximumSize(diagnosticPanel.getPreferredSize());
		add(diagnosticPanel);
	}

	@Override
	public void setup(Game game)
	{
		// array is left as null
		builder = new AnimationBuilder();

		update();
	}

	private void update() {
		String diagnosticString = "";
		if (builder.getMode() == AnimationBuilder.MAX_MODE_COUNT)
			diagnosticString += "Mode: Complete\n";
		else
			diagnosticString += String.format
			(
				"Mode: %d\n",
				builder.getMode() + 1
			);
		//diagnosticString += String.format
		//(
		//	"0x85 command count for this mode: %d\n",
		//	builder.getCommandCount()
		//);
		diagnosticString += String.format
		(
			"Animation frame count for this mode: %d\n",
			builder.getFrameCount()
		);
		diagnosticString += "Last path: " + lastPath;
		diagnosticLabel.setText(diagnosticString);
		if
		(
			builder.getMode() > 0
			|| builder.getFrameCount() > 0
			//|| builder.getCommandCount() > 0
		)
			insertThrowableButton.setEnabled(false);
		if
		(
			builder.getFrameCount() == 0
			//|| builder.getCommandCount() == 0
		)
			modeTerminatorButton.setEnabled(false);
		else
			modeTerminatorButton.setEnabled(true);
		if (builder.getMode() < AnimationBuilder.MAX_MODE_COUNT)
			applyButton.setEnabled(false);
		else
			applyButton.setEnabled(true);
		if (!builder.canLoop() || builder.hasLoop())
			loopButton.setEnabled(false);
		else
			loopButton.setEnabled(true);
	}

	// Convenience method
	private GBAImage processImage(File loadedFile) {
		if (loadedFile != null) {
			lastPath = loadedFile.getPath();
		}
		return new GBAImage(loadedFile, builder.sharedPalette());
	}
	// processImage method; tested and working!

	class SyntaxException extends Exception
	{
		int line;
		String info;

		public SyntaxException(int line)
		{
			this.line = line;
			info = "!";
		}

		public SyntaxException(int line, String info)
		{
			this.line = line;
			this.info = ":\n" + info;
		}

		@Override
		public String getMessage()
		{
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
			CommonDialogs.showGenericErrorDialog(
				"Unexpected error loading or reading the script"
			);
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

		while (scriptFileReader.hasNextLine())
		{
			String loadedScriptLine = scriptFileReader.nextLine();
			line++;

			if (loadedScriptLine.length() == 0) { continue; }

			if (blockComment)
			{
				if (loadedScriptLine.charAt(0) == '*') { blockComment = false; }
				continue;
			}

			update();

			switch (loadedScriptLine.charAt(0))
			{
				case '/':
				if (loadedScriptLine.length() >= 2 && loadedScriptLine.charAt(1) == '*')
				{
					blockComment = true;
				}
				break;

				case 'C':
				try { tempInt = Integer.parseInt(loadedScriptLine.substring(1, 3), 16); }
				catch (Exception e) { throw new SyntaxException(line); }
				builder.addCommand((byte) tempInt);
				break;

				case 'S':
				try { tempInt = Integer.parseInt(loadedScriptLine.substring(1, 5), 16); }
				catch (Exception e) { throw new SyntaxException(line); }
				builder.addSoundCommand((short) tempInt);
				break;

				case 'L':
				if (!builder.canLoop()) {
					throw new SyntaxException(line, "There is nowhere to loop to.");
				} else if (builder.hasLoop()) {
					throw new SyntaxException(line, "Can't specify more than one loop.");
				}
				try {
					builder.addLoopMarker();
				} catch (Exception e) {
					CommonDialogs.showCatchErrorDialog(e);
				}
				break;

				case '~':
				if (builder.getFrameCount() == 0)
				{
					throw new SyntaxException
					(
						line,
						"Can't add a terminator to an empty mode."
					);
				}
				try {
					builder.addModeTerminator();
				} catch (Exception e) {
					CommonDialogs.showCatchErrorDialog(e);
				}
				break;

				default:
				if (loadedScriptLine.indexOf("-") == -1)
				{
					throw new SyntaxException(line);
				}
					
				boolean usePath = loadedScriptLine.indexOf("p-") != -1;

				final int addFrame = 0;
				final int addThrowable = 1;
				int type = addFrame;
				if (loadedScriptLine.charAt(0) == 'T')
				{
					type = addThrowable;
					if (builder.getMode() > 0 || builder.getFrameCount() > 0)
					{
						throw new SyntaxException
						(
							line,
							"Throwable sprites can only be specified at the beginning"
						);
					}
				}
				int i = 0;
				if (type == addFrame)
				{
					// FIXME: use a Scanner or something
					// Jeez, even C++ makes parsing easier than this
					for (; i < loadedScriptLine.length(); i++)
					{
						try { tempInt = Integer.parseInt(loadedScriptLine.substring(i, i + 1)); }
						catch (Exception e) { break; }
					}
					try { tempInt = Integer.parseInt(loadedScriptLine.substring(0, i)); }
					catch (Exception e) { throw new SyntaxException(line); }
				}
				int duration = tempInt;
				i = loadedScriptLine.indexOf("-");
				if (++i >= loadedScriptLine.length())
				{
					loadedScriptLine = "a frame";
				}
				else
				{
					for (; i < loadedScriptLine.length(); i++)
					{
						if (loadedScriptLine.charAt(i) != ' ') { break; }
					}
					loadedScriptLine = loadedScriptLine.substring(i);
				}
				File loadedFile;
				if (!usePath)
				{
					//Prompt user for a bitmap
					loadedFile = CommonDialogs.showOpenFileDialog(loadedScriptLine);
				}
				else
				{
					loadedFile = new File
					(
						scriptFile.getPath().substring
						(
							0,
							scriptFile.getParent().length()	+ 1
						)
						+ loadedScriptLine
					);
				}
				if (loadedFile == null)
				{
					throw new IOException();
				}
				lastPath = loadedFile.getPath();
				GBAImage testConverter = processImage(loadedFile);
				switch (type)
				{
					case addFrame:
					// Hextator: Made this a do while to force
					// additions of frames with a duration of 0
					// (which is valid) without causing an infinite loop
					// (which was a result of using <= instead of a concrete
					// inequality)
					do
					{
						int tempDuration = Math.min(
							duration, AnimationBuilder.MAX_DURATION_ALLOWED
						);
						duration -= tempDuration;
						// Hextator: Should be allowed to use shorts
						// (Invalid durations are caught by the model; the duration
						// actually does span 2 bytes in the data, it's just that
						// the maximum SAFE value only needs a byte)
						boolean success = true;
						try {
							success = builder.addFrame(testConverter, (short)tempDuration);
						} catch (Exception e) {
							CommonDialogs.showCatchErrorDialog(e);
							success = false;
						}
						if (!success)
							//CommonDialogs.showGenericErrorDialog("Invalid frame!");
							return;
					} while (duration > 0);
					break;

					case addThrowable:
					try {
						builder.setThrowableSprite(testConverter);
					} catch (Exception e) {
						CommonDialogs.showCatchErrorDialog(e);
					}
				}
				break;
			}
		}
	}
	// loadFromScript method; tested and working

	// ActionListening code

	public void actionPerformed(ActionEvent e) {
		// FIXME: This is breaking modularity
		if (busy) {
			CommonDialogs.showBusyDialog(this);
			return;
		}

		String command = e.getActionCommand();

		// FIXME: Make script reading interruptible by quit command.
		// Actually, make everything cancel-able in a nice way.
		if (command.equals("Quit")) {
			quit();
			return;
		}

		busy = true;

		if (command.equals("Save To File...")) {
			try {
				builder.create().writeToFile(
					CommonDialogs.showSaveFileDialog("new animation")
				);
				javax.swing.JOptionPane.showMessageDialog(
					null,
					"Animation saved!",
					"Operation complete",
					javax.swing.JOptionPane.INFORMATION_MESSAGE
				);
			} catch (IOException ioe) {
				CommonDialogs.showStreamErrorDialog();
			} catch (Exception e2) {
				CommonDialogs.showCatchErrorDialog(e2);
			}
		} else if (command.equals("Insert Frame")) {
			// Prompt user for a bitmap
			File loadedFile = CommonDialogs.showOpenFileDialog("image of animation frame");
			try {
				GBAImage testConverter = processImage(loadedFile);
				// Hextator: See note in the script processor about why
				// the duration is expected to be a short now
				builder.addFrame(
					testConverter, (short)(durationValue.getValue())
				);
			} catch (Exception e2) {
				CommonDialogs.showCatchErrorDialog(e2);
			}
		} else if (command.equals("Add Command")) {
			builder.addCommand((byte)(commandValue.getValue()));
		} else if (command.equals("Add Sound")) {
			builder.addSoundCommand((short)(soundValue.getValue()));
		} else if (command.equals("Add Mode Terminator")) {
			try {
				builder.addModeTerminator();
			} catch (Exception exc) {
				CommonDialogs.showCatchErrorDialog(exc);
			}
		} else if (command.equals("Save Animation...")) {
			File tempFile = CommonDialogs.showSaveFileDialog("animation");
			try {
				if (tempFile != null) {
					builder.create().writeToFile(tempFile);
				}
			} catch (Exception e2) {
				CommonDialogs.showStreamErrorDialog();
			}
		} else if (command.equals("Load From Script")) {
			loadFromScript();
		} else if (command.equals("Reset Animation")) {
			builder = new AnimationBuilder();
			lastPath = "N/A";
			insertThrowableButton.setEnabled(true);
			loopButton.setEnabled(true);
			modeTerminatorButton.setEnabled(false);
			applyButton.setEnabled(false);
		} else if (command.equals("Insert Throwable Sprite")) {
			// Prompt user for a bitmap
			File loadedFile = CommonDialogs.showOpenFileDialog("image of throwable sprite");
			try {
				builder.setThrowableSprite(processImage(loadedFile));
			} catch (Exception e3) {
				CommonDialogs.showCatchErrorDialog(e3);
			}
		} else if (command.equals("Add Loop Marker")) {
			try {
				builder.addLoopMarker();
			} catch (Exception exc) {
				CommonDialogs.showCatchErrorDialog(exc);
			}
		}

		busy = false;
		update();
	}
}

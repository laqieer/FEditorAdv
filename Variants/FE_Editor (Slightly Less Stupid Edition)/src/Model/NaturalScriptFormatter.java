/*
 *  FE Editor - GBA Fire Emblem (U) ROM editor
 *
 *  Copyright (C) 2008-2011 Hextator,
 *  hextator (AIM) hextator@gmail.com (MSN)
 *
 *  Thanks to Zahlman (AIM/MSN: zahlman@gmail.com) for specification.
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
 *  <Description> This class provides an interface for formatting scripts
 *  of the format specified by Zahlman into the format specified by FEditor
 */

package Model;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Scanner;
import Controls.Process;
import Controls.ProcessComponent;

public class NaturalScriptFormatter {
	private Game owner = null;
	private HashMap<String, Integer> widthMap;
	private int maxWidth;

	NaturalScriptFormatter() {}

	public NaturalScriptFormatter(Game game) {
		owner = game;
	}

	class Speaker {
		public static final int farfarleft = 0;
		public static final int farleft = 1;
		public static final int left = 2;
		public static final int nearleft = 3;
		public static final int nearright = 4;
		public static final int right = 5;
		public static final int farright = 6;
		public static final int farfarright = 7;
		public static final int exit = 8;

		Integer portraitID;
		Integer position;
		boolean valid = false;

		Speaker(Integer ID, Integer pos) { portraitID = ID; position = pos; validate(); }

		private void validate() {
			/*
			if (
				portraitID == null ||
				position == null ||
				position < 0 ||
				position > 8 ||
				portraitID < 0 ||
				portraitID > owner.portraitArray().maxSize()
			) {
				valid = false;
			}
			else valid = true;
			*/
			valid = true; // isValid isn't used anyway
		}

		public Integer getPortraitID() { return portraitID; }
		public Integer getPosition() { return position; }
		public boolean isValid() { return valid; }

		public void setPortraitID(Integer ID) {
			portraitID = ID;
			validate();
		}
		public void setPosition(Integer pos) {
			position = pos;
			validate();
		}
	}

	// Ugly helper
	private static Integer parsePositionString(String input) {
		Integer position = null;
		input = input.toLowerCase();
		if (
			input.equals("ffl") ||
			input.equals("farfarleft") ||
			input.equals("far-far-left")
		)
			position = Speaker.farfarleft;
		else if (
			input.equals("fl") ||
			input.equals("farleft") ||
			input.equals("far-left")
		)
			position = Speaker.farleft;
		else if (
			input.equals("l") ||
			input.equals("left") ||
			input.equals("midleft") ||
			input.equals("mid-left")
		)
			position = Speaker.left;
		else if (
			input.equals("nl") ||
			input.equals("nearleft") ||
			input.equals("near-left")
		)
			position = Speaker.nearleft;
		else if (
			input.equals("nr") ||
			input.equals("nearright") ||
			input.equals("near-right")
		)
			position = Speaker.nearright;
		else if (
			input.equals("r") ||
			input.equals("right") ||
			input.equals("midright") ||
			input.equals("mid-right")
		)
			position = Speaker.right;
		else if (
			input.equals("fr") ||
			input.equals("farright") ||
			input.equals("far-right")
		)
			position = Speaker.farright;
		else if (
			input.equals("ffr") ||
			input.equals("farfarright") ||
			input.equals("far-far-right")
		)
			position = Speaker.farfarright;
		else if (
			input.equals("x") ||
			input.equals("exit") ||
			input.equals("exits") ||
			input.equals("leaves")
		)
			position = Speaker.exit;
		return position;
	}
	// Tested and working

	// For unspecified games
	private void initializeDefaultWidthMap() {
		widthMap = new HashMap<String, Integer>();
		for (int i = 0xFF; i >= 0; i--)
			widthMap.put(String.format("%c", i), 1);
		maxWidth = 32;
	}

	// Ugly helper
	private String appendToEvenLines(String input) {
		String output = "";
		String[] delimited = input.split(Util.newline);
		if (delimited.length % 2 == 1)
			output = appendToOddLines(input);
		else
			for (int i = 0; i < delimited.length;) {
				output += delimited[i++] + Util.newline;
				String temp = delimited[i++] + "[A]";
				temp = temp.replaceAll("\\[NoA\\]\\[A\\]", "");
				output += temp + Util.newline;
			}
		return output;
	}
	// Tested and working

	// Ugly helper
	private String appendToOddLines(String input) {
		String[] blah = input.split(Util.newline, 2);
		if (blah == null | blah.length < 2)
			return "";
		return blah[0] + Util.newline + appendToEvenLines(blah[1]);
	}
	// Tested and working

	// Helper
	private String formatDialogText(String input) {
		input = input.replaceAll("_", "[ToggleRed]");
		input = input.replaceAll("\t", "[0x02]");
		if (owner != null) {
			widthMap = owner.getTextWidthMap();
			maxWidth = owner.getMaxTextWidth();
		}
		if (owner == null || widthMap == null)
			initializeDefaultWidthMap();
		String output = "";

		int lines = 0;
		int character = 0;
		int start = 0;
		int currWidth = 0;
		int end = 0;
		boolean firstSpace = true;
		boolean beginningOfLine = true;
		while (true) {
			String currChar = String.format("%c", input.charAt(character));
			// Handle control codes
			if (currChar.equals("[") && input.indexOf("]", character + 1) != -1) {
				beginningOfLine = false;
				int tempEnd = input.indexOf("]", character + 1) + 1;
				String temp = input.substring(character, tempEnd);
				character = tempEnd;
				Integer value = null;
				try {
					value = Util.parseInt(temp.substring(3, 5));
				} catch (Exception e) {}
				if (value != null && value <= 0xFF && value > 0) {
					if (value != 1) {
						try {
							currWidth += widthMap.get(
								String.format("%c", value)
							);
						} catch (Exception e) {}
					}
					// Handle newline control code
					else {
						output += input.
							substring(start, character).
							replaceAll("\\[0x01\\]", "").
							trim() + Util.newline;
						start = end = character;
						currWidth = 0;
						beginningOfLine = true;
						firstSpace = true;
						lines++;
					}
				}
			}
			// Handle spaces
			else if (currChar.equals(" ")) {
				if (firstSpace) {
					firstSpace = false;
					end = character;
				}
				if (!beginningOfLine) {
					try {
						currWidth += widthMap.get(" ");
					} catch (Exception e) {}
				}
				else {
					start++;
				}
				character++;
			}
			// Handle newlines
			else if (currChar.equals(Util.newline) || currChar.equals("\n")) {
				output += input.
					substring(start, character).
					replaceAll(currChar, "").
					trim() + Util.newline;
				start = end = ++character;
				currWidth = 0;
				beginningOfLine = true;
				firstSpace = true;
				lines++;
			}
			// Handle all other characters
			else {
				beginningOfLine = false;
				firstSpace = true;
				currWidth += widthMap.get(currChar);
				character++;
			}
			// Handle excess width
			if (currWidth > maxWidth) {
				output += input.
					substring(start, end).
					trim() + Util.newline;
				start = end;
				currWidth = 0;
				character = start;
				beginningOfLine = true;
				firstSpace = true;
				lines++;
			}
			if (character >= input.length()) {
				output += input.
					substring(start, character).
					trim();
				lines++;
				break;
			}
		}

		if (lines < 3) {
			output += "[A]";
			output = output.replaceAll("\\[NoA\\]\\[A\\]", "");
		}
		else if (lines % 2 == 0)
			output = appendToEvenLines(output);
		else
			output = appendToOddLines(output);

		return output;
	}
	// Tested and working

	// Helper
	private void outputFormattedScript(
		LinkedList<String> outputFileList,
		LinkedList<String> outputList,
		File source
	) {
		String parentPath = source.getParent() + File.separator;
		FileWriter outputWriter = null;
		int originalSize = outputList.size();
		for (int i = 0; i < originalSize; i++) {
			try {
				String outputFileName = outputFileList.remove(0);
				if (outputFileName.indexOf(".") == -1)
					outputFileName += ".txt";
				outputWriter = new FileWriter(new File(parentPath + outputFileName));
				outputWriter.write(outputList.remove(0).trim() + System.getProperty("line.separator"));
			} catch (Exception e) {}
			finally {
				try {
					outputWriter.close();
				} catch (Exception e) {}
			}
		}
	}
	// Tested and working

	// Convenience method for outputting an array of Strings of text entries
	// obtained from a text file formatted after a standardized script form
	// that is more "natural" than the format expected by setText(String)
	public void formatNaturalScript(File input) {
		LinkedList<String> outputFileList = new LinkedList<String>();
		LinkedList<String> outputList = new LinkedList<String>();

		// "RAM" simulation locals
		// Don't make a file for the imaginary text being terminated
		// by the first heading declaration!
		boolean first = true;
		// This helps with naming output files when no name is specified
		int count = 1;
		// This helps optimize out redundant position selection control codes
		Integer lastPosition = null;
		// This is the title of the file to output to, initialized
		// to a reasonable name in case all name declarations are empty
		String title = "Formatted Text";
		// Extension for above
		String extension = ".txt";
		// This is the String for the current text index that will
		// get its own file
		String currTextEntry = "";
		// This HashMap allows Speaker information to be indexed using
		// the name of the speaker
		HashMap<String, Speaker> characters =
			new HashMap<String, Speaker>();
		// This LinkedList is used as a queue for who is on the screen
		LinkedList<String> onScreen = new LinkedList<String>();

		Scanner inputScanner = null;
		try {
			inputScanner = new Scanner(input);
		} catch (Exception e) {
			return;
		}

		String loadedString = null;
		boolean success = false;
		do {
			try {
				loadedString = inputScanner.nextLine();
				success = true;
			} catch (Exception e) {
				loadedString = null;
				success = false;
			}
			// Handle empty lines
			if (loadedString == null || loadedString.length() == 0) {
				currTextEntry += Util.newline;
				continue;
			}
			// Handle comment and heading lines
			else if (loadedString.charAt(0) == '#') {
				// Handle comment lines
				if (loadedString.length() <= 1) {
					continue;
				}
				// Handle heading lines
				else if (loadedString.charAt(1) == '#') {
					if (!first) {
						// Optimization
						currTextEntry = currTextEntry.replaceAll(
							"\\[ToggleColorInvert\\]\\[ToggleColorInvert\\]", ""
						);
						outputList.add(currTextEntry + "[X]");
						currTextEntry = "";
					}
					else {
						first = false;
					}
					String tempTitle = loadedString.substring(2).trim();
					// Handle renaming for cases when file name is specified
					if (!tempTitle.equals("")) {
						count = 1;
						title = tempTitle;
						if (title.indexOf(".") == -1)
							extension = "";
						else {
							extension = title.substring(title.lastIndexOf("."));
							title = title.substring(0, title.length() - extension.length());
						}
						outputFileList.add(
							title +
							((count > 1) ? (" " + count) : "") +
							extension
						);
						for (Speaker currEntry: characters.values())
							currEntry.setPosition(null);
					}
					// Handle renaming for cases when file name is not specified
					else {
						String tempExtension;
						String oldTitle;
						if (outputFileList.isEmpty())
							oldTitle = title;
						else
							oldTitle = outputFileList.removeLast();
						if (oldTitle.indexOf(".") == -1)
							tempExtension = "";
						else {
							try {
								tempExtension = title.substring(title.lastIndexOf("."));
							} catch (Exception e) {
								tempExtension = "";
							}
							title = title.substring(0, title.length() - tempExtension.length());
						}
						if (count == 1)
							outputFileList.addLast(
								oldTitle + " 1" + tempExtension
							);
						outputFileList.add(title + " " + ++count + tempExtension);
					}
				}
				else continue;
			}
			// Handle dialog and definitions
			else {
				// Remove comments
				int commentIndex;
				commentIndex = loadedString.indexOf("#");
				if (commentIndex != -1)
					loadedString = loadedString.substring(0, commentIndex);

				// Handle definitions
				if (loadedString.indexOf("=") != -1) {
					String[] tokens = loadedString.split("=");
					String name = tokens[0].trim();
					String IDstring = tokens[1].trim();
					int ID = -1;
					try {
						ID = Util.parseInt(IDstring);
					} catch (Exception e) {
						// Uh-oh, invalid definition!
						// Silently failing:
						continue;
					}
					characters.put(name, new Speaker(ID, null));
				}
				// Handle actual dialog
				else {
					// String to do line wrapping and [A] control code
					// appending to
					String name = null;
					String positionString = null;
					String dialogTypeString = null;
					String theText = "";
					String tokens[] = null;
					// Don't prepend "[Open]" preamble
					boolean justText = false;
					// Decide how many tokens there are and
					// separate them
					if (
						loadedString.indexOf(".") != -1 ||
						loadedString.indexOf(":") != -1 ||
						loadedString.indexOf("@") != -1 ||
						loadedString.indexOf("!") != -1
					) {
						int periodIndex = loadedString.indexOf(".");
						int colonIndex = loadedString.indexOf(":");
						int atIndex = loadedString.indexOf("@");
						int exclamationIndex = loadedString.indexOf("!");
						int compareIndex = -1;
						if (periodIndex != -1) {
							dialogTypeString = "[.]";
							compareIndex = periodIndex;
						}
						if (colonIndex != -1) {
							if (
								colonIndex < compareIndex ||
								compareIndex == -1
							) {
								dialogTypeString = ":";
								compareIndex = colonIndex;
							}
						}
						if (atIndex != -1) {
							if (
								atIndex < compareIndex ||
								compareIndex == -1
							) {
								dialogTypeString = "@";
								compareIndex = atIndex;
							}
						}
						if (exclamationIndex != -1) {
							if (
								exclamationIndex < compareIndex ||
								compareIndex == -1
							) {
								dialogTypeString = "!";
							}
						}
						tokens = loadedString.split(dialogTypeString, 2);
						// Regexs are dumb sometimes
						if (dialogTypeString.equals("[.]"))
							dialogTypeString = ".";
						String[] temp = tokens[0].split("[ \t]+");
						if (temp != null && temp.length <= 2 && temp.length > 0) {
							theText = tokens[1].trim();
						}
						else if (temp == null || temp.length == 0) {
							justText = true;
							dialogTypeString = ".";
						}
						else
							dialogTypeString = ".";
						name = temp[0].replaceAll(",", "");
						positionString = temp[(temp.length != 1) ? 1 : 0].replaceAll(",", "");
					}
					else {
						tokens = loadedString.split("[ \t]+", 3);
						if (tokens != null && tokens.length > 0) {
							name = tokens[0].replaceAll(",", "");
						}
						if (
							characters.get(name) == null &&
							tokens != null &&
							tokens.length > 0
						) {
							positionString = tokens[0].replaceAll(",", "");
						}
						else if (
							tokens != null &&
							tokens.length > 1
						) {
							positionString = tokens[1].replaceAll(",", "");
						}
						dialogTypeString = ".";
					}
					// Sanity (unnecessary?)
					if (theText == null) {
						theText = "";
						dialogTypeString = ".";
					}
					// Format name token
					name = characters.get(name) == null ? null : name;
					// Format position token
					Integer position = null;
					position = parsePositionString(
						positionString
					);
					if (name == null && position == null) {
						// Assume it's just text
						theText += loadedString + Util.newline;
						justText = true;
					}
					// Format dialog type token
					final int noSpeech = 0;
					final int speech = 1;
					final int invertColors = 2;
					final int forceMove = 3;
					// If no dialog type is specified,
					// assume no speech
					int dialogType = noSpeech;
					if (dialogTypeString.equals(":"))
						dialogType = speech;
					else if (dialogTypeString.equals("@"))
						dialogType = invertColors;
					else if (dialogTypeString.equals("!"))
						dialogType = forceMove;
					// Information on preamble
					boolean move = false;
					boolean load = false;
					boolean exit = false;
					// Skip if irrelevant, otherwise, set the previous
					// booleans appropriately
					if (!justText && name != null) {
						if (position == null)
							position = characters.get(name).getPosition();
						else if (
							characters.get(name).getPosition() == null
						)
							load = true;
						else if (
							position != characters.get(name).getPosition() &&
							position != Speaker.exit
						)
							move = true;
						else if (position == Speaker.exit) {
							Integer temp = characters.get(name).getPosition();
							characters.get(name).setPosition(null);
							position = temp;
							exit = true;
						}
						else {
							// Request for reload implied where appropriate
							if (dialogType != forceMove)
								load = true;
							else
								move = true;
						}
					}

					// Do preamble for theText
					if (dialogType == invertColors) {
						currTextEntry += "[ToggleColorInvert]";
					}
					String[] positionStrings = new String[] {
						"FarFarLeft",
						"FarLeft",
						"MidLeft",
						"Left",
						"Right",
						"MidRight",
						"FarRight",
						"FarFarRight"
					};
					if (!justText) {
						try {
							if (load && position != Speaker.exit) {
								// Handle issue of having too many characters on screen
								// For checking if the face is being loaded over an existing one
								boolean alreadyOpen = false;
								for (int i = 0; i < onScreen.size(); i++) {
									// Clean up face being overwritten
									if (position == characters.get(onScreen.get(i)).getPosition()) {
										alreadyOpen = true;
										String tempString = onScreen.remove(i);
										characters.get(tempString).setPosition(null);
										break;
									}
								}
								if (onScreen.size() >= 4 && !alreadyOpen) {
									String remove = onScreen.removeFirst();
									int removePos = characters.get(remove).getPosition();
									if (lastPosition != removePos)
										currTextEntry +=
											"[Open" +
											positionStrings[removePos] +
											"]";
									lastPosition = removePos;
									currTextEntry += "[ClearFace]";
									characters.get(remove).setPosition(null);
								}
								if (lastPosition != position)
									currTextEntry +=
										"[Open" +
										positionStrings[position] +
										"]";
								lastPosition = position;
								currTextEntry +=
									"[LoadFace][0x" +
									String.format(
										"%02X",
										characters.get(name).getPortraitID()
									);
								currTextEntry += "][0x01]";
								characters.get(name).setPosition(position);
								onScreen.addLast(name);
							}
							else if (move) {
								if (lastPosition != characters.get(name).getPosition())
									currTextEntry +=
										"[Open" +
										positionStrings[characters.get(name).getPosition()] +
										"]";
								lastPosition = position;
								currTextEntry +=
									"[Move" +
									positionStrings[position] +
									"]";
								characters.get(name).setPosition(position);
							}
							else if (exit) {
								if (lastPosition != position)
									currTextEntry +=
										"[Open" +
										positionStrings[position] +
										"]";
								lastPosition = position;
								currTextEntry += "[ClearFace]";
								characters.get(name).setPosition(null);
								// Character is no longer on screen;
								// update queue to reflect this
								// NOTE: It should be impossible for
								// duplicate names to be in the queue,
								// so no assertions about whether the
								// "right one" is being removed are made
								onScreen.remove(name);
							}
							else {
								if (lastPosition != position)
									currTextEntry +=
										"[Open" +
										positionStrings[position] +
										"]";
								else
									currTextEntry += Util.newline;
								lastPosition = position;
							}
						} catch (Exception e) {

						}
					}
					// Fix text only lines.
					else {
						currTextEntry += formatDialogText(Util.newline + loadedString);
						continue;
					}

					// Don't deal with dialog text if
					// dialog type says it should be ignored
					if (dialogType == noSpeech)
						continue;

					// Format text with line width consideration
					// and add it to the text entry
					if (!theText.equals(""))
						currTextEntry += formatDialogText(theText);

					// Color inversion footer for current
					// speaker's dialog
					if (dialogType == invertColors)
						currTextEntry += "[ToggleColorInvert]";
				}
			}
		} while (success);
		// Handle final text entry
		outputList.add(currTextEntry + "[X]");

		try {
			inputScanner.close();
		} catch (Exception e) {
			// Oh my.
		}

		if (outputList.size() == 0)
			return;
		outputFormattedScript(outputFileList, outputList, input);
	}
	// Tested and working
	
	public static void convert() {
		try {
			File testScript = FEditorAdvance.CommonDialogs.showOpenFileDialog("natural script");
			(new NaturalScriptFormatter()).formatNaturalScript(testScript);
		} catch (Exception e) {}
	}

	public static void main(String[] args) {
		try {
			File testScript = FEditorAdvance.CommonDialogs.showOpenFileDialog("natural script");
			(new NaturalScriptFormatter()).formatNaturalScript(testScript);
		} catch (Exception e) {}
		System.exit(0);
	}
}

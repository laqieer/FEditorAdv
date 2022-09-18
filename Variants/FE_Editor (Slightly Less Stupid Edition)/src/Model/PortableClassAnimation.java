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
 *  <Description> Record of binary data representing a class battle animation
 *  loaded for importing to another FE GBA game
 */

package Model;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import Compression.LempelZiv;
import Graphics.GBAImage;
import Graphics.GBASpritesheet;
import Graphics.Palette;
import FEditorAdvance.CommonDialogs;

public class PortableClassAnimation implements Serializable {
	// Constants
	public static final int NAME_LENGTH = 0x0000000C;
	public static final int MAX_MODE_COUNT = 0x0000000C;

	public static final short[] BLUES = new short[] { (short)0x7F73, (short)0x766C, (short)0x6168, (short)0x5085 };
	public static final short[] REDS = new short[] { (short)0x4B5F, (short)0x225F, (short)0x001A, (short)0x0410 };
	public static final short[] GREENS = new short[] { (short)0x5BFB, (short)0x1BE7, (short)0x26C8, (short)0x19C3 };
	public static final short[] PURPLES = new short[] { (short)0x737B, (short)0x5EB6, (short)0x4E0F, (short)0x3929 };

	// A name copied into the pointer array - not actually used for
	// anything as far as we're aware. This is actually logically a
	// byte[12], but we read and write it as ints for ease of copying
	// into pointer arrays.
	private int[] name;
        
        // BwdYeti: herp
        // The size of the tiles to use for the OAM data
        // 8 for all functional purposes as custom animations have tile
        // placement spaced by 8s, but 1 for rendering
        private int tileScale = 8;

	// Array of frame data offsets for each mode, in a byte-serialized form.
	private byte[] sectionData;

	// "transformed" section data in a common format. Must be
	// translated before being inserted into / after being
	// extracted from a ROM.
	// FIXME: Maybe we should deserialize when we read from the ROM
	// and work with the resulting AnimationCommands?
	private byte[] frameData;

	// OAM data in a byte-serialized form.
	private byte[] rightToLeftOAM;
	private byte[] leftToRightOAM;

	private Palette[] palettes;

	// Graphics referred to by the frame data. Each graphic
	// is byte-serialized; it's a 256x64, 16-colour bitmap
	// (8192 bytes).
	private List<byte[]> sheets = new ArrayList<byte[]>();

	public void setPalette(Palette p) {
		palettes[0] = p;
	}

	public BufferedImage getFrame(int requestedFrame) {
		int sheet = 0, frame = 0, mode = 0;
		for (int command: Util.bytesToInts(frameData)) {
			switch (mode) {
				case 0:
				if (((command >> 24) & 0xFF) == 0x86) {
					frame = (command >> 16) & 0xFF;
					++mode;
				}
				break;

				case 1:
				sheet = command;
				++mode;
				break;

				case 2:
				if (frame == requestedFrame) {
					return renderSingleRTL(sheet, command, false);
				}
				// This wasn't the frame we wanted; try again
				mode = 0;
				break;
			}
		}
		return null;
	}

	public BufferedImage[] renderRTL() {
            return renderRTL(false);
        }
	public BufferedImage[] renderRTL(boolean showPalette) {
		BufferedImage[] result = new BufferedImage[0x100];
		int sheet = 0, frame = 0, mode = 0;
		for (int command: Util.bytesToInts(frameData)) {
			switch (mode) {
				case 0:
				if (((command >> 24) & 0xFF) == 0x86) {
					frame = (command >> 16) & 0xFF;
					++mode;
				}
				break;

				case 1:
				sheet = command;
				++mode;
				break;

				case 2:
				if (result[frame] == null) {
					result[frame] = renderSingleRTL(sheet, command, showPalette);
				}
				mode = 0;
				break;
			}
		}
		return result;
	}

	public GBAImage[] getSheets() {
		int size = sheets.size();
		GBAImage[] result = new GBAImage[size];
		for (int i = 0; i < size; ++i) {
			result[i] = new GBAImage(sheets.get(i), palettes[0], 32, 8);
		}
		return result;
	}

	public BufferedImage renderSingleRTL(byte[] sheetData, int offset) {
		return renderSingleRTL(sheetData, offset);
	}
	
	public BufferedImage renderSingleRTL(byte[] sheetData, int offset, boolean showPalette) {
		Palette p = palettes[0];
		//System.out.println(Util.verboseReport("render single RTL with palette " + p));
                BufferedImage result;
                if (showPalette)
                    result = new GBAImage(p, 30, 20).getImageWithExtraColumn();
                else
                    result = new GBAImage(p, 30, 20).getImage();
		BufferedImage source = new GBAImage(sheetData, p, 32, 8).getImage();
		Graphics2D destination = result.createGraphics();

		//int count = rightToLeftOAM.length;
		double[] angle = new double[5];
		double[] scale = new double[10];
		// Use recursion to blit in reverse order.
		renderOAM(source, destination, offset, angle, scale);

		return result;
	}

	private BufferedImage renderSingleRTL(int sheet, int offset) {
		return renderSingleRTL(sheets.get(sheet), offset, false);
	}
	
	private BufferedImage renderSingleRTL(int sheet, int offset, boolean showPalette) {
		return renderSingleRTL(sheets.get(sheet), offset, showPalette);
	}

	private void renderOAM(
		BufferedImage source, Graphics2D destination, int offset, double[] rotate_angle, double[] xy_scale
	) {
		if (offset == rightToLeftOAM.length) return;
		OAM oam = OAM.deserialize(rightToLeftOAM, offset, false, rotate_angle, xy_scale, tileScale);
		if (oam == null) return;

		renderOAM(source, destination, offset + 12, oam.getRotation(), oam.getScale());
		oam.clearR_index();
		oam.blitToScreen(source, destination, false);
	}			

	// Construct from Animation_Builder (which cuts itself apart
	// to call the constructor).
	public PortableClassAnimation(
		int[] sectionData, List<AnimationCommand> commands, int totalCommandSize, 
		ArrayList<OAM> rightToLeft, Palette base_palette,
		List<GBASpritesheet> graphics
	) {
		name = new int[3];
		this.sectionData = Util.intsToBytes(sectionData);

		// Serialize the frame data, which is in a common format.
		frameData = new byte[totalCommandSize];
		int index = 0;
		for (AnimationCommand command: commands) {
			index = command.serialize(frameData, index);
		}

		// Make the flipped OAM, and store both sets, serialized.
		ArrayList<OAM> leftToRight = new ArrayList<OAM>();
		for (OAM oam: rightToLeft) {
			if (oam == null) { leftToRight.add(null); }
			else { leftToRight.add(oam.flipped()); }
		}
		rightToLeftOAM = serializeOAM(rightToLeft);
		leftToRightOAM = serializeOAM(leftToRight);

		palettes = new Palette[] {
			base_palette,
			base_palette.recolored(BLUES, REDS),
			base_palette.recolored(BLUES, GREENS),
			base_palette.recolored(BLUES, PURPLES),
		};
		for (GBASpritesheet graphic: graphics) { sheets.add(graphic.getData()); }
	}

	// Constructor from an existing ClassAnimationArray entry.
	public PortableClassAnimation(
		Game source,
		int[] name, int tileScale, ROM.Pointer rawSectionData, ROM.Pointer rawFrameData,
		ROM.Pointer rawRightToLeft, ROM.Pointer rawLeftToRight,
		ROM.Pointer rawPalettes
	) throws Exception {
		try {
			/*
			System.out.println(Util.verboseReport(
				"constructing \""
				+ new String(Util.intsToBytes(name))
				+ "\""
			));
			*/

			this.name = name;
                        this.tileScale = tileScale;
			sectionData = rawSectionData.getBytes(MAX_MODE_COUNT * 4);

			// Get frame data and sheets
			getCommonData(LempelZiv.decompress(rawFrameData), source);

			rightToLeftOAM = LempelZiv.decompress(rawRightToLeft);
			leftToRightOAM = LempelZiv.decompress(rawLeftToRight);
			byte[] palette_data = LempelZiv.decompress(rawPalettes);
			palettes = new Palette[4];
			for (int i = 0; i < 4; ++i) {
				palettes[i] = new Palette(palette_data, i * 32, 32);
			}
		} catch (Exception e) {
			//e.printStackTrace();
			throw new Exception(
				"PCA: Error loading animation to export"
			);
		}
	}
	// Tested and working

	// Constructor accepting file path to a serialized dump that contains
	// relative references to the frame data as a binary dump and the
	// sheets as PNG images
	@SuppressWarnings("unchecked")
	public PortableClassAnimation(File path) throws Exception {
		String parent = path.getParent() + File.separator;
		ObjectInputStream serializedStream = null;
		List<File> sheetPaths = null;
		File frameDataPath;
		try {
			serializedStream = new ObjectInputStream(new FileInputStream(path));
			frameDataPath = (File)serializedStream.readObject();
			sheetPaths = (List<File>)serializedStream.readObject();
			name = (int[])serializedStream.readObject();
			sectionData = (byte[])serializedStream.readObject();
			rightToLeftOAM = (byte[])serializedStream.readObject();
			leftToRightOAM = (byte[])serializedStream.readObject();
			byte[] palette_data = (byte[])serializedStream.readObject();
			palettes = new Palette[4];
			for (int i = 0; i < 4; ++i) {
				palettes[i] = new Palette(palette_data, i * 32, 32);
			}
		} catch (Exception e) {
			throw new Exception(
				"PCA: Stream error in File constructor\n" +
				"(Failed to read serialized file)"
			);
		} finally {
			try { serializedStream.close(); }
			catch (Exception e2) {}
		}

		FileInputStream frameDataInStream = null;
		try {
			File sizeGetter = new File(parent + frameDataPath);
			frameDataInStream = new FileInputStream(sizeGetter);
			frameData = new byte[(int)sizeGetter.length()];
			frameDataInStream.read(frameData);
		} catch (Exception e) {
			throw new Exception(
				"PCA: Stream error in File constructor\n" +
				"(Failed to read frame data file)"
			);
		} finally {
			try { frameDataInStream.close(); }
			catch (Exception e2) {}
		}

		sheets = new ArrayList<byte[]>();

		Palette sharedPalette = palettes[0];

		for (File currImagePath: sheetPaths) {
			GBAImage currImage = null;
			try {
				currImage = new GBAImage(
					new File(parent + currImagePath), sharedPalette
				);
			} catch (Exception e) {
				throw new Exception(
					"PCA: Stream error in File constructor\n" +
					e.getMessage()
				);
			}
			if (currImage.getTileHeight() != 8) {
				throw new Exception(
					"PCA: OAM sheet image is wrong height (should be 64 pixels)!"
				);
			}
			if (currImage.getTileWidth() == 33) {
				// Trim off the last column.
				currImage = new GBAImage(currImage, 0, 0, 32, 8);
			}
			if (currImage.getTileWidth() != 32) {
				throw new Exception(
					"PCA: OAM sheet image is wrong width (should be 256 or 264 pixels)!"
				);
			}
			sheets.add(currImage.getData());
		}
	}
	// Tested and working

	// Methods for outputting data elsewhere

	// Outputs this to a series of files; the sheets as PNGs, the frame
	// data as a binary dump and a serialized file containing relative
	// references to the others (the path of the latter is the argument
	// for this method)
	 public void writeToFile(File path) throws Exception {
    String parent = path.getParent() + File.separator;
    String baseString = path.getName();
    int index = baseString.lastIndexOf(".");
    baseString = (index == -1) ? baseString : baseString.substring(0, index);

    String frameDataPathString = baseString + " Frame Data.dmp";
    int sheetCount = 1;
    File frameDataPath = new File(frameDataPathString);
    ArrayList<File> sheetPaths = new ArrayList<File>();
    FileOutputStream frameDataOutStream = null;


    try {
      frameDataOutStream = new FileOutputStream(new File(parent + frameDataPathString));
      frameDataOutStream.write(frameData);
    } catch (Exception e) {
      throw new Exception("PCA: Stream error in call to dump(File)");
    } finally {
      try { frameDataOutStream.close(); }
      catch (Exception e2) {}
    }

    for (byte[] currImageData: sheets)
      try {
        String imagePath = String.format("%s Sheet %d.png", baseString, sheetCount++);
        File imageOutFile = new File(imagePath);
        sheetPaths.add(imageOutFile);
        imageOutFile = new File(parent + imagePath);
        GBAImage theImage = new GBAImage(
          currImageData,
          palettes[0],
          32, 8
        );
        // Hextator: 20091014 modification
        ImageIO.write(theImage.getImageWithExtraColumn(), "PNG", imageOutFile);
      } catch (Exception e) {
        throw new Exception("PCA: Stream error in call to dump(File)");
      }


     /* Zahlman: This is not useful for users currently, but maybe for us.
     * Actually, it might be a good idea to have *everything* separated out
     * the way the frame data and OAM spritesheets are.
     * Anyway, this just dumps the right-to-left OAM data.
     */
    try {
      frameDataOutStream = new FileOutputStream(new File(parent + baseString + " RTL.dmp"));
      frameDataOutStream.write(rightToLeftOAM);
    } catch (Exception e) {
      throw new Exception("PCA: Stream error in call to dump(File)");
    } finally {
      try { frameDataOutStream.close(); }
      catch (Exception e2) {}
    }

    try {
      frameDataOutStream = new FileOutputStream(new File(parent + baseString + " LTR.dmp"));
      frameDataOutStream.write(leftToRightOAM);
    } catch (Exception e) {
      throw new Exception("PCA: Stream error in call to dump(File)");
    } finally {
      try { frameDataOutStream.close(); }
      catch (Exception e2) {}
    }

    try {
      byte[] palette_data = Util.shortsToBytes(
        palettes[0].getShorts(),
        palettes[1].getShorts(),
        palettes[2].getShorts(),
        palettes[3].getShorts()
      );
      frameDataOutStream = new FileOutputStream(new File(parent + baseString + " Palette.dmp"));
      frameDataOutStream.write(palette_data);
    } catch (Exception e) {
      throw new Exception("PCA: Stream error in call to dump(File)");
    } finally {
      try { frameDataOutStream.close(); }
      catch (Exception e2) {}
    }

    try {
      frameDataOutStream = new FileOutputStream(new File(parent + baseString + " SectionData.dmp"));
      frameDataOutStream.write(sectionData);
    } catch (Exception e) {
      throw new Exception("PCA: Stream error in call to dump(File)");
    } finally {
      try { frameDataOutStream.close(); }
      catch (Exception e2) {}
    }

    ObjectOutputStream serializedStream = null;
    try {
      serializedStream = new ObjectOutputStream(new FileOutputStream(path));
      serializedStream.writeObject(frameDataPath);
      serializedStream.writeObject(sheetPaths);
      serializedStream.writeObject(name);
      serializedStream.writeObject(sectionData);
      serializedStream.writeObject(rightToLeftOAM);
      serializedStream.writeObject(leftToRightOAM);
      byte[] palette_data = Util.shortsToBytes(
        palettes[0].getShorts(),
        palettes[1].getShorts(),
        palettes[2].getShorts(),
        palettes[3].getShorts()
      );
      serializedStream.writeObject(palette_data);
    } catch (Exception e) {
      throw new Exception("PCA: Stream error in call to dump(File)");
    } finally {
      try { serializedStream.close(); }
      catch (Exception e2) {}
    }
  }
  // Tested and working

	// Insert self into a ClassAnimationArray.
	public void writeToArray(Game game, ClassAnimationArray array)
	throws Exception {
		try {
			// First, write the sheets so that we can resolve the graphics refs.
			ROM.Pointer[] imageRefs = new ROM.Pointer[sheets.size()];
			int i = 0;
			for (byte[] currImage: sheets) {
				imageRefs[i++] = game.write(LempelZiv.compress(currImage), false);
			}

			// Write own fields into the array. The array will compress stuff
			// when it writes the data to ROM.
			byte[] palette_data = Util.shortsToBytes(
				palettes[0].getShorts(),
				palettes[1].getShorts(),
				palettes[2].getShorts(),
				palettes[3].getShorts()
			);
			array.setEntry(
				name, sectionData,
				getGameSpecificData(imageRefs, !(game instanceof FE7)),
				rightToLeftOAM, leftToRightOAM, palette_data
			);
		} catch (Exception e) {
			throw new Exception(
				"PCA: Error writing animation being imported"
			);
		}
	}

	// *** Helpers

	// Convert the frame data (in common format) into data that can
	// be written to the current ROM, given the locations where
	// image data was written.
	// FIXME: use polymorphism instead of this hacky flag to indicate
	// that the game is FE6 or FE8 and therefore requires additional processing.
	private byte[] getGameSpecificData(ROM.Pointer[] imageRefs, boolean not_FE7) {
		int[] asInts = Util.bytesToInts(frameData);

		// FIXME: Do this with less magic and more modularity.
		if (not_FE7) {
			//System.out.println(Util.verboseReport("doing extra non-FE 7 processing"));
			// Can't use foreach here because we have to write back.
			for (int i = 0; i < asInts.length; i++) {
				int value = asInts[i];
				int high = (value >> 0x18) & 0xFF;
				int low = value & 0xFF;
				if (high == 0x85) {
					if (low == 0x3D || low == 0x50) {
						// Zahlman: Do I understand this correctly?
						// this command is invalid, so we replace it with a nop
						// by clearing the low byte.
						// Hextator: Most correct. We really do need that
						// transforming code, though.
						asInts[i] &= ~0xFF;
					}
				}
			}
		}

		for (int i = 0; i < asInts.length; i++) {
			if (((asInts[i] >> 0x18) & 0xFF) == 0x86) {
				// Check the next word of the data for the image index.
				i++;
				// Resolve the image index into a pointer.
				asInts[i] = imageRefs[asInts[i]].toInt();
			}
		}

		return Util.intsToBytes(asInts);
	}
	// Tested and working

	// Convert frame data from the ROM with resolved graphics refs
	// into data in common format. Initialize frameData and sheets.
	private void getCommonData(byte[] specificData, Game source) {
		int[] asInts = Util.bytesToInts(specificData);
		
		for (int i = 0; i < asInts.length; i++) {
			if (((asInts[i] >> 0x18) & 0xFF) == 0x86) {
				// Check the next word of the data for the pointer.
				i++;
				// Turn the pointer back into an index.
				byte[] loadedImage = LempelZiv.decompress(
					source.getPointer(asInts[i])
				);
				int writeIndex = sheets.size();
				for (int j = 0; j < sheets.size(); j++) {
					if (java.util.Arrays.equals(sheets.get(j), loadedImage)) {
						writeIndex = j;
						break;
					}
				}
				if (writeIndex == sheets.size()) { sheets.add(loadedImage); }
				asInts[i] = writeIndex;
			}
		}

		frameData = Util.intsToBytes(asInts);
	}

	private static byte[] serializeOAM(ArrayList<OAM> OAMs) {
		byte[] result = new byte[12 * OAMs.size()];
		int index = 0;
		for (OAM oam: OAMs) {
			index = OAM.serialize(oam, result, index);
		}
		return result;
	}
	// Fine

	private static File subfolder(File parent, String name) {
		String result = parent.getPath() + File.separator + name;
		return new File(result);
	}

	public String getName() {
		return new String(Util.intsToBytes(name)).replace("\000", "");
	}

	private String getName(int ID) {
		return String.format("%s_%d", getName(), ID);
	}

	// Test driver
	public static void main(String[] args) {
		try {
			File f = CommonDialogs.showOpenFileDialog("first file");
			Game game = Game.open(f.getPath());
			File folder = new File(f.getParent() + "/animations");
			folder.mkdir();

			// Use the source game/index to extract an animation, then
			// output it to a series of files
			ClassAnimationArray array = game.classAnimationArray();

			array.moveTo(0);
			do {
				PortableClassAnimation entry = array.getEntry();
				File sub = subfolder(folder, entry.getName(array.getPosition()));
				sub.mkdir();
				BufferedImage[] images = entry.renderRTL();
				for (int i = 0; i < 256; ++i) {
					if (images[i] != null) {
						ImageIO.write(
							images[i], "PNG",
							subfolder(sub, String.format("frame%03d.png", i))
						);
					}
				}
				entry.writeToFile(subfolder(sub, "animation"));
				array.next();
			} while (array.getPosition() != 0);
		} catch (Exception e) {
			CommonDialogs.showCatchErrorDialog(e);
			e.printStackTrace();
		}

		// Ensure resources are freed
		System.exit(0);
	}
}

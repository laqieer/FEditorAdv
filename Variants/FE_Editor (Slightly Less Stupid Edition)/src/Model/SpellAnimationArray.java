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
 *  - Nintenlord made some suggestions to help start off optimization
 *    coding
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
 *  <Description> This class is expected to provide an interface for editing
 *  graphical data relating to spell animations
 */

package Model;

//import java.io.File;
//import java.io.FileWriter;
import java.util.ArrayList;
//import java.util.List;
import Compression.LempelZiv;
import Graphics.GBAImage;
import Graphics.GBASpritesheet;
import Graphics.Palette;
//import FEditorAdvance.CommonDialogs;

public class SpellAnimationArray extends PointerArray {
	// Class specific constants to go here
	public static final int PAL_SIZE = 0x00000020;
	public static final int RAW_OAM_GFX_SIZE = 0x00001000;
	public static final int RAW_BG_GFX_SIZE = 0x00002000;
	public static final int OAM_GFX_TILE_COUNT = 0x00000080;
	public static final int BG_GFX_TILE_COUNT = 0x00000100;

	public static final int MAX_DURATION_ALLOWED = 0x0000FFFF;

	// For terminating variable length data that isn't compressed
	public static final int TERMINATOR = 0x5465726D;
	public static final byte[] TERMINATOR_ARRAY = new byte[] {
		0x6D, 0x72, 0x65, 0x54
	};

	// To hook custom spell animations, change the values in the
	// Game extending class in the spell animation and PC array
	// constructor calls
	// Arguments 0 and 1 of Game.spellProgramCounterArray() are the dim and no dim PCs
	// Argument 1 of Game.spellAnimationArray() is the spell array base
	// address

	// Source images
	GBAImage queuedOAMimage;
	GBAImage queuedBGimage;

	// Processed frame data
	private ArrayList<AnimationCommand> frameData =
		new ArrayList<AnimationCommand>();
	// Placeholder for mode 2/4 frame data
	// Fixed to accomodate spell requirement
	//private Vector<AnimationCommand> BGframeData = new Vector<AnimationCommand>();

	private int frameDataCount = 0;

	// Processed OAM
	private ArrayList<OAM> rightToLeftOAMVector = new ArrayList<OAM>();
	private ArrayList<OAM> BGrightToLeftOAMVector = new ArrayList<OAM>();

	// Vectors of decoded palettes
	private ArrayList<Palette> OAMpalettes = new ArrayList<Palette>();
	private ArrayList<Palette> BGpalettes = new ArrayList<Palette>();

	// Vector of sheets
	private ArrayList<GBASpritesheet> OAMgraphics =
		new ArrayList<GBASpritesheet>();
	// Vector of BG sheets
	private ArrayList<GBAImage> BGgraphics =
		new ArrayList<GBAImage>();
	// For optimization regarding reuse of exact same frames
	private ArrayList<SpellFrameAnimationCommand> pastFrameData =
		new ArrayList<SpellFrameAnimationCommand>();
	// Fixed to accomodate spell requirement
	//private Vector<AnimationCommand> pastBGFrameData = new Vector<AnimationCommand>();

	// Progress measuring fields
	private byte frame = 0;
	private int commandCount = 0;

	// Should be fine
	private static byte[] serializeOAM(ArrayList<OAM> OAMs) {
		// Extra 4 bytes for terminator
		byte[] result = new byte[12 * OAMs.size() + 4];
		int index = 0;
		for (OAM oam: OAMs) {
			index = OAM.serialize(oam, result, index);
		}
		// Auto-appending terminator
		for (int i = 0; i < 4; i++)
			result[result.length - (4 - i)] = TERMINATOR_ARRAY[i];
		return result;
	}

	public SpellAnimationArray(
		Game owner,
		ROM.Pointer handle, ROM.Pointer default_base,
		int default_size, int ints_per_record
	) {
		// We had been handling the spell animation array as an offset array, but this is wrong.
		// Index 0 of the array IS valid and SHOULD be moved with the array, but
		// simply SHOULD NOT be edited.
		super(owner, handle, default_base, default_size, ints_per_record);
		// An "unrelocated" spell animation array would be a non-existent one.
		// This means we need to create it. We could use the corresponding spell
		// PC array as a guide, but the entries in the spell animation array 
		// corresponding to stock ROM spells are useless anyway, because the stock ROM
		// spells are all hard-coded (which is why the CSA patch exists in the first place).
		// So, we'll just use the maximum size.
		if (!relocated()) { resize(maxSize()); }
		reset();
	}

	// Since index 0 is valid (but uneditable) for this array,
	// 0x100 entries are indexable.
	@Override
	public int maxSize() { return 0x100; }

	@Override
	public int minIndex() { return 1; }

	// Done
	public void reset() {
		// FIXME: Load existing data from ROM?
		// NOTE: Above could only be practically applied to custom spells
		frameData = new ArrayList<AnimationCommand>();
		frameDataCount = 0;
		rightToLeftOAMVector = new ArrayList<OAM>();
		BGrightToLeftOAMVector = new ArrayList<OAM>();
		OAMpalettes = new ArrayList<Palette>();
		BGpalettes = new ArrayList<Palette>();
		OAMgraphics = new ArrayList<GBASpritesheet>();
		BGgraphics = new ArrayList<GBAImage>();
		pastFrameData = new ArrayList<SpellFrameAnimationCommand>();
		frame = 0;
		commandCount = 0;
	}

	// Fine
	protected boolean markNewPointer() { return false; }

	protected byte[] prepare(byte[] data, int index) {
		if (index < 0 || index > 4)
			throw new IllegalArgumentException();
		return data;
	}
	// prepare method; done

	// The structure is:
	// 00 Frame data pointer
	// 04 OAM data (right to left)
	// 08 OAM data (left to right)
	// 0C OAM data (background right to left)
	// 10 OAM data (background left to right)
	/**
	class x86command {
		// Word 0
		short duration;
		byte frameID;
		byte x86ID;
		// Word 1 (LZ77 compressed)
		Sheet spriteGFX;
		// Word 2
		int OAMoffset;
		// Word 3
		int BGOAMoffset;
		// Word 4 (LZ77 compressed)
		Sheet map1GFX;
		// Word 5 (32 raw bytes)
		Palette OAMpalette;
		 // Word 6 (32 raw bytes)
		Palette BGpalette;
	};
	**/

	// XXX The assembly hack this class relies on does not account for
	// the delay in processing OAM data. Attempts to fix this here
	// failed.
	private byte[] fixFrameData(ArrayList<AnimationCommand> frameData) {
		byte[] frameBytes;
		int index;
		frameBytes = new byte[frameDataCount];
		index = 0;
		for (AnimationCommand command: frameData) {
			index = command.serialize(frameBytes, index);
		}
		return frameBytes;
	}

	// Index 0 is invalid.

	// Done
	@Override
	protected boolean isPointer(int index) {
		return (index >= 0) && (index < 5);
	}

	// Done
	@Override
	protected boolean isDoublePointer(int index) { return index == 0; }

	// Frame data is LZ77 compressed
	// Hextator: Won't ever not be compressed
	// and may contain pointers to Graphics that must be freed.
	// (If any word has a MSB of 0x86, the next word is a pointer.)
	// OAM data is no longer compressed and will be terminated by
	// the ASCII string "Term" defined above (as TERMINATOR)
	// Palettes should not be compressed; decompressed size is RAW_PAL_SIZE
	// Graphics should always be compressed; decompressed size is RAW_GFX_SIZE

	// FIXME: Needs testing
	private void freeSheetsAndPalettes(ROM.Pointer p) {
		// 0x80000000 is the value of the terminator for the frame
		// data, which is not compressed
		int currInt = p.nextInt();
		while (currInt != 0x80000000) {
			// Check if this is a graphics frame
			if (((currInt >> 0x18) & 0xFF) == 0x86) {
				// On word 1
				ROM.Pointer start;
				ROM.Pointer end;
				// Free spriteGFX
				start = p.deref();
				end = start.offsetBy(0);
				LempelZiv.decompress(end);
				owner.markFreeSpace(start, end);
				p.nextInt(); // On word 2
				p.nextInt(); // On word 3
				p.nextInt(); // On word 4
				// Free map1GFX
				start = p.deref();
				end = start.offsetBy(0);
				LempelZiv.decompress(end);
				owner.markFreeSpace(start, end);
				p.nextInt(); // On word 5
				// Free OAMpalette
				start = p.deref();
				end = start.offsetBy(32);
				owner.markFreeSpace(start, end);
				p.nextInt(); // On word 6
				// Free BGpalette
				start = p.deref();
				end = start.offsetBy(32);
				owner.markFreeSpace(start, end);
			}
			currInt = p.nextInt();
		}
	}

	// Data freeing helper
	protected void moveToEndOfData(ROM.Pointer p, int index) {
		if (!isPointer(index))
			throw new IllegalArgumentException();
		// This array was invented by FEditor, so some data might
		// be invalid
		if (p == null)
			return;
		if (index == 0) {
			freeSheetsAndPalettes(p);
			// Above call advances pointer, so we're done here
			return;
		}
		while (p.nextInt() != TERMINATOR) { }
	}

	// Tested and working
	private void fixGraphicsRefs() {
		// Write graphic sheets to ROM and correct 0x86 references
		ROM.Pointer[] OAMsheets = new ROM.Pointer[OAMgraphics.size()];
		for (int i = 0; i < OAMsheets.length; i++) {
			OAMsheets[i] = owner.write(
				LempelZiv.compress(OAMgraphics.get(i).getData()), false
			);
		}
		ROM.Pointer[] BGsheets = new ROM.Pointer[BGgraphics.size()];
		for (int i = 0; i < BGsheets.length; i++) {
			BGsheets[i] = owner.write(
				LempelZiv.compress(BGgraphics.get(i).getData()), false
			);
		}
		ROM.Pointer[] OAMpalettePointers = new ROM.Pointer[OAMpalettes.size()];
		for (int i = 0; i < OAMpalettePointers.length; i++) {
			OAMpalettePointers[i] = owner.write(
				OAMpalettes.get(i).getBytes(), false
			);
		}
		ROM.Pointer[] BGpalettePointers = new ROM.Pointer[BGpalettes.size()];
		for (int i = 0; i < BGpalettePointers.length; i++) {
			BGpalettePointers[i] = owner.write(
				BGpalettes.get(i).getBytes(), false
			);
		}
		for (AnimationCommand command: frameData) {
			if (command instanceof SpellFrameAnimationCommand)
				((SpellFrameAnimationCommand) command).fixPointers(
					OAMsheets, BGsheets,
					OAMpalettePointers, BGpalettePointers
				);
		}
	}

	public int getFrameCount() {
		return frame;
	}

	public int getCommandCount() {
		return commandCount;
	}

	// Fine
	private void add_helper(AnimationCommand command) {
		frameData.add(command);
		frameDataCount += command.size();
	}

	// For appending 0x85 commands onto the frame data
	public void addCommand(byte commandID) {
		byte tempWordCount = 0;
		add_helper(AnimationCommand.normal(commandID, tempWordCount));
		commandCount++;
	}

	// Tested and working
	// For appending robust 0x85 commands onto the frame data
	public void addCommand(byte commandID, byte param1, byte param2) {
		add_helper(AnimationCommand.robust(commandID, param1, param2));
		commandCount++;
	}

	// Tested and working
	// For appending 0x85 commands onto the frame data to play any sound
	public void addSoundCommand(short musicID) {
		add_helper(AnimationCommand.sound(musicID));
		commandCount++;
	}

	// Should be fine
	// For adding terminators of the miss-only or usual variety
	public void addModeTerminator(boolean missOnly) {
		if (!missOnly)
			add_helper(AnimationCommand.terminator());
		else
			add_helper(AnimationCommand.missTerminator());
	}

	// Should be fine
	private GBAImage[] processFrame(GBAImage frameImage) {
		// Check the obvious
		if (frameImage == null) {
			throw new IllegalArgumentException(
				"The sprite frame failed to load!"
			);
		}

		// The image should be 61x20 tiles (double width, containing
		// foreground and background information and palette column).
		int width = frameImage.getTileWidth();
		int height = frameImage.getTileHeight();

		if (height != 20) {
			throw new IllegalArgumentException(
				"That sprite frame has invalid dimensions!"
			);
		}

		if (width != 61) {
			throw new IllegalArgumentException(
				"That sprite frame has invalid dimensions!"
			);
		}

		GBAImage[] result = new GBAImage[] { null, null };

		result[1] = new GBAImage(frameImage, 30, 0, 30, 20);
		result[0] = new GBAImage(frameImage, 0, 0, 30, 20);

		return result;
	}

	// Should be fine
	private void processBG(GBAImage BGimage) {
		// Check the obvious
		if (BGimage == null) {
			throw new IllegalArgumentException(
				"The BG failed to load!"
			);
		}

		int width = BGimage.getTileWidth();
		int height = BGimage.getTileHeight();

		if (height != 8) {
			throw new IllegalArgumentException(
				"That BG has invalid dimensions!"
			);
		}

		if (width != 32) {
			throw new IllegalArgumentException(
				"That BG has invalid dimensions!"
			);
		}
	}

	// Should be fine
	private void setupGraphics(Palette palette) {
		if (!OAMgraphics.isEmpty()) {
			// Already set up.
			return;
		}

		// 32x4 tile sheets now because this game is weird
		GBASpritesheet first = new GBASpritesheet(palette, 32, 4);
		OAMgraphics.add(first);
	}

	// KA-done
	public void queueSprites(GBAImage image) {
		queuedOAMimage = image;
	}

	// KA-done
	// Hextator: Made this reverse compatible by using only the relevant
	// region of a 264x64 pixel image (as expected by older versions)
	public void queueBackground(GBAImage image) {
		queuedBGimage = new GBAImage(image, 0, 0, 32, 8);
	}

	// Tested and working
	// For appending actual frame data
	// Returns true for success and false for failure
	public boolean addFrame(short duration) {
		if (duration > MAX_DURATION_ALLOWED) { return false; }

		GBAImage[] images = null;
		try {
			images = processFrame(queuedOAMimage);
		} catch (IllegalArgumentException e) {
			//throw new RuntimeException(e);
			return false;
		}
		try {
			processBG(queuedBGimage);
		} catch (IllegalArgumentException e) {
			//throw new RuntimeException(e);
			return false;
		}

		setupGraphics(images[0].getPalette());

		// Check if frame is the same as a past frame
		for (int i = 0; i < pastFrameData.size(); i++) {
			if (
				OAMgraphics.get(
					pastFrameData.get(i).getOAMsheetIndex()
				).getImage().sameImageAs(queuedOAMimage) &&
				BGgraphics.get(
					pastFrameData.get(i).getBGsheetIndex()
				).sameImageAs(queuedBGimage) &&
				OAMpalettes.get(
					pastFrameData.get(i).getOAMpaletteIndex()
				).equals(queuedOAMimage.getPalette()) &&
				BGpalettes.get(
					pastFrameData.get(i).getBGpaletteIndex()
				).equals(queuedBGimage.getPalette())
			) {
				add_helper(pastFrameData.get(i).repeat(duration, frame));
				frame++;
				return true;
			}
		}

		int BGsheetIndex = BGgraphics.size();
		int OAMpaletteIndex = OAMpalettes.size();
		int BGpaletteIndex = BGpalettes.size();

		for (int i = 0; i < BGgraphics.size(); i++) {
			if (BGgraphics.get(i).sameImageAs(queuedBGimage)) {
				BGsheetIndex = i;
				break;
			}
		}
		for (int i = 0; i < OAMpalettes.size(); i++) {
			if (OAMpalettes.get(i).equals(queuedOAMimage.getPalette())) {
				OAMpaletteIndex = i;
				break;
			}
		}
		for (int i = 0; i < BGpalettes.size(); i++) {
			if (BGpalettes.get(i).equals(queuedBGimage.getPalette())) {
				BGpaletteIndex = i;
				break;
			}
		}

		if (BGsheetIndex == BGgraphics.size()) {
			BGgraphics.add(queuedBGimage);
		}
		if (OAMpaletteIndex == OAMpalettes.size()) {
			OAMpalettes.add(queuedOAMimage.getPalette());
		}
		if (BGpaletteIndex == BGpalettes.size()) {
			BGpalettes.add(queuedBGimage.getPalette());
		}

		// Set up tile maps and count total used tiles
		TileMap tileMap = new TileMap(images[0]);
		// Fixed to accomodate spell requirement
		TileMap BGtileMap =
			//images[1] == null ? null :
			new TileMap(images[1]);

		int usedTiles = tileMap.getCount();
		//if (BGtileMap != null) { usedTiles += BGtileMap.getCount(); }
		// Fixed to accomodate spell requirement
		usedTiles += BGtileMap.getCount();

		// Ensure that the tiles can fit on one sheet
		if (usedTiles > OAM_GFX_TILE_COUNT)
			throw new RuntimeException("That image uses too many tiles!");

		// Prepare OAM data

		ArrayList<OAM> OAMdata = OAM.calculateOptimumOAM(tileMap);
		// There is always a background tile map for spells.
		ArrayList<OAM> BGOAMdata = OAM.calculateOptimumOAM(BGtileMap);
		// Spells are weird:
		ArrayList<OAM> allOAMdata = new ArrayList<OAM>();
		allOAMdata.addAll(OAMdata);
		allOAMdata.addAll(BGOAMdata);

		int sheetIndex = OAM.selectSheet(
			images[0], images[1], allOAMdata, OAMgraphics, true
		);

		// Look for a sequence of elements in the rightToLeftOAMVector OAM vector
		// that matches the OAMdata for this frame; if absent, append
		// to the end of the vector. Set OAMdest as the index of the beginning
		// of this sequence.

		int OAMdest = rightToLeftOAMVector.size();
		int size = OAMdata.size();
		int max = OAMdest - size;
		for (int i = 0; i < max; i++) {
			int matched = 0;
			for (; matched < size; matched++) {
				OAM fromRTL = rightToLeftOAMVector.get(i);
				OAM fromCurrent = OAMdata.get(matched);
				// Hextator: I fixed the line above to use
				// "matched" instead of "i". "i" was causing
				// an exception. Is "matched" correct?

				if (fromRTL == null && fromCurrent == null) {
					// Matched
					continue;
				}
				if (fromRTL == null) {
					// Not a match; fromCurrent != null
					break;
				}
				if (fromCurrent == null) {
					// Not a match; fromRTL != null
					break;
				}
				if (!fromRTL.equals(fromCurrent)) {
					break;
				}
			}
			// Did we find a full sequence of
			// OAMdata.length many matches?
			if (matched == size) {
				OAMdest = i;
				break;
			}
		}

		// If no match was found, we need to append to the vector,
		// so that OAMdest is now the correct index.
		if (OAMdest == rightToLeftOAMVector.size()) {
			rightToLeftOAMVector.addAll(OAMdata);
		}

		// Repeat above for BGOAM
		int BGOAMdest = BGrightToLeftOAMVector.size();
		size = BGOAMdata.size();
		max = BGOAMdest - size;
		for (int i = 0; i < max; i++) {
			int matched = 0;
			for (; matched < size; matched++) {
				OAM fromRTL = BGrightToLeftOAMVector.get(i);
				OAM fromCurrent = BGOAMdata.get(matched);
				// Hextator: I fixed the line above to use
				// "matched" instead of "i". "i" was causing
				// an exception. Is "matched" correct?

				if (fromRTL == null && fromCurrent == null) {
					// Matched
					continue;
				}
				if (fromRTL == null) {
					// Not a match; fromCurrent != null
					break;
				}
				if (fromCurrent == null) {
					// Not a match; fromRTL != null
					break;
				}
				if (!fromRTL.equals(fromCurrent)) {
					break;
				}
			}
			// Did we find a full sequence of
			// BGOAMdata.length many matches?
			if (matched == size) {
				BGOAMdest = i;
				break;
			}
		}

		// If no match was found, we need to append to the vector,
		// so that BGOAMdest is now the correct index.
		if (BGOAMdest == BGrightToLeftOAMVector.size()) {
			BGrightToLeftOAMVector.addAll(BGOAMdata);
		}

		// Append 0x86 command onto frame data for this frame
		frameData.add(
			AnimationCommand.spellFrame(
				duration, frame, sheetIndex, OAMdest * 12,
				BGOAMdest * 12, BGsheetIndex,
				OAMpaletteIndex, BGpaletteIndex
			)
		);
		frameDataCount += 28;
		pastFrameData.add((SpellFrameAnimationCommand)
			AnimationCommand.spellFrame(
				duration, frame, sheetIndex, OAMdest * 12,
				BGOAMdest * 12, BGsheetIndex,
				OAMpaletteIndex, BGpaletteIndex
			)
		);
		// Fixed to accomodate spell requirement
		//OAMdest += BGOAMoffset;
		frame++;
		return true;
	}

	// NOTE: output was discarded because the integer count for the
	// record size was ignored instead of being used (as 5) in inherited
	// "resize" method
	// For inserting Spell_Animations stored as bitmaps to the ROM
	public void setSpellAnimation() {
		if (frameData.isEmpty())
			throw new RuntimeException(
				"Incomplete animation: Empty frame data!"
			);
		if (rightToLeftOAMVector.isEmpty())
			throw new RuntimeException(
				"Incomplete animation: Empty object attribute memory!"
			);

		// FIXME: Make a Process for this
		// Hextator: Why are we doing this? It's just going to be
		// overwritten anyway!
		//clear();
		fixGraphicsRefs();

		// Serialize the frame data and save it
		byte[] frameBytes = fixFrameData(frameData);
		setData(0, frameBytes);

		// Make the flipped OAM, and serialize both sets
		ArrayList<OAM> leftToRight = new ArrayList<OAM>();
		for (OAM oam: rightToLeftOAMVector) {
			if (oam == null)
				leftToRight.add(null);
			else
				leftToRight.add(oam.flipped());
		}
		ArrayList<OAM> BGleftToRight = new ArrayList<OAM>();
		for (OAM oam: BGrightToLeftOAMVector) {
			if (oam == null)
				BGleftToRight.add(null);
			else
				BGleftToRight.add(oam.flipped());
		}

		// Append variable length array terminators to these byte arrays
		// and write them to the ROM, updating references as usual
		// Terminator is appended automatically in the serialization
		// method now
		setData(1, serializeOAM(rightToLeftOAMVector));
		setData(2, serializeOAM(leftToRight));
		setData(3, serializeOAM(BGrightToLeftOAMVector));
		setData(4, serializeOAM(BGleftToRight));

		System.out.println("Writing done by " + Util.methodName());
	}
}

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
 *  <Description> Accumulates data to be used to create a
 *  PortableClassAnimation and save it to file.
 */

package Model;

import java.util.ArrayList;
import Graphics.GBAImage;
import Graphics.GBASpritesheet;
import Graphics.Palette;
import FEditorAdvance.CommonDialogs;

public class AnimationBuilder {
	// Class specific constants to go here
	public static final int MAX_MODE_COUNT = 0x0000000C;
	public static final int RAW_GFX_SIZE = 0x00002000;
	public static final int GFX_TILE_COUNT = 0x00000100;
	public static final int MAX_FRAME_DATA_LEN = 0x00002A00;
	public static final int MAX_OAM_DATA_LEN = 0x00005800;
	public static final int RAW_PAL_SIZE = 0x00000080; // 4 * 32

	public static final int MAX_DURATION_ALLOWED = 0x00000020;

	// Array of frame data offsets for each mode
	private int[] sectionData = new int[MAX_MODE_COUNT];
	// Processed frame data
	private ArrayList<AnimationCommand> frameData =
		new ArrayList<AnimationCommand>();
	// Placeholder for mode 2/4 frame data
	private ArrayList<AnimationCommand> BGframeData =
		new ArrayList<AnimationCommand>();

	private int frameDataCount = 0;

	// Container of all OAM for each frame, possibly overlapping.
	private ArrayList<OAM> rightToLeft = new ArrayList<OAM>();

	private Palette palette;

	// Vector of sheets
	private ArrayList<GBASpritesheet> graphics =
		new ArrayList<GBASpritesheet>();
	// For optimization regarding reuse of exact same frames
	private ArrayList<GBAImage> frameImageVector =
		new ArrayList<GBAImage>();
	private ArrayList<AnimationCommand> pastFrameData =
		new ArrayList<AnimationCommand>();
	private ArrayList<AnimationCommand> pastBGFrameData =
		new ArrayList<AnimationCommand>();
	// For holding sprite used by hardcoded animation commands
	GBAImage spriteToThrow;
	// Progress measuring fields
	private int mode = 0;
	private byte frame = 0;
	private boolean canLoop = false;
	private boolean hasLoop = false;
	private int wordCount = 0;

	public boolean canLoop() {
		return canLoop;
	}
	// canLoop accessor

	public boolean hasLoop() {
		return hasLoop;
	}
	// hasLoop accessor

	public int getMode() {
		return mode;
	}
	// getMode accessor

	public int getFrameCount() {
		return frame;
	}
	// getFrameCount accessor

	private void add_helper(AnimationCommand command) {
		frameData.add(command);
		frameDataCount += command.size();
	}
	// Fine

	private void terminate() {
		add_helper(AnimationCommand.terminator());
		if (++mode < MAX_MODE_COUNT) {
			sectionData[mode] = frameDataCount;
		}
	}
	// Fine

	// For appending 0x85 commands onto the frame data
	public void addCommand(byte commandID) {
		if (mode == MAX_MODE_COUNT) { return; }
		if (commandID == 0x04 || commandID == 0x05) { canLoop = true; }
		byte tempWordCount = 0;
		if (commandID == 0x01 && hasLoop) {
			if (wordCount > 0xff) {
				throw new RuntimeException("循环太长");
			}
			tempWordCount = (byte)wordCount;
		}
		if (mode == 0 || mode == 2) {
			BGframeData.add(AnimationCommand.normal(
				commandID, tempWordCount
			));
		}
		add_helper(AnimationCommand.normal(commandID, tempWordCount));
		if (hasLoop)
			wordCount++;
	}
	// addCommand method; tested and working!

	// For appending 0x85 commands onto the frame data to play any sound
	public void addSoundCommand(short musicID) {
		if (mode == MAX_MODE_COUNT)
			return;
		if (mode == 0 || mode == 2) {
			// NOP command to avoid anticipated issues
			BGframeData.add(AnimationCommand.normal((byte)0, (byte)0));
		}
		add_helper(AnimationCommand.sound(musicID));
		if (hasLoop)
			wordCount++;
	}
	// addCommand method; tested and working!

	// For terminating a mode
	public void addModeTerminator() throws RuntimeException {
		if (frame == 0)
			throw new RuntimeException(
				"当前模式未完成"
			);
		if (mode >= MAX_MODE_COUNT)
			return;
		canLoop = false;
		hasLoop = false;
		wordCount = 0;
		frame = 0;

		terminate();
		if (mode == 1 || mode == 3) {
			for (AnimationCommand command: BGframeData) {
				add_helper(command);
			}
			BGframeData.clear();
			terminate();
		}
	}
	// addModeTerminator method; tested and working!

	// For setting "hasLoop"
	public void addLoopMarker() throws RuntimeException {
		if (canLoop)
			hasLoop = true;
		else
			throw new RuntimeException(
				"无法设定循环"
			);
	}
	// addLoopMarker method; doesn't need testing

	private GBAImage[] processFrame(GBAImage frameImage) {
		// Check the obvious
		if (frameImage == null) {
			throw new IllegalArgumentException(
				"图片载入失败"
			);
		}

		// The image should be either 31x20 tiles (GBA screen size
		// with spare column for BG color specification),
		// or (only in modes 0 and 2) 61x20 tiles (double width, containing
		// foreground and background information and column for specifying
		// BG color).
		int width = frameImage.getTileWidth();
		int height = frameImage.getTileHeight();
		boolean include_bg = (mode == 0) || (mode == 2);

		if (height != 20) {
			throw new IllegalArgumentException(
				"图片高度无效"
			);
		}

		if (width != 31 && !(width == 61 && include_bg)) {
			throw new IllegalArgumentException(
				"图片宽度无效"
			);
		}

		GBAImage[] result = new GBAImage[] { null, null };

		// Hextator: Modified this for cases of width == 31 && !include_bg
		// Also modified the "include_bg" case to be reverse compatible
		// If we have a double frame, cut up the image.
		if (width == 61) {
			result[1] = new GBAImage(frameImage, 30, 0, 30, 20);
			result[0] = new GBAImage(frameImage, 0, 0, 30, 20);
		} else if (include_bg) {
			// Have a blank background to draw from otherwise - needed?
			result[1] = new GBAImage(frameImage.getPalette(), 30, 20);
			result[0] = new GBAImage(frameImage, 0, 0, 30, 20);
		}
		else {
			result[1] = null;
			result[0] = new GBAImage(frameImage, 0, 0, 30, 20);
		}

		return result;
	}
	// processFrame method; should be fine

	private void setupGraphics() {
		if (!graphics.isEmpty()) { return; } // already set up.

		GBASpritesheet first = new GBASpritesheet(palette, 32, 8);
		graphics.add(first);

		// If there is a throwable sprite, put it on the first sheet
		// in the top-right corner.
		if (spriteToThrow != null) {
			first.blit(spriteToThrow, 28, 0);
		}
	}
	// setUpGraphics method; should be fine

	public Palette sharedPalette() {
		return palette;
	}

	// For appending actual frame data
	// Returns true for success and false for failure
	public boolean addFrame(
		GBAImage frameImage,
		short duration
	) throws IllegalArgumentException {
		if (mode == MAX_MODE_COUNT) { return true; } // Silent failure
		if (duration > MAX_DURATION_ALLOWED) { return false; }

		GBAImage[] images = null;
		try {
			images = processFrame(frameImage);
		} catch (IllegalArgumentException e) {
			CommonDialogs.showCatchErrorDialog(e);
			return false;
		}

		if (palette == null) { palette = images[0].getPalette(); }
		setupGraphics();

		// Check if frame is the same as a past frame
		for (int i = 0; i < frameImageVector.size(); i++) {
			if (
				frameImageVector.get(i).sameImageAs(images[0])
			) {
				add_helper(pastFrameData.get(i).repeat(duration, frame));
				if (hasLoop)
					wordCount += 3;
				if (mode == 0 || mode == 2)
					BGframeData.add(
						pastBGFrameData.get(i).
						repeat(duration, frame)
					);
				frame++;
				return true;
			}
		}

		// Set up tile maps and count total used tiles
		TileMap tileMap = new TileMap(images[0]);
		TileMap BGtileMap = images[1] == null ? null : new TileMap(images[1]);

		int usedTiles = tileMap.getCount();
		if (BGtileMap != null) { usedTiles += BGtileMap.getCount(); }

		// Ensure that the tiles can fit on one sheet
		if (usedTiles > GFX_TILE_COUNT)
			throw new IllegalArgumentException(
				"图片所用图块过多"
			);

		// Prepare OAM data

		ArrayList<OAM> OAMdata = OAM.calculateOptimumOAM(tileMap);
		int BGOAMoffset = OAMdata.size();
		if (images[1] != null)
			OAMdata.addAll(OAM.calculateOptimumOAM(BGtileMap));

		int sheetIndex = OAM.selectSheet(
			images[0], images[1], OAMdata, graphics, false
		);

		// Look for a sequence of elements in the rightToLeft OAM vector
		// that matches the OAMdata for this frame; if absent, append
		// to the end of the vector. Set OAMdest as the index of the beginning
		// of this sequence.

		int OAMdest = rightToLeft.size();
		int size = OAMdata.size();
		int max = OAMdest - size;
		// At each 'i' position in fromRTL,
		for (int i = 0; i < max; ++i) {
			// Try to match size-many consecutive elements in fromCurrent.
			// 'matched' counts how many matches we got.
			int matched = 0;
			for (; matched < size; ++matched) {
				OAM fromRTL = rightToLeft.get(i);
				OAM fromCurrent = OAMdata.get(matched);

				if (fromRTL == null && fromCurrent == null) { continue; } // matched
				if (fromRTL == null) { break; } // not a match; fromCurrent != null
				if (fromCurrent == null) { break; } // not a match; fromRTL != null
				if (!fromRTL.equals(fromCurrent)) { break; }
			}
			// Did we find a full sequence of OAMdata.length many matches?
			if (matched == size) {
				OAMdest = i;
				break;
			}
		}

		// If no match was found, we need to append to the vector, so that
		// OAMdest is now the correct index.
		if (OAMdest == rightToLeft.size()) {
			rightToLeft.addAll(OAMdata);
		}

		// Append 0x86 command onto frame data for this frame
		frameData.add(AnimationCommand.frame(duration, frame, sheetIndex, OAMdest * 12));
		frameDataCount += 12;
		pastFrameData.add(AnimationCommand.frame(duration, frame, sheetIndex, OAMdest * 12));
		frameImageVector.add(frameImage);
		if (hasLoop)
			wordCount += 3;
		if (images[1] != null) {
			OAMdest += BGOAMoffset;
			BGframeData.add(AnimationCommand.frame(duration, frame, sheetIndex, OAMdest * 12));
			pastBGFrameData.add(AnimationCommand.frame(duration, frame, sheetIndex, OAMdest * 12));
		}
		frame++;
		return true;
	}
	// addFrame method; tested and working

	// For setting the sprite that can be thrown by the corresponding
	// animation commands
	// Returns false/true for failure/success to set
	public boolean setThrowableSprite(GBAImage throwableSprite)
	throws RuntimeException {
		if (mode > 0 || frame > 0)
			throw new RuntimeException(
				"指定精灵太迟!"
			);
		if (throwableSprite == null)
			throw new RuntimeException(
				"没有指定精灵!"
			);
		if (
			throwableSprite.getTileWidth() != 5
			|| throwableSprite.getTileHeight() != 4
		)
			throw new RuntimeException(
				"图片尺寸无效"
			);
		spriteToThrow = throwableSprite;
		return true;
	}
	// Fine

	// For inserting Class_Animations stored as bitmaps to the ROM
	public PortableClassAnimation create() {
		for (int i = 4; i < sectionData.length; ++i)
			if (sectionData[i] == 0) {
				throw new RuntimeException("未完成的动画");
			}
		if (frameData.isEmpty()) {
			throw new RuntimeException("未完成的动画");
		}
		if (rightToLeft.isEmpty()) {
			throw new RuntimeException("未完成的动画");
		}
		if (mode != MAX_MODE_COUNT) {
			throw new RuntimeException("未完成的动画");
		}
		if (frameDataCount > MAX_FRAME_DATA_LEN) {
			throw new RuntimeException("帧数据溢出");
		}
		if (rightToLeft.size() * 12 > MAX_OAM_DATA_LEN) {
			throw new RuntimeException("OAM数据溢出");
		}

		return new PortableClassAnimation(
			sectionData, frameData, frameDataCount, 
			rightToLeft, palette, graphics
		);
	}
}

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
 *  <Description> This is a model class for representing animation data.
 */

package Model;

public class AnimationCommand {
	// The base class represents terminators.
	protected AnimationCommand() {}

	public int size() {
		return 4;
	}

	public AnimationCommand repeat(short duration, byte frame) {
		throw new UnsupportedOperationException();
	}

	protected static void serializeInt(
		int to_serialize, byte[] buffer, int index
	) {
		for (int i = 0; i < 4; i++) {
			buffer[index + i] = (byte)to_serialize;
			to_serialize >>= 8;
		}
	}

	public int serialize(byte[] buffer, int index) {
		buffer[index++] = 0;
		buffer[index++] = 0;
		buffer[index++] = 0;
		buffer[index++] = (byte)0x80;
		return index;
	}

	public void fixSheetPointer(ROM.Pointer[] pointers) {}

	public static AnimationCommand terminator() {
		return new AnimationCommand();
	}

	public static AnimationCommand missTerminator() {
		return new MissTerminator();
	}

	public static AnimationCommand normal(byte commandID, byte wordCount) {
		return new NormalAnimationCommand(commandID, wordCount);
	}

	public static AnimationCommand robust(
		byte commandID, byte param1, byte param2
	) {
		return new RobustAnimationCommand(commandID, param1, param2);
	}

	public static AnimationCommand sound(short soundID) {
		return new SoundAnimationCommand(soundID);
	}

	public static AnimationCommand frame(
		short duration, byte frame, int sheet, int OAMindex
	) {
		return new FrameAnimationCommand(duration, frame, sheet, OAMindex);
	}

	public static AnimationCommand spellFrame(
		short duration, byte frame, int OAMsheet, int OAMindex,
		int BGOAMindex, int BGsheet,
		int OAMpaletteIndex, int BGpaletteIndex
	) {
		return new SpellFrameAnimationCommand(
			duration, frame, OAMsheet, OAMindex,
			BGOAMindex, BGsheet,
			OAMpaletteIndex, BGpaletteIndex
		);
	}
}

class MissTerminator extends AnimationCommand {
	MissTerminator() {}

	@Override
	public int serialize(byte[] buffer, int index) {
		buffer[index++] = 0;
		buffer[index++] = (byte)0x01;
		buffer[index++] = 0;
		buffer[index++] = (byte)0x80;
		return index;
	}
}

class NormalAnimationCommand extends AnimationCommand {
	byte commandID;
	byte wordCount;

	NormalAnimationCommand(byte commandID, byte wordCount) {
		this.commandID = commandID;
		this.wordCount = wordCount;
	}

	@Override
	public int serialize(byte[] buffer, int index) {
		buffer[index++] = commandID;
		buffer[index++] = wordCount;
		buffer[index++] = 0;
		buffer[index++] = (byte)0x85;
		return index;
	}
}

class RobustAnimationCommand extends AnimationCommand {
	byte commandID;
	byte param1;
	byte param2;

	RobustAnimationCommand(byte commandID, byte param1, byte param2) {
		this.commandID = commandID;
		this.param2 = param2;
		this.param1 = param1;
	}

	@Override
	public int serialize(byte[] buffer, int index) {
		buffer[index++] = commandID;
		buffer[index++] = param2;
		buffer[index++] = param1;
		buffer[index++] = (byte)0x85;
		return index;
	}
}

class SoundAnimationCommand extends AnimationCommand {
	short musicID;

	SoundAnimationCommand(short musicID) { this.musicID = musicID; }

	@Override
	public int serialize(byte[] buffer, int index) {
		buffer[index++] = 0x48;
		buffer[index++] = (byte)musicID;
		buffer[index++] = (byte)(musicID >> 8);
		buffer[index++] = (byte)0x85;
		return index;
	}
}

class FrameAnimationCommand extends AnimationCommand {
	private int sheet, OAMindex;
	private short duration;
	private byte frame;

	FrameAnimationCommand(
		short duration, byte frame, int sheet, int OAMindex
	) {
		this.duration = duration;
		this.frame = frame;
		this.sheet = sheet;
		this.OAMindex = OAMindex;
	}

	@Override
	public AnimationCommand repeat(short newDuration, byte newFrame) {
		return new FrameAnimationCommand(
			newDuration, newFrame, sheet, OAMindex
		);
	}

	@Override
	public int size() { return 12; }

	@Override
	public int serialize(byte[] buffer, int index) {
		buffer[index] = (byte)duration;
		buffer[index + 1] = (byte)(duration >> 8);
		buffer[index + 2] = frame;
		buffer[index + 3] = (byte)0x86;
		serializeInt(sheet, buffer, index + 4);
		serializeInt(OAMindex, buffer, index + 8);
		return index + 12;
	}

	@Override
	public void fixSheetPointer(ROM.Pointer[] pointers) {
		sheet = pointers[sheet].toInt();
	}
}

// Format is:
/*
typedef struct
{
	short duration; //0
	byte frameID; //0
	byte x86ID; //0
	sheet* spriteGFX; //4
	int OAMoffset; //8
	int BGOAMoffset; //12
	sheet* map1GFX; //16
	palette* OAMpalette; //20
	palette* BGpalette; //24
} frame;
*/
class SpellFrameAnimationCommand extends AnimationCommand {
	private int OAMsheet, OAMindex, BGsheet, BGOAMindex;
	private int OAMpaletteIndex, BGpaletteIndex;
	private short duration;
	private byte frame;

	SpellFrameAnimationCommand(
		short duration, byte frame, int OAMsheet, int OAMindex,
		int BGOAMindex, int BGsheet,
		int OAMpaletteIndex, int BGpaletteIndex
	) {
		this.duration = duration;
		this.frame = frame;
		this.OAMsheet = OAMsheet;
		this.OAMindex = OAMindex;
		this.BGOAMindex = BGOAMindex;
		this.BGsheet = BGsheet;
		this.OAMpaletteIndex = OAMpaletteIndex;
		this.BGpaletteIndex = BGpaletteIndex;
	}

	@Override
	public AnimationCommand repeat(short newDuration, byte newFrame) {
		return new SpellFrameAnimationCommand(
			newDuration, newFrame, OAMsheet, OAMindex,
			BGOAMindex, BGsheet,
			OAMpaletteIndex, BGpaletteIndex
		);
	}

	@Override
	public int size() { return 28; }

	@Override
	public int serialize(byte[] buffer, int index) {
		buffer[index] = (byte)duration;
		buffer[index + 1] = (byte)(duration >> 8);
		buffer[index + 2] = frame;
		buffer[index + 3] = (byte)0x86;
		serializeInt(OAMsheet, buffer, index + 4);
		serializeInt(OAMindex, buffer, index + 8);
		serializeInt(BGOAMindex, buffer, index + 12);
		serializeInt(BGsheet, buffer, index + 16);
		serializeInt(OAMpaletteIndex, buffer, index + 20);
		serializeInt(BGpaletteIndex, buffer, index + 24);
		return index + 28;
	}

	public short getDuration() { return duration; }
	public byte getFrameID() { return frame; }
	public int getOAMindex() { return OAMindex; }
	public int getBGOAMindex() { return BGOAMindex; }
	public int getOAMsheetIndex() { return OAMsheet; }
	public int getBGsheetIndex() { return BGsheet; }
	public int getOAMpaletteIndex() { return OAMpaletteIndex; }
	public int getBGpaletteIndex() { return BGpaletteIndex; }

	public void fixPointers(
		ROM.Pointer[] OAMpointers, ROM.Pointer[] BGpointers,
		ROM.Pointer[] OAMpalettePointers, ROM.Pointer[] BGpalettePointers
	) {
		OAMsheet = OAMpointers[OAMsheet].toInt();
		BGsheet = BGpointers[BGsheet].toInt();
		OAMpaletteIndex = OAMpalettePointers[OAMpaletteIndex].toInt();
		BGpaletteIndex = BGpalettePointers[BGpaletteIndex].toInt();
	}
}

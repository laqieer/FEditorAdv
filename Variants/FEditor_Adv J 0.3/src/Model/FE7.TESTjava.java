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
 *  <Description> For representing Fire Emblem 7's unique attributes.
 */

package Model;

import java.util.HashMap;

class FE7 extends Game {
	public static String GAME_ID() {
		return "FIREEMBLEM7\u0000AE7J01";
	}

	@Override
	public int CLEAN_CHECKSUM() {
		return 0x2A524221;
	}

	@Override
	public int[][] getDefaultFreespace() {
		return new int[][] {
			// XXX This is supposedly wrong
			{ 0xDD0000, 0x100000 }
		};
	}

//“\‚è•t‚¯‚±‚±‚©‚ç
	@Override
	public int[][][] getPatches() {
		return new int[][][] {
			{ // Custom Spell Animation support.
				{ 0x080C6C00, 0x00000019 },
				{
					0x080C6C00,
					0x00000019, 0x00000000, 0x00000003, 0x080C6D59,
					0x00000000, 0x00000000, 0x1C05B570, 0xB4012001,
					//0xF7886BB8-54E60‚É”ò‚Î‚·, 0x1C06FFE9, 0xF7841C28-507CC, 0xF784FCA1-507E4,
					0x4838FCAB, //0xF7382103-080003788, 0x1C04FEED, 0x200065E5,
					0x1C2885A0, //0xF89EF789-54FE4, 0x14000400, 0xFFDEF788-54F04,
					0x70203429, 0x49304070, 0xD0002800, 0x78093902,
					0x00890085, 0x1989008E, 0x198E4E2C, 0xD0012800,
					0xE0002044, 0x4C2620AC, 0x70A0342C, 0x70A03448,
					0x20803C74, 0x60E00600, 0x4CC361E0, 0x60206830,
					0x59703504, 0x63203C54, 0x72A02038, 0x59703508,
					0x63203C48, 0x72A020FF, 0x3D0C4C1D, 0x2011022D,
					0x43280200, 0x02762601, 0x2D0019A3, 0x8020D105,
					0x34023001, 0xD1FA429C, 0x3820E00B, 0x19402520,
					0x19423D01, 0x80223A02, 0x2D003402, 0x429CD1F8,
					0x4C10D1F4, 0x71202018, 0x04242404, 0x02243402,
					0x21028820, 0x80204308, 0x490C480B, 0xBC016001,
					0xD1172800, 0xB570E018, 0x20001C05, 0xE787B401,
					0x0203FF34, 0x080C6C00, 0x0203E002, 0x08000000,
					0x0203FC00, 0x03002870, 0x030028E4, 0x080C6D41,
					0x0201FDB0, //0xF9DAF784-50348, 0xBC01BC70, 0x46C04700,
					0x06002004, 0x22018881, 0x2A00400A, 0x88C1D103,
					0x4252084A, 0x477082C2, 0x1C04B5FF, 0x1C066DE0,
					//0xFFE2F788-54F90, 0x4F8C1C05, 0x6818683B, 0x29860E01,
					0x2980D064, 0x0601D061, 0x291F0E09, 0x2929D01B,
					0x292AD021, 0x2940D02E, 0x2948D035, 0x291AD039,
					0x2908D001, 0x2209D13D, 0x43028A28, 0x1C0B822A,
					0x78213429, 0x2B1A1C28, //0xF784D102-5091C, 0xE040FC89,
				//	0xFD5CF784-50AC8, 0x3429E03D, 0x29007821, 0x1C28D139,
				//	0xFA68F79C-68500, 0x4E77E035, 0x2134223F, 0x213C5472,
					0x3242020A, 0x31095272, 0x54720A02, 0x0A123901,
					0xE0265472, 0x04024E6F, 0x211F0E12, 0xD1002A00,
					0x70712113, 0x1C30E01D, 0x42492101, //0xFE0CF782-4EC74,
					0x0200E017, 0x21800C00, 0x2302886A, //0xFCAAF79C-689D0,
					0x2913E00F, 0x7571DD0D, 0x300188F0, 0x210180F0,
					0x0D094249, 0x400889B0, 0x18403101, 0x210181B0,
					0x683B7531, 0x603B3304, 0xE07AE0AC, 0x3A018DA2,
					0x230085A2, 0xDC032A00, 0x85A0D001, 0x2301E000,
					0x1C3CB408, 0x60203CC8, 0x48506120, 0x22804950,
					0xDF0B0052, 0x6858683B, 0x60604950, 0x28006160,
					0x8008D107, 0x22011C08, 0x324004D2, 0xDF0B0152,
					0xDF12E000, 0x689A683B, 0x60A168D9, 0x64E461A2,
					0x1C233410, 0x60233484, 0x34101C1C, 0x70202001,
					0x34547320, 0x71A07020, 0x71A03C48, 0x88B03448,
					0x3C4880A0, 0x202880A0, 0x30400200, 0x34488120,
					0x02002024, 0x81203040, 0x20003C48, 0x6BA86365,
					0xD00142A0, 0x634463A0, 0x344863AC, 0x680E4932,
					0x63B46BB3, 0xD00042A3, 0x636663A3, 0x4930683B,
					0x28006918, 0x8008D107, 0x22011C08, 0x328004D2,
					0xDF0B0152, 0xDF12E000, 0x6998683B, 0xD0032800,
					0x22084928, 0x683BDF0C, 0x28006958, 0x4926D002,
					0xDF0C2208, 0x2800BC01, 0x683BD034, 0x603B331C,
					0x3304E030, 0x8DA2603B, 0xD4003A01, 0x2A0085A2,
					0x0401DC28, 0x29000E09, 0x3429D004, 0x3C297821,
					0xD01F2900, 0x49102200, 0x700A399C, 0x700A3148,
					0x2100480F, 0x1C018001, 0x05D22201, 0x00523280,
					0x480CDF0B, 0x71012108, 0x38A84807, 0x70012101,
					0x21846480, 0xF7845040, //0xF784FAF9-507D8, 0x1C20F8DB,
					//0xFE46F738-50548, 0xBD00BCFF, 0x0203FFFC, 0x0203FC00,
					0x06006800, 0x03002870, 0x02029C7C, 0x06010800,
					0x06002000, 0x02022880, 0x02022AA0
				}
			}
		};
	}

	@Override
	public PortraitArray portraitArray() {
		return new FE7PortraitArray(this);
	}

	@Override
	public ClassAnimationArray classAnimationArray() {
		return new ClassAnimationArray(
			this,
			r.new Pointer(0x080549DC), // base pointer
			r.new Pointer(0x08E00008), // clean base address
			// Hextator: Fixed the value below. Correct value
			// with current system is equal to the last value
			// seen in an "Animation List.txt" file accompanying
			// Nightmare modules (for other arrays it's that
			// value plus 1)
			// I'm making this change to the other constructors as well
			0x000000A2, // clean number of entries
			8 // words per entry
		);
	}

	@Override
	public TextArray textArray() {
		return new TextArray(
			r.new Pointer(0x080006BC), // Huffman tree start (indirected once)
			r.new Pointer(0x080006B8), // Huffman tree end (indirected twice)
			this,
			r.new Pointer(0x08012CB8), // base pointer
			r.new Pointer(0x08B808AC), // clean base address
			0x0000133E, // clean number of entries
			1 // words per entry
		);
	}

	@Override
	public SpellAnimationArray spellAnimationArray() {
		return new SpellAnimationArray(
			this,
			r.new Pointer(0x080C6D1C), // base pointer
			r.new Pointer(0x08000000), // clean base address
			0, // clean number of entries
			5 // words per entry
		);
	}

	@Override
	public SpellProgramCounterArray spellProgramCounterArray() {
		return new SpellProgramCounterArray(
			0x080C6C19, // dim_pc
			0x080C6D07, // no_dim_pc
			this,
			r.new Pointer(0x0805609C), // base pointer
			r.new Pointer(0x08C1071C), // clean base address
			0x3E, // clean number of entries
			1 // words per entry
		);
	}

	// Replaces instances in a String of condensed FE control codes
	// with their expanded counterparts
	@Override
	public String expandControlCodes(String input) {
		input = input.replace("[0x04]", "[....]");
		input = input.replace("[0x05]", "[.....]");
		input = input.replace("[0x06]", "[......]");
		input = input.replace("[0x07]", "[.......]");
		input = input.replace("[0x08]", "[OpenFarLeft]");
		input = input.replace("[0x09]", "[OpenMidLeft]");
		input = input.replace("[0x0A]", "[OpenLeft]");
		input = input.replace("[0x0B]", "[OpenRight]");
		input = input.replace("[0x0C]", "[OpenMidRight]");
		input = input.replace("[0x0D]", "[OpenFarRight]");
		input = input.replace("[0x0E]", "[OpenFarFarLeft]");
		input = input.replace("[0x0F]", "[OpenFarFarRight]");
		// Broken
		//input = input.replace("[0x10", "[LoadFace][0x");
		input = input.replace("[0x11]", "[ClearFace]");
		input = input.replace("[0x14]", "[CloseSpeechFast]");
		input = input.replace("[0x15]", "[CloseSpeechSlow]");
		input = input.replace("[0x16]", "[ToggleMouthMove]");
		input = input.replace("[0x17]", "[ToggleSmile]");
		input = input.replace("[0x18]", "[Yes]");
		input = input.replace("[0x19]", "[No]");
		input = input.replace("[0x1A]", "[Buy/Sell]");
		input = input.replace("[0x1C]", "[SendToBack]");
		input = input.replace("[0x1F]", "[.]");
		input = input.replace("[0x8004]", "[LoadOverworldFaces]");
		input = input.replace("[0x8005]", "[G]");
		input = input.replace("[0x800A]", "[MoveFarLeft]");
		input = input.replace("[0x800B]", "[MoveMidLeft]");
		input = input.replace("[0x800C]", "[MoveLeft]");
		input = input.replace("[0x800D]", "[MoveRight]");
		input = input.replace("[0x800E]", "[MoveMidRight]");
		input = input.replace("[0x800F]", "[MoveFarRight]");
		input = input.replace("[0x8010]", "[MoveFarFarLeft]");
		input = input.replace("[0x8011]", "[MoveFarFarRight]");
		input = input.replace("[0x8016]", "[EnableBlinking]");
		input = input.replace("[0x8018]", "[DelayBlinking]");
		input = input.replace("[0x8019]", "[PauseBlinking]");
		input = input.replace("[0x801B]", "[DisableBlinking]");
		input = input.replace("[0x801C]", "[OpenEyes]");
		input = input.replace("[0x801D]", "[CloseEyes]");
		input = input.replace("[0x801E]", "[HalfCloseEyes]");
		input = input.replace("[0x801F]", "[Wink]");
		input = input.replace("[0x8020]", "[Tact]");
		input = input.replace("[0x8021]", "[ToggleRed]");
		input = input.replace("[0x8022]", "[Item]");
		input = input.replace("[0x8023]", "[SetName]");
		input = input.replace("[0x8025]", "[ToggleColorInvert]");
		input = input.replace("\\]", "]");
		return input;
	}
	// expandControlCodes method; tested and working!

	// Replaces instances in a String of expanded FE control codes
	// with their condensed counterparts
	@Override
	public String condenseControlCodes(String input) {
		input = input.replace("[X]", "[0x00]");
		/* This causes problems
		input = input.replace("\n\n", "[0x02]");
		*/
		input = input.replace("\n", "[0x01]");
		input = input.replace("[A]", "[0x03]");
		input = input.replace("[....]", "[0x04]");
		input = input.replace("[.....]", "[0x05]");
		input = input.replace("[......]", "[0x06]");
		input = input.replace("[.......]", "[0x07]");
		input = input.replace("[OpenFarLeft]", "[0x08]");
		input = input.replace("[OpenMidLeft]", "[0x09]");
		input = input.replace("[OpenLeft]", "[0x0A]");
		input = input.replace("[OpenRight]", "[0x0B]");
		input = input.replace("[OpenMidRight]", "[0x0C]");
		input = input.replace("[OpenFarRight]", "[0x0D]");
		input = input.replace("[OpenFarFarLeft]", "[0x0E]");
		input = input.replace("[OpenFarFarRight]", "[0x0F]");
		input = input.replace("[LoadFace][0x", "[0x10");
		input = input.replace("[ClearFace]", "[0x11]");
		input = input.replace("[CloseSpeechFast]", "[0x14]");
		input = input.replace("[CloseSpeechSlow]", "[0x15]");
		input = input.replace("[ToggleMouthMove]", "[0x16]");
		input = input.replace("[ToggleSmile]", "[0x17]");
		input = input.replace("[Yes]", "[0x18]");
		input = input.replace("[No]", "[0x19]");
		input = input.replace("[Buy/Sell]", "[0x1A]");
		input = input.replace("[SendToBack]", "[0x1C]");
		input = input.replace("[.]", "[0x1F]");
		input = input.replace("[LoadOverworldFaces]", "[0x8004]");
		input = input.replace("[G]", "[0x8005]");
		input = input.replace("[MoveFarLeft]", "[0x800A]");
		input = input.replace("[MoveMidLeft]", "[0x800B]");
		input = input.replace("[MoveLeft]", "[0x800C]");
		input = input.replace("[MoveRight]", "[0x800D]");
		input = input.replace("[MoveMidRight]", "[0x800E]");
		input = input.replace("[MoveFarRight]", "[0x800F]");
		input = input.replace("[MoveFarFarLeft]", "[0x8010]");
		input = input.replace("[MoveFarFarRight]", "[0x8011]");
		input = input.replace("[EnableBlinking]", "[0x8016]");
		input = input.replace("[DelayBlinking]", "[0x8018]");
		input = input.replace("[PauseBlinking]", "[0x8019]");
		input = input.replace("[DisableBlinking]", "[0x801B]");
		input = input.replace("[OpenEyes]", "[0x801C]");
		input = input.replace("[CloseEyes]", "[0x801D]");
		input = input.replace("[HalfCloseEyes]", "[0x801E]");
		input = input.replace("[Wink]", "[0x801F]");
		input = input.replace("[Tact]", "[0x8020]");
		input = input.replace("[ToggleRed]", "[0x8021]");
		input = input.replace("[Item]", "[0x8022]");
		input = input.replace("[SetName]", "[0x8023]");
		input = input.replace("[ToggleColorInvert]", "[0x8025]");
		return input;
	}
	// condenseControlCodes method; tested and working!

	@Override
	public HashMap<String, Integer> getTextWidthMap() {
		return null;
	}
	// XXX

	@Override
	public int getMaxTextWidth() {
		return -1;
	}
	// XXX
}

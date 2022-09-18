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
		return "FIREEMBLEME\u0000AE7E01";
	}

	@Override
	public int CLEAN_CHECKSUM() {
		return 0x2A524221;
	}

	@Override
	public int[][] getDefaultFreespace() {
		return new int[][] {
			// XXX This is supposedly wrong
			{ 0xD00000, 0x100000 }
		};
	}

	@Override
	public int[][][] getPatches() {
		return new int[][][] {
			{ // Anti-Huffman patch.
				{ 0x08012C6C, 0x1C284902 }, // The patch signature.
				{
					0x08012C6C, // We write 9 quads starting at this address
					0x1C284902, 0xF814F000, 0xE00C6035, 0x0202A5B4,
					0x061B2380, 0x78021AC0, 0x3101700A, 0x2A003001,
					0x4770D1F9
				},
				{
					0x08004364, // and 7 more at this address
					0x0FC2B500, 0xD0022A00, 0xFC86F00E, 0x4A02E003,
					0xF0BB6812, 0xBD00FC6D, 0x03003940
				},
			},
			{ // Staff EXP patch.
				{ 0x0802A02C, 0xFA75F0A1 },
				{
					0x0801745C | FIRST_SHORT, // So we actually start writing at 0x0001745E.
					0x000046C0, 0x46C046C0 // NOPs
				},
				{ 0x0802A02C, 0xFA75F0A1 }, // BL 0x080CB51A
				{
					0x080CB518 | FIRST_SHORT,
					0x00004902, 0x78001840, 0x46C04770, 0x080CB4DE,
					0x16110C0B, 0x231E143C, 0x0F285528, 0x00112828
				} // Watch endianness! Instructions are 4902, 1840, 7800, 4770, 46C0
			},
			{ // Animation command 01.
				{ 0x080CB550, 0xBD301C20 },
				{
					0x080067B0 | FIRST_SHORT,
					0x00000000, 0xFEC0F0C4 //BL 0x080CB538
				},
				{
					0x080CB538,
					0x0E1B041B, 0xF782009B, 0x2801F819, 0x2300D100,
					0x6A103304, 0x62101AC0, 0xBD301C20
				}
			},
			{ // Animation command 48.
				{ 0x08067920, 0x080CB554 },
				{ 0x08067920, 0x080CB554 },
				{
					0x080CB554, 
					0x9C00B007, 0x6A24B087, 0x68243C04, 0x0C240224,
					0x47004800, 0x08067AED // return address
				}
			},
			{ // HP bar graphic fix
				{ 0x080CB56C, 0x46B92600 },
				{ 0x0804C334, 0xF91AF07F },
				{
					0x080CB56C,
					0x46B92600, 0x4A024901, 0x4770DF0C, 0x02016E48,
					0x01000200
				}
			},
			{ // Resume hack
				{ 0x080CB580, 0x2604B403 },
				{ 
					0x080A1160 | FIRST_SHORT | LAST_SHORT,
					0xF02A, 0xFA0D
				},
				{ 
					0x080A12A4 | FIRST_SHORT | LAST_SHORT,
					0xF02A, 0xF989
				},
				{
					0x080CB580,
					0x2604B403, 0x30146801, 0x578A310D, 0x1A9B5783,
					0x3E015583, 0xBC03D5F9, 0xF7D5B503, 0x2604FF27,
					0xB403BC03, 0x30146801, 0x578A310D, 0x189B5783,
					0x3E015583, 0xBC03D5F9, 0xBD001C06, 0x34013548,
					0x3946B443, 0x68082604, 0x300D3114, 0x578B5782,
					0x558B189B, 0xD5F93E01, 0x4770BC43
				}
			},
			{ // Link Arena hack
				{ 0x080CB5DC, 0xB5084905 },
				{
					0x0803E8D4 | FIRST_SHORT | LAST_SHORT,
					0xF08C, 0xFEB5
				},
				{
					0x08040490 | FIRST_SHORT | LAST_SHORT,
					0xF08B, 0xF8CF
				},
				{
					0x080A0854 | FIRST_SHORT | LAST_SHORT,
					0xF02A, 0xFECF
				},
				{ 0x080A0950, 0xFE44F02A },
				{ 0x080A1E5C, 0xFBCCF029 },
				{
					0x080CB5DC | LAST_SHORT,
					0xB5084905, 0xE037232C, 0xFFECF7FF, 0x3B013102,
					0x21F3D5F9, 0xBD0800C9, 0x0202BD50, 0x2604B543,
					0x30146801, 0x578A310D, 0x1A9B5783, 0x3E015583,
					0xBC03D5F9, 0xFA26F7D5, 0xB440BC40, 0x46511C30,
					0x26041840, 0x30146801, 0x578A310D, 0x189B5783,
					0x3E015583, 0xBD40D5F9, 0x1C29B500, 0xFFD1F7FF,
					0x1C722700, 0xBD00
				},
				{
					0x080CB644,
					0x4902B005, 0xFFC9F7FF, 0x0000BD10, 0x0202BD50
				},
				{
					0x080CB654,
					0xB4083146, 0xFFB2F7FF, 0xE7C3BC08
				}
			},
			{ // Custom Spell Animation support.
				{ 0x0800179C, 0x47004800 },
				{
					0x080032F0,
					0x47104A00, 0x080CBA69
				},
				{
					0x0800179C,
					0x47004800, 0x080CBAA1
				},
				{
					0x080CB680,
					0x00000019, 0x00000000, 0x00000003, 0x080CB7F5,
					0x00000000, 0x00000000, 0x1C05B570, 0xB4012001,
					0xF7886BB8, 0x1C06FFE9, 0xF7841C28, 0xF784FCA1,
					0x483FFCAB, 0xF7382103, 0x1C04FEED, 0x200065E5,
					0x1C2885A0, 0xF89EF789, 0x14000400, 0xFFDEF788,
					0x70203429, 0x49374070, 0xD0002800, 0x78093902,
					0x00890085, 0x1989008E, 0x198E4E33, 0xD0012800,
					0xE0002044, 0x4C2D20AC, 0x70A0342C, 0x70A03448,
					0x20803C74, 0x60E00600, 0x4CCE61E0, 0x60206830,
					0x59703504, 0x63203C54, 0x72A02038, 0x59703508,
					0x63203C48, 0x72A020FF, 0x3D0C4C24, 0x2011022D,
					0x43280200, 0x02762601, 0x2D0019A3, 0x8020D105,
					0x34023001, 0xD1FA429C, 0x3820E00B, 0x19402520,
					0x19423D01, 0x80223A02, 0x2D003402, 0x429CD1F8,
					0xB4FFD1F4, 0x20004904, 0x1C088008, 0x04D22201,
					0x01523280, 0xE001DF0B, 0x06002000, 0x4C10BCFF,
					0x71202018, 0x04242404, 0x02243402, 0x21028820,
					0x80204308, 0x490C480B, 0xBC016001, 0xD1172800,
					0xB570E018, 0x20001C05, 0xE779B401, 0x0203FF34,
					0x080CB680, 0x0203E026, 0x08000000, 0x0203FC00,
					0x03002870, 0x030028E4, 0x080CB7DD, 0x0201FDB0,
					0xF9CCF784, 0xBC01BC70, 0x46C04700, 0x06002004,
					0x22018881, 0x2A00400A, 0x88C1D103, 0x4252084A,
					0x477082C2, 0x1C04B5FF, 0x1C066DE0, 0xFFD4F788,
					0x4F901C05, 0x6818683B, 0x29860E01, 0x2980D064,
					0x0601D061, 0x291F0E09, 0x2929D01B, 0x292AD021,
					0x2940D02E, 0x2948D035, 0x291AD039, 0x2908D001,
					0x2209D13D, 0x43028A28, 0x1C0B822A, 0x78213429,
					0x2B1A1C28, 0xF784D102, 0xE040FC7B, 0xFD4EF784,
					0x3429E03D, 0x29007821, 0x1C28D139, 0xFA5AF79C,
					0x4E7BE035, 0x2134223F, 0x213C5472, 0x3242020A,
					0x31095272, 0x54720A02, 0x0A123901, 0xE0265472,
					0x04024E73, 0x211F0E12, 0xD1002A00, 0x70712113,
					0x1C30E01D, 0x42492101, 0xFDFEF782, 0x0200E017,
					0x21800C00, 0x2302886A, 0xFC9CF79C, 0x2913E00F,
					0x7571DD0D, 0x300188F0, 0x210180F0, 0x0D094249,
					0x400889B0, 0x18403101, 0x210181B0, 0x683B7531,
					0x603B3304, 0xE082E0B4, 0x3A018DA2, 0x230085A2,
					0xDC032A00, 0x85A0D001, 0x2301E000, 0x1C3CB408,
					0x60203CC8, 0x48546120, 0x22804954, 0xDF0B0052,
					0x6858683B, 0x60604954, 0x28006160, 0x8008D107,
					0x22011C08, 0x324004D2, 0xDF0B0152, 0x4901E004,
					0xE0016008, 0x0203FF2C, 0x689A683B, 0x60A168D9,
					0x64E461A2, 0x1C233410, 0x60233484, 0x34101C1C,
					0x70202001, 0x34547320, 0x71A07020, 0x71A03C48,
					0x88B03448, 0x3C4880A0, 0x202880A0, 0x30400200,
					0x34488120, 0x02002024, 0x81203040, 0x20003C48,
					0x6BA86365, 0xD00142A0, 0x634463A0, 0x344863AC,
					0x680E4934, 0x63B46BB3, 0xD00042A3, 0x636663A3,
					0x4932683B, 0x28006918, 0x8008D107, 0x22011C08,
					0x328004D2, 0xDF0B0152, 0x4901E004, 0xE0016008,
					0x0203FF24, 0x6998683B, 0xD0032800, 0x22084928,
					0x683BDF0C, 0x28006958, 0x4926D002, 0xDF0C2208,
					0x2800BC01, 0x683BD034, 0x603B331C, 0x3304E030,
					0x8DA2603B, 0xD4003A01, 0x2A0085A2, 0x0401DC28,
					0x29000E09, 0x3429D004, 0x3C297821, 0xD01F2900,
					0x49102200, 0x700A399C, 0x700A3148, 0x2100480F,
					0x1C018001, 0x05D22201, 0x00523280, 0x480CDF0B,
					0x71012108, 0x38A84807, 0x70012101, 0x21846480,
					0xF7845040, 0xF784FAE3, 0x1C20F8C5, 0xFE30F738,
					0xBD00BCFF, 0x0203FFFC, 0x0203FC00, 0x06006800,
					0x03002870, 0x02029C88, 0x06010800, 0x06002000,
					0x02022880, 0x02022AA0, 0x46964A08, 0x00531C1A,
					0x0AE202DC, 0x4A08B4FF, 0x68104B06, 0x42886819,
					0x6018D002, 0xDF124905, 0x4770BCFF, 0x080032F9,
					0x0300291C, 0x0203FF28, 0x0203FF2C, 0x06010800,
					0x46864809, 0x21A04809, 0x228004C9, 0xDF0C0052,
					0x4A08B4FF, 0x68104B06, 0x42886819, 0x6018D002,
					0xDF124905, 0x4770BCFF, 0x08001805, 0x02022860,
					0x0203FF20, 0x0203FF24, 0x06002000
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
			r.new Pointer(0x080541F4), // base pointer
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
			r.new Pointer(0x080CB7B8), // base pointer
			r.new Pointer(0x08000000), // clean base address
			0, // clean number of entries
			5 // words per entry
		);
	}

	@Override
	public SpellProgramCounterArray spellProgramCounterArray() {
		return new SpellProgramCounterArray(
			0x080CB699, // dim_pc
			0x080CB7A3, // no_dim_pc
			this,
			r.new Pointer(0x080558B4), // base pointer
			r.new Pointer(0x08BA13D0), // clean base address
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

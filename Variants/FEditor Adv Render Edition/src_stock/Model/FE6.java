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
 *  <Description> For representing Fire Emblem 6's unique attributes.
 */

package Model;

import java.util.HashMap;

class FE6 extends Game {
	public static String GAME_ID() {
		return "FIREEMBLEM6\u0000AFEJ01";
	}

	@Override
	public int CLEAN_CHECKSUM() {
		// FE 6 support will be for translated ROM, not the clean one!
		// Translation used is version 2.1 from RHDN
		return 0x35F5B06B;
	}

	@Override
	public int[][] getDefaultFreespace() {
		return new int[][] {
			{ 0x817A00, 0x1E8600 }, // to end of 9's
			{ 0xA297B0, 0x0D6850 }, // to end of A's
			{ 0xB013F0, 0x2FEC10 }, // to end of D's
			{ 0xE08000, 0x1F8000 }  // to end of ROM
		};
	}

	@Override
	public int[][][] getPatches() {
		return new int[][][] {
			{ // Anti-Huffman patch
				{ 0x08E00000, 0x061B2380 },
				{
					0x0800384C,
					0xB002B503, 0x2A000FC2, 0x4A05D001, 0x4A034697,
					0xF09A6812, 0xBC01FB5B, 0x00004700, 0x03003780,
					0x08E00000
				},
				{
					0x08E00000,
					0x061B2380, 0x78021AC0, 0x3101700A, 0x2A003001,
					0x4A01D1F9, 0x00004697, 0x08003862
				}
			},
			{ // Animation command 01 hack; tested and working
				{ 0x082DBF5C, 0xFD32F568 },
				{ 0x08007874 | FIRST_SHORT, 0x0000, 0xFB6AF2D4 },
				{
					0x082DBF50,
					0x38046A08, 0x041B6803, 0x009B0E1B, 0xFD32F568,
					0xD1002801, 0x33042300, 0x1AD26A0A, 0x68B8620A,
					0xBD90B003
				}
			},
			{ // Animation command 48 hack
				{ 0x0804A768, 0x082DBF74 },
				{ 0x0804A768, 0x082DBF74 },
				{
					0x082DBF74,
					0x38046A38, 0x02006800, 0xF5C00C00, 0x4801FC6F,
					0x00004700, 0x0804AD83 // return address
				}
			},
			{ // Stat hack helper functions
				{ 0x082DBF94, 0x340D3012 },
				{
					0x082DBF8C | LAST_SHORT,
					0x6804B5F0, 0xD00D2C00, 0x340D3012, 0x5DC52704,
					0x29005DE6, 0x19ADD101, 0x1BADE000, 0x3F0155C5,
					0x3812D5F5, 0xBDF0
				},
				{
					0x082DBFB4,
					0x2800B510, 0x4281DB0F, 0x2448DD0D, 0x48064344,
					0x24481900, 0x4904434C, 0x1C111864, 0xFFDCF7FF,
					0x42A03048, 0xBD10DBFA, 0x0202AB78
				}
			},
			{ // Resume Stat hack
				{ 0x08085938, 0xFB52F256 },
				{ 0x08085938, 0xFB52F256 },
				{
					0x082DBFE0 | LAST_SHORT,
					0x25002400, 0x2000B500, 0x22002134, 0xFFE2F7FF,
					0xBD00
				},
				// This was originally at 0x08085802;
				// it needed to be sliced into halfwords and rearranged
				{ 0x08085800 | FIRST_SHORT | LAST_SHORT, 0xF256, 0xFBF7 },
				// Changes to the Link Arena hack are similar.
				{
					0x082DBFF4, // | LAST_SHORT,
					0x24334E04, 0x2000B500, 0x22012134, 0xFFD8F7FF,
					0x0000BD00, 0x0202AB78 // added 'BASE' .long
				},
				{ 0x08085818, 0xFBF8F256 },
				{
					0x082DC00C,
					0xF7FFB500, 0x2038FFE9, 0xBD0019C0
				}
			},
			{ // Link Arena Stat hack
				{ 0x0808646C, 0xFDDAF255 },
				{ 0x0808520C | FIRST_SHORT | LAST_SHORT, 0xF256, 0xFF03 },
				{
					0x082DC018,
					0xF7FFB505, 0x2184FFE3, 0xBD050109
				},
				{ 0x0808646C, 0xFDDAF255 },
				{
					0x082DC024,
					0xF7FFB504, 0x20D8FFDD, 0xBD044641
				},
				{ 0x080863F4, 0xFE1CF255 },
				{
					0x082DC030 | LAST_SHORT,
					0x203EB507, 0x22012143, 0xFFBCF7FF, 0x26044644,
					0xBD07
				},
				{ 0x0808640C | FIRST_SHORT | LAST_SHORT, 0xF255, 0xFE19 },
				{
					0x082DC044 | LAST_SHORT,
					0x203EB504, 0x22002143, 0xFFB2F7FF, 0x20024669,
					0xBD04
				},
				// LA hack - fix loaded data in WRAM, part 2
				{ 0x080393D0 | FIRST_SHORT | LAST_SHORT, 0xF2A2, 0xFE41 },
				{
					0x082DC058,
					0x1C722700, 0xF7FFB507, 0xBD07FFCB
				},
				// LA hack - fix loaded data in WRAM, part 3
				{ 0x08039462 | FIRST_SHORT | LAST_SHORT, 0xF2A2, 0xFDFF },
				{
					0x082DC064 | LAST_SHORT,
					0x70012100, 0x2000B507, 0x22002189, 0xFFA0F7FF,
					0xBD07
				}
			},
			{ // Restart Chapter Stat hack
				// Tested and working
				{ 0x082DC428, 0x46A64C04 },
				{
					0x0808513C,
					0x47204C00, 0x082DC429
				},
				{
					0x082DC428,
					0x46A64C04, 0x34201C3C, 0x25332600, 0xF7FFB500,
					0xBD00FDDF, 0x08085145
				},
				{
					0x08085158,
					0x47204C00, 0x082DC441
				},
				{
					0x082DC440,
					0x46A64C04, 0x21842400, 0x18780109, 0xF7FFB500,
					0xBD00FDC9, 0x08085161
				}
			},
			{ // Custom Spell Animation support.
				{ 0x082DC0D0, 0xD0002800 },
				{
					0x082DC078,
					0x00000019, 0x00000000, 0x00000003, 0x082DC1C5,
					0x00000000, 0x00000000, 0x1C05B570, 0xB4012001,
					0xF56F6BB8, 0x1C06FB13, 0xF56B1C28, 0xF56BFA05,
					0x4838FA0F, 0xF5272103, 0x1C04FCA9, 0x200065E5,
					0x1C2885A0, 0xFBD4F56F, 0x14000400, 0xFB08F56F,
					0x70203429, 0x49304070, 0xD0002800, 0x78093902,
					0x00890085, 0x1989008E, 0x198E4E2C, 0xD0012800,
					0xE0002044, 0x4C2620AC, 0x70A0342C, 0x70A03448,
					0x20803C74, 0x60E00600, 0x4CC061E0, 0x60206830,
					0x59703504, 0x63203C54, 0x72A02038, 0x59703508,
					0x63203C48, 0x72A020FF, 0x3D0C4C1D, 0x2011022D,
					0x43280200, 0x02762601, 0x2D0019A3, 0x8020D105,
					0x34023001, 0xD1FA429C, 0x3820E00B, 0x19402520,
					0x19423D01, 0x80223A02, 0x2D003402, 0x429CD1F8,
					0x4CADD1F4, 0x71202018, 0x04242404, 0x02243402,
					0x21028820, 0x80204308, 0x490B480A, 0xBC016001,
					0xD1132800, 0xB570E014, 0x20001C05, 0xE787B401,
					0x0203FF34, 0x082DC078, 0x0203CD0E, 0x08000000,
					0x0203FC00, 0x03002724, 0x082DC1AD, 0xF8ACF56B,
					0x46C0BD70, 0x06002004, 0x22018881, 0x2A00400A,
					0x88C1D103, 0x4252084A, 0x477082C2, 0x1C04B5FF,
					0x1C066DE0, 0xFB1EF56F, 0x4F8C1C05, 0x6818683B,
					0x29860E01, 0x2980D064, 0x0601D061, 0x291F0E09,
					0x2929D01B, 0x292AD021, 0x2940D02E, 0x2948D035,
					0x291AD039, 0x2908D001, 0x2209D13D, 0x43028A28,
					0x1C0B822A, 0x78213429, 0x2B1A1C28, 0xF56BD102,
					0xE001F9F3, 0xFAC6F56B, 0x3429E03D, 0x29007821,
					0x1C28D139, 0xF894F580, 0x4E77E035, 0x2134223F,
					0x213C5472, 0x3242020A, 0x31095272, 0x54720A02,
					0x0A123901, 0xE0265472, 0x04024E6F, 0x211F0E12,
					0xD1002A00, 0x70712113, 0x1C30E01D, 0x42492101,
					0xFD12F569, 0x0200E017, 0x21800C00, 0x2302886A,
					0xFAC4F580, 0x2913E00F, 0x7571DD0D, 0x300188F0,
					0x210180F0, 0x0D094249, 0x400889B0, 0x18403101,
					0x210181B0, 0x683B7531, 0x603B3304, 0xE07AE0AC,
					0x3A018DA2, 0x230085A2, 0xDC032A00, 0x85A0D001,
					0x2301E000, 0x1C3CB408, 0x60203CC8, 0x48506120,
					0x22804950, 0xDF0B0052, 0x6858683B, 0x60604950,
					0x28006160, 0x8008D107, 0x22011C08, 0x324004D2,
					0xDF0B0152, 0xDF12E000, 0x689A683B, 0x60A168D9,
					0x64E461A2, 0x1C233410, 0x60233484, 0x34101C1C,
					0x70202001, 0x34547320, 0x71A07020, 0x71A03C48,
					0x88B03448, 0x3C4880A0, 0x202880A0, 0x30400200,
					0x34488120, 0x02002024, 0x81203040, 0x20003C48,
					0x6BA86365, 0xD00142A0, 0x634463A0, 0x344863AC,
					0x680E4932, 0x63B46BB3, 0xD00042A3, 0x636663A3,
					0x4930683B, 0x28006918, 0x8008D107, 0x22011C08,
					0x328004D2, 0xDF0B0152, 0xDF12E000, 0x6998683B,
					0xD0032800, 0x22084928, 0x683BDF0C, 0x28006958,
					0x4926D002, 0xDF0C2208, 0x2800BC01, 0x683BD034,
					0x603B331C, 0x3304E030, 0x8DA2603B, 0xD4003A01,
					0x2A0085A2, 0x0401DC28, 0x29000E09, 0x3429D004,
					0x3C297821, 0xD01F2900, 0x49102200, 0x700A399C,
					0x700A3148, 0x2100480F, 0x1C018001, 0x05D22201,
					0x00523280, 0x480CDF0B, 0x71012108, 0x38A84807,
					0x70012101, 0x21846480, 0xF56B5040, 0xF56AF863,
					0x1C20FFAF, 0xFD30F527, 0x46C0BDFF, 0x0203FFFC,
					0x0203FC00, 0x06006800, 0x030026B0, 0x02028AE0,
					0x06010800, 0x06002000, 0x02021728, 0x02021948
				}
			}
		};
	}

	@Override
	public PortraitArray portraitArray() {
		return new FE6PortraitArray(this);
	}

	@Override
	public ClassAnimationArray classAnimationArray() {
		return new ClassAnimationArray(
			this,
			r.new Pointer(0x0804B0F8), // base pointer
			r.new Pointer(0x086A0008), // clean base address
			0x0000007A, // clean number of entries
			8 // words per entry
		);
	}

	@Override
	public TextArray textArray() {
		return new TextArray(
			r.new Pointer(0x080006E0), // Huffman tree start (indirected once)
			r.new Pointer(0x080006DC), // Huffman tree end (indirected twice)
			this,
			r.new Pointer(0x08013B10), // base pointer
			r.new Pointer(0x080F635C), // clean base address
			0x00000D0E, // clean number of entries
			1 // clean words per entry
		);
	}

	@Override
	public SpellAnimationArray spellAnimationArray() {
		return new SpellAnimationArray(
			this,
			r.new Pointer(0x082DC194), // base pointer
			r.new Pointer(0x08000000), // clean base address
			0, // clean number of entries
			5 // words per entry
		);
	}

	@Override
	public SpellProgramCounterArray spellProgramCounterArray() {
		return new SpellProgramCounterArray(
			0x082DC091, // dim_pc
			0x082DC17F, // no_dim_pc
			this,
			r.new Pointer(0x0804C8C8), // base pointer
			r.new Pointer(0x085D0DA0), // clean base address
			0x32, // clean number of entries
			1 // words per entry
		);
	}

	// Replaces instances in a String of condensed FE control codes
	// with their expanded counterparts
	@Override
	public String expandControlCodes(String input)
	{
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
		input = input.replace("[0x12]", "[NormalPrint]");
		input = input.replace("[0x13]", "[FastPrint]");
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
		input = input.replace("[0x82][0x9F]", "2");
		input = input.replace("[0x82][0xA0]", "A");
		input = input.replace("[0x82][0xA1]", "3");
		input = input.replace("[0x82][0xA2]", "B");
		input = input.replace("[0x82][0xA3]", "4");
		input = input.replace("[0x82][0xA4]", "C");
		input = input.replace("[0x82][0xA5]", "5");
		input = input.replace("[0x82][0xA6]", "D");
		input = input.replace("[0x82][0xA7]", "6");
		input = input.replace("[0x82][0xA8]", "E");
		input = input.replace("[0x82][0xA9]", "F");
		input = input.replace("[0x82][0xAA]", "u");
		input = input.replace("[0x82][0xAB]", "G");
		input = input.replace("[0x82][0xAC]", "v");
		input = input.replace("[0x82][0xAD]", "H");
		input = input.replace("[0x82][0xAE]", "w");
		input = input.replace("[0x82][0xAF]", "I");
		input = input.replace("[0x82][0xB0]", "x");
		input = input.replace("[0x82][0xB1]", "J");
		input = input.replace("[0x82][0xB2]", "y");
		input = input.replace("[0x82][0xB3]", "K");
		input = input.replace("[0x82][0xB4]", "z");
		input = input.replace("[0x82][0xB5]", "L");
		input = input.replace("[0x82][0xB7]", "M");
		input = input.replace("[0x82][0xB8]", " ");
		input = input.replace("[0x82][0xB9]", "N");
		input = input.replace("[0x82][0xBA]", "!");
		input = input.replace("[0x82][0xBB]", "O");
		input = input.replace("[0x82][0xBC]", "\"");
		input = input.replace("[0x82][0xBD]", "P");
		input = input.replace("[0x82][0xBE]", "#");
		input = input.replace("[0x82][0xBF]", "Q");
		input = input.replace("[0x82][0xC0]", "$");
		input = input.replace("[0x82][0xC1]", "7");
		input = input.replace("[0x82][0xC2]", "R");
		input = input.replace("[0x82][0xC3]", "%");
		input = input.replace("[0x82][0xC4]", "S");
		input = input.replace("[0x82][0xC5]", "&");
		input = input.replace("[0x82][0xC6]", "T");
		input = input.replace("[0x82][0xC7]", "'");
		input = input.replace("[0x82][0xC8]", "U");
		input = input.replace("[0x82][0xC9]", "V");
		input = input.replace("[0x82][0xCA]", "W");
		input = input.replace("[0x82][0xCB]", "X");
		input = input.replace("[0x82][0xCC]", "Y");
		input = input.replace("[0x82][0xCD]", "Z");
		input = input.replace("[0x82][0xCE]", "(");
		input = input.replace("[0x82][0xCF]", "-");
		input = input.replace("[0x82][0xD0]", "a");
		input = input.replace("[0x82][0xD1]", ")");
		input = input.replace("[0x82][0xD2]", ".");
		input = input.replace("[0x82][0xD3]", "b");
		input = input.replace("[0x82][0xD4]", "*");
		input = input.replace("[0x82][0xD5]", "/");
		input = input.replace("[0x82][0xD6]", "c");
		input = input.replace("[0x82][0xD7]", "+");
		input = input.replace("[0x82][0xD8]", "0");
		input = input.replace("[0x82][0xD9]", "d");
		input = input.replace("[0x82][0xDA]", ",");
		input = input.replace("[0x82][0xDB]", "1");
		input = input.replace("[0x82][0xDC]", "e");
		input = input.replace("[0x82][0xDD]", "f");
		input = input.replace("[0x82][0xDE]", "g");
		input = input.replace("[0x82][0xDF]", "h");
		input = input.replace("[0x82][0xE0]", "i");
		input = input.replace("[0x82][0xE1]", "8");
		input = input.replace("[0x82][0xE2]", "j");
		input = input.replace("[0x82][0xE3]", "9");
		input = input.replace("[0x82][0xE4]", "k");
		input = input.replace("[0x82][0xE5]", ":");
		input = input.replace("[0x82][0xE6]", "l");
		input = input.replace("[0x82][0xE7]", "m");
		input = input.replace("[0x82][0xE8]", "n");
		input = input.replace("[0x82][0xE9]", "o");
		input = input.replace("[0x82][0xEA]", "p");
		input = input.replace("[0x82][0xEB]", "q");
		input = input.replace("[0x82][0xED]", "r");
		input = input.replace("[0x82][0xF0]", "s");
		input = input.replace("[0x82][0xF1]", "t");
		input = input.replace("[0x83]\u0041", ";");
		input = input.replace("[0x83]\u0043", "<");
		input = input.replace("[0x83]\u0045", "=");
		input = input.replace("[0x83]\u0047", ">");
		input = input.replace("[0x83]\u0049", "?");
		input = input.replace("[0x83]\u004A", "@");
		input = input.replace("[0x83]\u004C", "\\[");
		input = input.replace("[0x83]\u0050", "]");
		input = input.replace("[0x83]\u0052", "^");
		input = input.replace("[0x83]\u0054", "_");
		input = input.replace("[0x83]\u0056", "`");
		input = input.replace("[0x83]\u0058", "{");
		input = input.replace("[0x83]\u005A", "|");
		input = input.replace("[0x83]\\", "}");
		input = input.replace("[0x83]\u005E", "~");
		return input;
	}
	// expandControlCodes method; tested and working!

	// Hacky
	private static String replace
	(
		String sample,
		String ifIsThis,
		String becomeThis
	)
	{
		if (sample.equals(ifIsThis))
			sample = becomeThis;
		return sample;
	}
	// replace method; doesn't need testing

	// Ugh inserting the text is so much harder
	private static String translate(String input) {
		String output = "";
		String sample;
		String lastSample = "";
		String tempSample = "";
		for (int i = 0; i < input.length(); i++) {
			sample = String.copyValueOf(input.toCharArray(), i, 1);
			if (
				!lastSample.equals("\\")
				&& sample.equals("[")
			) {
				int tempInt = input.indexOf("]", i);
				String tempString = input
					.substring(i, tempInt + 1);
				output += tempString.replace("\\", "");
				i = tempInt;
				lastSample = "]";
			}
			else {
				tempSample = lastSample;
				lastSample = sample;
				if (
					!sample.equals("\\")
				) {
					sample = replace(sample, "\u005B", "[0x83][0x4C]");
					sample = replace(sample, "\u005D", "[0x83][0x50]");
					sample = replace(sample, "\u0032", "[0x82][0x9F]");
					sample = replace(sample, "\u0041", "[0x82][0xA0]");
					sample = replace(sample, "\u0033", "[0x82][0xA1]");
					sample = replace(sample, "\u0042", "[0x82][0xA2]");
					sample = replace(sample, "\u0034", "[0x82][0xA3]");
					sample = replace(sample, "\u0043", "[0x82][0xA4]");
					sample = replace(sample, "\u0035", "[0x82][0xA5]");
					sample = replace(sample, "\u0044", "[0x82][0xA6]");
					sample = replace(sample, "\u0036", "[0x82][0xA7]");
					sample = replace(sample, "\u0045", "[0x82][0xA8]");
					sample = replace(sample, "\u0046", "[0x82][0xA9]");
					sample = replace(sample, "\u0075", "[0x82][0xAA]");
					sample = replace(sample, "\u0047", "[0x82][0xAB]");
					sample = replace(sample, "\u0076", "[0x82][0xAC]");
					sample = replace(sample, "\u0048", "[0x82][0xAD]");
					sample = replace(sample, "\u0077", "[0x82][0xAE]");
					sample = replace(sample, "\u0049", "[0x82][0xAF]");
					sample = replace(sample, "\u0078", "[0x82][0xB0]");
					sample = replace(sample, "\u004A", "[0x82][0xB1]");
					sample = replace(sample, "\u0079", "[0x82][0xB2]");
					sample = replace(sample, "\u004B", "[0x82][0xB3]");
					sample = replace(sample, "\u007A", "[0x82][0xB4]");
					sample = replace(sample, "\u004C", "[0x82][0xB5]");
					sample = replace(sample, "\u004D", "[0x82][0xB7]");
					sample = replace(sample, "\u0020", "[0x82][0xB8]");
					sample = replace(sample, "\u004E", "[0x82][0xB9]");
					sample = replace(sample, "\u0021", "[0x82][0xBA]");
					sample = replace(sample, "\u004F", "[0x82][0xBB]");
					sample = replace(sample, "\"", "[0x82][0xBC]");
					sample = replace(sample, "\u0050", "[0x82][0xBD]");
					sample = replace(sample, "\u0023", "[0x82][0xBE]");
					sample = replace(sample, "\u0051", "[0x82][0xBF]");
					sample = replace(sample, "\u0024", "[0x82][0xC0]");
					sample = replace(sample, "\u0037", "[0x82][0xC1]");
					sample = replace(sample, "\u0052", "[0x82][0xC2]");
					sample = replace(sample, "\u0025", "[0x82][0xC3]");
					sample = replace(sample, "\u0053", "[0x82][0xC4]");
					sample = replace(sample, "\u0026", "[0x82][0xC5]");
					sample = replace(sample, "\u0054", "[0x82][0xC6]");
					sample = replace(sample, "\u0027", "[0x82][0xC7]");
					sample = replace(sample, "\u0055", "[0x82][0xC8]");
					sample = replace(sample, "\u0056", "[0x82][0xC9]");
					sample = replace(sample, "\u0057", "[0x82][0xCA]");
					sample = replace(sample, "\u0058", "[0x82][0xCB]");
					sample = replace(sample, "\u0059", "[0x82][0xCC]");
					sample = replace(sample, "\u005A", "[0x82][0xCD]");
					sample = replace(sample, "\u0028", "[0x82][0xCE]");
					sample = replace(sample, "\u002D", "[0x82][0xCF]");
					sample = replace(sample, "\u0061", "[0x82][0xD0]");
					sample = replace(sample, "\u0029", "[0x82][0xD1]");
					sample = replace(sample, "\u002E", "[0x82][0xD2]");
					sample = replace(sample, "\u0062", "[0x82][0xD3]");
					sample = replace(sample, "\u002A", "[0x82][0xD4]");
					sample = replace(sample, "\u002F", "[0x82][0xD5]");
					sample = replace(sample, "\u0063", "[0x82][0xD6]");
					sample = replace(sample, "\u002B", "[0x82][0xD7]");
					sample = replace(sample, "\u0030", "[0x82][0xD8]");
					sample = replace(sample, "\u0064", "[0x82][0xD9]");
					sample = replace(sample, "\u002C", "[0x82][0xDA]");
					sample = replace(sample, "\u0031", "[0x82][0xDB]");
					sample = replace(sample, "\u0065", "[0x82][0xDC]");
					sample = replace(sample, "\u0066", "[0x82][0xDD]");
					sample = replace(sample, "\u0067", "[0x82][0xDE]");
					sample = replace(sample, "\u0068", "[0x82][0xDF]");
					sample = replace(sample, "\u0069", "[0x82][0xE0]");
					sample = replace(sample, "\u0038", "[0x82][0xE1]");
					sample = replace(sample, "\u006A", "[0x82][0xE2]");
					sample = replace(sample, "\u0039", "[0x82][0xE3]");
					sample = replace(sample, "\u006B", "[0x82][0xE4]");
					sample = replace(sample, "\u003A", "[0x82][0xE5]");
					sample = replace(sample, "\u006C", "[0x82][0xE6]");
					sample = replace(sample, "\u006D", "[0x82][0xE7]");
					sample = replace(sample, "\u006E", "[0x82][0xE8]");
					sample = replace(sample, "\u006F", "[0x82][0xE9]");
					sample = replace(sample, "\u0070", "[0x82][0xEA]");
					sample = replace(sample, "\u0071", "[0x82][0xEB]");
					sample = replace(sample, "\u0072", "[0x82][0xED]");
					sample = replace(sample, "\u0073", "[0x82][0xF0]");
					sample = replace(sample, "\u0074", "[0x82][0xF1]");
					sample = replace(sample, "\u003B", "[0x83][0x41]");
					sample = replace(sample, "\u003C", "[0x83][0x43]");
					sample = replace(sample, "\u003D", "[0x83][0x45]");
					sample = replace(sample, "\u003E", "[0x83][0x47]");
					sample = replace(sample, "\u003F", "[0x83][0x49]");
					sample = replace(sample, "\u0040", "[0x83][0x4A]");
					sample = replace(sample, "\u005E", "[0x83][0x52]");
					sample = replace(sample, "\u005F", "[0x83][0x54]");
					sample = replace(sample, "\u0060", "[0x83][0x56]");
					sample = replace(sample, "\u007B", "[0x83][0x58]");
					sample = replace(sample, "\u007C", "[0x83][0x5A]");
					sample = replace(sample, "\u007D", "[0x83][0x5C]");
					sample = replace(sample, "\u007E", "[0x83][0x5E]");
					output += sample;
				}
				else if (tempSample.equals("\\"))
					lastSample = "";
			}
		}
		return output;
	}
	// translate method; tested and working!

	// Replaces instances in a String of expanded FE control codes
	// with their condensed counterparts
	@Override
	public String condenseControlCodes(String input) {
		input = translate(input);
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
		input = input.replace("[NormalPrint]", "[0x12]");
		input = input.replace("[FastPrint]", "[0x13]");
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

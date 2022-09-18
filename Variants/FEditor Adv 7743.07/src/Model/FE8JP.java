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
 *  <Description> For representing Fire Emblem 8's unique attributes.
 */

package Model;

import java.util.HashMap;

class FE8JP extends Game {
	public static String GAME_ID() {
		return "FIREEMBLEM8\u0000BE8J01";
	}

	@Override
	public int CLEAN_CHECKSUM() {
		return 0xA47246AE;
	}

	@Override
	public int[][] getDefaultFreespace() {
		return new int[][] {
			{ 0xB2A610, 0x0D59F0 }  // to end of B's
		};
	}

	@Override
	public int[][][] getPatches() {
		return new int[][][] {
			{ // Anti-Huffman patch
				{ 0x0800A278, 0x08464470 },
				{
					0x08002BA4 | LAST_SHORT,
					0x0FC2B500, 0xF007D002, 0xE001FB63, 0xFB58F007,
					0xBD00
				},
				{
					0x0800A248 | FIRST_SHORT,
					0xD005, 0x1C284904, 0xF816F000, 0xE0006035,
					0xBC704801, 0x0000BD00, 0x0202A6AC, 0x4A02B500,
					0xF0C76812, 0xBD00FB2D, 0x03004150, 0x46974A00,
					0x08464470
				},
				{
					0x08464470,
					0x061B2380, 0x78021AC0, 0x3101700A, 0x2A003001,
					0x4770D1F9
				}
			},
			{ // Animation command 01 hack
				{ 0x0846446C, 0x080522CD },
				{
					0x08005138,
					0x47004800, 0x08464421
				},
				{
					0x08464420,
					0x401820FF, 0xD00A2801, 0xD8062818, 0x6A10D103,
					0x62103804, 0x4B0BE013, 0x4B0B4718, 0x041B4718,
					0x009B0E1B, 0x3101A102, 0x4908468E, 0x46C04708,
					0xD1002801, 0x33042300, 0x1AC06A10, 0x1C206210,
					0x46C0BD30, 0x08005145, 0x08005159, 0x080522CD,
				}
			},
			{ // Animation command 48 hack
				{ 0x08058D64, 0x08464400 },
				{ 0x08058D64, 0x08464400 },
				{
					0x08464400,
					0x3D046A3D, 0x022D682D, 0xA5020C28, 0x46AE3501,
					0x47284D01, 0x47004801, 0x08071991, 0x080596CD
				}
			},
			{ // LA/Resume stat hack helper functions
				// Tested and working
				{ 0x0846448C, 0x340D3014 },
				{
					0x08464484 | LAST_SHORT,
					0x6804B5F0, 0xD00D2C00, 0x340D3014, 0x5DC52704,
					0x29005DE6, 0x19ADD101, 0x1BADE000, 0x3F0155C5,
					0x3814D5F5, 0xBDF0
				},
				{
					0x084644AC,
					0x2800B510, 0x4281DB0F, 0x2448DD0D, 0x48064344,
					0x24481900, 0x4904434C, 0x1C111864, 0xFFDCF7FF,
					0x42A03048, 0xBD10DBFA, 0x0202BE4C
				}
			},
			{ // Resume hack
				// Tested and working
				{ 0x080A5C74, 0xFC30F3BE },
				{ 0x080A5C74, 0xFC30F3BE },
				{
					0x084644D8 | LAST_SHORT,
					0x25002400, 0x2000B507, 0x22002134, 0xFFE2F7FF,
					0xBD07
				},
				{ 0x080A5AAA | FIRST_SHORT | LAST_SHORT, 0xF3BE, 0xFD1F },
				{
					0x084644EC | LAST_SHORT,
					0x920F3238, 0x2000B507, 0x22012134, 0xFFD8F7FF,
					0xBD07
				},
				{ 0x080A5AC8, 0xFD1AF3BE },
				{
					0x08464500,
					0x31841C39, 0xF7FFB500, 0xBD00FFE9
				}
			},
			{ // Link Arena hack
				// Tested and working
				{ 0x0846450C, 0x00C921EF },
				{ 0x080A518A | FIRST_SHORT | LAST_SHORT, 0xF3BF, 0xF9BF },
				{
					0x0846450C,
					0x00C921EF, 0xF7FFB500, 0xBD00FFE3
				},
				{ 0x080A697C, 0xFDCCF3BD },
				{
					0x08464518,
					0x464120C4, 0xF7FFB500, 0xBD00FFDD
				},
				{ 0x08044970, 0x00004778, 0xEB107EEA },
				{
					0x08464524 | LAST_SHORT,
					0xE28EE001, 0xE1A0A000, 0xE28F0001, 0xE12FFF10,
					0x4689B4E0, 0xB5002081, 0xFFD8F7FF, 0xBD00
				},
				{ 0x080A691C, 0xFE12F3BD },
				{
					0x08464544,
					0x46699000, 0xF7FFB500, 0xBD00FFC7
				},
				// LA hack - fix loaded data in WRAM, part 2
				{
					0x08045B3E | FIRST_SHORT,
					0x4911, 0x47384F00, 0x08464551
				},
				{
					0x08464550,
					0x1C702700, 0x00704682, 0xFFCAF7FF, 0x4802B401,
					0xBC014686, 0x00004770, 0x08045B49
				},
				// LA hack - fix loaded data in WRAM, part 3
				{
					0x08045BCE | FIRST_SHORT,
					0x4A0F, 0x47084900, 0x0846456D
				},
				{
					0x0846456C,
					0x70012100, 0x71816810, 0x2000B407, 0x22002189,
					0xFF96F7FF, 0x4B01BC07, 0x00004718, 0x08045BD9
				}
			},
			{ // Restart Chapter hack
				// Tested and working
				{ 0x0846458C, 0x4905344C },
				{
					0x080A504C,
					0x47304E00, 0x0846458D
				},
				{
					0x0846458C,
					0x4905344C, 0x4E034689, 0x260046B6, 0xF7FFB500,
					0xBD00FFA9, 0x080A5055, 0x0202BE4C
				},
				{
					0x080A506C,
					0x47304E00, 0x084645A9
				},
				{
					0x084645A8,
					0x3601A602, 0x4E0646B6, 0x46C04730, 0x4C034E05,
					0x240046A6, 0xF7FFB500, 0xBD00FF8D, 0x080A5075,
					0x080A2D29, 0x0202BE4C
				}
			},
			{ // Custom Spell Animation support.
//				{ 0x0895D7A0, 0x683FBC01 },
				{ 0x08EFBE00, 0x683FBC01 },
				{
					0x08EFBE00,
					0x00000001, 0x08EFBE10, 0x00000003, 0x08EFBFB9
/*
					0x0895D780,
					0x00000001, 0x0895D790, 0x00000003, 0x0895D939,
					0x00000000, 0x00000000, 0xB40100BF, 0x183F4802,
					0x683FBC01, 0x46C04738, 0x0895D7AC, 0x0805A16D,
					0x08055161, 0x08055179, 0x08002C7D, 0x0805A311,
					0x0805A185, 0x08054FA9, 0x0805A2B5, 0x08055279,
					0x08055425, 0x08072451, 0x080533D1, 0x080729A5,
					0x0805516D, 0x08055001, 0x08002E95, 0x1C05B570,
					0xB4012001, 0xB4801C38, 0xF7FF2700, 0x2601FFCD,
					0x1C064070, 0x27011C28, 0xFFC6F7FF, 0xF7FF2702,
					0x483AFFC3, 0x27032103, 0xFFBEF7FF, 0x65E51C04,
					0x85A02000, 0x27041C28, 0xFFB6F7FF, 0x14000400,
					0xF7FF2705, 0xBC80FFB1, 0x70203429, 0x49301C30,
					0xD0002E00, 0x78093902, 0x00890085, 0x1989008E,
					0x198E4E2C, 0xD0012800, 0xE0002044, 0x4C2620AC,
					0x70A0342C, 0x70A03448, 0x20803C74, 0x60E00600,
					0x4CCA61E0, 0x60206830, 0x59703504, 0x63203C54,
					0x72A02038, 0x59703508, 0x63203C48, 0x72A020FF,
					0x3D0C4C1D, 0x2011022D, 0x43280200, 0x02762601,
					0x2D0019A3, 0x8020D105, 0x34023001, 0xD1FA429C,
					0x3820E00B, 0x19402520, 0x19423D01, 0x80223A02,
					0x2D003402, 0x429CD1F8, 0x4CB7D1F4, 0x71202018,
					0x04242404, 0x02243402, 0x21028820, 0x80204308,
					0x490B480A, 0xBC016001, 0xD1132800, 0xB570E017,
					0x20001C05, 0xE77DB401, 0x0203FF34, 0x0895D780,
					0x0203E11A, 0x08000000, 0x0203FC00, 0x030030F4,
					0x0895D921, 0x2706B480, 0xFF3EF7FF, 0xBD70BC80,
					0x06002004, 0x22018881, 0x2A00400A, 0x88C1D103,
					0x4252084A, 0x477082C2, 0x1C04B5FF, 0x1C066DE0,
					0xF7FF2707, 0x1C05FF29, 0x683B4F94, 0x0E016818,
					0xD0732986, 0xD0702980, 0x0E090601, 0xD021291F,
					0xD02A2929, 0xD037292A, 0xD03E2940, 0xD0452948,
					0xD001291A, 0xD14C2908, 0x8A282209, 0x822A4302,
					0x34291C0B, 0x1C287821, 0xD1052B1A, 0x2708B480,
					0xFF02F7FF, 0xE004BC80, 0x2709B480, 0xFEFCF7FF,
					0xE046BC80, 0x78213429, 0xD1422900, 0xB4801C28,
					0xF7FF270A, 0xBC80FEF1, 0x4E7BE03B, 0x2134223F,
					0x213C5472, 0x3242020A, 0x31095272, 0x54720A02,
					0x0A123901, 0xE02C5472, 0x04024E73, 0x211F0E12,
					0xD1002A00, 0x70712113, 0x1C30E023, 0x42492101,
					0x270BB480, 0xFED0F7FF, 0xE01ABC80, 0x0C000200,
					0x886A2180, 0xB4802302, 0xF7FF270C, 0xBC80FEC5,
					0x2913E00F, 0x7571DD0D, 0x300188F0, 0x210180F0,
					0x0D094249, 0x400889B0, 0x18403101, 0x210181B0,
					0x683B7531, 0x603B3304, 0xE07AE0AF, 0x3A018DA2,
					0x230085A2, 0xDC032A00, 0x85A0D001, 0x2301E000,
					0x1C3CB408, 0x60203CC8, 0x48516120, 0x22804951,
					0xDF0B0052, 0x6858683B, 0x60604951, 0x28006160,
					0x8008D107, 0x22011C08, 0x324004D2, 0xDF0B0152,
					0xDF12E000, 0x689A683B, 0x60A168D9, 0x64E461A2,
					0x1C233410, 0x60233484, 0x34101C1C, 0x70202001,
					0x34547320, 0x71A07020, 0x71A03C48, 0x88B03448,
					0x3C4880A0, 0x202880A0, 0x30400200, 0x34488120,
					0x02002024, 0x81203040, 0x20003C48, 0x6BA86365,
					0xD00142A0, 0x634463A0, 0x344863AC, 0x680E4933,
					0x63B46BB3, 0xD00042A3, 0x636663A3, 0x4931683B,
					0x28006918, 0x8008D107, 0x22011C08, 0x328004D2,
					0xDF0B0152, 0xDF12E000, 0x6998683B, 0xD0032800,
					0x22084929, 0x683BDF0C, 0x28006958, 0x4927D002,
					0xDF0C2208, 0x2800BC01, 0x683BD037, 0x603B331C,
					0x3304E033, 0x8DA2603B, 0xD4003A01, 0x2A0085A2,
					0x0401DC2B, 0x29000E09, 0x3429D004, 0x3C297821,
					0xD0222900, 0x49112200, 0x700A399C, 0x700A3148,
					0x21004810, 0x1C018001, 0x05D22201, 0x00523280,
					0x480DDF0B, 0x71012108, 0x38A84808, 0x70012101,
					0x21846480, 0x270D5040, 0xFE06F7FF, 0xF7FF270E,
					0x1C20FE03, 0xF7FF270F, 0xBDFFFDFF, 0x0203FFFC,
					0x0203FC00, 0x06006800, 0x03003080, 0x02029D88,
					0x06010800, 0x06002000, 0x020228C8, 0x02022AE8
*/
				}
			}
		};
	}

	@Override
	public PortraitArray portraitArray() {
		return new FE8JPPortraitArray(this);
	}

	@Override
	public ClassAnimationArray classAnimationArray() {
		return new ClassAnimationArray(
			this,
			r.new Pointer(0x0805A97C), // base pointer
			r.new Pointer(0x08C00008), // clean base address
			0x000000C9, // clean number of entries
			8 // words per entry
		);
	}

	@Override
	public TextArray textArray() {
		return new TextArray(
			r.new Pointer(0x080006E0), // Huffman tree start (indirected once)
			r.new Pointer(0x080006DC), // Huffman tree end (indirected twice)
			this,
			r.new Pointer(0x08009FD0), // base pointer
			r.new Pointer(0x0814D08C), // clean base address
			0x00000D0A, // clean number of entries
			1 // words per entry
		);
	}

	@Override
	public SpellAnimationArray spellAnimationArray() {
		return new SpellAnimationArray(
			this,
//			r.new Pointer(0x0895D904), // base pointer
//			r.new Pointer(0x08EFBB20), // base pointer
			r.new Pointer(0x08EFBF84), // base pointer
//			r.new Pointer(0x08000000), // clean base address
//			r.new Pointer(0x08ED5560), // clean base address
			r.new Pointer(0x08EFB4E0), // clean base address
			0x71, // clean number of entries
			5 // words per entry
		);
	}

	@Override
	public SpellProgramCounterArray spellProgramCounterArray() {
		return new SpellProgramCounterArray(
//			0x0895D7ED, // dim_pc
			0x08EFBE6D, // dim_pc
//			0x0895D8EF, // no_dim_pc
			0x08EFBF6F, // no_dim_pc
			this,
//			r.new Pointer(0x0805B3F8), // base pointer
			r.new Pointer(0x0805C19C), // base pointer
//			r.new Pointer(0x0805C19C), // base pointer
//			r.new Pointer(0x085D4E60), // clean base address
			r.new Pointer(0x08EFB300), // clean base address
//			r.new Pointer(0x08EFB300), // clean base address
			0x71, // clean number of entries
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
		input = input.replace("[0x1D]", "[FastPrint]");
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
		input = input.replace("[FastPrint]", "[0x1D]");
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

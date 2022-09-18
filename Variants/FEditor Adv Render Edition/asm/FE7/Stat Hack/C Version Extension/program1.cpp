#include "E:\curr\IO\Google Drive\Cloud\Dropbox\Public\Software\Hextator's Doc\Development\Documentation\Programming\devkitPro\Register Control\regcontrol.h"

// GBA
HARDWARE_OFFSET(0x08000000)

MAIN_START //{
	EMBED(shrFEA_sig, program2, ".cpp", "arm");

	EMBED(shr1ally_hook, program2, ".cpp", "arm");
	EMBED(shr1enemy_hook, program2, ".cpp", "arm");
	EMBED(shr1NPC_hook, program2, ".cpp", "arm");

	EMBED(shr1ally_hack_wrap, program2, ".cpp", "arm");
	EMBED(shr1enemy_hack_wrap, program2, ".cpp", "arm");
	EMBED(shr1NPC_hack_wrap, program2, ".cpp", "arm");

	EMBED(shr1_hack, program2, ".cpp", "arm");

	EMBED(shr2ally_hook, program2, ".cpp", "arm");
	EMBED(shr2enemy_hook, program2, ".cpp", "arm");
	EMBED(shr2NPC_hook, program2, ".cpp", "arm");

	EMBED(shr2ally_hack_wrap, program2, ".cpp", "arm");
	EMBED(shr2enemy_hack_wrap, program2, ".cpp", "arm");
	EMBED(shr2NPC_hack_wrap, program2, ".cpp", "arm");

	EMBED(shr2_hack, program2, ".cpp", "arm");
MAIN_END //}

#include "C:\curr\IO\Dropbox\Public\Hextator's Doc\Development\Documentation\Programming\devkitPro\Register Control\regcontrol.h"

// GBA
HARDWARE_OFFSET(0x08000000)

MAIN_START //{
	EMBED(dmm_hook, program2, ".cpp", "arm");
	EMBED(dmm_hack, program2, ".cpp", "arm");
	EMBED(dmm_hack_data, program2, ".cpp", "arm");
MAIN_END //}

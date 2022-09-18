#include "E:\curr\IO\Google Drive\Cloud\Dropbox\Public\Software\Hextator's Doc\Development\Documentation\Programming\devkitPro\Register Control\regcontrol.h"

// GBA
HARDWARE_OFFSET(0x08000000)

MAIN_START //{
	EMBED(iibf_hook, program2, ".cpp", "arm");
	EMBED(iibf_link, program2, ".cpp", "arm");
	EMBED(iibf_hack, program2, ".cpp", "arm");
MAIN_END //}

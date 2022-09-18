#include "C:\curr\IO\Dropbox\Public\Hextator's Doc\Development\Documentation\Programming\devkitPro\Register Control\regcontrol.h"

THUMB_START

THUMB_FUNC(void, iibf_hook) //{
	HACK_HEADER(iibf_hook, 0x08004DC2);

	REG_BRANCH(r0);

	HACK_FOOTER(iibf_hook);
THUMB_FUNC_END //}

THUMB_FUNC(void, iibf_link) //{
	HACK_HEADER(iibf_link, 0x08004DD8);

	LONG(HACK_ADDR(iibf_hack) + 1);

	HACK_FOOTER(iibf_link);
THUMB_FUNC_END //}

// Code seen commented above rewritten in C
THUMB_FUNC(void, iibf_hack) //{
	HACK_HEADER(iibf_hack, 0x080CB660);

	// Index is in r4
	int index; READ_REG(index, r4);
	int offset = index << 2;
	// We need this to be returned in r1 so we push it to make the process
	// of restoring this value to r1 have atomic complexity such that
	// restoring the value will clobber no other registers the way the
	// following instructions will be clobbering the register for this
	// variable
	PUSHVAR(offset);
	// This will correctly generate code which references the same
	// variable from the .pool twice, once each for each return (as
	// it must be returned in the normal return and used to calculate
	// a value being returned by the hack return)
	int base = 0x02026A50;
	if (index < 0x80) {
		// Normal return
		MOVR(r0, base);
		POP(r1); // Offset
		REG_RETURN(r5, 0x08004DC5);
	}
	// Hack return
	MOVR(r5, base + offset);
	POP(r1); // Offset
	REG_RETURN(r0, 0x08004DDD);

	HACK_FOOTER(iibf_hack);
	// Using r0, r1 and r5
	// r0 should become 0x02026A50 (for the normal return only)
	// r1 should become r4 << 2
	NO_REG(r2);
	NO_REG(r3);
	NO_REG(r4);
	// r5 should become the above r0 and r1 added together (for the hack return only)
	NO_REG(r6);
	NO_REG(r7);
	NO_REG(r8);
	NO_REG(r9);
	NO_REG(r10);
	NO_REG(r11);
	NO_REG(r12);
	NO_REG(r13);
	NO_REG(r14);
THUMB_FUNC_END //}

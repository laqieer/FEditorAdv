#include "E:\curr\IO\Google Drive\Cloud\Dropbox\Public\Software\Hextator's Doc\Development\Documentation\Programming\devkitPro\Register Control\regcontrol.h"

THUMB_START

/**
 * Disassembly
.org				0x0802AC36
ldr	r0,	[r4,	#0x00]	@Load potrait pointer
ldr	r1,	[r4,	#0x04]	@Load class pointer
ldr	r0,	[r0,	#0x28]	@Load character abilities
ldr	r1,	[r1,	#0x28]	@Load class abilities
orr	r0,	r1		@Combine them
.org				0x0802AC40

The hook for the hack below this block comment skips the following:

Abilities are in r0
r1 is free
Critical percentage is in r2
Address of halfword to store critical percentage to is in r3
Unit level is at [r4, #0x08]

r0-r4 are negligible after being used; return using pop {r4, pc}
[r3] must be set to critical percentage

mov	r1,	#0x40		@Critical bonus
and	r0,	r1		@r0 is now a boolean of whether the unit has it
cmp	r0,	#0x00		@
beq				0x0802AC4E
mov	r0,	r2		@r0 = Critical percentage
.org				0x0802AC4A
add	r0,	#0x0F		@15% bonus
strh	r0,	[r3]		@Update it
.org				0x0802AC4E
pop	{r4}			@This could be a pop {r4, pc} to skip the next
pop	{r0}			@2 instructions
bx	r0			@

Of interest is that the function 0x08017624 returns the critical bonus of a weapon
in r0 if passed the halfword of the weapon data like 0xXXXXUUID
where UU is the number of uses remaining and ID is the item ID
**/

THUMB_FUNC(void, cbm_hook) //{
	HACK_HEADER(cbm_hook, 0x0802AC40);

	// r1 is free
	LOAD_SYM(r1, cbm_hook_targ);
	REG_BRANCH(r1);

	ALIGN_4;
	LABEL(cbm_hook_targ);
	LONG(HACK_ADDR(cbm_hack) + 1);

	HACK_FOOTER(cbm_hook);
THUMB_FUNC_END //}

THUMB_FUNC(void, cbm_hack) //{
	HACK_HEADER(cbm_hack, 0x08464420);

	// Push abilities, criticalPercent, data*
	PUSH(r0, r2, r4);
	// This is a hacky way to read the registers' values into the correct variables
	int abilities; POPVAR(abilities);
	// criticalPercent is actually a short, and identifying it generates
	// appropriate lsl/lsr instructions to ensure its type but this is not
	// what we want since we're just doing a strh anyway
	int criticalPercent; POPVAR(criticalPercent);
	char* data; POPVAR(data);
	if (abilities & 0x40) {
		char level = *(data + 8);
		criticalPercent += level + 15;
		_asm("strh\t%0, [r3]" :: "r" (criticalPercent));
	}
	POP(r4, pc);

	HACK_FOOTER(cbm_hack);
	/**
	 * We get to use all of these
	NO_REG(r0);
	NO_REG(r1);
	NO_REG(r2);
	NO_REG(r4);
	**/
	NO_REG(r3);
	NO_REG(r5);
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

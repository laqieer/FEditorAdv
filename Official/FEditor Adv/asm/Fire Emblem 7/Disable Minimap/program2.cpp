//#define DEBUG_DMM 1

#include "C:\curr\IO\Dropbox\Public\Hextator's Doc\Development\Documentation\Programming\devkitPro\Register Control\regcontrol.h"

THUMB_START

/**
 * [04000130]?
 * Breaks in function at 0x08001BC8 which reads and processes input from I/O
 * Setting r0 to 8 ("Start pressed") at 0x08001BE4 will open the mini map if done at a time when opening the mini map is allowed
**/

/**
 * [03007DC4]?
 * These PCs are part of an input processing function that rolls input through a queue to determine newly pressed buttons in contrast to held buttons and is somewhat of a pain to trace through
 * 0x08001C06 - input in r1
 * 0x08001A2E - input in [r0 + 4]
 * 0x08001AD8 - only executes if start is newly pressed, as far as the start button is of concern (similar code exists which executes only when B is newly pressed but not start)
**/

/**
 * 0x080A3260 calls 0x080046A0, which sets [r0 + 0xC] to NULL to close the minimap
 * 0x080A328A calls 0x08004494, which opens the minimap
**/

THUMB_FUNC(void, dmm_hook) //{
	HACK_HEADER(dmm_hook, 0x080A3284);

	// r0 and r1 are free
	LOAD_SYM(r0, dmm_hook_targ);
	REG_BRANCH(r0);

	ALIGN_4;
	LABEL(dmm_hook_targ);
	LONG(HACK_ADDR(dmm_hack) + 1);

	HACK_FOOTER(dmm_hook);
THUMB_FUNC_END //}

/**
 * Original code
.org				0x080A3284
push	{lr}			@
ldr	r0,			DMM_DEFAULT_DATA
mov	r1,	#0x03		@
bl				0x08004494
pop	{r0}			@
bx	r0			@
@.short				0
.align				2
				DMM_DEFAULT_DATA:
.long				0x08CE3B6C
**/

/**
 * Need to do pop{pc} if [0x0202BC06] is found in an 0xFF terminated array
 * Otherwise, needs to perform the instructions that were replaced above
 * 0x0800459C clobbers r1 and r2 immediately and so r1 and r2 can be used to set up the long call to 0x0800459C
 * Use lr = 0x080044A1, bx lr to return if unable to simply pop{r4-r6, pc}
**/

#define DMM_ARRAY_TERMINATOR_VAL 0xFF
#define DMM_ARRAY_TERMINATOR     ((unsigned char)DMM_ARRAY_TERMINATOR_VAL)
#define DMM_DRAW_MINIMAP         0x08004494

// Not used; long call required
void (*DMM_DRAW_MINIMAP_CALL)(void*, int) = (void (*)(void*, int))DMM_DRAW_MINIMAP;

THUMB_FUNC(void, dmm_hack) //{
#ifdef DEBUG_DMM
	HACK_HEADER(dmm_hack, 0x08D00000);
#else
	HACK_HEADER(dmm_hack, 0x08DF00D0);
#endif

	// r0 and r1 are free
	PUSH(r2-r3, lr);
	// Now r2 and r3 are free too
	unsigned char currChap = *(unsigned char*)0x0202BC06;
	unsigned char* chapList;
	LOAD_VAR_SYM(chapList, dmm_hack_data_ref);
	unsigned char check = *chapList;
	// This is generating a few extra opcodes for some reason
	// (the loop is being "unrolled" or something and causes
	// another implementation of the loop's iterative process
	// to be embedded after the POP instructions)
	while (check != DMM_ARRAY_TERMINATOR) {
		if (currChap == check) {
			POP(r2-r3, pc);
		}
		chapList++;
		check = *chapList;
	}
	// Replaced instructions
	LOAD_LIT(r0, 0x08CE3B6C);
	MOVI(r1, 3);
	LONG_CALL_THUMB(DMM_DRAW_MINIMAP, r4);
	POP(r2-r3, pc);

	// Reference to data
	ALIGN_4;
	LABEL(dmm_hack_data_ref);
	LONG(HACK_LABEL_ADDR(dmm_hack_data, dmm_data));

	HACK_FOOTER(dmm_hack);
	/**
	 * Can use these
	NO_REG(r0);
	NO_REG(r1);
	NO_REG(r2);
	NO_REG(r3);
	**/
	NO_REG(r4);
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

THUMB_FUNC(void, dmm_hack_data) //{
	HACK_HEADER(dmm_hack_data, HACK_END_ALIGN_4(dmm_hack));

	// Data
	LABEL(dmm_data);
	BYTE(0x0D);
	BYTE(DMM_ARRAY_TERMINATOR_VAL);

	HACK_FOOTER(dmm_hack_data);
	// Marking registers as unsafe is not applicable here
THUMB_FUNC_END //}

//#define DEBUG_STAT_HACK 1

#include "E:\curr\IO\Google Drive\Cloud\Dropbox\Public\Software\Hextator's Doc\Development\Documentation\Programming\devkitPro\Register Control\regcontrol.h"

THUMB_START

// Resume Hack FEditor signature

THUMB_FUNC(void, shrFEA_sig) //{
	HACK_HEADER(shrFEA_sig, 0x080CB580);

	LONG(0x2604B403);

	HACK_FOOTER(shrFEA_sig);
THUMB_FUNC_END //}

// End Resume Hack FEditor signature

// Resume Hack Part 1

THUMB_FUNC(void, shr1ally_hook) //{
	HACK_HEADER(shr1ally_hook, 0x080A1160);

	LOAD_SYM(r0, shr1ally_hook_targ);
	REG_BRANCH(r0);

	ALIGN_4;
	LABEL(shr1ally_hook_targ);
	LONG(HACK_ADDR(shr1ally_hack_wrap) + 1);

	HACK_FOOTER(shr1ally_hook);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr1enemy_hook) //{
	HACK_HEADER(shr1enemy_hook, 0x080A117C);

	LOAD_SYM(r0, shr1enemy_hook_targ);
	REG_BRANCH(r0);

	ALIGN_4;
	LABEL(shr1enemy_hook_targ);
	LONG(HACK_ADDR(shr1enemy_hack_wrap) + 1);

	HACK_FOOTER(shr1enemy_hook);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr1NPC_hook) //{
	HACK_HEADER(shr1NPC_hook, 0x080A1194);

	LOAD_SYM(r6, shr1NPC_hook_targ);
	REG_BRANCH(r6);

	ALIGN_4;
	LABEL(shr1NPC_hook_targ);
	LONG(HACK_ADDR(shr1NPC_hack_wrap) + 1);

	HACK_FOOTER(shr1NPC_hook);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr1ally_hack_wrap) //{
#ifdef DEBUG_STAT_HACK
	HACK_HEADER(shr1ally_hack_wrap, 0x08D00000);
#else
	HACK_HEADER(shr1ally_hack_wrap, 0x09A502E8);
#endif

	MOV(r0, r6);
	// Return address
	THUMB_SET_RETURN(0x080A116A);
	BRANCH(HACK_ADDR(shr1_hack), shr1ally_hack_wrap);

	HACK_FOOTER(shr1ally_hack_wrap);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr1enemy_hack_wrap) //{
	HACK_HEADER(shr1enemy_hack_wrap, HACK_END_ALIGN_4(shr1ally_hack_wrap));

	MOV(r0, r6);
	// Return address
	THUMB_SET_RETURN(0x080A1186);
	BRANCH(HACK_ADDR(shr1_hack), shr1enemy_hack_wrap);

	HACK_FOOTER(shr1enemy_hack_wrap);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr1NPC_hack_wrap) //{
	HACK_HEADER(shr1NPC_hack_wrap, HACK_END_ALIGN_4(shr1enemy_hack_wrap));

	// Return address
	THUMB_SET_RETURN(0x080A119C);
	BRANCH(HACK_ADDR(shr1_hack), shr1NPC_hack_wrap);

	HACK_FOOTER(shr1NPC_hack_wrap);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr1_hack) //{
	HACK_HEADER(shr1_hack, HACK_END_ALIGN_4(shr1NPC_hack_wrap));

	PUSH(r0-r3, r6);
	MOVI(r6, 4);
_asm("	ldr	r1,	[r0,	#0x00]	@");
_asm("	cmp	r1,	#0x00		@");
_asm("	beq				shr1_hack_NULL1");
_asm("	add	r0,	#0x14		@");
_asm("	add	r1,	#0x0D		@");
					LABEL(shr1_hack_SUB_LOOP);
_asm("	ldsb	r2,	[r1,	r6]	@");
_asm("	ldsb	r3,	[r0,	r6]	@");
_asm("	sub	r3,	r3,	r2	@");
_asm("	strb	r3,	[r0,	r6]	@");
_asm("	sub	r6,	#0x01		@");
_asm("	bpl				shr1_hack_SUB_LOOP");
					LABEL(shr1_hack_NULL1);
	POP(r0-r3, r6);
	PUSH(r0, r1, lr);
	PUSH(r7);
	LONG_CALL_THUMB(0x080A13EC, r7);
	POP(r7);
	MOVI(r6, 4);
	POP(r0, r1);
	PUSH(r0, r1);
_asm("	ldr	r1,	[r0,	#0x00]	@");
_asm("	cmp	r1,	#0x00		@");
_asm("	beq				shr1_hack_NULL2");
_asm("	add	r0,	#0x14		@");
_asm("	add	r1,	#0x0D		@");
					LABEL(shr1_hack_ADD_LOOP);
_asm("	ldsb	r2,	[r1,	r6]	@");
_asm("	ldsb	r3,	[r0,	r6]	@");
_asm("	add	r3,	r3,	r2	@");
_asm("	strb	r3,	[r0,	r6]	@");
_asm("	sub	r6,	#0x01		@");
_asm("	bpl				shr1_hack_ADD_LOOP");
					LABEL(shr1_hack_NULL2);
	POP(r0, r1);
	MOV(r6, r0);
_asm("	add	r6,	#0x48		@");
_asm("	sub	r4,	#0x01		@");
	POP(pc);

	/**
	 * Can use commented regs;
	 * this is irrelevant since the code is still mostly in raw assembly
	NO_REG(r0);
	NO_REG(r1);
	NO_REG(r2);
	NO_REG(r3);
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
	**/
	HACK_FOOTER(shr1_hack);
THUMB_FUNC_END //}

// End Resume Hack Part 1

// Resume Hack Part 2

THUMB_FUNC(void, shr2ally_hook) //{
	HACK_HEADER(shr2ally_hook, 0x080A12A0);

	LOAD_SYM(r2, shr2ally_hook_targ);
	REG_BRANCH(r2);

	ALIGN_4;
	LABEL(shr2ally_hook_targ);
	LONG(HACK_ADDR(shr2ally_hack_wrap) + 1);

	HACK_FOOTER(shr2ally_hook);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr2enemy_hook) //{
	HACK_HEADER(shr2enemy_hook, 0x080A12C0);

	LOAD_SYM(r2, shr2enemy_hook_targ);
	REG_BRANCH(r2);

	ALIGN_4;
	LABEL(shr2enemy_hook_targ);
	LONG(HACK_ADDR(shr2enemy_hack_wrap) + 1);

	HACK_FOOTER(shr2enemy_hook);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr2NPC_hook) //{
	HACK_HEADER(shr2NPC_hook, 0x080A12DC);

	LOAD_SYM(r2, shr2NPC_hook_targ);
	REG_BRANCH(r2);

	ALIGN_4;
	LABEL(shr2NPC_hook_targ);
	LONG(HACK_ADDR(shr2NPC_hack_wrap) + 1);

	HACK_FOOTER(shr2NPC_hook);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr2ally_hack_wrap) //{
	HACK_HEADER(shr2ally_hack_wrap, HACK_END_ALIGN_4(shr1_hack));

_asm("	add	r1,	r5,	r1	@");
	// Return address
	THUMB_SET_RETURN(0x080A12AA);
	BRANCH(HACK_ADDR(shr2_hack), shr2ally_hack_wrap);

	HACK_FOOTER(shr2ally_hack_wrap);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr2enemy_hack_wrap) //{
	HACK_HEADER(shr2enemy_hack_wrap, HACK_END_ALIGN_4(shr2ally_hack_wrap));

	// Return address
	THUMB_SET_RETURN(0x080A12C8);
	BRANCH(HACK_ADDR(shr2_hack), shr2enemy_hack_wrap);

	HACK_FOOTER(shr2enemy_hack_wrap);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr2NPC_hack_wrap) //{
	HACK_HEADER(shr2NPC_hack_wrap, HACK_END_ALIGN_4(shr2enemy_hack_wrap));

_asm("	add	r1,	r5,	r1	@");
	// Return address
	THUMB_SET_RETURN(0x080A12E6);
	BRANCH(HACK_ADDR(shr2_hack), shr2NPC_hack_wrap);

	HACK_FOOTER(shr2NPC_hack_wrap);
THUMB_FUNC_END //}

THUMB_FUNC(void, shr2_hack) //{
	HACK_HEADER(shr2_hack, HACK_END_ALIGN_4(shr2NPC_hack_wrap));

	PUSH(r0, r1, r6, r7, lr);
	LONG_CALL_THUMB(0x080A16C0, r7);
_asm("	add	r5,	#0x48		@");
_asm("	add	r4,	#0x1		@");
					LABEL(shr2_hack_ADD_CALL);
_asm("	sub	r1,	#0x46		@");
	MOVI(r6, 4);
_asm("	ldr	r0,	[r1,	#0x00]	@");
_asm("	cmp	r0,	#0x00		@");
_asm("	beq				shr2_hack_NULL");
_asm("	add	r1,	#0x14		@");
_asm("	add	r0,	#0x0D		@");
					LABEL(shr2_hack_ADD_LOOP);
_asm("	ldsb	r2,	[r0,	r6]	@");
_asm("	ldsb	r3,	[r1,	r6]	@");
_asm("	add	r3,	r3,	r2	@");
_asm("	strb	r3,	[r1,	r6]	@");
_asm("	sub	r6,	#0x01		@");
_asm("	bpl				shr2_hack_ADD_LOOP");
					LABEL(shr2_hack_NULL);
	POP(r0, r1, r6, r7, pc);

	/**
	 * Can use commented regs;
	 * this is irrelevant since the code is still mostly in raw assembly
	NO_REG(r0);
	NO_REG(r1);
	NO_REG(r2);
	NO_REG(r3);
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
	**/
	HACK_FOOTER(shr2_hack);
THUMB_FUNC_END //}

// End Resume Hack Part 2

// NOTE: The following TODO only applies if this hack is to be made to
// replace the one it is based off of rather than extending it as it currently does
// TODO: Link Arena, unit menu and bugfix parts

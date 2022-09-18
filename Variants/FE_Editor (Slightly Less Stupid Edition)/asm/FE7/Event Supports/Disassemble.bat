@echo off

SET startDir=E:\curr\IO\Google Drive\Cloud\Dropbox\Public\Software\Hextator's Doc\Development\Utilities\Programming\devkitPro\arm-eabi\

SET disassemble_arm="%startDir%Disassemble ARM"
SET disassemble_thumb="%startDir%Disassemble Thumb"
CALL %disassemble_arm% "[AP] Event Supports.dmp"
CALL %disassemble_thumb% "[AP] Event Supports.dmp"

pause

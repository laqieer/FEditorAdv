@echo off

SET startDir=E:\curr\IO\Google Drive\Cloud\Dropbox\Public\Software\Hextator's Doc\Development\Utilities\Programming\devkitPro\arm-eabi\

SET disassemble_arm="%startDir%Disassemble ARM"
SET disassemble_thumb="%startDir%Disassemble Thumb"
CALL %disassemble_arm% "program1.cpp.formatted.dmp"
CALL %disassemble_thumb% "program1.cpp.formatted.dmp"

pause

@echo off

SET startDir=E:\curr\IO\Google Drive\Cloud\Dropbox\Public\Software\Hextator's Doc\Development\Utilities\Programming\devkitPro\arm-eabi\

@REM SET program1=%~dp0
@REM SET program2=%~dp0

@REM SET program1="%program1%program1.cpp"
@REM SET program2="%program2%program2.cpp"

SET compile="%startDir%Compile ARM and Thumb"
@REM CALL %compile% %program1% %program2%
CALL %compile% program1.cpp program2.cpp

pause

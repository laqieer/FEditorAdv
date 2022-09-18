This hack causes the minimap to be disabled for chapters specified in a null terminated chapter list pointed to by a literal in the hack's code.

The hack works by making the function for loading the minimap return immediately if the current chapter is in the list; the start button is still being detected as pressed and is not disabled.

The source currently places the hack at a location requested by the person who commissioned the hack; for reference it was sold for 15 USD.

There is a JFP patch of the hack included, but it may not work for you. If not, recompile the source code with a different target address or take some other action to ensure the data being patched is safe to do so.

The JFP specification and a Java implementation of it can be found here:

http://dl.dropbox.com/u/336940/Hextator%27s%20Doc/Development/Applications/Patching/JFP%20Format/folDIR.html

A debug flag may be enabled to place the code at a location suitable for debugging in an otherwise unmodified version of the game the hack is for.

The file called "program1.cpp.formatted.dmp" is the output of running "Reg Control.bat".

It is compliant with the format and associated utility here:

http://dl.dropbox.com/u/336940/Hextator%27s%20Doc/Development/Applications/Patching/Assembly%20Patcher/folDIR.html

which can be used to apply the output file as a patch.

Naturally the "program1.cpp" and "program2.cpp" files are the source files.

More information on "Reg Control.bat" can be found here:

http://dl.dropbox.com/u/336940/Hextator%27s%20Doc/Development/Documentation/Programming/devkitPro/Register%20Control/folDIR.html

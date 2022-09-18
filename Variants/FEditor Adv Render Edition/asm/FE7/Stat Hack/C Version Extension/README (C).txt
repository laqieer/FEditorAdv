This hack enhances the stat hack auto patch FEditor normally applies (which might need to be applied before this hack is applied to ensure the auto patch's exclusive functionality continues to work).
The FEditor auto patch subtracts certain base stats from units before their data is saved and adds it back after it is loaded to allow higher stats.
This hack extends this to work for enemies and NPCs as well.

The hack is largely based off of the original assembly code written for FEditor; its source is still somewhat sloppy.

The hack writes certain values to trick FEditor into not interfering with it so it can be used to replace the existing auto patch with the enhanced version.

To use it however will most likely require manual work to adapt the hack for the user's needs:

The source currently places the hack at a location requested by the person who commissioned the hack; for reference it was sold for 90 USD.

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

This is not an auto patch.
Also, this patch is for a specific hack and may not work for you; you may need to remake it by altering the original source file (included).
It may be applied manually using software that applies patches of the "JFP" format.

The purpose of this patch is described in the source file (look for something like "@This hack [...]").

Here is where a Java implementation of the patching format can be found:

In

http://dl.dropbox.com/u/336940/Hextator%27s%20Doc.7z

at

Hextator's Doc/Development/Applications/Patching/JFP Format/Java Implementation

Here is the JFP format specification:

JFP - "Just F***ing Patch" file patching format specification
~~~~~
Terms:
VLI - Variable length integer
~~~~~
File format:
MD5 digest of bytes being patched before patching
MD5 digest of bytes being patched after patching
VLI of necessary file length
Repeat the next 3 elements for each following address where a difference in the input files occurs until the first end of file is reached in either file
VLI of address of next change relative to the address of the last non change
VLI of length of changes at previous address until address of non change occurs
String of bytes each of which is the result of XOR'ing the bytes at the corresponding address in the old and new files
~~~~~
VLI format:
There will be k - 1 1 bits and a 0 bit, or k bits to indicate the number of bytes of data
After will be 0 bit padding until the data begins, which will be the big endian representation of the data with the least significant bit in the least significant bit position of the least significant byte to remove the need for shifting
When reading the VLI, the byte containing the first bit after the length indicating 0 bit is the first counted as part of the data length
~~~~~
Special notes:
When making a patch, the smaller of the two files (if the given files aren't the same size) is padded with 0 bytes onto the end of the file until it matches the size of the other file
When applying a patch, if the file being patched is smaller than the file size specified in the patch, the file being patched is padded with 0 bytes onto the end of the file until it is the size specified in the patch

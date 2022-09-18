Welcome to my (Obviam's) FEditor. I'm releasing it under the name "Hextator" because it suits my
affinity for being SUCH A BEAST HACKER OMFG.

In the folder you've found this file should be the following key elements of getting use out of
this utility:

- doc:
	Documentation folder. Contains key information about making use of the utility.
	DO NOT IGNORE THE CONTENTS OF THIS FOLDER. You can seriously injure your ability
	to work with your ROM if you do. Not because FEditor is super hard to use, but
	because there are a lot of things users who are not "power users" will need done
	for them automatically to avoid issues associated with their lack of understanding
	of how these things work. FEditor can't do certain things automatically if you need
	to override what it does, and in some cases it even needs help with things it CAN
	do. The contents of this folder detail exactly how to use FEditor's features to
	avoid complications, which typically involve management of what data is and is
	not in use.
- internal:
	Also a documentation folder, but the information within is not required reading.
	However, if things like "checksums" or "footers" are discussed when you ask about
	this utility, reading the contents of this directory could help answer your new
	questions about what those things even are or where they're located.
- dist:
	Distribution folder. The editor is literally distributed in this folder; the .jar file
	named "FEditor Adv.jar" within is the Java executable ARchive that you must have a Java
	Runtime Environment (preferably the latest version) to execute, in which the editor
	resides.
- License.txt and COPYING:
	More important than you think, these files give the short and long
	version of the license this application has been released under, respectively. It is
	this license that allows you to freely play with the contents of the directory listed
	below (under certain reasonable circumstances detailed in the license):
- src:
	Source folder. The gears of the editor are contained here. If something about the
	editor is particularly awesome, the code (likely written by me and usually specified
	if otherwise) within this folder is the cause. If there is a glaring bug and/or other
	type of issue with the editor, you can blame [s]me[/s] the same thing for that, too.
- asm:
	Assembly hack source folder. The origin of the data in the int[][][]s returned by
	"getPatches()" in the source for the various model classes representing the GBA FEs
	comes from the assembly source code (written by me unless otherwise specified) in
	this folder. If you're capable of understanding assembly and want to try to truly
	understand my mostly incomprehensible genius, have a look in here.
- Release Info.txt: This file contains a history of all the releases and other modifications
	to the editor I cared to document. I cared enough to be rather verbose, so it's getting
	big, fast. It also contains information about known bugs and planned features. I
	consider it required reading each time a new release is reported.
- Credits.txt: Should be obvious what this is. I consider this required reading, too.

If you're a Windows user, you may have an easier time using the batch files "Execute FE (game
number).bat" to boot the application than running the .jar in the dist folder.

Either way, the application is written in Java and should work on all of the many operating
systems supported by Java's Runtime Environment system, which includes Vista 64 - the OS I
was running on at the time of this writing.

Cool, right? Right. Enjoy~

IF YOU LIKE THIS SOFTWARE

Feel free to donate:

https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=EL8TGBR6SFH86

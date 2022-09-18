import os, shutil, sys
path = os.path

for step in os.walk('src'):
	os.system('del %s 2> NUL' % path.join(step[0], '*.class'))

compiler = 'javac %s-cp src -cp dist\\lib\\appframework-1.0.3.jar -sourcepath src' % (sys.argv[1] + ' ' if len(sys.argv) > 1 else '')

os.system(compiler + ' src\\fe_editor\\App.java')
os.system(compiler + ' src\\Graphics\\Graphic_Editor.java')
os.system(compiler + ' src\\Graphics\\Palette_Dumper.java')
os.system(compiler + ' src\\Controls\\PaletteFrame.java')
os.system(compiler + ' src\\Model\\Natural_Script_Formatter.java')

try:
	shutil.rmtree('build')
except OSError as err:
	if err.args[0] != 3: raise

def myfilter(src, names):
	return ('.svn',) + tuple(
		name for name in names
		if not (
			path.isdir(path.join(src, name)) or
			name.endswith('.class') or
			'resources' in src or
			src.endswith('services')
		)
	)

shutil.copytree('src', 'build', ignore = myfilter)

os.system('jar cfm dist\\FE_Editor.jar MANIFEST.MF -C build .')

for step in os.walk('src'):
	os.system('del %s 2> NUL' % path.join(step[0], '*.class'))

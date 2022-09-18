set tempVar=%~dp0
set tempVar=%tempVar%build\classes
cd %tempVar%
java -Xmx256m Graphics.Format_Converter_4BPP
pause

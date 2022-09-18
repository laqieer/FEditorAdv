set tempVar=%~dp0
set tempVar=%tempVar%dist
cd %tempVar%
java -jar -Xmx256m FE_Editor.jar %1 "FE 6"
pause

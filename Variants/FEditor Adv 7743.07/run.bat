cd %~dp0
rem java -jar -Xmx256m "dist\FEditor Adv.jar" %1 2> log.txt
java -jar -Xmx256m "dist\FEditor Adv.jar" %1
type log.txt

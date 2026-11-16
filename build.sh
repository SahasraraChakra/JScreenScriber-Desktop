#!/bin/sh
# One-step build without Maven: downloads FlatLaf, compiles for Java 17+, produces dist/jscreenscriber.jar (runnable, FlatLaf bundled).
set -e
cd "$(dirname "$0")"
FL=3.5.4
rm -rf build dist; mkdir -p lib build dist
[ -f lib/flatlaf-$FL.jar ] || curl -L -o lib/flatlaf-$FL.jar https://repo1.maven.org/maven2/com/formdev/flatlaf/$FL/flatlaf-$FL.jar
find src/main/java -name '*.java' > sources.txt
javac --release 17 -encoding UTF-8 -cp lib/flatlaf-$FL.jar -d build @sources.txt
cp -r src/main/resources/* build/
(cd build && jar xf ../lib/flatlaf-$FL.jar && rm -f module-info.class META-INF/MANIFEST.MF META-INF/*.SF META-INF/*.RSA META-INF/*.DSA)
printf 'Main-Class: com.jscreenscriber.Main\nEnable-Native-Access: ALL-UNNAMED\n' > manifest.txt
jar cfm dist/jscreenscriber.jar manifest.txt -C build .
echo "Built dist/jscreenscriber.jar"
echo "Run with: java -jar dist/jscreenscriber.jar   (or ./run.sh to see errors in the console)"

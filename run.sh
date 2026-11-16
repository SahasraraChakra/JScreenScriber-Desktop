#!/bin/sh
cd "$(dirname "$0")"
java -version
java -jar dist/jscreenscriber.jar

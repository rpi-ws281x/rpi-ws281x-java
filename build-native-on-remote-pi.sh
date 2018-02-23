#!/usr/bin/env bash

if [ -z "$1" ]; then
    echo "Please provide the Raspberry PI location and username as first parameter."
    echo "For example: $0 pi@192.168.1.111"
    exit
fi

RASPBERRY_PI="$1"
REMOTE_BUILD_DIR="/tmp/rpi_ws281x_build"

echo "Creating target dir in /tmp"
ssh "$RASPBERRY_PI" -t "mkdir $REMOTE_BUILD_DIR"

echo "Copy necessary files"
rsync -a ./* "$RASPBERRY_PI":$REMOTE_BUILD_DIR --delete --exclude build --exclude copy-to-rpi.sh

echo "Run build.sh on Raspberry PI"
ssh "$RASPBERRY_PI" -t "cd $REMOTE_BUILD_DIR; bash src/scripts/createNativeLib.sh"

echo "Copy rpiWs821xJava.jar back to development PC"
scp -r "$RASPBERRY_PI":$REMOTE_BUILD_DIR/build .

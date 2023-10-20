#!/bin/bash
set -e
set -o pipefail
cd ~
sudo apt-get --yes --force-yes install build-essential libpcre2-dev libpcre3-dev
sudo apt-get --yes --force-yes  install default-jdk-headless
wget http://prdownloads.sourceforge.net/swig/swig-4.1.1.tar.gz
tar -xzvf swig-4.1.1.tar.gz
cd swig-4.1.1
./configure && make
cd Examples/java/class && make check
cd ../../../
sudo make install
swig -version | grep "SWIG Version 4.1.1"
if [ $? -ne 0 ]
then
  echo "swig -version command didn't output expected info. Install probably failed 😣"
  exit 1;
else
  echo "Install worked!!! 🔥🔥😀"
  exit 0;
fi
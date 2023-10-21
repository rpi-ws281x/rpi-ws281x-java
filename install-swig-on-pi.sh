#!/bin/bash
set -e
set -o pipefail
cd ~
#sudo apt-get --yes --force-yes install build-essential libpcre2-dev libpcre3-dev
echo "check if java is already installed..."
java --version | grep jdk
if [ $? -ne 0 ]
then
   echo "didn't detect install, will try..."
   echo "check cpu architecture..."
   cat /proc/cpuinfo | grep ARMv6
   if [ $? -eq 0 ]
   then
     echo "DETECTED ARMv6, WILL TRY AND INSTALL ZULU JDK!"
     mkdir -p /usr/lib/jvm
     cd /usr/lib/jvm
     sudo wget https://cdn.azul.com/zulu-embedded/bin/zulu11.41.75-ca-jdk11.0.8-linux_aarch32hf.tar.gz
     sudo tar -xzvf zulu11.41.75-ca-jdk11.0.8-linux_aarch32hf.tar.gz
     sudo rm zulu11.41.75-ca-jdk11.0.8-linux_aarch32hf.tar.gz
     sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/zulu11.41.75-ca-jdk11.0.8-linux_aarch32hf/bin/java 1
     sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/zulu11.41.75-ca-jdk11.0.8-linux_aarch32hf/bin/javac 1
   else
     echo "didn't detect ARMv6, will try and install default jdk..."
     sudo apt-get --yes --force-yes  install default-jdk-headless
   fi
else
  echo "detected java, skipping install"
fi

mkdir -p ~/rpi_ws281x_build/swigInstall
cd ~/rpi_ws281x_build/swigInstall
if [ -d "swig-4.1.1" ]; then
    echo "found swig folder, skipping download..."
else
    echo "attempting swig 4.1.1 download"
    wget http://prdownloads.sourceforge.net/swig/swig-4.1.1.tar.gz
    tar -xzvf swig-4.1.1.tar.gz
    rm swig-4.1.1.tar.gz
fi

cd swig-4.1.1
echo "trying to figure out if --with-javainc arg needs/can to be passed to swig configure..."
echo "executing part of swig configure script to determine if it will find jni.h..."

JAVAINCDIR="/usr/j2sdk*/include /usr/local/j2sdk*/include /usr/jdk*/include /usr/local/jdk*/include /opt/j2sdk*/include /opt/jdk*/include /usr/java/include /usr/java/j2sdk*/include /usr/java/jdk*/include /usr/local/java/include /opt/java/include /usr/include/java /usr/local/include/java /usr/lib/java/include /usr/lib/jvm/java*/include /usr/lib64/jvm/java*/include /usr/include/kaffe /usr/local/include/kaffe /usr/include"


for d in $JAVAINCDIR ; do
  if test -r "$d/jni.h" ; then
    echo "yep, found something, setting flag"
	  FOUNDJNI="true"
	  break
  fi
done

if [ "$FOUNDJNI" != "true" ]; then
  echo "configure won't find anything, will try to get a path for JNI.h"
  for d2 in /usr/lib/jvm/*/include ; do
    if test -r "$d2/jni.h" ; then
      echo "SUCCESS! found $d2"
      echo "will pass to configure"
  	  SELFJNI=$d2
  	  break
    fi
  done
fi




if [ "$FOUNDJNI" = "true" ]; then
  sudo ./configure
else
  if [ -z ${SELFJNI+x} ]; then
    echo "NO JNI FOUND! NO POINT IN COMPILING SWIG ONLY FOR IT TO FAIL!"
    echo "you could try running ./configure in $(pwd) yourself. If at the end it outputs:"
    echo "'The SWIG test-suite and examples are configured for the following languages:'"
    echo "    java perl5"
    echo "...then you're fine. You can then run the very time consuming command 'make' to compile swig."
    exit 1
  else
    echo "passing include dir '$SELFJNI' to ./configure..."
    sudo ./configure --with-javaincl=$SELFJNI
  fi
fi
sudo make
echo "COMPILATION DONE!!"
cd Examples/java/class && make check
echo "CHECK DONE!!"
cd ../../../
sudo make install
swig -version | grep "SWIG Version 4.1.1"
if [ $? -ne 0 ]; then
  echo "swig -version command didn't output expected info. Install probably failed 😣"
  exit 1;
else
  echo "Install worked!!! 🔥🔥😀"
  exit 0;
fi
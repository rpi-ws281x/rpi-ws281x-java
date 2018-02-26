# rpi-ws281x-java
rpi281x wrapper for Java using SWIG

### To build on a raspberry pi

Run `scripts/createNativeLib.sh` to generate the SWIG java code and generate the libws2811.so native library.

Run `./gradlew assemble` to compile the java code and create a jar containing the compile class files and the native .so file.

### To build from another machine

The machine must be a linux machine, or have a bash-compatible shell like babun, cygwin, or git-bash on windows.

Run `./build-native-on-remote-pi.sh`.  This will copy the project to the pi, and run the script in the previous section, and copy the .so library back to the dev machine. After that, run .\gradlew assemble to compile the java code and create a jar containing the compile class files and the native .so file.  The easiest way to use the jar would be to publish it to a maven repository, or your local .m2 repository using `./gradlew publishToMavenLocal` and use maven coordinates in your maven or gradle project. 

Attribution
-----------
* SWIG generation and native lib compiling based on scripts at https://github.com/limpygnome/build-tv
* build-native-on-remote-pi script based on script at https://github.com/Cacodaimon/rpi_ws281x4j
* Loading JNI native .so file code based on code at https://github.com/mattjlewis/diozero/blob/master/diozero-ws281x-java

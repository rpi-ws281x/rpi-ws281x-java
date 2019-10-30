#!/bin/bash

# ---------------------------------------------------------------------------------------------------------------------
# Compiles shared libraries required for interfacing with Raspberry Pi GPIO hardware from Java, using JNI.
# ---------------------------------------------------------------------------------------------------------------------
# Author(s):    limpygnome <limpygnome@gmail.com>, mbelling <matthew.bellinger@gmail.com>
# ---------------------------------------------------------------------------------------------------------------------

# *********************************************************************************************************************
# Configuration
# *********************************************************************************************************************

# This defines the JDK to use for JNI header files; automatically picks first dir using ls
JDK_PATH="/usr/lib/jvm"
JDK_DIR=$(ls "${JDK_PATH}" | head -n 1)
JDK_FULL_PATH="${JDK_PATH}/${JDK_DIR}"

# The libs to include when building C files using GCC
GCC_INCLUDES="-I${JDK_FULL_PATH}/include -I${JDK_FULL_PATH}/include/linux"

# Relative dir names for input/output
BASE_DIR="$(dirname "$0")/../.."
OUTPUT="${BASE_DIR}/build"
SWIG_SRC="${BASE_DIR}/src/swig"
SWIG_OUT="${OUTPUT}/swig"
SWIG_OUT_JAVA="${OUTPUT}/generatedSource/java/com/github/mbelling/ws281x/jni"
SWIG_PACKAGE_NAME="com.github.mbelling.ws281x.jni"
NATIVE_SRC="${OUTPUT}/ws281x"
NATIVE_OUT="${NATIVE_SRC}/output"
NATIVE_LIB_NAME="ws281x.so"
LIB_BASE_NAME="libws281x"
WRAPPER_LIB_NAME="${LIB_BASE_NAME}.so"


# *********************************************************************************************************************
# Functions
# *********************************************************************************************************************

function compileSrc
{
    SRC="${1}"
    OUT="${2}"

    gcc -shared -fPIC -w -o "${OUT}" -c "${SRC}" -I./ -I${JDK_FULL_PATH}/include
}

function programInstalled
(
    CMD="${1}"
    EXPECTED="${2}"
    ERROR="${3}"
    SUCCESS="${4}"

    OUTPUT=$(eval ${CMD} || echo "fail")
    if [[ "${OUTPUT}" != *"${EXPECTED}"* ]]; then
        echo "${ERROR}"
        exit 1
    else
        echo "${SUCCESS}"
    fi
)

# *********************************************************************************************************************
# Main
# *********************************************************************************************************************

echo "NeoPixel ws281x Library Compiler"
echo "****************************************************"

# Check dependencies installed
set -e
programInstalled "swig -version" "SWIG Version" "Error - SWIG is not installed, cannot continue!" "Check - SWIG installed..."
programInstalled "gcc --version" "free software" "Error - GCC is not installed, cannot continue!" "Check - GCC installed..."
programInstalled "ar --version" "free software" "Error - AR is not installed, cannot continue!" "Check - AR installed..."
programInstalled "ranlib -v" "free software" "Error - ranlib is not installed, cannot continue!" "Check - ranlib installed..."
programInstalled "git --version" "git version" "Error - git is not installed, cannot continue!" "Check - git installed..."
set +e

# Clean workspace
echo "Deleting build directory to start clean..."
rm -rf build

# Retrieve rpi_ws281x repository
echo "Cloning rpi_ws281x repository..."
git clone https://github.com/jgarff/rpi_ws281x.git ${NATIVE_SRC}

# At the time of this writing this repository does not tag versions, so checking out at a specific commit so we build a consistent library
echo "Checking out specific revision..."
pushd ${NATIVE_SRC}
git checkout 6851d9fb090f8a4703d2ceac97da2de617b09e8d
popd

# Create all the required dirs
echo "Creating required dirs..."
mkdir -p "${SWIG_OUT}"
mkdir -p "${SWIG_OUT_JAVA}"
mkdir -p "${NATIVE_OUT}"
mkdir -p "${OUTPUT}/nativeLib"


# Building swig wrapper
echo "Building JNI interface using SWIG..."

swig -java -outdir "${SWIG_OUT_JAVA}" -package "${SWIG_PACKAGE_NAME}" -o "${SWIG_OUT}/rpi_ws281x_wrap.c" "${SWIG_SRC}/rpi_ws281x.i"


# Compile library objects
echo "Compiling ws281x library objects..."

compileSrc "${NATIVE_SRC}/ws2811.c"        "${NATIVE_OUT}/ws2811.o"
compileSrc "${NATIVE_SRC}/pwm.c"           "${NATIVE_OUT}/pwm.o"
compileSrc "${NATIVE_SRC}/pcm.c"           "${NATIVE_OUT}/pcm.o"
compileSrc "${NATIVE_SRC}/dma.c"           "${NATIVE_OUT}/dma.o"
compileSrc "${NATIVE_SRC}/rpihw.c"         "${NATIVE_OUT}/rpihw.o"
compileSrc "${NATIVE_SRC}/mailbox.c"       "${NATIVE_OUT}/mailbox.o"


# Compile library
echo "Compiling ws281x library..."
gcc -shared -o "${OUTPUT}/${NATIVE_LIB_NAME}" "${NATIVE_OUT}/ws2811.o" "${NATIVE_OUT}/pwm.o" "${NATIVE_OUT}/pcm.o" "${NATIVE_OUT}/dma.o" "${NATIVE_OUT}/rpihw.o" "${NATIVE_OUT}/mailbox.o"

echo "Creating archive..."
ar rc "${NATIVE_OUT}/${LIB_BASE_NAME}.a" "${NATIVE_OUT}/ws2811.o" "${NATIVE_OUT}/pwm.o" "${NATIVE_OUT}/pcm.o" "${NATIVE_OUT}/dma.o" "${NATIVE_OUT}/rpihw.o" "${NATIVE_OUT}/mailbox.o"

echo "Indexing archive..."
ranlib "${NATIVE_OUT}/${LIB_BASE_NAME}.a"


# Compile wrapper into object
echo "Compiling wrapper into object..."
gcc -pthread -fno-strict-aliasing -DNDEBUG -g -fwrapv -O2 -Wall -Wstrict-prototypes -fPIC ${GCC_INCLUDES} -Ilib/ -c "${SWIG_OUT}/rpi_ws281x_wrap.c" -o "${SWIG_OUT}/rpi_ws281x_wrap.o"


# Compile wrapper into shared lib
echo "Compiling wrapper into shared library..."
gcc -pthread -shared -Wl,-O1 -Wl,-Bsymbolic-functions -Wl,-z,relro "${SWIG_OUT}/rpi_ws281x_wrap.o" -L${NATIVE_OUT}/ -lws281x -o "${OUTPUT}/nativeLib/${WRAPPER_LIB_NAME}"

echo "Cleaning up intermediate items..."
rm -rf ${SWIG_OUT} ${NATIVE_SRC} ${NATIVE_OUT}

echo "Done!"

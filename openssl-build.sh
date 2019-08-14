#!/bin/bash
set -e
set -x

# Set directory
SCRIPTPATH=`pwd`
export ANDROID_NDK_HOME=$ANDROID_NDK_ROOT
OPENSSL_DIR=/Users/algavris/Downloads/openssl-1.1.1c

# Find the toolchain for your build machine
toolchains_path=$(python toolchains_path.py --ndk ${ANDROID_NDK_ROOT})

# Configure the OpenSSL environment, refer to NOTES.ANDROID in OPENSSL_DIR
# Set compiler clang, instead of gcc by default
CC=clang

# Add toolchains bin directory to PATH
PATH=$toolchains_path/bin:$PATH

# Set the Android API levels
ANDROID_API=14

# Set the target architecture
# Can be android-arm, android-arm64, android-x86, android-x86 etc
SSLARCH=android-arm
JNI_LIBS=armeabi-v7a

# Create the make file
cd ${OPENSSL_DIR}
./Configure ${SSLARCH} -D__ANDROID_API__=$ANDROID_API

# Build
make

# Copy the outputs
OUTPUT_INCLUDE=$SCRIPTPATH/app/src/main/jni/headers
OUTPUT_LIB=$SCRIPTPATH/app/src/main/jniLibs/$JNI_LIBS
mkdir -p $OUTPUT_INCLUDE
mkdir -p $OUTPUT_LIB
cp -RL include/openssl $OUTPUT_INCLUDE
cp libcrypto.so $OUTPUT_LIB
cp libcrypto.a $OUTPUT_LIB
cp libssl.so $OUTPUT_LIB
cp libssl.a $OUTPUT_LIB
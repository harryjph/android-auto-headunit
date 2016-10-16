cmake_minimum_required(VERSION 3.6.0 FATAL_ERROR)

include (CheckIncludeFiles)
include (CheckLibraryExists)

link_directories(src/main/jni/libs/arm)

add_library(hu_jni SHARED
 src/main/jni/hu_uti.c
 src/main/jni/hu_ssl.c
 src/main/jni/hu_jni.c
)

target_link_libraries(hu_jni log)
target_link_libraries(hu_jni libssl.so)
target_link_libraries(hu_jni libcrypto.so)


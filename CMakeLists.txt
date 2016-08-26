cmake_minimum_required(VERSION 3.6.0 FATAL_ERROR)

include (CheckIncludeFiles)
include (CheckLibraryExists)

include_directories(src/main/jni/libs/headers/libusb)
link_directories(src/main/jni/libs/arm)

CHECK_INCLUDE_FILES(libusb.h HAVE_LIBUSB_H)

if (!HAVE_LIBUSB_H)
  MESSAGE( ERROR "HAVE_LIBUSB_H: NO" )
endif ()

CHECK_LIBRARY_EXISTS(libusbnok.so libusb_bulk_transfer "" HAVE_LIBUSBNOK)

if (!HAVE_LIBUSBNOK)
  MESSAGE( ERROR "HAVE_LIBUSBNOK: NO" )
endif ()

add_library(hu_jni SHARED
 src/main/jni/hu_aad.c
 src/main/jni/hu_uti.c
 src/main/jni/hu_ssl.c
 src/main/jni/hu_usb.c
 src/main/jni/hu_tcp.c
 src/main/jni/hu_aap.c
 src/main/jni/hu_jni.c
)

target_link_libraries(hu_jni log)
target_link_libraries(hu_jni libusbnok.a)
target_link_libraries(hu_jni libssl.so)
target_link_libraries(hu_jni libcrypto.so)


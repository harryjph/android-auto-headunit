
#undef NDEBUG // Ensure debug stuff

#define hu_STATE_INITIAL   0
#define hu_STATE_STARTIN   1
#define hu_STATE_STARTED   2
#define hu_STATE_STOPPIN   3
#define hu_STATE_STOPPED   4

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>

#include <string.h>
#include <signal.h>

#include <pthread.h>

#include <errno.h>

#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>

#include <dirent.h>                                                   // For opendir (), readdir (), closedir (), DIR, struct dirent.

#define byte unsigned char
//This is the value used in DHU // Default buffer size is maximum for USB
#define DEFBUF  131080

#define CHAR_BUF 512                                                   // For Ascii strings and such

#ifdef __ANDROID_API__
#include <android/log.h>
#else
// UNKNOWN    0
#define ANDROID_LOG_DEFAULT 1
#define ANDROID_LOG_VERBOSE 2
#define ANDROID_LOG_DEBUG   3
// INFO       4
#define ANDROID_LOG_WARN    5
#define ANDROID_LOG_ERROR   6
// FATAL      7
// SILENT     8
#endif

#ifdef NDEBUG

#define  logv(...)
#define  logd(...)
#define  logw(...)
#define  loge(...)

#else

#define  logv(...)  hu_log(ANDROID_LOG_VERBOSE,__VA_ARGS__)
#define  logd(...)  hu_log(ANDROID_LOG_DEBUG,__VA_ARGS__)
#define  logw(...)  hu_log(ANDROID_LOG_WARN,__VA_ARGS__)
#define  loge(...)  hu_log(ANDROID_LOG_ERROR,__VA_ARGS__)

#endif

int hu_log(int priority, const char *fmt, ...);

unsigned long ms_get();

unsigned long ms_sleep(unsigned long ms);

void hex_dump(char *prefix, int width, unsigned char *buf, int len);

char *vid_write_tail_buf_get(int len);

char *vid_read_head_buf_get(int *len);

char *aud_write_tail_buf_get(int len);

char *aud_read_head_buf_get(int *len);

extern int vid_buf_buf_tail;    // Tail is next index for writer to write to.   If head = tail, there is no info.
extern int vid_buf_buf_head;    // Head is next index for reader to read from.
extern int aud_buf_buf_tail;    // Tail is next index for writer to write to.   If head = tail, there is no info.
extern int aud_buf_buf_head;    // Head is next index for reader to read from.

char *state_get(int state);
int sock_tmo_set(int fd, int tmo);
int file_get(const char *filename);
int sock_reuse_set(int fd);

#ifndef __ANDROID_API__
#define strlcpy   strncpy
#define strlcat   strncat
#endif

// Android USB device priority:

#define USB_VID_GOO 0x18D1    // The vendor ID should match Google's ID ( 0x18D1 ) and the product ID should be 0x2D00 or 0x2D01 if the device is already in accessory mode (case A).

#define USB_VID_HTC 0x0bb4
#define USB_VID_MOT 0x22b8

#define USB_VID_SAM 0x04e8
#define USB_VID_O1A 0xfff6  // Samsung ?

#define USB_VID_SON 0x0fce
#define USB_VID_LGE 0xfff5

#define USB_VID_LIN 0x1d6b
#define USB_VID_QUA 0x05c6





#define DEFBUF  131080

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

#define  logv(...)  hu_log(ANDROID_LOG_VERBOSE,__VA_ARGS__)
#define  logd(...)  hu_log(ANDROID_LOG_DEBUG,__VA_ARGS__)
#define  logw(...)  hu_log(ANDROID_LOG_WARN,__VA_ARGS__)
#define  loge(...)  hu_log(ANDROID_LOG_ERROR,__VA_ARGS__)

extern int ena_log_verbo;

int hu_log(int priority, const char *fmt, ...);

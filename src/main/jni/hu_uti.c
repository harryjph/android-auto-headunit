#include <stdarg.h>
#include <stdio.h>
#include "hu_uti.h"

// Log stuff:
int ena_log_verbo = 0;
int ena_log_debug = 1;
int ena_log_warni = 1;
int ena_log_error = 1;
int ena_log_hexdu = 1;

char *prio_get(int priority) {
    switch (priority) {
        case ANDROID_LOG_VERBOSE:
            return "V";
        case ANDROID_LOG_DEBUG:
            return "D";
        case ANDROID_LOG_WARN:
            return "W";
        case ANDROID_LOG_ERROR:
            return "E";
        default:
            return "?";
    }
}

int hu_log(int priority, const char *fmt, ...) {

    if (!ena_log_verbo && priority == ANDROID_LOG_VERBOSE)
        return -1;
    if (!ena_log_debug && priority == ANDROID_LOG_DEBUG)
        return -1;
    if (!ena_log_warni && priority == ANDROID_LOG_WARN)
        return -1;
    if (!ena_log_error && priority == ANDROID_LOG_ERROR)
        return -1;

    va_list ap;
    va_start (ap, fmt);
#ifdef __ANDROID_API__
    __android_log_vprint(priority, "CAR.HU.N", fmt, ap);
#else
    char log_line [4096] = {0};
    va_list aq;
    va_start (aq, fmt); 
    int len = vsnprintf (log_line, sizeof (log_line), fmt, aq);
    time_t timet = time (NULL);
    const time_t * timep = & timet;
    char asc_time [DEFBUF] = "";
    ctime_r (timep, asc_time);
    int len_time = strlen (asc_time);
    asc_time [len_time - 1] = 0;        // Remove trailing \n
    printf ("%s %s: %s:: %s\n", & asc_time [11], prio_get (priority), tag, log_line);
#endif

    return 0;
}
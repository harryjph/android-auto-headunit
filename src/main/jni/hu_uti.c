#include "hu_uti.h"

char *state_get(int state) {
    switch (state) {
        case hu_STATE_INITIAL:                                           // 0
            return ("hu_STATE_INITIAL");
        case hu_STATE_STARTIN:                                           // 1
            return ("hu_STATE_STARTIN");
        case hu_STATE_STARTED:                                           // 2
            return ("hu_STATE_STARTED");
        case hu_STATE_STOPPIN:                                           // 3
            return ("hu_STATE_STOPPIN");
        case hu_STATE_STOPPED:                                           // 4
            return ("hu_STATE_STOPPED");
    }
    return ("hu_STATE Unknown error");
}

// Log stuff:
int ena_log_verbo = 0;
int ena_log_debug = 1;
int ena_log_warni = 1;
int ena_log_error = 1;
int ena_log_hexdu = 1;

//1;    // Hex dump master enable
int max_hex_dump = 64;//32;

#ifdef  LOG_FILE
int logfd = -1;
void logfile (char * log_line) {
  if (logfd < 0)
    logfd = open ("/sdcard/hulog", O_RDWR | O_CREAT, S_IRWXU | S_IRWXG | S_IRWXO);
  int written = -77;
  if (logfd >= 0)
    written = write (logfd, log_line, strlen (log_line));
}
#endif

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
    __android_log_vprint(priority, "Headunit", fmt, ap);
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


#define MAX_ITOA_SIZE 32      // Int 2^32 need max 10 characters, 2^64 need 21

char *itoa(int val, char *ret, int radix) {
    if (radix == 10)
        snprintf(ret, MAX_ITOA_SIZE - 1, "%d", val);
    else if (radix == 16)
        snprintf(ret, MAX_ITOA_SIZE - 1, "%x", val);
    else
        loge ("radix != 10 && != 16: %d", radix);
    return (ret);
}

unsigned long ms_get() {                                                      // !!!! Affected by time jumps ?
    struct timespec tspec = {0, 0};
    int res = clock_gettime(CLOCK_MONOTONIC, &tspec);
    //logd ("sec: %ld  nsec: %ld", tspec.tv_sec, tspec.tv_nsec);

    unsigned long millisecs = (tspec.tv_nsec / 1000000L);
    millisecs += (tspec.tv_sec *
                  1000L);       // remaining 22 bits good for monotonic time up to 4 million seconds =~ 46 days. (?? - 10 bits for 1024 ??)

    return (millisecs);
}


#define quiet_ms_sleep  ms_sleep

unsigned long ms_sleep(unsigned long ms) {
    struct timespec tm;
    tm.tv_sec = 0;
    tm.tv_sec = ms / 1000L;
    tm.tv_nsec = (ms % 1000L) * 1000000L;

    unsigned long ms_end = ms_get() + ms;
    unsigned long ctr = 0;
    while (ms_get() < ms_end) {
        usleep(32000L);

        ctr++;

        if (ctr > 25) {
            ctr = 0L;
        }
    }
    return (ms);
}

int file_get(const char *filename) {                                // Return 1 if file, or directory, or device node etc. exists
    struct stat sb = {0};
    int ret = 0;                                                        // 0 = No file
    errno = 0;
    if (stat(filename, &sb) == 0) {                                   // If file exists...
        ret = 1;                                                          // 1 = File exists
//      logd ("file_get ret: %d  filename: %s", ret, filename);
    } else {
//      if (errno == ENOENT)                                              // 2
//        logd ("file_get ret: %d  filename: %s  errno ENOENT = No File/Dir", ret, filename);
//      else
//        loge ("file_get ret: %d  filename: %s  errno: %d (%s)", ret, filename, errno, strerror (errno));
    }
    return (ret);
}


#define HD_MW 256

void hex_dump(char *prefix, int width, unsigned char *buf, int len) {
    if (!ena_log_hexdu) {
        return;
    }
    //loge ("hex_dump prefix: \"%s\"  width: %d   buf: %p  len: %d", prefix, width, buf, len);

    if (buf == NULL || len <= 0)
        return;

    if (len > max_hex_dump)
        len = max_hex_dump;

    char tmp[3 * HD_MW + 8] = "";                                     // Handle line widths up to HD_MW
    char line[3 * HD_MW + 8] = "";
    if (width > HD_MW) {
        width = HD_MW;
    }
    int i, n;
    line[0] = 0;

    if (prefix) {
        //strlcpy (line, prefix, sizeof (line));
        strlcat(line, prefix, sizeof(line));
    }

    snprintf(tmp, sizeof(tmp), " %8.8x ", 0);
    strlcat(line, tmp, sizeof(line));

    for (i = 0, n = 1; i < len; i++, n++) {                           // i keeps incrementing, n gets reset to 0 each line
        snprintf(tmp, sizeof(tmp), "%2.2x ", buf[i]);
        strlcat(line, tmp, sizeof(line));                               // Append 2 bytes hex and space to line

        if (n == width) {                                                 // If at specified line width
            n = 0;                                                          // Reset position in line counter
            logd (line);                                                    // Log line

            line[0] = 0;
            if (prefix) {
                //strlcpy (line, prefix, sizeof (line));
                strlcat(line, prefix, sizeof(line));
            }

            //snprintf (tmp, sizeof (tmp), " %8.8x ", i + 1);
            snprintf(tmp, sizeof(tmp), "     %4.4x ", i + 1);
            strlcat(line, tmp, sizeof(line));
        } else if (i == len - 1) {                                            // Else if at last byte
            logd (line);                                                    // Log line
        }
    }
}

#include <netinet/in.h>
#include <netdb.h>

#ifndef SK_FORCE_REUSE
#define SK_FORCE_REUSE  2
#endif

#ifndef SO_REUSEPORT
#define SO_REUSEPORT 15
#endif

int sock_reuse_set(int fd) {
    errno = 0;
    int val = SK_FORCE_REUSE;//SK_CAN_REUSE;
    int ret = setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &val, sizeof(val));
    if (ret != 0)
        loge ("sock_tmo_set setsockopt SO_REUSEADDR errno: %d (%s)", errno, strerror(errno));
    else
        logd ("sock_tmo_set setsockopt SO_REUSEADDR Success");
    return (0);
}

int sock_tmo_set(int fd, int tmo) {                                 // tmo = timeout in milliseconds
    struct timeval tv = {0, 0};
    tv.tv_sec = tmo / 1000;                                               // Timeout in seconds
    tv.tv_usec = (tmo % 1000) * 1000;
    errno = 0;
    int ret = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, (struct timeval *) &tv,
                         sizeof(struct timeval));
    if (ret != 0)
        loge ("sock_tmo_set setsockopt SO_RCVTIMEO errno: %d (%s)", errno, strerror(errno));
    else
        logv ("sock_tmo_set setsockopt SO_RCVTIMEO Success");
    //errno = 0;
    //ret = setsockopt (fd, SOL_SOCKET, SO_SNDTIMEO, (struct timeval *) & tv, sizeof (struct timeval));
    //if (ret != 0) {
    //  loge ("timeout_set setsockopt SO_SNDTIMEO errno: %d (%s)", errno, strerror (errno));
    //}
    return (0);
}


int pid_get(char *cmd, int start_pid) {
    DIR *dp;
    struct dirent *dirp;
    FILE *fdc;
    struct stat sb;
    int pid = 0;
    int ret = 0;
    logd ("pid_get: %s  start_pid: %d", cmd, start_pid);

    errno = 0;
    if ((dp = opendir("/proc")) ==
        NULL) {                             // Open the /proc directory. If error...
        loge ("pid_get: opendir errno: %d (%s)", errno, strerror(errno));
        return (0);                                                       // Done w/ no process found
    }
    while ((dirp = readdir(dp)) != NULL) {                             // For all files/dirs in this directory... (Could terminate with errno set !!)
        //logd ("pid_get: readdir: %s", dirp->d_name);
        errno = 0;
        pid = atoi(dirp->d_name);                                        // pid = directory name string to integer
        if (pid <= 0) {                                                   // Ignore non-numeric directories
            //loge ("pid_get: not numeric ret: %d  errno: %d (%s)", pid, errno, strerror (errno));
            continue;
        }
        if (pid < start_pid) {                                            // Ignore PIDs we have already checked. Depends on directories in PID order which seems to always be true
            //loge ("pid_get: pid < start_pid");
            continue;
        }

        //logd ("pid_get: test pid: %d", pid);

        char fcmdline[CHAR_BUF] = "/proc/";
        strlcat(fcmdline, dirp->d_name, sizeof(fcmdline));
        errno = 0;
        ret = stat(fcmdline, &sb);                                      // Get file/dir status.
        if (ret == -1) {
            logd ("pid_get: stat errno: %d (%s)", errno,
                  strerror(errno)); // Common: pid_get: stat errno: 2 = ENOENT
            continue;
        }
        if (S_ISDIR (
                sb.st_mode)) {                                       // If this is a directory...
            //logd ("pid_get: dir %d", sb.st_mode);
            char cmdline[CHAR_BUF] = {0};
            strlcat(fcmdline, "/cmdline", sizeof(fcmdline));
            errno = 0;
            if ((fdc = fopen(fcmdline, "r")) == NULL) {                    // Open /proc/???/cmdline file read-only, If error...
                loge ("pid_get: fopen errno: %d (%s)", errno, strerror(errno));
                continue;
            }
            errno = 0;
            ret = fread(cmdline, sizeof(char), sizeof(cmdline) - 1, fdc);// Read
            if (ret < 0 || ret > sizeof(cmdline) - 1) {                    // If error...
                loge ("pid_get fread ret: %d  errno: %d (%s)", ret, errno, strerror(errno));
                fclose(fdc);
                continue;
            }
            cmdline[ret] = 0;
            fclose(fdc);
            int cmd_len = strlen(cmd);
            ret = strlen(
                    cmdline);                                         // The buffer includes a trailing 0, so adjust ret to actual string length (in case not always true) (Opts after !)
            //logd ("pid_get: cmdline bytes: %d  cmdline: %s", ret, cmdline);

            if (ret >=
                cmd_len) {                                           // Eg: ret = strlen ("/bin/a") = 6, cmd_len = strlen ("a") = 1, compare 1 at cmd_line[5]

                if (!strcmp(cmd,
                            &cmdline[ret - cmd_len])) {              // If a matching process name
                    logd ("pid_get: got pid: %d for cmdline: %s  start_pid: %d", pid, cmdline,
                          start_pid);
                    closedir(
                            dp);                                              // Close the directory.
                    return (pid);                                               // SUCCESS: Done w/ pid
                }
            }
        } else if (S_ISREG (sb.st_mode)) {                                  // If this is a regular file...
            loge ("pid_get: reg %d", sb.st_mode);
        } else {
            loge ("pid_get: unk %d", sb.st_mode);
        }
    }
    closedir(dp);                                                      // Close the directory.
    return (0);                                                         // Done w/ no PID found
}

// Buffers: Audio, Video, identical code, should generalize

#define aud_buf_BUFS_SIZE  65536 * 4      // Up to 256 Kbytes
#define NUM_aud_buf_BUFS   16            // Maximum of NUM_aud_buf_BUFS - 1 in progress; 1 is never used
int num_aud_buf_bufs = NUM_aud_buf_BUFS;

char aud_buf_bufs[NUM_aud_buf_BUFS][aud_buf_BUFS_SIZE];

int aud_buf_lens[NUM_aud_buf_BUFS] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

int aud_buf_buf_tail = 0;    // Tail is next index for writer to write to.   If head = tail, there is no info.
int aud_buf_buf_head = 0;    // Head is next index for reader to read from.

int aud_buf_errs = 0;
int aud_max_bufs = 0;
int aud_sem_tail = 0;
int aud_sem_head = 0;

char *aud_write_tail_buf_get(int len) {                          // Get tail buffer to write to

    if (len > aud_buf_BUFS_SIZE) {
        loge ("!!!!!!!!!! aud_write_tail_buf_get too big len: %d", len);   // E/aud_write_tail_buf_get(10699): !!!!!!!!!! aud_write_tail_buf_get too big len: 66338
        return (NULL);
    }

    int bufs = aud_buf_buf_tail - aud_buf_buf_head;
    if (bufs < 0)                                                       // If underflowed...
        bufs += num_aud_buf_bufs;                                         // Wrap
    //logd ("aud_write_tail_buf_get start bufs: %d  head: %d  tail: %d", bufs, aud_buf_buf_head, aud_buf_buf_tail);

    if (bufs >
        aud_max_bufs)                                            // If new maximum buffers in progress...
        aud_max_bufs = bufs;                                              // Save new max
    if (bufs >= num_aud_buf_bufs -
                1) {                                 // If room for another (max = NUM_aud_buf_BUFS - 1)
        loge ("aud_write_tail_buf_get out of aud_buf_bufs");
        aud_buf_errs++;
        //aud_buf_buf_tail = aud_buf_buf_head = 0;                        // Drop all buffers
        return (NULL);
    }

    int max_retries = 4;
    int retries = 0;
    for (retries = 0; retries < max_retries; retries++) {
        aud_sem_tail++;
        if (aud_sem_tail == 1)
            break;
        aud_sem_tail--;
        loge ("aud_sem_tail wait");
        ms_sleep(10);
    }
    if (retries >= max_retries) {
        loge ("aud_sem_tail could not be acquired");
        return (NULL);
    }

    if (aud_buf_buf_tail < 0 || aud_buf_buf_tail > num_aud_buf_bufs - 1)   // Protect
        aud_buf_buf_tail &= num_aud_buf_bufs - 1;

    aud_buf_buf_tail++;

    if (aud_buf_buf_tail < 0 || aud_buf_buf_tail > num_aud_buf_bufs - 1)
        aud_buf_buf_tail &= num_aud_buf_bufs - 1;

    char *ret = aud_buf_bufs[aud_buf_buf_tail];
    aud_buf_lens[aud_buf_buf_tail] = len;

    //logd ("aud_write_tail_buf_get done  ret: %p  bufs: %d  tail len: %d  head: %d  tail: %d", ret, bufs, len, aud_buf_buf_head, aud_buf_buf_tail);

    aud_sem_tail--;

    return (ret);
}

char *aud_read_head_buf_get(int *len) {                              // Get head buffer to read from

    if (len == NULL) {
        loge ("!!!!!!!!!! aud_read_head_buf_get");
        return (NULL);
    }
    *len = 0;

    int bufs = aud_buf_buf_tail - aud_buf_buf_head;
    if (bufs < 0)                                                       // If underflowed...
        bufs += num_aud_buf_bufs;                                          // Wrap
    //logd ("aud_read_head_buf_get start bufs: %d  head: %d  tail: %d", bufs, aud_buf_buf_head, aud_buf_buf_tail);

    if (bufs <= 0) {                                                    // If no buffers are ready...
        //logd ("aud_read_head_buf_get no aud_buf_bufs");
        //aud_buf_errs ++;  // Not an error; just no data
        //aud_buf_buf_tail = aud_buf_buf_head = 0;                          // Drop all buffers
        return (NULL);
    }

    int max_retries = 4;
    int retries = 0;
    for (retries = 0; retries < max_retries; retries++) {
        aud_sem_head++;
        if (aud_sem_head == 1)
            break;
        aud_sem_head--;
        loge ("aud_sem_head wait");
        ms_sleep(10);
    }
    if (retries >= max_retries) {
        loge ("aud_sem_head could not be acquired");
        return (NULL);
    }

    if (aud_buf_buf_head < 0 || aud_buf_buf_head > num_aud_buf_bufs - 1)   // Protect
        aud_buf_buf_head &= num_aud_buf_bufs - 1;

    aud_buf_buf_head++;

    if (aud_buf_buf_head < 0 || aud_buf_buf_head > num_aud_buf_bufs - 1)
        aud_buf_buf_head &= num_aud_buf_bufs - 1;

    char *ret = aud_buf_bufs[aud_buf_buf_head];
    *len = aud_buf_lens[aud_buf_buf_head];

    //logd ("aud_read_head_buf_get done  ret: %p  bufs: %d  head len: %d  head: %d  tail: %d", ret, bufs, * len, aud_buf_buf_head, aud_buf_buf_tail);

    aud_sem_head--;

    return (ret);
}


#define vid_buf_BUFS_SIZE 65536 * 4      // Up to 256 Kbytes
#define NUM_vid_buf_BUFS  16            // Maximum of NUM_vid_buf_BUFS - 1 in progress; 1 is never used

int num_vid_buf_bufs = NUM_vid_buf_BUFS;

char vid_buf_bufs[NUM_vid_buf_BUFS][vid_buf_BUFS_SIZE];

int vid_buf_lens[NUM_vid_buf_BUFS] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

int vid_buf_buf_tail = 0;    // Tail is next index for writer to write to.   If head = tail, there is no info.
int vid_buf_buf_head = 0;    // Head is next index for reader to read from.

int vid_buf_errs = 0;
int vid_max_bufs = 0;
int vid_sem_tail = 0;
int vid_sem_head = 0;

char *vid_write_tail_buf_get(int len) {                          // Get tail buffer to write to

    if (len > vid_buf_BUFS_SIZE) {
        loge ("!!!!!!!!!! vid_write_tail_buf_get too big len: %d", len);   // E/vid_write_tail_buf_get(10699): !!!!!!!!!! vid_write_tail_buf_get too big len: 66338
        return (NULL);
    }

    int bufs = vid_buf_buf_tail - vid_buf_buf_head;
    if (bufs < 0)                                                       // If underflowed...
        bufs += num_vid_buf_bufs;                                         // Wrap
    //logd ("vid_write_tail_buf_get start bufs: %d  head: %d  tail: %d", bufs, vid_buf_buf_head, vid_buf_buf_tail);

    if (bufs > vid_max_bufs)                                            // If new maximum buffers in progress...
        vid_max_bufs = bufs;                                              // Save new max
    if (bufs >= num_vid_buf_bufs - 1) {                                 // If room for another (max = NUM_vid_buf_BUFS - 1)
        loge ("vid_write_tail_buf_get out of vid_buf_bufs");
        vid_buf_errs++;
        //vid_buf_buf_tail = vid_buf_buf_head = 0;                        // Drop all buffers
        return (NULL);
    }

    int max_retries = 4;
    int retries = 0;
    for (retries = 0; retries < max_retries; retries++) {
        vid_sem_tail++;
        if (vid_sem_tail == 1)
            break;
        vid_sem_tail--;
        loge ("vid_sem_tail wait");
        ms_sleep(10);
    }
    if (retries >= max_retries) {
        loge ("vid_sem_tail could not be acquired");
        return (NULL);
    }

    if (vid_buf_buf_tail < 0 || vid_buf_buf_tail > num_vid_buf_bufs - 1)   // Protect
        vid_buf_buf_tail &= num_vid_buf_bufs - 1;

    vid_buf_buf_tail++;

    if (vid_buf_buf_tail < 0 || vid_buf_buf_tail > num_vid_buf_bufs - 1)
        vid_buf_buf_tail &= num_vid_buf_bufs - 1;

    char *ret = vid_buf_bufs[vid_buf_buf_tail];
    vid_buf_lens[vid_buf_buf_tail] = len;

    //logd ("vid_write_tail_buf_get done  ret: %p  bufs: %d  tail len: %d  head: %d  tail: %d", ret, bufs, len, vid_buf_buf_head, vid_buf_buf_tail);

    vid_sem_tail--;

    return (ret);
}

char *vid_read_head_buf_get(int *len) {                              // Get head buffer to read from

    if (len == NULL) {
        loge ("!!!!!!!!!! vid_read_head_buf_get");
        return (NULL);
    }
    *len = 0;

    int bufs = vid_buf_buf_tail - vid_buf_buf_head;
    if (bufs < 0)                                                       // If underflowed...
        bufs += num_vid_buf_bufs;                                          // Wrap
    //logd ("vid_read_head_buf_get start bufs: %d  head: %d  tail: %d", bufs, vid_buf_buf_head, vid_buf_buf_tail);

    if (bufs <= 0) {                                                    // If no buffers are ready...
        //logd ("vid_read_head_buf_get no vid_buf_bufs");
        //vid_buf_errs ++;  // Not an error; just no data
        //vid_buf_buf_tail = vid_buf_buf_head = 0;                          // Drop all buffers
        return (NULL);
    }

    int max_retries = 4;
    int retries = 0;
    for (retries = 0; retries < max_retries; retries++) {
        vid_sem_head++;
        if (vid_sem_head == 1)
            break;
        vid_sem_head--;
        loge ("vid_sem_head wait");
        ms_sleep(10);
    }
    if (retries >= max_retries) {
        loge ("vid_sem_head could not be acquired");
        return (NULL);
    }

    if (vid_buf_buf_head < 0 || vid_buf_buf_head > num_vid_buf_bufs - 1)   // Protect
        vid_buf_buf_head &= num_vid_buf_bufs - 1;

    vid_buf_buf_head++;

    if (vid_buf_buf_head < 0 || vid_buf_buf_head > num_vid_buf_bufs - 1)
        vid_buf_buf_head &= num_vid_buf_bufs - 1;

    char *ret = vid_buf_bufs[vid_buf_buf_head];
    *len = vid_buf_lens[vid_buf_buf_head];

    //logd ("vid_read_head_buf_get done  ret: %p  bufs: %d  head len: %d  head: %d  tail: %d", ret, bufs, * len, vid_buf_buf_head, vid_buf_buf_tail);

    vid_sem_head--;

    return (ret);
}

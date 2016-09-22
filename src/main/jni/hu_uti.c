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

int gen_server_loop_func(unsigned char *cmd_buf, int cmd_len, unsigned char *res_buf, int res_max);

int gen_server_poll_func(int poll_ms);

// Log stuff:

int ena_log_verbo = 0;
//1;
int ena_log_debug = 0;
int ena_log_warni = 1;
int ena_log_error = 1;

int ena_log_aap_send = 0;

// Enables for hex_dump:
int ena_hd_hu_aad_dmp = 1;        // Higher level
int ena_hd_tra_send = 0;        // Lower  level
int ena_hd_tra_recv = 0;

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

char *prio_get(int prio) {
    switch (prio) {
        case hu_LOG_VER:
            return ("V");
        case hu_LOG_DEB:
            return ("D");
        case hu_LOG_WAR:
            return ("W");
        case hu_LOG_ERR:
            return ("E");
    }
    return ("?");
}

int hu_log(int prio, const char *fmt, ...) {

    if (!ena_log_verbo && prio == hu_LOG_VER)
        return -1;
    if (!ena_log_debug && prio == hu_LOG_DEB)
        return -1;
    if (!ena_log_warni && prio == hu_LOG_WAR)
        return -1;
    if (!ena_log_error && prio == hu_LOG_ERR)
        return -1;

    va_list ap;
    va_start (ap, fmt);
#ifdef __ANDROID_API__
    __android_log_vprint(prio, "Headunit", fmt, ap);
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
    printf ("%s %s: %s:: %s\n", & asc_time [11], prio_get (prio), tag, log_line);
#endif

#ifdef  LOG_FILE
    char log_line [4096] = {0};
    va_list aq;
    va_start (aq, fmt); 
    int len = vsnprintf (log_line, sizeof (log_line), fmt, aq);
    strlcat (log_line, "\n", sizeof (log_line));
    logfile (log_line);
#endif
    return (0);
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
/*
    usleep (ms * 1000L);
    return (0);
//*/

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
    }
    else {
//      if (errno == ENOENT)                                              // 2
//        logd ("file_get ret: %d  filename: %s  errno ENOENT = No File/Dir", ret, filename);
//      else
//        loge ("file_get ret: %d  filename: %s  errno: %d (%s)", ret, filename, errno, strerror (errno));
    }
    return (ret);
}


#define HD_MW 256

void hex_dump(char *prefix, int width, unsigned char *buf, int len) {
    if (0)//! strncmp (prefix, "AUDIO: ", strlen ("AUDIO: ")))
        len = len;
    else if (!ena_log_hexdu) {
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

        char fcmdline[DEF_BUF] = "/proc/";
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
            char cmdline[DEF_BUF] = {0};
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

int kill_gentle_first = 1;

int pid_kill(int pid, int brutal, char *cmd_to_verify) {
    logd ("pid_kill pid: %d  brutal: %d", pid, brutal);
    int ret = 0;
    int sig = SIGTERM;
    if (brutal) {
        if (kill_gentle_first) {
            errno = 0;
            ret = kill(pid, sig);
            if (ret) {
                loge ("pid_kill kill_gentle_first kill() errno: %d (%s)", errno, strerror(errno));
            }
            else {
                logd ("pid_kill kill_gentle_first kill() success");
                errno = 0;
                int new_pid_check1 = pid_get(cmd_to_verify, pid);
                if (new_pid_check1 == pid) {
                    loge ("pid_kill kill() success detected but same new_pid_check: %d  errno: %d (%s)",
                          new_pid_check1, errno, strerror(errno));  // Fall through to brutal kill
                }
                else {
                    logd ("Full Success pid != new_pid_check1: %d  errno: %d (%s)", new_pid_check1,
                          errno, strerror(errno));
                    return (ret);
                }
            }
        }
        sig = SIGKILL;
    }
    errno = 0;
    ret = kill(pid, sig);
    if (ret) {
        loge ("pid_kill kill() errno: %d (%s)", errno, strerror(errno));
    }
    else {
        logd ("pid_kill kill() success");
        errno = 0;
        int new_pid_check2 = pid_get(cmd_to_verify, pid);
        if (new_pid_check2 == pid)
            loge ("pid_kill kill() success detected but same new_pid_check2: %d", new_pid_check2);
        else
            logd ("pid != new_pid_check: %d  errno: %d (%s)", new_pid_check2, errno,
                  strerror(errno));
    }
    return (ret);
}

// Buffers: Audio, Video, identical code, should generalize

#define aud_buf_BUFS_SIZE    65536 * 4      // Up to 256 Kbytes
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
        loge ("!!!!!!!!!! aud_write_tail_buf_get too big len: %d",
              len);   // E/aud_write_tail_buf_get(10699): !!!!!!!!!! aud_write_tail_buf_get too big len: 66338
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


#define vid_buf_BUFS_SIZE    65536 * 4      // Up to 256 Kbytes
#define   NUM_vid_buf_BUFS   16            // Maximum of NUM_vid_buf_BUFS - 1 in progress; 1 is never used

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
        loge ("!!!!!!!!!! vid_write_tail_buf_get too big len: %d",
              len);   // E/vid_write_tail_buf_get(10699): !!!!!!!!!! vid_write_tail_buf_get too big len: 66338
        return (NULL);
    }

    int bufs = vid_buf_buf_tail - vid_buf_buf_head;
    if (bufs < 0)                                                       // If underflowed...
        bufs += num_vid_buf_bufs;                                         // Wrap
    //logd ("vid_write_tail_buf_get start bufs: %d  head: %d  tail: %d", bufs, vid_buf_buf_head, vid_buf_buf_tail);

    if (bufs >
        vid_max_bufs)                                            // If new maximum buffers in progress...
        vid_max_bufs = bufs;                                              // Save new max
    if (bufs >= num_vid_buf_bufs -
                1) {                                 // If room for another (max = NUM_vid_buf_BUFS - 1)
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


//#endif  //#ifndef UTILS_INCLUDED


// Client/Server:

//#ifdef  CS_AF_UNIX                                                      // For Address Family UNIX sockets
//#include <sys/un.h>
//#else                                                                   // For Address Family NETWORK sockets

// Unix datagrams requires other write permission for /dev/socket, or somewhere else (ext, not FAT) writable.

//#define CS_AF_UNIX        // Use network sockets to avoid filesystem permission issues w/ Unix Domain Address Family sockets
#define CS_DGRAM            // Use datagrams, not streams/sessions

#ifdef  CS_AF_UNIX                                                      // For Address Family UNIX sockets
//#include <sys/un.h>
#define DEF_API_SRVSOCK    "/dev/socket/srv_spirit"
#define DEF_API_CLISOCK    "/dev/socket/cli_spirit"
char api_srvsock [DEF_BUF] = DEF_API_SRVSOCK;
char api_clisock [DEF_BUF] = DEF_API_CLISOCK;
#define CS_FAM   AF_UNIX

#else                                                                   // For Address Family NETWORK sockets
//#include <netinet/in.h>
//#include <netdb.h>
#define CS_FAM   AF_INET
#endif

#ifdef  CS_DGRAM
#define CS_SOCK_TYPE    SOCK_DGRAM
#else
#define CS_SOCK_TYPE    SOCK_STREAM
#endif

#define   RES_DATA_MAX  1280


#ifdef  GENERIC_CLIENT
#ifndef  GENERIC_CLIENT_INCLUDED
#define  GENERIC_CLIENT_INCLUDED

// Generic IPC API:

int gen_client_cmd (unsigned char * cmd_buf, int cmd_len, unsigned char * res_buf, int res_max, int net_port, int rx_tmo) {
logv ("net_port: %d  cmd_buf: \"%s\"  cmd_len: %d", net_port, cmd_buf, cmd_len);
static int sockfd = -1;
int res_len = 0, written = 0, ctr = 0;
static socklen_t srv_len = 0;
#ifdef  CS_AF_UNIX
static struct sockaddr_un  srv_addr;
#ifdef  CS_DGRAM
#define   CS_DGRAM_UNIX
  struct sockaddr_un  cli_addr;                                     // Unix datagram sockets must be bound; no ephemeral sockets.
  socklen_t cli_len = 0;
#endif
#else
//struct hostent *hp;
struct sockaddr_in  srv_addr, cli_addr;
socklen_t cli_len = 0;
#endif

if (sockfd < 0) {
  errno = 0;
  if ((sockfd = socket (CS_FAM, CS_SOCK_TYPE, 0)) < 0) {            // Get an ephemeral, unbound socket
    loge ("gen_client_cmd: socket errno: %d (%s)", errno, strerror (errno));
    return (0);
  }
#ifdef  CS_DGRAM_UNIX                                               // Unix datagram sockets must be bound; no ephemeral sockets.
  strlcpy (api_clisock, DEF_API_CLISOCK, sizeof (api_clisock));
  char itoa_ret [MAX_ITOA_SIZE] = {0};
  strlcat (api_clisock, itoa (net_port, itoa_ret, 10), sizeof (api_clisock));
  unlink (api_clisock);                                             // Remove any lingering client socket
  memset ((char *) & cli_addr, sizeof (cli_addr), 0);
  cli_addr.sun_family = AF_UNIX;
  strlcpy (cli_addr.sun_path, api_clisock, sizeof (cli_addr.sun_path));
  cli_len = strlen (cli_addr.sun_path) + sizeof (cli_addr.sun_family);

  errno = 0;
  if (bind (sockfd, (struct sockaddr *) & cli_addr, cli_len) < 0) {
    loge ("gen_client_cmd: bind errno: %d (%s)", errno, strerror (errno));
    close (sockfd);
    sockfd = -1;
    return (0);                                                     // OK to continue w/ Internet Stream but since this is Unix Datagram and we ran unlink (), let's fail
  }
#endif
}
//!! Can move inside above
// Setup server address
memset ((char *) & srv_addr, sizeof (srv_addr), 0);
#ifdef  CS_AF_UNIX
strlcpy (api_srvsock, DEF_API_SRVSOCK, sizeof (api_srvsock));
char itoa_ret [MAX_ITOA_SIZE] = {0};
strlcat (api_srvsock, itoa (net_port, itoa_ret, 10), sizeof (api_srvsock));
srv_addr.sun_family = AF_UNIX;
strlcpy (srv_addr.sun_path, api_srvsock, sizeof (srv_addr.sun_path));
srv_len = strlen (srv_addr.sun_path) + sizeof (srv_addr.sun_family);
#else
srv_addr.sin_family = AF_INET;
srv_addr.sin_addr.s_addr = htonl (INADDR_LOOPBACK);
//errno = 0;
//hp = gethostbyname ("localhost");
//if (hp == 0) {
//  loge ("gen_client_cmd: Error gethostbyname  errno: %d (%s)", errno, strerror (errno));
//  return (0);
//}
//bcopy ((char *) hp->h_addr, (char *) & srv_addr.sin_addr, hp->h_length);
srv_addr.sin_port = htons (net_port);
srv_len = sizeof (struct sockaddr_in);
#endif


// Send cmd_buf and get res_buf
#ifdef CS_DGRAM
errno = 0;
written = sendto (sockfd, cmd_buf, cmd_len, 0, (const struct sockaddr *) & srv_addr, srv_len);
if (written != cmd_len) {                                           // Dgram buffers should not be segmented
  loge ("gen_client_cmd: sendto errno: %d (%s)", errno, strerror (errno));
#ifdef  CS_DGRAM_UNIX
  unlink (api_clisock);
#endif
  close (sockfd);
  sockfd = -1;
  return (0);
}

sock_tmo_set (sockfd, rx_tmo);

res_len = -1;
ctr = 0;
while (res_len < 0 && ctr < 2) {
  errno = 0;
  res_len = recvfrom (sockfd, res_buf, res_max, 0, (struct sockaddr *) & srv_addr, & srv_len);
  ctr ++;
  if (res_len < 0 && ctr < 2) {
    if (errno == EAGAIN)
      logw ("gen_client_cmd: recvfrom errno: %d (%s)", errno, strerror (errno));           // Occasionally get EAGAIN here
    else
      loge ("gen_client_cmd: recvfrom errno: %d (%s)", errno, strerror (errno));
  }
}
if (res_len <= 0) {
  loge ("gen_client_cmd: recvfrom errno: %d (%s)", errno, strerror (errno));
#ifdef  CS_DGRAM_UNIX
  unlink (api_clisock);
#endif
  close (sockfd);
  sockfd = -1;
  return (0);
}
#ifndef CS_AF_UNIX
// !!   ?? Don't need this ?? If srv_addr still set from sendto, should restrict recvfrom to localhost anyway ?
if ( srv_addr.sin_addr.s_addr != htonl (INADDR_LOOPBACK) ) {
  loge ("gen_client_cmd: Unexpected suspicious packet from host");// %s", inet_ntop(srv_addr.sin_addr.s_addr)); //inet_ntoa(srv_addr.sin_addr.s_addr));
}
#endif
#else
errno = 0;
if (connect (sockfd, (struct sockaddr *) & srv_addr, srv_len) < 0) {
  loge ("gen_client_cmd: connect errno: %d (%s)", errno, strerror (errno));
  close (sockfd);
  sockfd = -1;
  return (0);
}
errno = 0;
written = write (sockfd, cmd_buf, cmd_len);                           // Write the command packet
if (written != cmd_len) {                                             // Small buffers under 256 bytes should not be segmented ?
  loge ("gen_client_cmd: write errno: %d (%s)", errno, strerror (errno));
  close (sockfd);
  sockfd = -1;
  return (0);
}

sock_tmo_set (sockfd, rx_tmo);

errno = 0;
res_len = read (sockfd, res_buf, res_max)); // Read response
if (res_len <= 0) {
  loge ("gen_client_cmd: read errno: %d (%s)", errno, strerror (errno));
  close (sockfd);
  sockfd = -1;
  return (0);
}
#endif
//hex_dump ("", 32, res_buf, n);
#ifdef  CS_DGRAM_UNIX
  unlink (api_clisock);
#endif
//close (sockfd);
return (res_len);
}

#endif      //#ifndef GENERIC_CLIENT_INCLUDED
#endif      //#ifdef  GENERIC_CLIENT


#ifdef  GENERIC_SERVER
#ifndef  GENERIC_SERVER_INCLUDED
#define  GENERIC_SERVER_INCLUDED

int gen_server_exiting = 0;

int gen_server_loop (int net_port, int poll_ms) {                     // Run until gen_server_exiting != 0, passing incoming commands to gen_server_loop_func() and responding with the results
  int sockfd = -1, newsockfd = -1, cmd_len = 0, ctr = 0;
  socklen_t cli_len = 0, srv_len = 0;
#ifdef  CS_AF_UNIX
  struct sockaddr_un  cli_addr = {0}, srv_addr = {0};
  srv_len = strlen (srv_addr.sun_path) + sizeof (srv_addr.sun_family);
#else
  struct sockaddr_in  cli_addr = {0}, srv_addr = {0};
  //struct hostent *hp;
#endif
  unsigned char cmd_buf [DEF_BUF] ={0};

#ifdef  CS_AF_UNIX
  strlcpy (api_srvsock, DEF_API_SRVSOCK, sizeof (api_srvsock));
  char itoa_ret [MAX_ITOA_SIZE] = {0};
  strlcat (api_srvsock, itoa (net_port, itoa_ret, 10), sizeof (api_srvsock));
  unlink (api_srvsock);
#endif
  errno = 0;
  if ((sockfd = socket (CS_FAM, CS_SOCK_TYPE, 0)) < 0) {              // Create socket
    loge ("gen_server_loop socket  errno: %d (%s)", errno, strerror (errno));
    return (-1);
  }

  sock_reuse_set (sockfd);

  if (poll_ms != 0)
    sock_tmo_set (sockfd, poll_ms);                                   // If polling mode, set socket timeout for polling every poll_ms milliseconds

  memset ((char *) & srv_addr, sizeof (srv_addr), 0);
#ifdef  CS_AF_UNIX
  srv_addr.sun_family = AF_UNIX;
  strlcpy (srv_addr.sun_path, api_srvsock, sizeof (srv_addr.sun_path));
  srv_len = strlen (srv_addr.sun_path) + sizeof (srv_addr.sun_family);
#else
  srv_addr.sin_family = AF_INET;
  srv_addr.sin_addr.s_addr = htonl (INADDR_LOOPBACK);                 // Will bind to loopback instead of common INADDR_ANY. Packets should only be received by loopback and never Internet.
                                                                      // For 2nd line of defence see: loge ("Unexpected suspicious packet from host");
  //errno = 0;
  //hp = gethostbyname ("localhost");
  //if (hp == 0) {
  //  loge ("Error gethostbyname  errno: %d (%s)", errno, strerror (errno));
  //  return (-2);
  //}
  //bcopy ((char *) hp->h_addr, (char *) & srv_addr.sin_addr, hp->h_length);
  srv_addr.sin_port = htons (net_port);
  srv_len = sizeof (struct sockaddr_in);
#endif

#ifdef  CS_AF_UNIX
logd ("srv_len: %d  fam: %d  path: %s", srv_len, srv_addr.sun_family, srv_addr.sun_path);
#else
logd ("srv_len: %d  fam: %d  addr: 0x%x  port: %d", srv_len, srv_addr.sin_family, ntohl (srv_addr.sin_addr.s_addr), ntohs (srv_addr.sin_port));
#endif
  errno = 0;
  if (bind (sockfd, (struct sockaddr *) & srv_addr, srv_len) < 0) {   // Bind socket to server address
    loge ("Error bind  errno: %d (%s)", errno, strerror (errno));
#ifdef  CS_AF_UNIX
    return (-3);
#endif
#ifdef CS_DGRAM
    return (-3);
#endif
    loge ("Inet stream continuing despite bind error");               // OK to continue w/ Internet Stream
  }

  // Done after socket() and before bind() so don't repeat it here ?
  //if (poll_ms != 0)
  //  sock_tmo_set (sockfd, poll_ms);                                   // If polling mode, set socket timeout for polling every poll_ms milliseconds

// Get command from client
#ifndef CS_DGRAM
  errno = 0;
  if (listen (sockfd, 5)) {                                           // Backlog= 5; likely don't need this
    loge ("Error listen  errno: %d (%s)", errno, strerror (errno));
    return (-4);
  }
#endif

  logd ("gen_server_loop Ready");

  while (! gen_server_exiting) {
    memset ((char *) & cli_addr, sizeof (cli_addr), 0);               // ?? Don't need this ?
    //cli_addr.sun_family = CS_FAM;                                   // ""
    cli_len = sizeof (cli_addr);

    //logd ("ms_get: %d",ms_get ());
#ifdef  CS_DGRAM
    errno = 0;
    cmd_len = recvfrom (sockfd, cmd_buf, sizeof (cmd_buf), 0, (struct sockaddr *) & cli_addr, & cli_len);
    if (cmd_len <= 0) {
      if (errno == EAGAIN) {
        if (poll_ms != 0)                                             // If timeout polling is enabled...
          gen_server_poll_func (poll_ms);                             // Do the polling work
        else
          loge ("gen_server_loop EAGAIN !!!");                        // Else EGAIN is an unexpected error for blocking mode
      }
      else {                                                          // Else if some other error, sleep it off for 100 ms
        if (errno == EINTR)
          logw ("Error recvfrom errno: %d (%s)", errno, strerror (errno));
        else
          loge ("Error recvfrom errno: %d (%s)", errno, strerror (errno));
        quiet_ms_sleep (101);
      }
      continue;
    }
#ifndef CS_AF_UNIX
// !!
    if ( cli_addr.sin_addr.s_addr != htonl (INADDR_LOOPBACK) ) {
      //loge ("Unexpected suspicious packet from host %s", inet_ntop (cli_addr.sin_addr.s_addr));
      loge ("Unexpected suspicious packet from host");// %s", inet_ntoa (cli_addr.sin_addr.s_addr));
    }
#endif
#else
    errno = 0;
    newsockfd = accept (sockfd, (struct sockaddr *) & cli_addr, & cli_len);
    if (newsockfd < 0) {
      loge ("Error accept  errno: %d (%s)", errno, strerror (errno));
      ms_sleep (101);                                                 // Sleep 0.1 second to try to clear errors
      continue;
    }
#ifndef  CS_AF_UNIX
// !!
    if ( cli_addr.sin_addr.s_addr != htonl (INADDR_LOOPBACK) ) {
      //loge ("Unexpected suspicious packet from host %s", inet_ntop (cli_addr.sin_addr.s_addr));
      loge ("Unexpected suspicious packet from host");// %s", inet_ntoa (cli_addr.sin_addr.s_addr));
    }
#endif
    errno = 0;
    cmd_len = read (newsockfd, cmd_buf, sizeof (cmd_buf));
    if (cmd_len <= 0) {
      loge ("Error read  errno: %d (%s)", errno, strerror (errno));
      ms_sleep (101);                                                 // Sleep 0.1 second to try to clear errors
      close (newsockfd);
      ms_sleep (101);                                                 // Sleep 0.1 second to try to clear errors
      continue;
    }
#endif

#ifdef  CS_AF_UNIX
    //logd ("cli_len: %d  fam: %d  path: %s",cli_len,cli_addr.sun_family,cli_addr.sun_path);
#else
    //logd ("cli_len: %d  fam: %d  addr: 0x%x  port: %d",cli_len,cli_addr.sin_family, ntohl (cli_addr.sin_addr.s_addr), ntohs (cli_addr.sin_port));
#endif
    //hex_dump ("", 32, cmd_buf, n);

    unsigned char res_buf [RES_DATA_MAX] = {0};
    int res_len = 0;

    cmd_buf [cmd_len] = 0;                                            // Null terminate for string usage
                                                                      // Do server command function and provide response
    res_len = gen_server_loop_func ( cmd_buf, cmd_len, res_buf, sizeof (res_buf));

    if (ena_log_verb)
      logd ("gen_server_loop gen_server_loop_func res_len: %d", res_len);

    if (res_len < 0) {                                                // If error
      res_len = 2;
      res_buf [0] = 0xff;                                             // '?';   ?? 0xff for HCI ?
      res_buf [1] = 0xff;                                             // '\n';
      res_buf [2] = 0;
    }
    //hex_dump ("", 32, res_buf, res_len);


// Send response
#ifdef  CS_DGRAM
    errno = 0;
    if (sendto (sockfd, res_buf, res_len, 0, (struct sockaddr *) & cli_addr, cli_len) != res_len) {
      loge ("Error sendto  errno: %d (%s)  res_len: %d", errno, strerror (errno), res_len);
      ms_sleep (101);                                                 // Sleep 0.1 second to try to clear errors
    }
#else
    errno = 0;
    if (write (newsockfd, res_buf, res_len) != res_len) {             // Write, if can't write full buffer...
      loge ("Error write  errno: %d (%s)", errno, strerror (errno));
      ms_sleep (101);                                                 // Sleep 0.1 second to try to clear errors
    }
    close (newsockfd);
#endif
  }
  close (sockfd);
#ifdef  CS_AF_UNIX
  unlink (api_srvsock);
#endif

  return (0);
}

#endif      //#ifndef GENERIC_SERVER_INCLUDED
#endif      //#ifdef  GENERIC_SERVER



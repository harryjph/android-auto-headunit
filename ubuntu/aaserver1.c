#include <microhttpd.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <glib.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <signal.h>

#define PORT 54323

#define READ 0
#define WRITE 1


pid_t popen2(const char **command, int *infp, int *outfp)
{
    int p_stdin[2], p_stdout[2];
    pid_t pid;

    if (pipe(p_stdin) != 0 || pipe(p_stdout) != 0)
        return -1;

    pid = fork();

    if (pid < 0)
        return pid;
    else if (pid == 0)
    {
        close(p_stdin[WRITE]);
        dup2(p_stdin[READ], READ);
        close(p_stdout[READ]);
        dup2(p_stdout[WRITE], WRITE);

        execvp((const char *)*command, command);
        perror("execvp");
        exit(1);
    }

    if (infp == NULL)
        close(p_stdin[WRITE]);
    else
        *infp = p_stdin[WRITE];

    if (outfp == NULL)
        close(p_stdout[READ]);
    else
        *outfp = p_stdout[READ];

    return pid;
}

int app_started = 0;

const char *parseStart = "parseResponse({\"Status\": \"";
const char *parseEnd = "\"});";
char *parseStatus;
const char *parseTag = "STATUS:";
const char *defaultStatus = "parseResponse({\"Status\": \"Unknown status\"});";
const char *duplicatereq = "parseResponse({\"Status\": \"Android Auto started already\"});";

int parse_status(char *line_buf) {
	char *strTag = malloc(8);
	
	strncpy(strTag,line_buf,7);
		
	if (strncmp(strTag,parseTag,7) == 0) {
		int a = strnlen(parseStart,50);
		int b = strnlen(line_buf,107)-7;
		int c = strnlen(parseEnd,50);
		int msglen = a + b + c;
		parseStatus = malloc(msglen +1);
		char *statusStr = malloc(b+1);
		strncpy(statusStr,line_buf+7,b);
		strncpy(parseStatus,parseStart, a);
		strncat(parseStatus,statusStr,b);
		strncat(parseStatus,parseEnd,c);
		free(statusStr);	

		return 1;
	}
	
	return 0;
}

const char *unknownerror = "parseResponse({\"Status\": \"Unknown Error\"});";
 

static int answer_to_connection (void *cls, struct MHD_Connection *connection,
                      const char *url, const char *method,
                      const char *version, const char *upload_data,
                      size_t *upload_data_size, void **con_cls)
{
 
	struct MHD_Response *response;
	
	printf(" url %s \n", url);
	printf(" method %s \n", method);
	printf(" upload_data %s \n", upload_data);
	printf(" upload_data_size %d \n", upload_data_size);
	
	
	int ret;
	if (app_started == 1) {
		response = MHD_create_response_from_buffer (strlen (duplicatereq), (void *) duplicatereq, MHD_RESPMEM_MUST_COPY);
	} else {
		app_started = 1;
		
		pid_t hu_pid = -1;
		
		int hu_in_fp = -1;
		int hu_out_fp = -1;

		const char *command[] = {"headunit",NULL};
		
		int status = 0;
		
		hu_pid = popen2(command,&hu_in_fp, &hu_out_fp);
		
		waitpid(hu_pid, &ret, 0);
				
		if (WIFEXITED(ret)) {
			FILE * fp;
			char * line = NULL;
			size_t len = 0;
			ssize_t read;
			int status_found = 0;
			
			fp = fdopen(hu_out_fp,"r");
			
			printf("reading file\n");
			
			while (((read = getline(&line, &len, fp)) != -1)) {
				printf("%s/n",line);
					if (parse_status(line) == 1) {
//						free(line);
						status_found = 1;
					}
			} 
			

						
			if (status_found) { 
				response = MHD_create_response_from_buffer (strlen (parseStatus), (void *) parseStatus, MHD_RESPMEM_MUST_COPY);
				free(parseStatus);
			} else {
				response = MHD_create_response_from_buffer (strlen (defaultStatus), (void *) defaultStatus, MHD_RESPMEM_MUST_COPY);
			}			
 		}
 		
 		app_started = 0;
	}

	ret = MHD_queue_response (connection, MHD_HTTP_OK, response);

	MHD_destroy_response (response);

	return ret;
}

int main (int argc, char *argv[])
{

	struct MHD_Daemon *daemon;

//  daemon = MHD_start_daemon (MHD_USE_SELECT_INTERNALLY, PORT, NULL, NULL,
//                             &answer_to_connection, NULL, MHD_OPTION_END);
	daemon = MHD_start_daemon (MHD_USE_SELECT_INTERNALLY, PORT, NULL, NULL,
                             &answer_to_connection, NULL, MHD_OPTION_END);

                             
	if (NULL == daemon) {
		printf("Ended very fast.\n");
		return 1;
	}
	
	sleep(-1);

	MHD_stop_daemon (daemon);
	return 0;
}

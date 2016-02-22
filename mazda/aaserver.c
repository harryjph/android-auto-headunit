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


pid_t popen2(const char **command)
{
    pid_t pid;

    pid = fork();

    if (pid < 0)
        return pid;
    else if (pid == 0)
    {
        execvp((const char *)*command, command);
        perror("execvp");
        exit(1);
    }

    return pid;
}


int app_started = 0;
const char *success = "parseResponse({\"Status\": \"Press Back or Home button to close\"});";
const char *duplicatereq = "parseResponse({\"Status\": \"Android Auto is running already\"});";
const char *accessorymod = "parseResponse({\"Status\": \"Phone switched to accessory mode. Reconnect to enter AA mode\"});";
const char *notconnected = "parseResponse({\"Status\": \"Phone is not connected. Connect a supported phone and restart\"});";
const char *touchscreenerr = "parseResponse({\"Status\": \"Touch screen device error\"});";
const char *gstpipelinerrr = "parseResponse({\"Status\": \"gst_pipeline_init() error\"});";
const char *gstlooperr = "parseResponse({\"Status\": \"gst_loop() error\"});";
const char *huaaperr = "parseResponse({\"Status\": \"hu_aap_stop() error\"});";
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
		
		const char *command[] = {"headunit", NULL};
		
		int status = 0;
		
		hu_pid = popen2(command);
		
		waitpid(hu_pid, &ret, 0);
				
		if (WIFEXITED(ret)) {
			status = WEXITSTATUS(ret);
			
			printf("aaserver ret status : %d \n", status);
			const char *resptext;
			switch (status) {
				case 0: 
					resptext = success;
					break;
				case 255: 
					resptext = accessorymod;
					break;
				case 254: 
					resptext = notconnected;
					break;
				case 253: 
					resptext = touchscreenerr;
					break;										
				case 252: 
					resptext = gstpipelinerrr;
					break;
				case 251: 
					resptext = gstlooperr;
					break;
				case 250: 
					resptext = huaaperr;
					break;
				default: 
					resptext = unknownerror;
					break;
			}
			
			response = MHD_create_response_from_buffer (strlen (resptext), (void *) resptext, MHD_RESPMEM_MUST_COPY);
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
	daemon = MHD_start_daemon (MHD_USE_SELECT_INTERNALLY | MHD_USE_DEBUG , PORT, NULL, NULL,
								&answer_to_connection, NULL, MHD_OPTION_END);

                             
	if (NULL == daemon) {
		printf("http Daemon ended very quickly.\n");
		return 1;
	}
	
	sleep(-1);

	MHD_stop_daemon (daemon);
	return 0;
}

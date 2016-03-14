#include <glib.h>
#include <stdio.h>
#include <stdlib.h>
#include <gst/gst.h>
#include <gst/app/gstappsrc.h>
#include <linux/input.h>
#include <time.h>
#include <signal.h>
#include <errno.h>


#include "hu_uti.h"
#include "hu_aap.h"

#define EVENT_DEVICE    "/dev/input/filtered-touchscreen0"
#define EVENT_TYPE      EV_ABS
#define EVENT_CODE_X    ABS_X
#define EVENT_CODE_Y    ABS_Y

__asm__(".symver realpath1,realpath1@GLIBC_2.11.1");


typedef struct {
	GMainLoop *loop;
	GstPipeline *pipeline;
	GstAppSrc *src;
	GstElement *sink;
	GstElement *decoder;
	GstElement *queue;
	guint sourceid;
} gst_app_t;


static gst_app_t gst_app;

GstElement *mic_pipeline, *mic_sink;

pthread_mutex_t mutexsend;


typedef struct {
	int fd;
	int x;
	int y;
	uint8_t action;
	int action_recvd;
} mytouch;

mytouch mTouch = (mytouch){0,0,0,0,0};


struct cmd_arg_struct {
    int retry;
    int chan;
    int cmd_len; 
    unsigned char *cmd_buf; 
    int res_max; 
    unsigned char *res_buf;
    int result;
};

void *send_aa_cmd_thread(void *arguments)
{
    struct cmd_arg_struct *args = arguments;
    
	int ret = 0;

	pthread_mutex_lock (&mutexsend);

	ret = hu_aap_enc_send (args->retry, args->chan, args->cmd_buf, args->cmd_len);
	
	pthread_mutex_unlock (&mutexsend);
	
	
	if (ret < 0) {
		printf("send_aa_cmd_thread(): hu_aap_enc_send() failed with (%d)\n", ret);
	}

    args->result = ret;
        
    pthread_exit(NULL);
}

void *recv_video_thread(void *ret) {
	
	int *iret = (int *)ret;

	pthread_mutex_lock (&mutexsend);
	
	*iret = hu_aap_recv_process ();                       

	pthread_mutex_unlock (&mutexsend);
	
	pthread_exit(NULL);
}


int mic_change_state = 0;

static void read_mic_data (GstElement * sink);

int64_t getRunningTimeUs(gst_app_t *app)
{
    GstClock *clock = gst_element_get_clock((GstElement*)app->pipeline);
    if (clock && GST_IS_CLOCK(clock))
    {
        GstClockTime base_time = gst_element_get_base_time((GstElement*)app->pipeline);
        GstClockTime clock_time = gst_clock_get_time(clock);
        gst_object_unref(GST_OBJECT (clock));
        clock = NULL;
        return (int64_t)GST_TIME_AS_USECONDS(clock_time) - (int64_t)GST_TIME_AS_USECONDS(base_time);
    }
    else
    {
        return 0;
    }
}


static gboolean read_data(gst_app_t *app)
{
	GstBuffer *buffer;
	guint8 *ptr;
	GstFlowReturn ret;
	int iret;
	char *vbuf;
	int res_len = 0;

	pthread_t recv_thread;
	
	pthread_create(&recv_thread, NULL, &recv_video_thread, (void *)&iret);

	pthread_join(recv_thread, NULL);

	if (iret != 0) {
		printf("hu_aap_recv_process() iret: %d\n", iret);
		g_main_loop_quit(app->loop);		
		return FALSE;
	}

	/* Is there a video buffer queued? */
	vbuf = vid_read_head_buf_get (&res_len);

	if (vbuf != NULL) {
		ptr = (guint8 *)g_malloc(res_len);
		g_assert(ptr);
		memcpy(ptr, vbuf, res_len);
		
		buffer = gst_buffer_new_and_alloc(res_len);
		memcpy(GST_BUFFER_DATA(buffer),ptr,res_len);
		
		g_free(ptr);
		
		int64_t timestamp = getRunningTimeUs(app) * GST_USECOND; 
		GST_BUFFER_TIMESTAMP(buffer) = timestamp;

		ret = gst_app_src_push_buffer(app->src, buffer);

		if(ret !=  GST_FLOW_OK){
			printf("push buffer returned %d for %d bytes \n", ret, res_len);
			return FALSE;
		}
	}
	
	return TRUE;
}


static void * threadEntry(void *app)
{    
	int frameLoadCount = 0;

	while (read_data(app) == TRUE)
	{        
		frameLoadCount++;
	}

	return NULL;
}

pthread_t threadid;

static void start_feed (GstElement * pipeline, guint size, void *app)
{
    if (threadid == 0) {
//        g_print ("start feeding\n");        
        pthread_attr_t attr;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);
        if (pthread_create(&threadid, &attr, threadEntry, (void *)app))
            printf("Couldn't start thread. %s\n", strerror(errno));
        pthread_attr_destroy(&attr);
    
    }
}

static void stop_feed (GstElement * pipeline, void *app)
{
//	printf("stopping feed\n");
    if (threadid != 0) {
//        g_print ("stop feeding\n");
        
        pthread_join(threadid, NULL);
        
        threadid = 0;
    }
}


static gboolean bus_callback(GstBus *bus, GstMessage *message, gpointer *ptr)
{
	gst_app_t *app = (gst_app_t*)ptr;

	switch(GST_MESSAGE_TYPE(message)){

		case GST_MESSAGE_ERROR:{
					       gchar *debug;
					       GError *err;

					       gst_message_parse_error(message, &err, &debug);
					       g_print("Error %s\n", err->message);
					       g_error_free(err);
					       g_free(debug);
					       g_main_loop_quit(app->loop);
				       }
				       break;

		case GST_MESSAGE_WARNING:{
						 gchar *debug;
						 GError *err;
						 gchar *name;

						 gst_message_parse_warning(message, &err, &debug);
						 g_print("Warning %s\nDebug %s\n", err->message, debug);

						 name = (gchar *)GST_MESSAGE_SRC_NAME(message);

						 g_print("Name of src %s\n", name ? name : "nil");
						 g_error_free(err);
						 g_free(debug);
					 }
					 break;

		case GST_MESSAGE_EOS:
					 g_print("End of stream\n");
					 g_main_loop_quit(app->loop);
					 break;

		case GST_MESSAGE_STATE_CHANGED:
					 break;

		default:
//					 g_print("got message %s\n", \
							 gst_message_type_get_name (GST_MESSAGE_TYPE (message)));
					 break;
	}

	return TRUE;
}

static int gst_pipeline_init(gst_app_t *app)
{
	GstBus *bus;
	GstStateChangeReturn state_ret;
	
	GError *error = NULL;

	gst_init(NULL, NULL);

	app->pipeline = (GstPipeline*)gst_parse_launch("appsrc name=mysrc is-live=true block=false max-latency=1000000 ! h264parse ! vpudec low-latency=true framedrop=true framedrop-level-mask=0x200 ! mfw_v4lsink max-lateness=1000000000 sync=false async=false", &error);
	
//	app->pipeline = (GstPipeline*)gst_parse_launch("appsrc name=mysrc is-live=true block=false do-timestamp=true format=3 min-latency=0 max-latency=10000000 ! h264parse ! queue max-size-bytes=0 max-size-time=0 ! vpudec low-latency=true framedrop=true frame-plus=2 framedrop-level-mask=0x100 ! queue max-size-bytes=0 max-size-time=0 ! mfw_v4lsink async=false", &error);

//	app->pipeline = (GstPipeline*)gst_parse_launch("appsrc name=mysrc is-live=true do-timestamp=true ! h264parse ! vpudec low-latency=true framedrop=true ! queue max-size-bytes=0 max-size-time=0 !mfw_isink axis-left=0 axis-top=0 disp-width=800 disp-height=480 sync=false", &error);
	
	if (error != NULL) {
		printf("could not construct pipeline: %s\n", error->message);
		g_clear_error (&error);	
		return -1;
	}
	
	bus = gst_pipeline_get_bus(app->pipeline);
	gst_bus_add_watch(bus, (GstBusFunc)bus_callback, app);
	gst_object_unref(bus);

	app->src = (GstAppSrc*)gst_bin_get_by_name (GST_BIN (app->pipeline), "mysrc");
	
	gst_app_src_set_stream_type(app->src, GST_APP_STREAM_TYPE_STREAM);

	g_signal_connect(app->src, "need-data", G_CALLBACK(start_feed), app);
		
	g_signal_connect(app->src, "enough-data", G_CALLBACK(stop_feed), app);


	mic_pipeline = gst_parse_launch("alsasrc name=micsrc ! audioconvert ! audio/x-raw-int, signed=true, endianness=1234, depth=16, width=16, channels=1, rate=16000 ! queue !appsink name=micsink async=false emit-signals=true blocksize=8192",&error);
	
	if (error != NULL) {
		printf("could not construct mic pipeline: %s\n", error->message);
		g_clear_error (&error);	
		return -1;
	}
	
	mic_sink = gst_bin_get_by_name (GST_BIN (mic_pipeline), "micsink");

	g_object_set(G_OBJECT(mic_sink), "throttle-time", 3000000, NULL);
		
	g_signal_connect(mic_sink, "new-buffer", G_CALLBACK(read_mic_data), NULL);
	
	state_ret = gst_element_set_state (mic_pipeline, GST_STATE_READY);

	return 0;

}

/*
static int aa_cmd_send(int cmd_len, unsigned char *cmd_buf, int res_max, unsigned char *res_buf)
{
	int chan = cmd_buf[0];
	int res_len = 0;
	int ret = 0;

	ret = hu_aap_enc_send (0, chan, cmd_buf+4, cmd_len - 4);
	if (ret < 0) {
		printf("aa_cmd_send(): hu_aap_enc_send() failed with (%d)\n", ret);
		return ret;
	}
	
	return ret;

} */

static size_t uleb128_encode(uint64_t value, uint8_t *data)
{
	uint8_t cbyte;
	size_t enc_size = 0;

	do {
		cbyte = value & 0x7f;
		value >>= 7;
		if (value != 0)
			cbyte |= 0x80;
		data[enc_size++] = cbyte;
	} while (value != 0);

	return enc_size;
}

static size_t varint_encode (uint64_t val, uint8_t *ba, int idx) {
	
	if (val >= 0x7fffffffffffffff) {
		return 1;
	}

	uint64_t left = val;
	int idx2 = 0;
	
	for (idx2 = 0; idx2 < 9; idx2 ++) {
		ba [idx+idx2] = (uint8_t) (0x7f & left);
		left = left >> 7;
		if (left == 0) {
			return (idx2 + 1);
		}
		else if (idx2 < 9 - 1) {
			ba [idx+idx2] |= 0x80;
		}
	}
	
	return 9;
}

#define ACTION_DOWN	0
#define ACTION_UP	1
#define ACTION_MOVE	2
#define TS_MAX_REQ_SZ	32
static const uint8_t ts_header[] ={0x80, 0x01, 0x08};
static const uint8_t ts_sizes[] = {0x1a, 0x09, 0x0a, 0x03};
static const uint8_t ts_footer[] = {0x10, 0x00, 0x18};

static void aa_touch_event(uint8_t action, int x, int y) {
	struct timespec tp;
	uint8_t *buf;
	int idx;
	int siz_arr = 0;
	int size1_idx, size2_idx, i;
	int axis = 0;
	int coordinates[3] = {x, y, 0};

	buf = (uint8_t *)malloc(TS_MAX_REQ_SZ);
	if(!buf) {
		printf("Failed to allocate touchscreen event buffer\n");
		return;
	}

	/* Fetch the time stamp */
	clock_gettime(CLOCK_REALTIME, &tp);

	/* Copy header */
	memcpy(buf, ts_header, sizeof(ts_header));
//	idx = sizeof(ts_header) +
//	      uleb128_encode(tp.tv_nsec, buf + sizeof(ts_header));

	idx = sizeof(ts_header) +
	      varint_encode(tp.tv_sec * 1000000000 +tp.tv_nsec, buf + sizeof(ts_header),0);

	size1_idx = idx + 1;
	size2_idx = idx + 3;

	/* Copy sizes */
	memcpy(buf+idx, ts_sizes, sizeof(ts_sizes));
	idx += sizeof(ts_sizes);

	/* Set magnitude of each axis */
	for (i=0; i<3; i++) {
		axis += 0x08;
		buf[idx++] = axis;
		/* FIXME The following can be optimzed to update size1/2 at end of loop */
		siz_arr = uleb128_encode(coordinates[i], &buf[idx]);
		idx += siz_arr;
		buf[size1_idx] += siz_arr;
		buf[size2_idx] += siz_arr;
	}

	/* Copy footer */
	memcpy(buf+idx, ts_footer, sizeof(ts_footer));
	idx += sizeof(ts_footer);

	buf[idx++] = action;

	/* Send touch event */
//	aa_cmd_send (idx, buf, 0, NULL);

	pthread_t send_thread;
	struct cmd_arg_struct args;
		
	args.retry = 0;
	args.chan = AA_CH_TOU;
	args.cmd_len = idx; 
	args.cmd_buf = buf; 
	args.res_max = 0; 
	args.res_buf = NULL;

	pthread_create(&send_thread, NULL, &send_aa_cmd_thread, (void *)&args);

	pthread_join(send_thread, NULL);	
	
	free(buf);
}

static size_t uptime_encode(uint64_t value, uint8_t *data)
{

	int ctr = 0;
	for (ctr = 7; ctr >= 0; ctr --) {                           // Fill 8 bytes backwards
		data [6 + ctr] = (uint8_t)(value & 0xFF);
		value = value >> 8;
	}

	return 8;
}

static const uint8_t mic_header[] ={0x00, 0x00};
static const int max_size = 8192;


static void read_mic_data (GstElement * sink)
{		
	GstBuffer *gstbuf;
	
	g_signal_emit_by_name (sink, "pull-buffer", &gstbuf,NULL);
	
	if (gstbuf) {

		struct timespec tp;

		/* if mic is stopped, don't bother sending */	

		if (mic_change_state == 0) {
			printf("Mic stopped.. dropping buffers \n");
			gst_buffer_unref(gstbuf);
			return;
		}
		
		/* Fetch the time stamp */
		clock_gettime(CLOCK_REALTIME, &tp);
		
		gint mic_buf_sz;
		mic_buf_sz = GST_BUFFER_SIZE (gstbuf);
		
		int idx;
		
		if (mic_buf_sz <= 64) {
			printf("Mic data < 64 \n");
			return;
		}
		
		uint8_t *mic_buffer = (uint8_t *)malloc(14 + mic_buf_sz);
		
		/* Copy header */
		memcpy(mic_buffer, mic_header, sizeof(mic_header));
		
		idx = sizeof(mic_header) + uptime_encode(tp.tv_nsec * 0.001, mic_buffer);

		/* Copy PCM Audio Data */
		memcpy(mic_buffer+idx, GST_BUFFER_DATA(gstbuf), mic_buf_sz);
		idx += mic_buf_sz;
		
		
//		aa_cmd_send (idx, mic_buffer, 0, NULL);
		pthread_t send_thread;
		struct cmd_arg_struct args;
		
		args.retry = 1;
		args.chan = AA_CH_MIC;
		args.cmd_len = idx; 
		args.cmd_buf = mic_buffer; 
		args.res_max = 0; 
		args.res_buf = NULL;

		pthread_create(&send_thread, NULL, &send_aa_cmd_thread, (void *)&args);

		pthread_join(send_thread, NULL);
		
		gst_buffer_unref(gstbuf);
		free(mic_buffer);
	}
} 

int nightmode = 0;

gboolean touch_poll_event(gpointer data)
{
	int mic_ret = hu_aap_mic_get ();
	
	if (mic_change_state == 0 && mic_ret == 2) {
		printf("SHAI1 : Mic Started\n");
		mic_change_state = 2;
		gst_element_set_state (mic_pipeline, GST_STATE_PLAYING);
	}
		
	if (mic_change_state == 2 && mic_ret == 1) {
		printf("SHAI1 : Mic Stopped\n");
		mic_change_state = 0;
		gst_element_set_state (mic_pipeline, GST_STATE_READY);
	}	
	
	struct input_event event[64];
	const size_t ev_size = sizeof(struct input_event);
	const size_t buffer_size = ev_size * 64;
    ssize_t size;
    gst_app_t *app = (gst_app_t *)data;
	
	fd_set set;
	struct timeval timeout;
	int unblocked;

	FD_ZERO(&set);
	FD_SET(mTouch.fd, &set);

	timeout.tv_sec = 0;
	timeout.tv_usec = 10000;
	
	unblocked = select(mTouch.fd + 1, &set, NULL, NULL, &timeout);

	if (unblocked == -1) {
		printf("Error in read...\n");
		g_main_loop_quit(app->loop);
		return FALSE;
	}
	else if (unblocked == 0) {
			return TRUE;
	}
	
	size = read(mTouch.fd, &event, buffer_size);
	
	if (size == 0 || size == -1)
		return FALSE;
	
	if (size < ev_size) {
		printf("Error size when reading\n");
		g_main_loop_quit(app->loop);
		return FALSE;
	}
	
	int num_chars = size / ev_size;
	
	int i;
	for (i=0;i < num_chars;i++) {
		switch (event[i].type) {
			case EV_ABS:
				switch (event[i].code) {
					case ABS_MT_POSITION_X:
						mTouch.x = event[i].value * 800/4095;
						break;
					case ABS_MT_POSITION_Y:
						mTouch.y = event[i].value * 480/4095;
						break;
				}
				break;
			case EV_KEY:
				if (event[i].code == BTN_TOUCH) {
					mTouch.action_recvd = 1;
					if (event[i].value == 1) {
						mTouch.action = ACTION_DOWN;
					}
					else {
						mTouch.action = ACTION_UP;
					}
				}
				break;
			case EV_SYN:
				if (mTouch.action_recvd == 0) {
					mTouch.action = ACTION_MOVE;
					aa_touch_event(mTouch.action, mTouch.x, mTouch.y);
				} else {
					aa_touch_event(mTouch.action, mTouch.x, mTouch.y);
				//mTouch = (mytouch){0,0,0,0,0};
				}
				break;
		}
	} 

//  CHECK NIGHT MODE	
	time_t rawtime;
	struct tm *timenow;

	time( &rawtime );
	timenow = localtime( &rawtime );
	
	int nightmodenow = 1;
	
	if (timenow->tm_hour >= 6 && timenow->tm_hour <= 18)
		nightmodenow = 0;
	
	if (nightmode != nightmodenow) {
		nightmode = nightmodenow;
		byte rspds [] = {-128, 0x03, 0x52, 0x02, 0x08, 0x01}; 	// Day = 0, Night = 1 
		if (nightmode == 0)
			rspds[5]= 0x00;
		hu_aap_enc_send (0,AA_CH_SEN, rspds, sizeof (rspds)); 	// Send Sensor Night mode
	}
	
	return TRUE;
}


//COMMANDER
//UP:
uint8_t cd_up1[] = { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x13,0x10,0x01,0x18,0x00,0x20,0x00 };
uint8_t cd_up2[] = { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x13,0x10,0x00,0x18,0x00,0x20,0x00 };

//DOWN:
uint8_t cd_down1[] = { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x14,0x10,0x01,0x18,0x00,0x20,0x00 };
uint8_t cd_down2[] = { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x14,0x10,0x00,0x18,0x00,0x20,0x00 };


//LEFT:
uint8_t cd_left1[] = { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x15,0x10,0x01,0x18,0x00,0x20,0x00 };
uint8_t cd_left2[] = { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x15,0x10,0x00,0x18,0x00,0x20,0x00 };

//RIGHT
uint8_t cd_right1[] = { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x16,0x10,0x01,0x18,0x00,0x20,0x00 };
uint8_t cd_right2[] = { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x16,0x10,0x00,0x18,0x00,0x20,0x00 };

//LEFT turn
uint8_t cd_lefturn[] = { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x32,0x11,0x0A,0x0F,0x08,-128,-128,0x04,0x10,-1,-1,-1,-1,-1,-1,-1,-1,-1,0x01 };

//RIGHT turn
uint8_t cd_rightturn[] =  { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x32,0x08,0x0A,0x06,0x08,-128,-128,0x04,0x10,0x01 };

//BACK
uint8_t cd_back1[]  =  { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x04,0x10,0x01,0x18,0x00,0x20,0x00 };
uint8_t cd_back2[]  =  { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x04,0x10,0x00,0x18,0x00,0x20,0x00 };

//ENTER
uint8_t cd_enter1[] =  { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x17,0x10,0x01,0x18,0x00,0x20,0x00 };
uint8_t cd_enter2[] =  { -128,0x01,0x08,0,0,0,0,0,0,0,0,0x14,0x22,0x0A,0x0A,0x08,0x08,0x17,0x10,0x00,0x18,0x00,0x20,0x00 };


/*
gboolean commander_poll_event(gpointer data)
{
	
	struct input_event event[64];
	const size_t ev_size = sizeof(struct input_event);
	const size_t buffer_size = ev_size * 64;
    ssize_t size;
    gst_app_t *app = (gst_app_t *)data;
	
	fd_set set;
	struct timeval timeout;
	int unblocked;

	FD_ZERO(&set);
	FD_SET(mCommander.fd, &set);

	timeout.tv_sec = 0;
	timeout.tv_usec = 10000;
	
	unblocked = select(mCommander.fd + 1, &set, NULL, NULL, &timeout);

	if (unblocked == -1) {
		printf("Error in read...\n");
		g_main_loop_quit(app->loop);
		return FALSE;
	}
	else if (unblocked == 0) {
			return TRUE;
	}
	
	size = read(mCommander.fd, &event, buffer_size);
	
	if (size == 0 || size == -1)
		return FALSE;
	
	if (size < ev_size) {
		printf("Error size when reading\n");
		g_main_loop_quit(app->loop);
		return FALSE;
	}
	
	int num_chars = size / ev_size;
	
	int i;
	for (i=0;i < num_chars;i++) {
		switch (event[i].type) {
			case EV_ABS:
				switch (event[i].code) {
					case ABS_MT_POSITION_X:
						mTouch.x = event[i].value * 800/4095;
						break;
					case ABS_MT_POSITION_Y:
						mTouch.y = event[i].value * 480/4095;
						break;
				}
				break;
			case EV_KEY:
				if (event[i].code == BTN_TOUCH) {
					mTouch.action_recvd = 1;
					if (event[i].value == 1) {
						mTouch.action = ACTION_DOWN;
					}
					else {
						mTouch.action = ACTION_UP;
					}
				}
				break;
			case EV_SYN:
				if (mTouch.action_recvd == 0) {
					mTouch.action = ACTION_MOVE;
					aa_touch_event(mTouch.action, mTouch.x, mTouch.y);
				} else {
					aa_touch_event(mTouch.action, mTouch.x, mTouch.y);
				//mTouch = (mytouch){0,0,0,0,0};
				}
				break;
		}
	} 
	
	return TRUE;
}
*/


static void * input_thread(void *app) {
	
	while (touch_poll_event(app)) {		
		ms_sleep(100);
	}
}

GMainLoop *mainloop;


static int gst_loop(gst_app_t *app)
{
	int ret;
	GstStateChangeReturn state_ret;

	state_ret = gst_element_set_state((GstElement*)app->pipeline, GST_STATE_PLAYING);
//	g_warning("set state returned %d\n", state_ret);

	app->loop = g_main_loop_new (NULL, FALSE);
	
	mainloop = app->loop;
	
//	g_timeout_add_full(G_PRIORITY_HIGH, 100, touch_poll_event, (gpointer)app, NULL);

	printf("Starting Android Auto...\n");
  	g_main_loop_run (app->loop);

// TO-DO
	state_ret = gst_element_set_state((GstElement*)app->pipeline, GST_STATE_NULL);
//	g_warning("set state null returned %d\n", state_ret);

	gst_object_unref(app->pipeline);
	gst_object_unref(mic_pipeline);

	return ret;
}

static void signals_handler (int signum)
{
  if (mainloop && g_main_loop_is_running (mainloop))
    {
      g_main_loop_quit (mainloop);
    }
}


int main (int argc, char *argv[])
{	
	signal (SIGTERM, signals_handler);
	
	gst_app_t *app = &gst_app;
	int ret = 0;
	errno = 0;
	byte ep_in_addr  = -2;
	byte ep_out_addr = -2;
	
	/* Init gstreamer pipeline */
	ret = gst_pipeline_init(app);
	if (ret < 0) {
		printf("gst_pipeline_init() ret: %d\n", ret);
		return (-4);
	}

	/* Start AA processing */
	ret = hu_aap_start (ep_in_addr, ep_out_addr);
	if (ret < 0) {
		if (ret == -2)
			printf("Phone is not connected. Connect a supported phone and restart.\n");
		else if (ret == -1)
			printf("Phone switched to accessory mode. Restart to enter AA mode.\n");
		else
			printf("hu_app_start() ret: %d\n", ret);
		return (ret);
	}

	printf("SHAI1 : aap start.\n");
	
   /* Open Device */
   mTouch.fd = open(EVENT_DEVICE, O_RDONLY);
   
   if (mTouch.fd == -1) {
        fprintf(stderr, "%s is not a vaild device\n", EVENT_DEVICE);
        return -3;
    }
 
	pthread_t ts_thread;

	pthread_create(&ts_thread, NULL, &input_thread, (void *)app);
    
	/* Start gstreamer pipeline and main loop */
	ret = gst_loop(app);
	if (ret < 0) {
		printf("gst_loop() ret: %d\n", ret);
		ret = -5;
	}

	/* Stop AA processing */
	ret = hu_aap_stop ();
	if (ret < 0) {
		printf("hu_aap_stop() ret: %d\n", ret);
		ret = -6;
	}
		
	close(mTouch.fd);
	
	pthread_join(ts_thread, NULL);

	return (ret);
}

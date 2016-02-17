#include <glib.h>
#include <stdio.h>
#include <stdlib.h>
#include <gst/gst.h>
#include <gst/app/gstappsrc.h>
#include <linux/input.h>

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


typedef struct {
	int fd;
	int x;
	int y;
	uint8_t action;
	int action_recvd;
} mytouch;

mytouch mTouch = (mytouch){0,0,0,0,0};

static char nameBuff[] = "/data_persist/dev/mytmpfile-XXXXXX";
static char srtBuff[] = "\n1\n00:00:30,150 --> 00:00:45,850\nFREE APP! DO NOT PAY\n";
int srtfd = -1;

int mic_change_state = 0;

static GstFlowReturn read_mic_data (GstElement * sink);

static gboolean read_data(gst_app_t *app)
{
	GstBuffer *buffer;
	guint8 *ptr;
	GstFlowReturn ret;
	int iret;
	char *vbuf;
	int res_len = 0;

	iret = hu_aap_recv_process ();                                    // Process 1 message
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

		ret = gst_app_src_push_buffer(app->src, buffer);

		if(ret !=  GST_FLOW_OK){
			printf("push buffer returned %d for %d bytes \n", ret, res_len);
			return FALSE;
		}
	}

	return TRUE;
}



static void start_feed (GstElement * pipeline, guint size, gst_app_t *app)
{
	if (app->sourceid == 0) {
		printf("start feeding\n");
		app->sourceid = g_idle_add ((GSourceFunc) read_data, app);
	}
}

static void stop_feed (GstElement * pipeline, gst_app_t *app)
{
	if (app->sourceid != 0) {
		printf("stop feeding\n");
		g_source_remove (app->sourceid);
		app->sourceid = 0;
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

	app->pipeline = (GstPipeline*)gst_pipeline_new("mypipeline");
	bus = gst_pipeline_get_bus(app->pipeline);
	gst_bus_add_watch(bus, (GstBusFunc)bus_callback, app);
	gst_object_unref(bus);
	
	app->src = (GstAppSrc*)gst_element_factory_make("appsrc", "mysrc");
	app->sink = gst_element_factory_make("mfw_v4lsink", "myvsink");	
	app->decoder = gst_element_factory_make("vpudec", "mydecoder");
	app->queue = gst_element_factory_make("queue", "myqueue");
	app->sink = gst_element_factory_make("mfw_v4lsink", "myvsink");


	g_assert(app->src);
	g_assert(app->decoder);
	g_assert(app->queue);	
	g_assert(app->sink);

	g_object_set (G_OBJECT (app->src), "caps",
	gst_caps_new_simple ("video/x-h264",
				     "width", G_TYPE_INT, 800,
				     "height", G_TYPE_INT, 480,
				     "framerate", GST_TYPE_FRACTION, 30, 1,
				     NULL), NULL);

	
	g_object_set(G_OBJECT(app->src), "is-live", TRUE, "block", FALSE,"do-timestamp", TRUE, 
				  "format",GST_FORMAT_TIME,NULL);

//	g_object_set(G_OBJECT(app->src), "is-live", TRUE, "do-timestamp", TRUE, 
//				  "format",GST_FORMAT_TIME,NULL);

				  		
	g_object_set(G_OBJECT(app->decoder), "low-latency", TRUE, NULL);
	
	g_object_set(G_OBJECT(app->sink), "max-lateness", -1, NULL);

	g_signal_connect(app->src, "need-data", G_CALLBACK(start_feed), app);
	g_signal_connect(app->src, "enough-data", G_CALLBACK(stop_feed), app);

//	gst_bin_add_many(GST_BIN(app->pipeline), (GstElement*)app->src, app->decoder, app->queue, app->sink, NULL);
	gst_bin_add_many(GST_BIN(app->pipeline), (GstElement*)app->src, app->decoder, app->sink, NULL);

	if(!gst_element_link((GstElement*)app->src, app->decoder)){
		g_warning("failed to link src and decoder");
	}

	if(!gst_element_link(app->decoder, app->sink)){
		g_warning("failed to link decoder and sink");
	}
		
/*	if(!gst_element_link(app->decoder, app->queue)){
		g_warning("failed to link decoder and queue");
	}

	if(!gst_element_link(app->queue, app->sink)){
		g_warning("failed to link queue and sink");
	} */

	gst_app_src_set_stream_type(app->src, GST_APP_STREAM_TYPE_STREAM);
	
	mic_pipeline = gst_parse_launch("alsasrc name=micsrc ! audioconvert ! audio/x-raw-int, rate=16000, channels=1, width=16, depth=16, signed=true ! appsink name=micsink",&error);
	
	if (error != NULL) {
		printf("could not construct pipeline: %s\n", error->message);
		g_clear_error (&error);	
		return -1;
	}
	
	GstElement *mic_src = gst_bin_get_by_name (GST_BIN (mic_pipeline), "micsrc");
	
	g_object_set(G_OBJECT(mic_src), "do-timestamp", TRUE, NULL);
	
	mic_sink = gst_bin_get_by_name (GST_BIN (mic_pipeline), "micsink");
	
	g_object_set(G_OBJECT(mic_sink), "async", FALSE, "emit-signals", TRUE, NULL);
	
	g_signal_connect(mic_sink, "new-buffer", G_CALLBACK(read_mic_data), NULL);
	
	gst_element_set_state (mic_pipeline, GST_STATE_PAUSED);

	return 0;

}

static int aa_cmd_send(int cmd_len, unsigned char *cmd_buf, int res_max, unsigned char *res_buf)
{
	int chan = cmd_buf[0];
	int res_len = 0;
	int ret = 0;
	char *dq_buf;

/*	res_buf = (unsigned char *)malloc(res_max);
	if (!res_buf) {
		printf("TOTAL FAIL\n");
		return -1;
	} */

//	printf("chan: %d cmd_len: %d\n", chan, cmd_len);
	ret = hu_aap_enc_send (chan, cmd_buf+4, cmd_len - 4);
	if (ret < 0) {
		printf("aa_cmd_send(): hu_aap_enc_send() failed with (%d)\n", ret);
	//	free(res_buf);
		return ret;
	}
	
	return ret;

//	dq_buf = vid_read_head_buf_get(&res_len);
//	if (!dq_buf || res_len <= 0) {
	//	printf("No data dq_buf!\n");
//		free(res_buf);
//		return 0;
//	}
	
//	printf("dq_buf %s\n",dq_buf);
	
//	memcpy(res_buf, dq_buf, res_len);
	/* FIXME - we do nothing with this crap, probably check for ack and move along */

//	free(res_buf);

//    return res_len;
}

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

#define ACTION_DOWN	0
#define ACTION_UP	1
#define ACTION_MOVE	2
#define TS_MAX_REQ_SZ	32
static const uint8_t ts_header[] ={AA_CH_TOU, 0x0b, 0x03, 0x00, 0x80, 0x01, 0x08};
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
	idx = sizeof(ts_header) +
	      uleb128_encode(tp.tv_nsec, buf + sizeof(ts_header));
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
	aa_cmd_send (idx, buf, 0, NULL);

	free(buf);
}

static const uint8_t mic_header[] ={AA_CH_MIC, 0x0b, 0x00, 0x00, 0x00, 0x00};

static GstFlowReturn read_mic_data (GstElement * sink)
{
	GstBuffer *gstbuf;
	
	printf("SHAI1: inside read_mic_data.\n");

	gstbuf = (GstBuffer *) gst_app_sink_pull_buffer (sink);


	if (gstbuf) {

		if (mic_change_state == 0) {
			gst_buffer_unref(gstbuf);
			return GST_FLOW_OK;
		}
		
		struct timespec tp;
		uint8_t *buf;
		int idx;

		int mic_buf_sz = GST_BUFFER_SIZE(gstbuf);

		buf = (uint8_t *)malloc(14 + mic_buf_sz);
		if(!buf) {
			printf("Failed to allocate mic data buffer\n");
			return;
		}

		/* Fetch the time stamp */
		clock_gettime(CLOCK_REALTIME, &tp);

		/* Copy header */
		memcpy(buf, mic_header, sizeof(mic_header));
		idx = sizeof(mic_header) +
			  uleb128_encode(tp.tv_nsec, buf + sizeof(mic_header));

		/* Copy PCM Audio Data */
		memcpy(buf+idx, GST_BUFFER_DATA(gstbuf), mic_buf_sz);
		idx += sizeof(mic_buf_sz);

		/* Send Mic Audio */
		aa_cmd_send (idx, buf, 0, NULL);

		free(buf);
		
		gst_buffer_unref(gstbuf);
	}

	return GST_FLOW_OK;
}


gboolean input_poll_event(gpointer data)
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
	
	if (mic_change_state != 2)
		mic_change_state = hu_aap_mic_get ();
	
	if (mic_change_state == 2) {
		gst_element_set_state (mic_pipeline, GST_STATE_PLAYING);
	}
	
	if (mic_change_state == 1) {
		mic_change_state = 0;
		gst_element_set_state (mic_pipeline, GST_STATE_PAUSED);
	}
	
	return TRUE;
}

static int gst_loop(gst_app_t *app)
{
	int ret;
	GstStateChangeReturn state_ret;

	state_ret = gst_element_set_state((GstElement*)app->pipeline, GST_STATE_PLAYING);
//	g_warning("set state returned %d\n", state_ret);

	app->loop = g_main_loop_new (NULL, FALSE);
	g_timeout_add_full(G_PRIORITY_HIGH, 50, input_poll_event, (gpointer)app, NULL);

	printf("Starting Android Auto...\n");
  	g_main_loop_run (app->loop);


	state_ret = gst_element_set_state((GstElement*)app->pipeline, GST_STATE_NULL);
//	g_warning("set state null returned %d\n", state_ret);

	gst_object_unref(app->pipeline);
	gst_object_unref(mic_pipeline);

	return ret;
}


int main (int argc, char *argv[])
{
	gst_app_t *app = &gst_app;
	int ret = 0;
	errno = 0;
	byte ep_in_addr  = -1;
	byte ep_out_addr = -1;
	
	
	/* create temp SRT file */
	srtfd = mkstemp(nameBuff);
	unlink(nameBuff);
	
	if (srtfd < 1) {
		printf("\n Creation of temp file failed with error [%s]\n",strerror(errno));
		return (srtfd);
	}

	ret = write(srtfd,srtBuff,sizeof(srtBuff));

	if(ret == -1 )
    {
        printf("\n write failed with error [%s]\n",strerror(errno));
        return ret;
    }

	/* Init gstreamer pipelien */
	ret = gst_pipeline_init(app);
	if (ret < 0) {
		printf("gst_pipeline_init() ret: %d\n", ret);
		return (ret);
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
        return EXIT_FAILURE;
    }
    
    
	/* Start gstreamer pipeline and main loop */
	ret = gst_loop(app);
	if (ret < 0) {
		printf("gst_loop() ret: %d\n", ret);
	}

	/* Stop AA processing */
	ret = hu_aap_stop ();
	if (ret < 0) {
		printf("hu_aap_stop() ret: %d\n", ret);
		return (ret);
	}
		
	close(mTouch.fd);

	return (ret);
}

rm -rf aaserver.o
rm -rf aaserver
gcc -g -fPIC `sdl-config --libs` `pkg-config --cflags gstreamer-0.10 gstreamer-app-0.10 gstreamer-video-0.10 libcrypto libusb-1.0 openssl`-I../jni -c aaserver.c  -o aaserver.o
gcc  -g -o aaserver aaserver.o `pkg-config --libs gstreamer-0.10 gstreamer-app-0.10 gstreamer-video-0.10 libcrypto libusb-1.0 openssl `   -lrt -lmicrohttpd

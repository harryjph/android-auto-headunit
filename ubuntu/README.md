The following instructions only tested on ubuntu 14.04 x64

# How to run headunit
Install ffmpeg:

```
sudo add-apt-repository ppa:mc3man/gstffmpeg-keep
sudo apt-get update
sudo apt-get install gstreamer0.10-ffmpeg
```

Install SDL:

```
sudo apt-get install libsdl2-2.0-0 libsdl2-ttf-2.0-0 libportaudio2 libpng12-0
```

# How to compile headunit for Ubuntu
We need development copies of openssl, libusb, gstreamer, and sdl

```
sudo apt-get install libssl-dev libusb-1.0-0-dev libgstreamer-plugins-base0.10-dev libsdl1.2-dev
```

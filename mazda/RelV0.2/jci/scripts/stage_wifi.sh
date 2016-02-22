#!/bin/sh

# AAserver start
export LD_LIBRARY_PATH=/tmp/mnt/data_persist/dev/androidauto/custlib:/jci/lib:/jci/opera/3rdpartylibs/freetype:/usr/lib/imx-mm/audio-codec:/usr/lib/imx-mm/parser:/data_persist/dev/lib: 
aaserver &

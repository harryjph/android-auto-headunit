#!/bin/bash

protoc common.proto --javanano_out=../java/
protoc control.proto --javanano_out=../java/
protoc input.proto --javanano_out=../java/
protoc media.proto --javanano_out=../java/
protoc navigation.proto --javanano_out=../java/
protoc playback.proto --javanano_out=../java/
protoc sensors.proto --javanano_out=../java/

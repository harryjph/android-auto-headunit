#!/bin/bash

protoc common.proto --java_out=../java/
protoc control.proto --java_out=../java/
protoc input.proto --java_out=../java/
protoc media.proto --java_out=../java/
protoc navigation.proto --java_out=../java/
protoc playback.proto --java_out=../java/
protoc sensors.proto --java_out=../java/

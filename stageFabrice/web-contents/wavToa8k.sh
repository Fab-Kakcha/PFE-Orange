#!/bin/bash

if [ $# -eq 2 ]
then
sox -t wav $1.wav -t raw -r8000 -e a-law -b 8 -c 1 $1.a8k
sox -t wav $2.wav -t raw -r8000 -e a-law -b 8 -c 1 $2.a8k
else
echo "syntaxe: $0 audio_1 audio_2"
fi

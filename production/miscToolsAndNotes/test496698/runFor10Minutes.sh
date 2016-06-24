#!/usr/bin/env bash

max=10
count=1
echo -e "\n\tScript that runs for $max minutes: Loop $count of $max\n"
sleep 1m
count=$((count + 1))
if [[ $count >= $max ]]
then 
  exit 0
fi


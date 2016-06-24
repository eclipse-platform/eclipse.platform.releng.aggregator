#!/usr/bin/env bash

# wait 30 seconds, and start other script.
./runFor11Minutes.sh

max=10
count=1
echo -e "\tScript that runs for $max minutes: Loop $count of $max\n" 
while [[ $count -lt $max ]]
do
  sleep 1m
  count=$(($count + 1))
  echo -e "\n\n\tcount: $count of $max from ${0##*/}\n"
  echo -e "\n\tps:\n"
  ps -f
  echo -e "\n\tps -ef | grep \"${0##*/}\":\n"
  ps -ef | grep ${0##*/} | grep -v grep
  if [[ $count -ge $max ]]
  then
    echo -e "\n\tExiting ${0##*/} normally\n"
    exit 0
  fi
done

# if "exits" loop for some other reason, we will exit with error code
echo -e "\n\tExiting ${0##*/} under abnormal conditions\n"
exit 1


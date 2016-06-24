#!/usr/bin/env bash

max=10
count=1
echo -e "\tScript that runs for $max minutes: Loop $count of $max\n"
while [[ $count -lt $max ]]
do
  sleep 1m
  count=$(($count + 1))
  echo -e "\n\tcount: $count of $max\n" | tee /shared/eclipse/test496698/test496698out.txt
  echo -e "\n\tps:\n"
  ps -f
  echo -e "\n\tps -ef | grep \"${0##*/}\":\n"
  ps -ef | grep ${0##*/}
  if [[ $count -ge $max ]]
  then
    echo -e "\n\tExiting ${0##*/} normally\n"
    exit 0
  fi
done

# if "exits" loop for some other reason, we will exit with error code
echo -e "\n\tExiting ${0##*/} under abnormal conditions\n"
exit 1


# !/bin/sh
cd .
#environment variables
PATH=$PATH:`pwd`/../linux;export PATH
xhost +$HOSTNAME
MOZILLA_FIVE_HOME=/usr/lib/firefox-1.5.0.12; export MOZILLA_FIVE_HOME
LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$MOZILLA_FIVE_HOME
USERNAME=`whoami`
DISPLAY=$HOSTNAME:0.0
ulimit -c unlimited

export USERNAME DISPLAY LD_LIBRARY_PATH

#execute command to run tests

./runtests -os linux -ws gtk -arch x86 -vm `pwd`/../jdk6_17/jre/bin/java -properties vm.properties 1> linux.gtk-6.0_consolelog.txt 2>&1

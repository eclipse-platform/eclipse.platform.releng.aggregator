# !/bin/sh
cd .
#environment variables
PATH=$PATH:`pwd`/../linux;export PATH
xhost +$HOSTNAME
MOZILLA_FIVE_HOME=/usr/lib/mozilla-1.7.12;export MOZILLA_FIVE_HOME
LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$MOZILLA_FIVE_HOME
USERNAME=`whoami`
DISPLAY=$HOSTNAME:0.0
ulimit -c unlimited

export USERNAME DISPLAY LD_LIBRARY_PATH

# add Cloudscape plugin to junit tests zip file
zip eclipse-junit-tests-$1.zip -rm eclipse

#all tests
./runtests -os linux -ws gtk -arch x86 -vm `pwd`/../jdk6_17/jre/bin/java -properties vm.properties -Dtest.target=performance -Dplatform=linuxgtkperf2 1> linux.gtk.perf2_consolelog.txt 2>&1



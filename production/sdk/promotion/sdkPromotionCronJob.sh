#!/usr/bin/env bash

# cron job a committer can run,
# say, every 15 minutes, or similar. If a
# file appears in the promoteLocation, then execute it, and if all goes
# well, then remove (or move) that file.

# Note: if there are errors that occur during this cron job, they go to the
# "default user" for that crontab, which may be what's desired, but you can also
# set MAILTO in your crontab, cautiously, to send it where ever you'd like.


# Simple utility to run as cronjob to run Eclipse Platform builds
# Normally resides in $BUILD_HOME

# Start with minimal path for consistency across machines
# plus, cron jobs do not inherit an environment
# care is needed not have anything in ${HOME}/bin that would effect the build 
# unintentionally, but is required to make use of "source buildeclipse.shsource" on 
# local machines.  
# Likely only a "release engineer" would be interested, such as to override "SIGNING" (setting it 
# to false) for a test I-build on a remote machine. 
export PATH=/usr/local/bin:/usr/bin:/bin:${HOME}/bin
# unset common variables (some defined for e4Build) which we don't want (or, set ourselves)
unset JAVA_HOME
unset JAVA_ROOT
unset JAVA_JRE
unset CLASSPATH
unset JAVA_BINDIR
unset JRE_HOME

# 0002 is often the default for shell users, but it is not when ran from
# a cron job, so we set it explicitly, so releng group has write access to anything
# we create.
oldumask=`umask`
umask 0002
# Remember, don't echo except when testing, or mail will be sent each time it runs. 
#echo "umask explicitly set to 0002, old value was $oldumask"

# this buildeclipse.shsource file is to ease local builds to override some variables. 
# It should not be used for production builds.
source buildeclipse.shsource 2>/dev/null



# The 'workLocation' provides a handy central place to have the
# promote script, and log results. ASSUMING this works for all
# types of builds, etc (which is the goal for the sdk promotions).
workLocation=/shared/eclipse/sdk/promotion

# masterBuilder.sh must know about and use this same
# location to put its promotions scripts. (i.e. implicit tight coupling)
promoteScriptLocation=$workLocation/queue

# Note: if we ever need to handle spaces, or newlines in names (seems unlikely) this
# for loop won't quiet work, and will be more complicated (or, at least unintuitive).

# Remember, do no call "exit" from for loop for normal cases, else
# the whole script exits. Could use "continue" or "break" if needed.

allfiles=$( find $promoteScriptLocation -name "promote*.sh" | sort )
for promotefile in $allfiles
do

    # having an echo here will cause cron job to send mail for EACH job, even if all is fine.
    # so use only for testing.
    #echo $promotefile

    if [[ -z "$promotefile" ]]
    then
        # would be an odd error, but nothing to do (Remember, can not have an empty if/then/else clause! Syntax error.
        echo "WARNING: unexpectedly found promotefile variable to be null or empty."
    else
        # found a file, make sure it is executable
        # I've discovered, just testing, that even if $promotefile is a
        #       directory, and executable, and fits the pattern, it is attempted to be
        #       processed. It was just a test case, nearly impossible to occur in reality,
        #       but best to test it is a file, for safety.
        if [[ -x $promotefile  && -f $promotefile ]]
        then

            # if found a file to execute, temporarily change its name to "RUNNING-$promotefile
            # so a subsequent cron job won't find it (if it does not finish by the time of the next cron job).
            runningpromotefile=$promoteScriptLocation/RUNNING_$(basename $promotefile)
            mv  $promotefile $runningpromotefile
            # notice these logs are concatenated on purpose, to give some "history", but
            # that means has to be "manually" removed every now and then.
            # improve as desired.
            /bin/bash $runningpromotefile 1>>$workLocation/promotion-out.txt 2>>$workLocation/promotion-err.txt
            # to test cron job, without doing anything, comment out above line, and uncomment folloiwng line.
            # then try various types of files file names, etc.
            # echo "DEBUG: normally would execute file here: $promotefile" 1>>$workLocation/promotion-out.txt 2>>$workLocation/promotion-err.txt
            rccode=$?
            if [[ $rccode != 0 ]]
            then
                echo "ERROR: promotion returned an error: $rccode"
                echo "       promotefile: $promotefile"
                mv $runningpromotefile $promoteScriptLocation/ERROR_$(basename $promotefile)
                    # probably would not have to exit here, could continue looping since renamed problematic
                    # file , but since something unexpected happened, best to pause to give some opportunity
                    # to examine the issue and make sure not something harmful.
                exit 1
            else
                # all is ok, we'll move the file to "RAN-" in case needed for later inspection,
                # if things go wrong. Perhaps eventually just remove them?
                mv $runningpromotefile $promoteScriptLocation/RAN_$(basename $promotefile)
            fi
        else
            echo "ERROR: promotion file found, but was not executable?"
            echo "         promotefile: $promotefile"
            # We could likely do some rename-and-proceed thing here, but should
            # be so rare and unexpected something is likely very wrong? So, we'll just exit.
            exit 1
        fi
    fi
done


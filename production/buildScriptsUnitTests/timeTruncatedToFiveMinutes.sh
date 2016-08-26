#!/usr/bin/env bash

# small test utility to verify that "time truncation" works as expected. 
# Such as, confirms there is no "off by one" errors at our choosen interval of 5 minutes. 

# Have not tried running over "midnight", but should not impact results. 
# Have also not tested if "system time" changes in the middle of things. That might impact accuracy, 
# but should still be aligned on interval since we only "get" the time in one statement. 

# run for one hour. Can vary as desired, but does interact with the "sleep interval". 
max_loops=$(( 3600 * 1))
count=0
# debug=true produces more detailed ouput
#debug=true

while [[ $count -lt $max_loops ]]
do
  # use a "empty statement" (:), to do increment only, without result being interpreted as a command. 
  : $(( count++))

  # %s is seconds since epoch. That is, seconds since 1970-01-01 00:00:00 UTC
  RAWDATE=$( date +%s )

  # Find "remainder" after dividing by 5 minutes (300 seconds)

  # Doesn't seem to matter, for bash aritmetic, if variables in equation have '$' or not.
  # Confirmed in http://wiki.bash-hackers.org/scripting/newbie_traps
  # And, better, in the official Bash Reference Manual:
  # https://www.gnu.org/software/bash/manual/html_node/Shell-Arithmetic.html#Shell-Arithmetic
  remainder=$(( RAWDATE % 300 )) 
  RAWDATE_TRUNC=$(( RAWDATE - remainder ))
  TIMESTAMP_SECONDS=$( date +%Y%m%d-%H%M%S --date='@'$RAWDATE )
  TIMESTAMP_MIN=$( date +%Y%m%d-%H%M --date='@'$RAWDATE )
  TIMESTAMP=$( date +%Y%m%d-%H%M --date='@'$RAWDATE_TRUNC )

  # print results only on "5 minute" intervals. 
  # This can be modify in many ways depending on degree of output desired. 
  if [[ $remainder == 0 ]]
  then
    printf "\n\t================"
    #fi
    if [[ "$debug" == "true" ]] 
    then
      printf "\n\t%-45s\t%15s" "[DEBUG] loop count:" "$count"
      printf "\n\t%-45s\t%15s" "[DEBUG] Epoch seconds:" "$RAWDATE"
      printf "\n\t%-45s\t%15s" "[DEBUG] Truncated epoch seconds:" "$RAWDATE_TRUNC"
      printf "\n\t%-45s\t%15s" "[DEBUG] Remainder: " "$remainder"
    fi
    printf "\n\t%-45s" "Timestamps:"
    printf "\n\t\t%-45s\t\t%s" "with seconds:" "$TIMESTAMP_SECONDS"
    printf "\n\t\t%-45s\t\t%s" "to previous minute (as in current BUILD_ID):" "$TIMESTAMP_MIN"
    printf "\n\t\t%-45s\t%s\n" "to previous 5 minutes (as proposed in bug 500233):" "$TIMESTAMP"
  fi

  # We sleep for exactly one second, currently, though another
  # test routine could wait and start on an exact minute, and then sleep for one minute. 
  sleep 1

done


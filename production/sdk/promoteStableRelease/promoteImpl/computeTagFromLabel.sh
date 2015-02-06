#!/usr/bin/env bash

# Utility to compute "git tag" from "label" for when we
# promote a milestone or RC. This is for aggregator only.
# Note, normal builds, are promoted with their build id as tag,
# such as I20150202-9899.
# and R-builds are tagged later, using same convention as all the
# other repositories.

# In this case, (milestones, and RC's) we always prefix with "S" for
# sorting consistency, then the build numeric label, with underscores,
# instead of periods. If there is no service field, we assume, and use, "0".
# Finally, anything that follows the numbers, as suffixed with
# _suffix. And, note, we assume something always follows, for this
# routine, such as "M5", "RC1", etc., it is not intended for a
# literal release number, such as "4.5".

# Examples;
#  4.4.2RC3 ==> S4_4_2_RC3
#  4.5M6 ==> S4_5_0_M6

# For production use, DEBUG should be false or undefined.
function computeTagFromLabel () {
buildLabel=$1
DEBUG=$2
PATTERN="([[:digit:]]+)\.([[:digit:]]+)((\.?)([[:digit:]]+))?(.+)"
if [[ $buildLabel =~ $PATTERN ]]
then
  if [[ $DEBUG == "true" ]]
  then
    # if there is a match at all, should always be 6 groups (length of 7).
    #echo "Debug/trace output in $TRACE_LOG"
    echo "DL_LABEL: " $buildLabel &>>$TRACE_LOG
    echo "Match array length: " ${#BASH_REMATCH[@]} &>>$TRACE_LOG
    echo "whole match: " $BASH_REMATCH &>>$TRACE_LOG
    echo "Group 1: " ${BASH_REMATCH[1]} &>>$TRACE_LOG
    echo "Group 2: " ${BASH_REMATCH[2]} &>>$TRACE_LOG
    echo "Group 3: " ${BASH_REMATCH[3]} &>>$TRACE_LOG
    echo "Group 4: " ${BASH_REMATCH[4]} &>>$TRACE_LOG
    echo "Group 5: " ${BASH_REMATCH[5]} &>>$TRACE_LOG
    echo "Group 6: " ${BASH_REMATCH[6]} &>>$TRACE_LOG
  fi
  TAG="S${BASH_REMATCH[1]}_${BASH_REMATCH[2]}"
  if [[ -z "${BASH_REMATCH[5]}" ]]
  then
    SERVICE=0
  else
    SERVICE=${BASH_REMATCH[5]}
  fi
  TAG=${TAG}_${SERVICE}_${BASH_REMATCH[6]}
else
  printf "\n\tWARNING: %s\n" "Build label, $buildLabel, did not match expected 'release' pattern, $PATTERN." &>>$TRACE_LOG
  TAG=""
fi
if [[ $DEBUG == "true" ]]
then
  echo $TAG &>>$TRACE_LOG
fi
echo $TAG

}



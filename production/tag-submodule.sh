#!/usr/bin/env bash

if [ $# -ne 3 ]; then
    echo USAGE: $0 BUILD_ID REPOSITORIES_TXT SUBMODULE_NAME
    exit 1
fi

# USAGE: fn-tag-submodule.sh BUILD_ID REPOSITORIES_TXT SUBMODULE_NAME
#   BUILD_ID: M20121116-1100
#   REPOSITORIES_TXT: /shared/eclipse/builds/streams/repositories.txt
#   SUBMODULE_NAME: 

    BUILD_ID="$1"; shift
    REPOSITORIES_TXT="$1"; shift
    SUBMODULE_NAME="$1"; shift

    RC=0
    # should already be in correct subdirectory? 
    echo "[DEBUG] tagging $SUBMODULE_NAME from directory ${PWD}" > $TRACE_OUTPUT
    if [[ $( grep \"^\${SUBMODULE_NAME}:\" $REPOSITORIES_TXT >/dev/null ) ]]
    then 
      git tag $BUILD_ID 
      RC=$?
      if [[ $RC == 0 ]] 
      then
         $GIT_PUSH origin $BUILD_ID;
         RC=$?
       fi
     else
       # should be rare. Perhaps should be ERROR?  
       echo "[WARNING] In tag-submodule, $SUBMODULE_NAME was not found in repositories.txt." > $TRACE_OUTPUT 
     fi
     exit $RC


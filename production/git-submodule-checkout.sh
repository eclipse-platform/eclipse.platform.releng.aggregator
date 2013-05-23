#!/usr/bin/env bash
#

# TODO: this script is called with repoScript=$( echo $SCRIPT_PATH/git-submodule-checkout.sh )
# so any echo messages and exit codes are being "eaten" by the eval clause. 
# But, not sure it needs to be evaled for repoScript? 
# If needed, will need to write to "buildFailed_ type of file, then check for that file
# after its called. But, for now, will write to TRACE_OUTPUT

REPOSITORIES_TXT="$1"; shift
name="$1"; shift

VAL=$( grep "^${name}:" "$REPOSITORIES_TXT" | cut -f2 -d" ")

# Here we count on $BUILD_TYPE being exported. TODO: make parameter later? 
if [[ -n "$BUILD_TYPE" && "$BUILD_TYPE" == "N" ]] 
then
    if [[ "master" != $VAL ]]
    then
        echo "INFO: Branch forced to 'master', instead of '$VAL', since doing N-Build" >> ${TRACE_OUTPUT}
        VAL="master"
    fi
fi


if [ -z "$VAL" ]; then
    echo "WARNING: No tag or branch specified for $name in repositories.txt" >> ${TRACE_OUTPUT}
    echo "   Has a submodule been added? Perhaps just commented out?" >> ${TRACE_OUTPUT}
    echo "   Will use what ever was last added to aggregator"  >> ${TRACE_OUTPUT}
    # is this ever an error? 
    exit
fi

# always fetch before checkout, to be sure new 
# branches are in local repo (in case we are switching
# to a new branch or tag).
git fetch
RC=$?
if [[ $RC != 0 ]]
then 
    echo "ERROR: Return code of $RC during fetch of $name repository" >> ${TRACE_OUTPUT}
    exit $RC
fi

if [ -z "$(git tag -l $VAL)" ]; then
    echo Updating branch $VAL
    git checkout $VAL
    RC=$?
    if [[ $RC != 0 ]]
    then 
        echo "ERROR: Return code of $RC during checkout of $VAL for repository $name" >> ${TRACE_OUTPUT}
        exit $RC
    fi
    git pull
    RC=$?
    if [[ $RC != 0 ]]
    then 
        echo "ERROR: Return code of $RC during pull for repository $name" >> ${TRACE_OUTPUT}
        exit $RC
    fi
else
    echo Updating to tag $VAL
    git checkout $VAL
    RC=$?
    if [[ $RC != 0 ]]
    then 
        echo "ERROR: Return code of $RC during checkout of $VAL for repository $name" >> ${TRACE_OUTPUT}
        exit $RC
    fi
fi

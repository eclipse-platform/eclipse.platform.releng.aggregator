#!/usr/bin/env bash

# test for computeTagFromLabel.sh.
export TRACE_LOG=${PWD}/testTraceLog.txt
if [[ -z "${PROMOTE_IMPL}" ]]
then
  PROMOTE_IMPL=${PWD}
fi
source ${PROMOTE_IMPL}/computeTagFromLabel.sh

function display ()
{
  label=$1
  tag=$2
  if [[ -z "${tag}" ]]
  then
    echo "Invalid label, $label, for this routine (should exit with error)"
  else
    echo -e "For DL_LABEL: $label \tthe tag would be: $tag"
  fi
}

#DEBUG="true"
DEBUG=

DL_LABEL="4.4.2RC3"
TAG=$( computeTagFromLabel $DL_LABEL $DEBUG )
display $DL_LABEL $TAG

DL_LABEL="4.4RC3"
TAG=$( computeTagFromLabel $DL_LABEL $DEBUG )
display $DL_LABEL $TAG

DL_LABEL="4.4M5"
TAG=$( computeTagFromLabel $DL_LABEL $DEBUG )
display $DL_LABEL $TAG

DL_LABEL="I20150503-2222"
TAG=$( computeTagFromLabel $DL_LABEL $DEBUG )
display $DL_LABEL $TAG

DL_LABEL=" "
TAG=$( computeTagFromLabel "$DL_LABEL" $DEBUG )
display $DL_LABEL $TAG

DL_LABEL="4.4"
TAG=$( computeTagFromLabel $DL_LABEL $DEBUG )
display $DL_LABEL $TAG

DL_LABEL="45.99999.00000TTXX"
TAG=$( computeTagFromLabel $DL_LABEL $DEBUG )
display $DL_LABEL $TAG


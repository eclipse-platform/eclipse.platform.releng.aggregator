#!/usr/bin/env bash
#
# Utility function to convert repo site metadata files to XZ compressed files.
# See https://bugs.eclipse.org/bugs/show_bug.cgi?id=464614
#
# The utility makes the strong assumptions that the metadata files are in the zipped
# (jar) format already. Converts those to their original XML, invokes 'xz -e' on those
# XML files (which changed them to *.xml.xz compressed files, 
# and then (re-)create the p2.index file.

function usage
{
  printf "\n\tUtility script to create *.xml.xz compressed metadata files from *.jar counterparts.\n" >&2
  printf "\n\tUsage: %s [-h] | [-d path] [-n], where " "$(basename $0)" >&2
  printf "\n\t\t%s\t%s" "h" "help"
  printf "\n\t\t%s\t%s" "d path"  "directory (absolute path) to simple repository "
  printf "\n\t\t\t%s" "(if not specified, current directory, $PWD,  is used)"
  printf "\n\t\t%s\t%s" "n" "no force (i.e does not recreate files, if p2.index says they already exist)."
  printf "\n\t\t\t%s" "Default is it will recreate them." >&2
  printf "\n" >&2
}


# The function 'createXZ' takes as input the absolute path of the simple repo.
# It can also take "-noforce" as an argument, in which case, it will not re-create
# the files, if they appear to already exist.
# Returns 0 upon successful completion, else greater than 0 if there
# is an error of any sort.
#
function createXZ
{
  # First get back to XML files by unzipping the jar files. 
  # Then XZ compress the XML files,
  # then create (or recreate) the p2.index file.

  # The BUILDMACHINE_SITE is the absolute directory to the simple repo directory.
  # (We typically create on the build machine, before copying (rsync'ing) to downloads server).
  # If a value is passed to this script as the first argument, it is assumed to be the simple repo to process.
  # For historical reasons if path not specified as argument, we look for (require) 
  # BUILDMACHINE_SITE to be defined as an environment variable with the absolute path of the 
  # simple repo to process.

  if [[ -n "$1" ]]
  then
    BUILDMACHINE_SITE=$1
  fi

  if [[ -z "${BUILDMACHINE_SITE}" ]]
  then
    echo -e "\n\tERROR: this script requires path to repo as first argument, or set in env variable of BUILDMACHINE_SITE,"
    echo "     \tthat is, the directory of the simple repo, that contains content.jar and artifacts.jar."
    return 1
  fi

  # note first argument MUST e specified, to specify second argument.
  # we assume "use force" by default, since that is the common case.
  FORCE_ARG=$2
  if [[ "-noforce" == "${FORCE_ARG}" ]]
  then
    FORCE="false"
  else 
    FORCE="true"
  fi

  # confirm both content.jar and artifacts.jar exist at this site. Note: strong assumption the jar already exists.
  # In theory, if it did not, we could create the jars from the content.xml and artifacts.xml file,
  # And then create the XZ compressed version of the XML file, but for now this is assumed to be a
  # rare case, so we do not handle it.
  CONTENT_JAR_FILE="${BUILDMACHINE_SITE}/content.jar"
  if [[ ! -e "${CONTENT_JAR_FILE}" ]]
  then
    echo -e "\n\tERROR: content.jar file did not exist at ${BUILDMACHINE_SITE}."
    return 1
  fi
  ARTIFACTS_JAR_FILE="${BUILDMACHINE_SITE}/artifacts.jar"
  if [[ ! -e "${ARTIFACTS_JAR_FILE}" ]]
  then
    echo -e "\n\tERROR: artifacts.jar file did not exist at ${BUILDMACHINE_SITE}."
    return 1
  fi

  # As an extra sanity check, if compositeContent.jar/xml or compositeArtifacts.jar/xml
  # exist at the same site, we also bale out, with error message, since this script isn't prepared
  # to handle those hybrid  sites.
  COMPOSITE_CONTENT_JAR="${BUILDMACHINE_SITE}/compositeContent.jar"
  COMPOSITE_CONTENT_XML="${BUILDMACHINE_SITE}/compositeContent.xml"
  COMPOSITE_ARTIFACTS_JAR="${BUILDMACHINE_SITE}/compositeArtifacts.jar"
  COMPOSITE_ARTIFACTS_XML="${BUILDMACHINE_SITE}/compositeArtifacts.xml"

  if [[ -e "${COMPOSITE_CONTENT_JAR}" || -e "${COMPOSITE_CONTENT_XML}" || -e "${COMPOSITE_CONTENT_JAR}" || -e "${COMPOSITE_CONTENT_JAR}" ]]
  then
    echo -e "\n\tERROR: composite files exists at this site, ${BUILDMACHINE_SITE},"
    echo -e "\n\t       but this script is not prepared to process hybrid sites, so exiting."
    return 1
  fi

  # We do a small heuristic test if this site has already been converted. If it has, touching the files, again, will
  # call mirrors to be think they are "out of sync".
  # If someone does want to "re-generate", then may have to delete p2.index file first, to get past this check.
  # If p2.index file exists, check if it contains the "key" value of 'content.xml.xz' and if it does, assume this
  # site has already been converted.
  # UNLESS, -force has been specified (as second argument).
  # Then we purposely reproduce. This is important, for example, after changing the mirrorsURL of artifacts.jar/xml file.
  # LATEST: we now assume "use force" by default, since most common case, unless -noforce is provided as command.
  P2_INDEX_FILE="${BUILDMACHINE_SITE}/p2.index"
  if [[ "true" != "${FORCE}" ]]
  then
    if [[ -e "${P2_INDEX_FILE}" ]]
    then
      grep "content.xml.xz" "${P2_INDEX_FILE}" 1>/dev/null
      RC=$?
      # For grep, an RC of 1 means "not found", in which case we continue.
      # An RC of 0 means "found", so then check for 'artifacts.xml.xz if it
      # it too is found, then we assume the repo has already been "converted",
      # and we do not touch anything and bail out.
      # An RC of 2 or greater means some sort of error, we will bail out anyway, but with
      # different message.
      if [[ $RC = 0 ]]
      then
        grep "artifacts.xml.xz" "${P2_INDEX_FILE}" 1>/dev/null
        RC=$?
        if [[ $RC = 0 ]]
        then
          echo -e "\n\tINFO: Will exit ${0##/*/}, since contents of p2.index file implies already converted this site at "
          echo -e "  \t${BUILDMACHINE_SITE}"
          return 0
        else
          if [[ $RC > 1 ]]
          then
            echo -e "\n\tERROR: Will exit ${0##/*/}, since grep returned an error code of $RC"
            return $RC
          fi
        fi
      else
        if [[ $RC > 1 ]]
        then
          echo -e "\n\tERROR: Will exit ${0##/*/}, since grep returned an error code of $RC"
          return $RC
        fi
      fi
    fi
  fi

  # Notice we overwrite the XML files, if they already exists.
  unzip -q -o "${CONTENT_JAR_FILE}" -d "${BUILDMACHINE_SITE}"
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: could not unzip ${CONTENT_JAR_FILE}."
    return $RC
  fi
  # Notice we overwrite the XML files, if they already exists.
  unzip -q -o "${ARTIFACTS_JAR_FILE}" -d "${BUILDMACHINE_SITE}"
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: could not unzip ${ARTIFACTS_JAR_FILE}."
    return $RC
  fi

  CONTENT_XML_FILE="${BUILDMACHINE_SITE}/content.xml"
  ARTIFACTS_XML_FILE="${BUILDMACHINE_SITE}/artifacts.xml"
  # We will check the content.xml and artifacts.xml files really exists. In some strange world, the jars could contain something else.
  if [[ ! -e "${CONTENT_XML_FILE}" || ! -e "${ARTIFACTS_XML_FILE}" ]]
  then
    echo -e "\n\tERROR: content.xml or artifacts.xml file did not exist as expected at ${BUILDMACHINE_SITE}."
    return 1
  fi

  # finally, compress them, using "extra effort"
  # Nice thing about xz, relative to other compression methods, it can take
  # longer to compress it, but not longer to decompress it.
  # We use 'which' to find the executable, just so we can test if it happens
  # to not exist on this particular machine, for some reason.
  # Notice we use "force" to over write any existing file, presumably there from a previous run?
  XZ_EXE=$(which xz)
  if [[ $? != 0 || -z "${XZ_EXE}" ]]
  then
    echo -e "\n\tERROR: xz executable did not exist."
    return 1
  fi
  echo -e "\n\tXZ compression of ${CONTENT_XML_FILE} ... "
  $XZ_EXE -e --force "${CONTENT_XML_FILE}"
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: could not compress, using $XZ_EXE -e ${CONTENT_XML_FILE}."
    return $RC
  fi

  echo -e "\tXZ compression of ${ARTIFACTS_XML_FILE} ... "
  $XZ_EXE -e --force "${ARTIFACTS_XML_FILE}"
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: could not compress, using $XZ_EXE -e ${ARTIFACTS_XML_FILE}."
    return $RC
  fi


  # Notice we just write over any existing p2.index file.
  # May want to make backup of this and other files, for production use.
  echo "version=1" > "${P2_INDEX_FILE}"
  echo "metadata.repository.factory.order= content.xml.xz,content.xml,!" >> "${P2_INDEX_FILE}"
  echo "artifact.repository.factory.order= artifacts.xml.xz,artifacts.xml,!" >> "${P2_INDEX_FILE}"
  echo -e "\tCreated ${P2_INDEX_FILE}"

  # In the distant future, there might be a time we'd provide only the xz compressed version.
  # If so, the p2.index file would be as follows. See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=464614
  #   version=1
  #   metadata.repository.factory.order= content.xml.xz,!
  #   artifact.repository.factory.order= artifacts.xml.xz,!
  return 0
}

PATH_TO_REPO="${PWD}"

# the initial ':' keeps getopts in quiet mode ... meaning it doesn't print additional "illegal argument" type messages.
# to get it in completely silent mode, assign $OPTERR=0
# The second colon means to "read string argument".
while getopts ':hd:n' OPTION
do
  options_found=1
  case $OPTION in
    h)
      usage
      exit 1
      ;;
    d)
      # directory
      PATH_TO_REPO="${OPTARG}"
      if [[ ! -d "${PATH_TO_REPO}" ]]
      then
        echo -e "\n\tERROR: provided path, ${PATH_TO_REPO}, does not exist or is not a directory."
        exit 1
      else if [[ ! -w "${PATH_TO_REPO}" ]]
      then
        echo -e "\n\tERROR: No write access to the provided path, ${PATH_TO_REPO}."
        exit 1
      fi
    fi
    ;;
  n)
    # noforce
    FORCE_ARG="-noforce"
    ;;
  *)
    # This fall-through is for unrecognized arguments
    printf "\n\t%s" "ERROR: unknown option found: -$OPTARG." >&2
    printf "\n" >&2
    usage
    exit 1
    ;;
esac
done

# while we currently don't use/expect additional arguments, it's best to
# shift away arguments handled by above getopts, so other code (in future) could
# handle additional trailing arguments not intended for getopts.
shift $(($OPTIND - 1))
#echo "n args: " $#i
# just doing this check here as a test (for learning/example)... could never be reached given initial checks
# at top of this script
if [[ $# > 1 ]]
then
  printf "\n\tUnexpected trailing arguments found:  %s.\n" "$*"
  usage
  exit 1
fi

createXZ "${PATH_TO_REPO}" "${FORCE_ARG}"

# for isolated testing
#echo PATH_TO_REPO: $PATH_TO_REPO
#echo FORCE_ARG: $FORCE_ARG


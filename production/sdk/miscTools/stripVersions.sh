#!/usr/bin/env bash

# Utility bash script to "strip" buildIds and version numbers out of directories and
# bundles names, in order to make it more meaningful to compare two "installs",
# or repositories.
# It also "unzips" any bundled jars, so subsequent "diff" commands will be
# comparing files to files.

# It allows "directory" to be passed in on command line, which should the the directory
# that contains both "plugins" and "features" directories. Contents of those
# two directories will be "stripped".
# It leaves given input unchanged, but puts results in subdirectories of features and
# plugins, named "stripped".

OUTPUT_DIR_SEGMENT="stripped"
DEBUG_PARSE="true"
DEBUG_VERBOSE_FILE_OPS="false"
# DEBUG function is used in abbreviated form.
# return 0 to have debug statements printed.
# non-zero (1) to not print debug statements
function DEBUG
{
  return 1;
}
function VERBOSE
{
  return 1;
}

DIRNAME=$1

if [[ -z "${DIRNAME}" ]]
then
  DIRNAME="${PWD}"
  echo -e "\n\tDirectory not specified on command line, assuming current directory of"
  echo -e "\t${DIRNAME}\n"
else
  echo -e "\n\tDirectory specified on command line:"
  echo -e "\t${DIRNAME}\n"
fi

if [[ ! "${DIRNAME:0:1}" = "/" ]]
then
  printf "\n\t%s %s\n\t%s\n\t%s\n" "DIRNAME argument, " "${DIRNAME}, " "was not absolute, so assuming it is a child of current working directory, " "${PWD}."
  DIRNAME="${PWD}/${DIRNAME}"
fi
PLUGINS_DIR="${DIRNAME}/plugins"
FEATURES_DIR="${DIRNAME}/features"

if [[ ! -e "${PLUGINS_DIR}" || ! -e "${FEATURES_DIR}" ]]
then
  echo -e "\n\tERROR: 'plugins' or 'features' directory (or both) did not exist under DIRNAME."
  echo -e "\n\t\tPlease check DIRNAME and re-enter."
  echo -e "\n\t\tDIRNAME: \t${DIRNAME}"
  exit 1
fi

function cleanStripped ()
{
  printf "\n\t%s\n" "Removing any existing directories named '${OUTPUT_DIR_SEGMENT}'; assuming left over from previous runs."
  find $WORK_DIR -name ${OUTPUT_DIR_SEGMENT} -type d -exec rm -fr '{}' \;
}

function processDirectory ()
{

  WORK_DIR=$1
  if [[ ! -e "${WORK_DIR}" ]]
  then
    echo -e "\n\tPROGRAM ERROR: WORK_DIR did not exist, even after earlier checking?\n"
    exit 1
  fi

  OUTPUTDIR="${WORK_DIR}/${OUTPUT_DIR_SEGMENT}"
  mkdir -p "${OUTPUTDIR}"
  DEBUG && echo "DEBUG: OUTPUTDIR: ${OUTPUTDIR}"
  VERSION_PATTERN="(([[:digit:]]+\.[[:digit:]]+\.[[:digit:]]+)(\.{1}([0-9A-Za-z_\-]+){1})?)"
  NAME_PATTERN="^.*/(.*)_${VERSION_PATTERN}(\.jar)?$"

  for filename in ${WORK_DIR}/*
  do
    # no sense checking the literal WORK_DIR itself,
    # just to avoid noise in log.
    if [[ "${filename}" == "${OUTPUTDIR}" ]]
    then
      continue
    fi
    OLDFILENAME="$filename"
    DEBUG && echo -e "DEBUG: Starting loop with ${OLDFILENAME}"
    if [[ "${OLDFILENAME}" =~ $NAME_PATTERN ]]
    then
      if [[ "${DEBUG_PARSE}" == "true" ]]
      then
        printf "\n%s\n" "MATCH"
        printf "\t%s\n" "${OLDFILENAME}"
        #printf "\t%s\n\n" "${BASH_REMATCH[@]}"
        for ((i=1; i < ${#BASH_REMATCH[*]}; i++))
        do
          printf "%u \t%s\n" ${i} "${BASH_REMATCH[${i}]}"
        done
      fi
      NEWFILENAME="${BASH_REMATCH[1]}${BASH_REMATCH[7]}"
      NEWFILEBASENAME=$NEWFILENAME
      if [[ "${NEWFILEBASENAME}" =~ ^(.*)\.jar ]]
      then
        NEWFILEBASENAME="${BASH_REMATCH[1]}"
      fi
      NEWSTRIPPEDDIR="${OUTPUTDIR}/${NEWFILEBASENAME}"
      DEBUG && echo -e "DEBUG: will copy or unzip \n\t${OLDFILENAME} to \n\t${NEWSTRIPPEDDIR}"
      if [[ -d ${OLDFILENAME} && ! -L "${OLDFILENAME}" ]]
      then
        DEBUG && echo "DEBUG: OLDFILENAME is a directory: ${OLDFILENAME}"
        mkdir -p "${NEWSTRIPPEDDIR}"
        DEBUG && echo "DEBUG: Made dir ${NEWSTRIPPEDDIR}"
        R_VERBOSE=
        if [[ "${DEBUG_VERBOSE_FILE_OPS}" == "true" ]]
        then
          R_VERBOSE="-v"
        fi
        VERBOSE && printf "\n\t%s\n\t%s\n\t%s\n\t%s\n" "copying" "${OLDFILENAME}" "to" "${NEWSTRIPPEDDIR}"
        #remember, no quotes around options!
        rsync -r --safe-links  ${R_VERBOSE} "${OLDFILENAME}/" "${NEWSTRIPPEDDIR}/"
        RC=$?
        if [[ ! $RC == 0 ]]
        then
          echo -e "[ERROR] Return from rsync was non-zero.RC: $RC"
        fi
      elif [[ -f "${OLDFILENAME}" ]]
      then
        DEBUG && echo "DEBUG: OLDFILENAME is a file: ${OLDFILENAME}"
        # TODO: To handle "multiples", such as org.eclipse.jdt.annotations,
        # and remember, in some repos, may be multiples that differ only in
        # suffix, we will add arbitrary "__1", "__2", etc., based on the order
        # encountered on file system. (We limit it to "10" duplicates, since
        # more than 3 or 5 is highly unlikely, and a sign of an error if there
        # are more than that.
        MULTIPLES_SUFFIX=1
        if [[ -e "${NEWSTRIPPEDDIR}" ]]
        then
          MULTIPLES_SUFFIX=1
          NEWSTRIPPEDDIR_SUFFIXED=${NEWSTRIPPEDDIR}__${MULTIPLES_SUFFIX}
          while [[ -e "${NEWSTRIPPEDDIR_SUFFIXED}" &&  ${MULTIPLES_SUFFIX} -lt 10 ]]
          do
            ((MULTIPLES_SUFFIX++))
            DEBUG && echo "DEBUG: MULTIPLLES_SUFFIX INCREMENTED: ${MULTIPLES_SUFFIX}"
            NEWSTRIPPEDDIR_SUFFIXED=${NEWSTRIPPEDDIR}__${MULTIPLES_SUFFIX}
          done
          NEWSTRIPPEDDIR=$NEWSTRIPPEDDIR_SUFFIXED
        fi
        Z_VERBOSE=
        if [[ ! "${DEBUG_VERBOSE_FILE_OPS}" == "true" ]]
        then
          Z_VERBOSE="-q"
        fi
        VERBOSE && printf "\n\t%s\n\t%s\n\t%s\n\t%s\n" "unzipping" "${OLDFILENAME}" "to" "${NEWSTRIPPEDDIR}"
        unzip ${Z_VERBOSE} -n "${OLDFILENAME}" -d "${NEWSTRIPPEDDIR}"
      else
        echo -e "A matching pattern was neither a file, nor a directory?!\n"
      fi
    else
      if [[ "${DEBUG_PARSE}" == "true" ]]
      then
        echo -e "\nNO MATCH\n"
        echo -e "${OLDFILENAME}\n did not match pattern of \n${NAME_PATTERN}\n"
      fi
    fi
  done
}


# remove earlier versions of "stripped" directories
cleanStripped

processDirectory "${PLUGINS_DIR}"
processDirectory "${FEATURES_DIR}"

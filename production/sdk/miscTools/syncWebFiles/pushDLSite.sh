#!/usr/bin/env bash

function usage ()
{
  printf "\n\t%s\n" "This utlity is to push web files (but not drop artifacts) up host machine, after local testing and development."
  printf "\t%s\n" "Care is needed since it can delete files on host, for the goal of making contents equal." 
  printf "\t%s\n" "Thus, at least one parameter is needed: -f or -s; for 'full' or 'synchronize'"
  printf "\t%s\n" "-f (full) should be rarely needed; only when just getting started, or similar."
  printf "\t\t%s\t%s\n" "-f" "Does a full copy from remote host to local client". 
  printf "\t\t%s\t%s\n" "-s" "Copies files from remote host to local client, but also deletes files on client, that are not on host."
  printf "\t\t%s\t%s\n" "-r" "All operations are 'dry-runs' unless '-r' ('run') is specified."
  printf "\t\t%s\t%s\n" "-v" "verbose output."
  printf "\t\t%s\t%s\n" "-h" "Displays this 'usage' information."

}

doit="--dry-run"
verbose=""
delete=""
trailing=""
full=""
sync=""
while getopts 'fsrvh' OPTION
do
  case $OPTION in
    h)    usage
      exit 1
      ;;
    f)    delete=""
          full="true"
      ;;
    s)    delete="--delete"
          sync="true"
      ;;
    v)    verbose="--verbose"
      ;;  
    r)    doit=""
      ;;
    #        ?)    usage
      #        exit 2
      #        ;;
  esac
done

# We do not currently use trailing arguments, 
# but for safety/sanity checks, issue "error" if found.
shift $(($OPTIND - 1))
trailing="$@"

if [[ -n "${trailing}" ]]
then
  printf "\n\tERROR: \t%s\n" "Unexpected trailing arguments were found, so exiting."
  usage
  exit 1
fi

# if BOTH f and s specified, it is an error. 
# if NEITHER f nor s is specified, it is a "no op", but we can treat as a "--dry-run" of full copy?

if [[ "${full}" == "true" && "${sync}" == "true" ]]
then
  printf "\n\tERROR: \t%s\n" "Both full and sync were specified."
  usage
  exit 1
fi

if [[ -z "${full}" && -z "${sync}" && -z "${doit}" ]]
then
  printf "\n\tERROR: \t%s\n" "Neither full nor sync were specified, but 'run' was."
  usage
  exit 1
fi

# we exclude 'index.html' since that is a "derived" file, and is specific to what is in "drop directories". 
# we excldue the drop directories since they can be massive, and do not need them to do web development.
# repeating --itemize-changes is intentional, and causes unchanged files to also be listed.
rsync ${doit} ${verbose} ${delete} --itemize-changes --itemize-changes -P -e ssh -a --exclude /index.html --exclude /drops4/  ./downloads/ build:~/downloads/eclipse/downloads/ 

if [[ -z $full && -z $sync ]]
then
  printf "\n\tWARNING: \t%s\n" "Neither full nor sync were specified, so symply did full (dry-run) list of what would be copied."
  usage
  exit 0
fi



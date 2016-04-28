
# TODO: an enhanced version was put directly in 'wgetFreshSDKdir.sh'
# Should check if this is used anywhere, and delete, if not.

function checkForErrorExit ()
{
  # arg 1 must be return code, $?
  # arg 2 (remaining line) can be message to print before exiting due to non-zero exit code
  exitCode=$1
  shift
  message="$*"
  if [ -z "${exitCode}" ]
  then
    echo "PROGRAM ERROR: checkForErrorExit called with no arguments"
    exit 1
  fi
  if [ -z "${message}" ]
  then
    echo "WARNING: checkForErrorExit called without message"
    message="(Calling program provided no message)"
  fi
  if [ "${exitCode}" -ne "0" ]
  then
    echo
    echo "   ERROR. exit code: ${exitCode}  ${message}"
    echo
    exit "${exitCode}"
  fi
}

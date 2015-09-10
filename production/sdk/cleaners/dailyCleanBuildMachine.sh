# Utility to clean build machine
echo -e "\n\tDaily clean of ${HOSTNAME} build machine on $(date )\n"


function removeOldDirectories ()
{
  rootdir=$1
  ctimeAge=$2
  pattern=$3
  [[ -e "${rootdir}" ]] && find "${rootdir}" -maxdepth 1 -type d -ctime ${ctimeAge} -name "${pattern}" -ls -exec rm -fr '{}' \;
  # Using following form is TOO verbose
  #[[ -e "${rootdir}" ]] && find "${rootdir}" -maxdepth 1 -ctime ${ctimeAge} -name "${pattern}" -exec rm -vfr '{}' \;

}

function cleanBuildMachine ()
{

  buildmachine=$1
  buildType=P
  basedir="/shared/eclipse/${buildmachine}/4${buildType}/siteDir"
  chkdir="eclipse/downloads/drops4"
  removeOldDirectories "${basedir}/${chkdir}" "+4" "${buildType}*" 
  chkdir="equinox/drops"
  removeOldDirectories "${basedir}/${chkdir}" "+4" "${buildType}*" 
  chkdir="updates/4.6-P-builds"
  removeOldDirectories "${basedir}/${chkdir}" "+4" "${buildType}*" 

  buildType=N
  basedir="/shared/eclipse/${buildmachine}/4${buildType}/siteDir"
  chkdir=eclipse/downloads/drops4
  removeOldDirectories "${basedir}/${chkdir}" "+2" "${buildType}*" 
  chkdir=equinox/drops
  removeOldDirectories "${basedir}/${chkdir}" "+2" "${buildType}*" 
  chkdir=updates/4.6-N-builds
  removeOldDirectories "${basedir}/${chkdir}" "+2" "${buildType}*" 

  buildType=I
  basedir="/shared/eclipse/${buildmachine}/4${buildType}/siteDir"
  chkdir="eclipse/downloads/drops4"
  removeOldDirectories "${basedir}/${chkdir}" "+4" "${buildType}20*" 
  chkdir="equinox/drops"
  removeOldDirectories "${basedir}/${chkdir}" "+4" "${buildType}20*" 
  chkdir="updates/4.6-I-builds"
  removeOldDirectories "${basedir}/${chkdir}" "+4" "${buildType}20*" 

  buildType=M
  basedir="/shared/eclipse/${buildmachine}/4${buildType}/siteDir"
  chkdir="eclipse/downloads/drops4"
  removeOldDirectories "${basedir}/${chkdir}" "+4" "${buildType}20*" 
  chkdir="equinox/drops"
  removeOldDirectories "${basedir}/${chkdir}" "+4" "${buildType}20*" 
  chkdir="updates/4.6-I-builds"
  removeOldDirectories "${basedir}/${chkdir}" "+4" "${buildType}20*" 
}

## remove old promotion scripts
find /shared/eclipse/sdk/promotion/queue -name "RAN*" -ctime +4 -ls -exec rm '{}' \;
find /shared/eclipse/sdk/promotion/queue -name "TEST*" -ctime +1 -ls -exec rm '{}' \;
find /shared/eclipse/sdk/promotion/queue -name "ERROR*" -ctime +4 -ls -exec rm '{}' \;
find /shared/eclipse/equinox/promotion/queue -name "RAN*" -ctime +4 -ls -exec rm '{}' \;
find /shared/eclipse/equinox/promotion/queue -name "TEST*" -ctime +1 -ls -exec rm '{}' \;
find /shared/eclipse/equinox/promotion/queue -name "ERROR*" -ctime +4 -ls -exec rm '{}' \;

cleanBuildMachine builds
#cleanBuildMachine buildsdavidw




function checkrepo ()
{
  cd $1
  echo $1
  #git fsck --unreachable | wc -l
  #find objects/?? -type f | wc -l
  #find objects/pack -ls | wc -l
  git gc
}
PROJECTROOT=/gitroot/platform

checkrepo ${PROJECTROOT}/eclipse.platform.common.git
checkrepo ${PROJECTROOT}/eclipse.platform.debug.git
checkrepo ${PROJECTROOT}/eclipse.platform.git
checkrepo ${PROJECTROOT}/eclipse.platform.news.git
checkrepo ${PROJECTROOT}/eclipse.platform.releng.eclipsebuilder.git
checkrepo ${PROJECTROOT}/eclipse.platform.releng.git
checkrepo ${PROJECTROOT}/eclipse.platform.releng.maps.git
checkrepo ${PROJECTROOT}/eclipse.platform.resources.git
checkrepo ${PROJECTROOT}/eclipse.platform.runtime.git
checkrepo ${PROJECTROOT}/eclipse.platform.swt.binaries.git
checkrepo ${PROJECTROOT}/eclipse.platform.swt.git
checkrepo ${PROJECTROOT}/eclipse.platform.team.git
checkrepo ${PROJECTROOT}/eclipse.platform.text.git
checkrepo ${PROJECTROOT}/eclipse.platform.ua.git
checkrepo ${PROJECTROOT}/eclipse.platform.ui.git


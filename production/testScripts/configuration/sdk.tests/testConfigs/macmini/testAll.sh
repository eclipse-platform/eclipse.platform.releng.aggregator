#!/bin/sh
ulimit -c unlimited

# This file should never exist or be needed for production machine,
# but allows an easy way for a "local user" to provide this file
# somewhere on the search path ($HOME/bin is common),
# and it will be included here, thus can provide "override values"
# to those defined by defaults for production machine.,
# such as for vmcmd

source localTestsProperties.shsource 2>/dev/null

echo "PWD: $PWD"

# This is the VM used to start the "ant runner" process. 
# It can be, but does not have to be, the same Java that's used for 
# running the tests. The Java can be (optionally) defined in 'vm.properties'.
# But, occasionally we do need to know exact path to VM, such as see bug 390286
${vmcmd:-/Library/Java/JavaVirtualMachines/jdk1.7.0_51.jdk/Contents/Home/jre/bin/java}

# TODO: This doesn't seem all that useful. Should be removed. 
# But, allow values to be specified in vm.properties, as well as command line? 
# production machine is x86_64, but some local setups may be 32 bit and will need to provide
# this value in localTestsProperties.shsource.
eclipseArch=${eclipseArch:-x86}

# vm.properties is used by default on production machines, but will
# need to override on local setups to specify appropriate vm (usually same as vmcmd).
# see bug 388269
propertyFile=${propertyFile:-vm.properties}

echo "vmcmd in testAll: ${vmcmd}"
echo "extdir in testAll (if any): ${extdir}"
echo "propertyFile in testAll: ${propertyFile}"
echo "buildId in testAll: ${buildId}"
echo "contents of propertyFile:"
cat ${propertyFile}

#execute command to run tests
/bin/chmod 755 runtestsmac.sh
/bin/mkdir -p results/consolelogs

#TODO: console logs can be renamed at end of process, with more exact names, if need,
#such as to reflect bitness, and VM level. 
consoleLog="results/consolelogs/macmini-${buildId}_consolelog.txt"

if [[ -n "${extdir}" ]]
then
./runtestsmac.sh -os macosx -ws cocoa -arch $eclipseArch -extdirprop "${extdir}" -vm "${vmcmd}" -properties ${propertyFile} $* > ${consoleLog}
else
./runtestsmac.sh -os macosx -ws cocoa -arch $eclipseArch -vm "${vmcmd}" -properties ${propertyFile} $* > ${consoleLog}
fi

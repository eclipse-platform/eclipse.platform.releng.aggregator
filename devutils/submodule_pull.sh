#!/bin/bash
# for this branch.
# the branches of submodules should match what is in ../streams/repositories.txt
# This script assumes aggregator has been cloned,
# and submodules initially 'updated', as per
# http://wiki.eclipse.org/Platform-releng/Platform_Build#cloning_platform_source_tree
#
# intended to be executed "in place" in devutils so we always 'move up' a directory to get to submodule directory
echo rt.equinox.bundles ; cd ../rt.equinox.bundles ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo rt.equinox.framework ; cd ../rt.equinox.framework ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo rt.equinox.binaries ; cd ../rt.equinox.binaries ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo rt.equinox.p2 ; cd ../rt.equinox.p2 ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.jdt.core.binaries ; cd ../eclipse.jdt.core.binaries ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.jdt.core ; cd ../eclipse.jdt.core ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.jdt.debug ; cd ../eclipse.jdt.debug ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.jdt ; cd ../eclipse.jdt ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.jdt.ui ; cd ../eclipse.jdt.ui ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.pde ; cd ../eclipse.pde ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.pde.build ; cd ../eclipse.pde.build ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.pde.ui ; cd ../eclipse.pde.ui ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.debug ; cd ../eclipse.platform.debug ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.resources ; cd ../eclipse.platform.resources ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform ; cd ../eclipse.platform ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.common ; cd ../eclipse.platform.common ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.releng ; cd ../eclipse.platform.releng ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.runtime ; cd ../eclipse.platform.runtime ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.team ; cd ../eclipse.platform.team ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.text ; cd ../eclipse.platform.text ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.ua ; cd ../eclipse.platform.ua ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.ui ; cd ../eclipse.platform.ui ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.swt ; cd ../eclipse.platform.swt ; git fetch; git checkout R4_4_maintenance ; git pull ;
echo eclipse.platform.swt.binaries ; cd ../eclipse.platform.swt.binaries ; git fetch; git checkout R4_4_maintenance ; git pull ;

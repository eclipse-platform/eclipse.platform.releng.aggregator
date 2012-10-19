#!/bin/bash
# for R3_8_maintenance
echo rt.equinox.bundles ; cd rt.equinox.bundles ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo rt.equinox.framework ; cd rt.equinox.framework ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo rt.equinox.incubator ; cd rt.equinox.incubator ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo rt.equinox.p2 ; cd rt.equinox.p2 ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.jdt.core.binaries ; cd eclipse.jdt.core.binaries ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.jdt.core ; cd eclipse.jdt.core ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.jdt.debug ; cd eclipse.jdt.debug ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.jdt ; cd eclipse.jdt ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.jdt.ui ; cd eclipse.jdt.ui ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.pde ; cd eclipse.pde ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.pde.build ; cd eclipse.pde.build ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.pde.ui ; cd eclipse.pde.ui ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.platform.debug ; cd eclipse.platform.debug ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.platform.resources ; cd eclipse.platform.resources ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.platform ; cd eclipse.platform ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.platform.common ; cd eclipse.platform.common ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.platform.releng ; cd eclipse.platform.releng ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.platform.runtime ; cd eclipse.platform.runtime ; git checkout R3_8_1 ; cd .. 
echo eclipse.platform.team ; cd eclipse.platform.team ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.platform.text ; cd eclipse.platform.text ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.platform.ua ; cd eclipse.platform.ua ; git checkout R3_8_maintenance ; git pull ; cd .. 
echo eclipse.platform.ui ; cd eclipse.platform.ui ; git checkout R3_8_maintenance ; git pull ; cd .. 

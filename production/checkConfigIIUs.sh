#!/usr/bin/env bash

# Small utility to test "build area" that correct number of 
# IUs ae created for "tooling-type" IUs and "config-IUs". 
# TODO: should formalize in Java, and check repository directly? 
# TODO: may need adjustment to better check "MacApp" layout? 
# 3/21/2015 - did check to check for zero symbolic links (used to check 
# for 6, but now we no longer create them. 

BUILD_HOME="/shared/eclipse/builds"

echo -e "\n\tBUILD_HOME: ${BUILD_HOME}\n"

BUILD_MAJOR_VERSION=4
BUILD_TYPE=$1
if [[ -z "$BUILD_TYPE" ]]
then
    BUILD_TYPE="I"
    echo -e "\tBUILD_TYPE not specified on command line, 'I' assumed.\n"
fi
NERRORS=0
# products means, sdk.ide (aka SDK), rcp.id, rcp.sdk.id, platform.ide (aka platform binary), platform.sdk
NPRODUCTS=5
AGGR_DIR="${BUILD_HOME}/${BUILD_MAJOR_VERSION}${BUILD_TYPE}/gitCache/eclipse.platform.releng.aggregator"
ECLIPSE_BUILD_DIR="${AGGR_DIR}/eclipse.platform.releng.tychoeclipsebuilder"

declare -a PLATFORMS=( \
cocoa.macosx.x86_64 \
gtk.aix.ppc \
gtk.aix.ppc64 \
gtk.hpux.ia64 \
gtk.linux.ppc \
gtk.linux.ppc64 \
gtk.linux.ppc64le \
gtk.linux.s390 \
gtk.linux.s390x \
gtk.linux.x86 \
gtk.linux.x86_64 \
gtk.solaris.sparc \
gtk.solaris.x86 \
win32.win32.x86 \
win32.win32.x86_64 \
)
# M builds, as of 4.4.x had Mac 32 bit
if [[ ${BUILD_TYPE} == "M" ]]
then
    PLATFORMS+=(cocoa.macosx.x86)
fi
NPLATFORMS=${#PLATFORMS[@]}

echo "Build Type: $BUILD_TYPE"
echo "N products: $NPRODUCTS"
echo "N platforms: $NPLATFORMS"

# echo "DEBUG: ${PLATFORMS[@]}"

printf "\n\t%s" "Configuration Roots Test (one for each platform): "
EXPECTED_CONFIG_ROOTS=$NPLATFORMS
nConfigs=$( ls -l ${ECLIPSE_BUILD_DIR}/rcp.config/target/*configuration_root* | wc -l )

if [[ ! $nConfigs == $EXPECTED_CONFIG_ROOTS ]]
then 
    printf "\n\tERROR: %s\n" "The number of 'configuration roots' did not equal expect number (expected $EXPECTED_CONFIG_ROOTS, found $nConfigs)"
    # should "count errors" as we get other tests
    : $((NERRORS++))
else
    printf "\t%s\n" "Ok."
fi

printf "\n\t%s" "Info.plist Tests: "
EXPECTED_NUMBER=0
# Expected files are Products (times 2, if 32 bit) Plus one for starter kit, plus 1 for delta pack (times 2, if 32 bit). 
NBITARCHS=1
if [[ ${BUILD_TYPE} == "M" ]]
then
    NBITARCHS=2
fi
EXPECTED_FILES=$(( ( $NPRODUCTS * $NBITARCHS ) + 1 + ( 1 * $NBITARCHS ) ))
nInfoPlistFiles=$( find ${ECLIPSE_BUILD_DIR}/* -name "Info.plist" | wc -l )
if [[ $nInfoPlistFiles -eq $EXPECTED_FILES ]]
then 
    nWrongIds=$( grep --include Info.plist -A 1 -r "<key>CFBundleIdentifier</key>" ${ECLIPSE_BUILD_DIR}/* | grep  org.eclipse.eclipse | wc -l )

    if [[ $nWrongIds -gt $EXPECTED_NUMBER ]]
    then 
        printf "\n\tERROR: %s\n" "The number of 'org.eclipse.eclipse' IDs was greater than expected number (expected $EXPECTED_NUMBER, found $nWrongIds)"
        # should "count errors" as we get other tests
        : $((NERRORS++))
    else
        printf "\t%s\n" "Ok."
    fi
else
    printf "\n\tERROR: %s\n" "The number of Info.plist files was not what was expected. (expected $EXPECTED_FILES, found $nInfoPlistFiles)"
    : $((NERRORS++))
fi
printf "\t\tFor full list: %s\n" "grep --include Info.plist -A 1 -r \"<key>CFBundleIdentifier</key>\" *" 



printf "\n\t%s" "Symbolic Link Test: "
# expect 0, now with new Mac App layout
EXPECTED_LINKS=0
## M builds still have 32 bit Mac's
#if [[ ${BUILD_TYPE} == "M" ]]
#then
#    EXPECTED_LINKS=$(( ${NPRODUCTS} * 2 ))
#fi
## add one, for the 'rt'. Always only 64 bit.
#EXPECTED_LINKS=$(( $EXPECTED_LINKS + 1 ))

nLinks=$( find ${ECLIPSE_BUILD_DIR}/ -lname "*eclipse" -or -lname "*rt" | wc -l ) 

if [[ $nLinks != "$EXPECTED_LINKS" ]]
then 
    printf "\n\tERROR: %s\n" "The number of symbolic links was not as expected (expected $EXPECTED_LINKS, found $nLinks)"
    # should "count errors" as we get other tests
    : $((NERRORS++))
else
    printf "\t%s\n" "Ok. Found ${nLinks}."
fi

if [[ $NERRORS != 0 ]]
then
    printf "\n\t%s\n" "Exiting ${0##*/} with errors: $NERRORS"
fi
exit $NERRORS


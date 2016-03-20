#!/usr/bin/env bash

# Assuming this is ran before "promote", so data is read and written to build machine, 
# and then will be promoted with rest of build.


JAVA_8_HOME=/shared/common/jdk1.8.0_x64-latest
export JAVA_HOME=${JAVA_8_HOME}
buildIdToTest=${BUILD_ID:-"I20160314-2000"}
buildIdToCompare="4.5/R-4.5.2-201602121500"
build_type=${buildIdToTest:0:1}
build_dir_root="/shared/eclipse/builds/4${build_type}/siteDir/eclipse/downloads/drops4"
build_update_root="/shared/eclipse/builds/4${build_type}/siteDir/updates"
dl_dir_root="/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4"
if [[ ${build_type} == "M" ]]
then
    update_dir_segment="4.5-M-builds"
elif [[ ${build_type} == "I" ]]
then
    update_dir_segment="4.6-I-builds"
fi

buildToTest="${build_update_root}/${update_dir_segment}/${buildIdToTest}"

buildToCompare="/home/data/httpd/download.eclipse.org/eclipse/updates/${buildIdToCompare}"


app_area="${build_dir_root}/${buildIdToTest}"

output_dir="${build_dir_root}/${buildIdToTest}/buildlogs"
#remove and re-create in case 'reports' exists from a previous run (eventually will not be needed)
if [[ -e ${output_dir}/reporeports ]] 
then
    rm -rf ${output_dir}/reporeports
fi
# just in case does not exist yet
mkdir -p ${output_dir}

tar_name=org.eclipse.cbi.p2repo.analyzers.product-linux.gtk.x86_64.tar.gz

report_app_area="${app_area}/reportApplication"
if [[ ! -e ${tar_name} ]]
then
    wget --no-verbose --no-cache https://hudson.eclipse.org/cbi/job/cbi.p2repo.analyzers.build/lastSuccessfulBuild/artifact/output/products/${tar_name} 2>&1
fi
# always extract anew each time.
if [[ -e ${report_app_area} ]]
then
    rm -fr ${report_app_area}
fi
mkdir -p ${report_app_area}

tar -xf org.eclipse.cbi.p2repo.analyzers.product-linux.gtk.x86_64.tar.gz -C ${report_app_area}

${report_app_area}/p2analyze -data ${output_dir}/workspace-report -vm ${JAVA_8_HOME}/bin -vmargs -Xmx1g \
-DreportRepoDir=${buildToTest} \
-DreportOutputDir=${output_dir} \
-DreferenceRepo=${buildToCompare}

${report_app_area}/p2analyze -data ${output_dir}/workspace-report -vm ${JAVA_8_HOME}/bin -vmargs -Xmx2g \
-DuseNewApi=true \
-DreportRepoDir=${buildToTest} \
-DreportOutputDir=${output_dir} \
-DreferenceRepo=${buildToCompare}


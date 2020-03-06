#############################README##################################################################################
#Before running this script configure the source and target POM versions correctly, follow detailed steps below:
# 1. Update "updateProductVersion.sh" script with proper source and target version properly for all 4 mentioned files.
# 2. From Shell command prompt go to "eclipse.platform.releng.aggregator" directory
# 3. Then we should run the script using command: ./scripts/updateProductVersion.sh
# 4. Above script will take around less than a minute max to update the complete Eclipse sources.
# 5. Make sure to create the gerrit for for "eclipse.platform.releng.aggregator" in first place:
# 6. gerrit build will fail for this change, you can commit without gerrit validation
# 7. After committing above change, you need to deploy the POM for the new Eclipse version 4.16
# 8. e.g. For new Eclipse release 4.16 [Note: version value will change depending on the release]  rub below jobs:
#    https://ci.eclipse.org/releng/job/deploy-eclipse-platform-parent-pom-4.16/
#    https://ci.eclipse.org/releng/job/deploy-eclipse-sdk-target-pom-4.16/
# 9. Now create the gerrit patches for all sub-modules which should pass.
#10. Finally commit all the modified files in each of the individual submodules of Releng.

script_location=$( (cd $(dirname $0) && pwd) )

find $script_location/.. -name pom.xml -exec sed -i 's/4.15.0/4.16.0/g' {} \;
find $script_location/.. -name MANIFEST.MF -exec sed -i 's/4.15.0/4.16.0/g' {} \;
find $script_location/.. -type f -name *.product -exec sed -i 's/4.15.0/4.16.0/g' {} \;
find $script_location/.. -name feature.xml -exec sed -i 's/4.15.0/4.16.0/g' {} \;

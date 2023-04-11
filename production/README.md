## Performance Tests

This folder contains files used to run Eclipse Platform Performance tests. Performance tests are run in the [Eclipse Platform Releng](https://ci.eclipse.org/releng/view/Performance%20Tests/) Jenkins instance.

Results collection is handled by the [collection job](https://ci.eclipse.org/releng/view/Performance%20Tests/job/ep-collectPerfResults/) in Jenkins which runs [collectTestResults.xml](../cje-production/scripts/collectTestResults.xml)

### Running Performance Tests Locally

In order to run the performance tests locally:
  * Set environment variables for `JOB_NAME`, `WORKSPACE` and `testToRun`.
    * `JOB_NAME` can be anything, it's used to generate the names for results files and folders
    * `WORKSPACE` is the location where generated files will be placed.
    * `testToRun` determines whether to run `selectPerformance` or `otherPerformance`
      For Reference:
      * `selectPerformance` is used when running the [ep424I-perf-lin64](https://ci.eclipse.org/releng/view/Performance%20Tests/job/ep424I-perf-lin64/) and [ep424I-perf-lin64-baseline](https://ci.eclipse.org/releng/view/Performance%20Tests/job/ep424I-perf-lin64-baseline/) tests.
      * `otherPerformance` is used to run [ep424ILR-perf-lin64](https://ci.eclipse.org/releng/view/Performance%20Tests/job/ep424ILR-perf-lin64/) and [ep424ILR-perf-lin64-baseline](https://ci.eclipse.org/releng/view/Performance%20Tests/job/ep424ILR-perf-lin64-baseline/)
    * Set `buildId` to the build you want to test  
      i.e `export buildId=I20220324-0140` or `export buildId=R-4.23-202203080310` etc
    * Set `baselinePerf=true` if running the baseline version of the tests.
  * Download and source `buildproperties.shsource` for the build.
    ```
    curl -o buildproperties.shsource https://download.eclipse.org/eclipse/downloads/drops4/${buildId}/buildproperties.shsource
    source buildproperties.shsource
    ```
  * Make sure you have the latest testScripts/hudsonBootstrap/getEBuilder.xml from this folder.  
    If you don't want to pull the whole repo:
    ```
    wget -O getEBuilder.xml --no-verbose https://download.eclipse.org/eclipse/relengScripts/production/testScripts/hudsonBootstrap/getEBuilder.xml 2>&1
    ```
  * Make sure you have ant installed so you can run the following command:
    ```
    ant -f getEBuilder.xml -Djava.io.tmpdir=${WORKSPACE}/tmp -DbuildId=$buildId -Djvm=$JAVA_HOME/bin/java -DeclipseStream=${STREAM} -DEBUILDER_HASH=${EBUILDER_HASH}  -DbaselinePerf=${baselinePerf} -DdownloadURL=http://download.eclipse.org/eclipse/downloads/drops4/${buildId}  -Dosgi.os=linux -Dosgi.ws=gtk -Dosgi.arch=x86_64 -DtestSuite=${testToRun} -Dtest.target=performance
    ```

The current process uses [getEBuilder.xml](testScripts/hudsonBootstrap/getEBuilder.xml) to download a pre-zipped version of the testScripts folder then calls [runTests2.xml](testScripts/runTests2.xml).

runTests2.xml downloads the installer for the build being tested and the zip of the tests being run, then runs them.

###  Select and Other Performance

(From the testToRun parameter description in Jenkins, probably needs to be updated)

```
Collections:
selectPerformance  (a group of tests that complete in about 3 hours)
otherPerformance   (a small group of tests that either are not working, or take greater than one hour each).

Individual Tests Suites, per collection:
selectPerformance:

antui
compare
coreresources
coreruntime
jdtdebug
jdtui
osgi
pdeui
swt
teamcvs
ua
uiforms
uiperformance
uircp

otherPerformance:
equinoxp2ui
pdeapitooling
jdtcoreperf
jdttext
jdtuirefactoring
```
### Performance Results Collection

After performance tests have run they produce a dat file with the results. These were originally designed to be consumed by a database, but the original database was decommissioned in 2016 and never replaced. 

Results collection now waits for all 4 dat files to have been built and coped to the posting directory by [collectTestResults.xml](../cje-production/scripts/collectTestResults.xml), then uses [BasicResultsTable](https://github.com/eclipse-platform/eclipse.platform.releng/blob/master/bundles/org.eclipse.test.performance/src/org/eclipse/test/performance/BasicResultsTable.java) to parse the information and create the results tables. 

Once the tables have been generated [publish.xml](../cje-production/scripts/publish.xml) is called which prepared and runs the [TestResultsGenerator](https://github.com/eclipse-platform/eclipse.platform.releng.buildtools/blob/master/bundles/org.eclipse.build.tools/src/org/eclipse/releng/generators/TestResultsGenerator.java).

Since the files are stored and processed on the eclipse storage machine you can access the perrformance results from the download site by going to https://download.eclipse.org/eclipse/downloads/drops4/BUILDID/performance/
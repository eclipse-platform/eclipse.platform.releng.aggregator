<?php
// from static

/*
This funtion returns a proper "linux time stamp" so we can compute time
elapsed since build started ... and give different messages if we exceed
certain amounts.

We expect the value to be in the form as saved in "buildproperties.php",
namely
$TIMESTAMP = "20140706-2000";
 */
function timeOfBuild ($timestamp) {

  $year = substr($timestamp, 0, 4);
  $month = substr($timestamp, 4, 2);
  $day = substr($timestamp, 6, 2);
  $hour = substr($timestamp,9,2);
  $minute = substr($timestamp,11,2);
  $seconds = 0;
  $mktimeTime = mktime ($hour,$minute,$seconds,$month,$day,$year);

  //echo "DEBUG: year: $year\n";
  //echo "DEBUG: month: $month\n";
  //echo "DEBUG: day: $day\n";
  //echo "DEBUG: hour: $hour\n";
  //echo "DEBUG: minute: $minute\n";
  //echo "DEBUG: second: $seconds\n\n";
  //echo "DEBUG: input timestamp: $timestamp\n";
  //echo "DEBUG: mktimeTime: $mktimeTime\n";
  //echo "DEBUG: date: ".date("r", $mktimeTime)."\n\n";
  return $mktimeTime;
}

/*
Sanity check. Make sure echo statements are not commented out.
 */
function testTimeOfBuild() {
  timeOfBuild("20140706-2000");
  timeOfBuild("20140201-2400");
  timeOfBuild("20140201-1200");
  timeOfBuild("20140201-0000");
  timeOfBuild("20140241-1200");
  timeOfBuild("20144101-0001");
}
/*
This function was originally copied from 'createIndex4x.php'.
Its purpose is to return number of "test runs completed", based
on the names of "console logs" that are present in ./testResults/consolelogs
directory.
 */

function calcTestConfigsRan($testResultsDirName) {

  global $expectedTestConfigs;
  global $testResults;

  $boxes=0;
  // the include file, testConfigs.php defines 'testConfigs' array,
  // which consists of strings defining what platforms and vms we test.
  // For example, a testConfigs.php file might consist of
  // <?php
  // $expectedTestConfigs = array();
  // $expectedTestConfigs[]="linux.gtk.x86_64_8.0";
  // $expectedTestConfigs[]="macosx.cocoa.x86_64_7.0";
  // $expectedTestConfigs[]="win32.win32.x86_7.0";

  if (file_exists("testConfigs.php")) {
    include "testConfigs.php";
  }
  else  {
    // minus 2 is code for "testConfigs not found"
    $boxes=-2;
    $expectedTestConfigs = array();
  }

  if (file_exists("buildproperties.php")) {
    // be sure any previous are reset
    unset ($BUILD_FAILED);
    include "buildproperties.php";
    if (isset ($BUILD_FAILED) && strlen($BUILD_FAILED) > 0) {
      // minus 1 is taken as numeric code that "build failed".
      $boxes=-1;
      unset ($BUILD_FAILED);
    }
  }

  // will be empty until there is at least one test result uploaded?
  if (empty($testResultsDirName)) {
    // contrived code to mean "no results yet"
    $boxes = -3;
  }
  if ($boxes != -1 && $boxes != -2 && $boxes != -3)  {

    // TEMP? appears "old style" builds had directories named "results", but now "testresults"
    // and we want to look in $testResultsDirName/consolelogs
    if (file_exists("$testResultsDirName/consolelogs")) {
      $consolelogDir = dir("$testResultsDirName/consolelogs");
      //echo "<br />DEBUG: count of expectedTestConfigs: ".count($expectedTestConfigs)." <br />";
      //echo "<br />DEBUG: and var_dump of expectedTestConfigs was: <br />";
      //var_dump($expectedTestConfigs);
      //echo "<br />";
      while ($file = $consolelogDir->read()) {
        for ($i = 0 ; $i < count($expectedTestConfigs) ; $i++) {
          //echo "file: ".$file."<br />";
          //echo "expectTestConfigs[$i] ". $expectedTestConfigs[$i]. "<br />";
          if ((strpos($file, $expectedTestConfigs[$i])) !== FALSE) {
            $boxes++;

            // First make sure we get "fresh" list of ALL test summary files, each time.
            $testResultsSummaryFiles = glob($testResultsDirName."/ep*-unit-*.xml");
            foreach ($testResultsSummaryFiles as $summFileName) {
              // echo "DEBUG: processing $summFileName<br />";
              $jobname=jobname($expectedTestConfigs[$i]);
              //echo "DEBUG: jobname: " . $jobname;
              if (substr_startswith($summFileName, $testResultsDirName."/".$jobname)) {
                //echo "DEBUG: found matching summary file: $summFileName<br />";
                $xmlResults = simplexml_load_file($summFileName);
                $testResults[$expectedTestConfigs[$i]]["duration"]=$xmlResults->duration;
                $testResults[$expectedTestConfigs[$i]]["failCount"]=$xmlResults->failCount;
                $testResults[$expectedTestConfigs[$i]]["passCount"]=$xmlResults->passCount;
                $testResults[$expectedTestConfigs[$i]]["skipCount"]=$xmlResults->skipCount;
              }
            }
          }
        }
      }
    }
  }
  //echo "DEBUG: boxes: $boxes";
  return $boxes;
}

/*
This function was originally copied from 'createIndex4x.php', where
the function was named 'printBuildColumns'. It's be heavily modified
to be used on individual build DL page.

Its purpose is to return a short summary of "state of the tests".
 */
function printTestSummaryStatus() {

  include_once("buildproperties.php");


  // date and time of build, and elapsed time

  $buildTime=timeOfBuild($TIMESTAMP);

  // compute minutes elapsed since build started
  $day=intval(date("Ymd"));
  $time=intval(date("H"))*60+intval(date("i"));
  $diff=($day-$buildDay)*24*60+$time-$buildTime;



  if (file_exists("testresults")) {
    $testResultsDirName="testresults";
  } elseif (file_exists("results")) {
    $testResultsDirName="results";
  } else {
    // Neither directory exists at first ... until a tests completes and one result uploaded.
    $testResultsDirName="";
  }

  $boxes=calcTestConfigsRan($fileName, $testResultsDirName);
  // boxes == -1 is code that "bulid failed" and no tests are expected.
  if ($boxes == -1) {
    $buildimage="build_failed.gif";
    $buildalt="Build failed";
  } else {
    $buildimage="build_done.gif";
    $buildalt="Build is available";
  }

  // set to zero globally, but computed in calcTestConfigsRan
  global $expectedtestConfigs;

  $boxesTitle="";

  // We skip the main "tests" part for patch builds, since don't expect any (for now).
  if ($BUILD_TYPE !== "P" && $BUILD_TYPE !== "U" && $boxes !== -2) {

    // always put in links, since someone may want to look at logs, even if not tests results, per se
    // don't forget to end link, after images decided.

    if ($boxes > -1) {
      $boxesTitle=$boxes." of ".count($expectedTestConfigs)." test platforms finished.";
    }
    if ($testResultsDirName === "results") {
      echo "<a href=\"results/testResults.html\" title=\"$boxesTitle\" style=\"text-decoration: none\">";
    } else {
      echo "<a href=\"testResults.php\" title=\"$boxesTitle\" style=\"text-decoration: none\">";
    }

    if ($boxes == -1) {
      $testimage="caution.gif";
      $testalt="Integration tests did not run due to failed build";
    } elseif ($boxes == 0 && $diff > 720) {
      // assume if no results at all, after 12 hours, assume they didn't run for unknown reasosn
      $testimage="caution.gif";
      $testalt="Integration tests did not run, due to unknown reasons.";
    } elseif ($boxes > 0 && $boxes < count($expectedTestConfigs)) {
      if ($diff > 1440) {
        $testimage="junit.gif";
        $testalt="Tests results are available but did not finish on all machines";
      } else {
        $testimage="runtests.gif";
        $testalt="Integration tests are running ...";
      }
    } elseif ($boxes == count($expectedTestConfigs)) {
      $testimage="junit.gif";
      $testalt="Tests results are available";
    } else {
      $testimage="runtests.gif";
      $testalt="Integration tests are running ...";
    }
    echo "<img style=\"border:0px\" src=\"../images/$testimage\" title=\"$testalt\" alt=\"$testalt\" />";
    if ($boxes > -1) {
      echo "&nbsp;(".$boxes." of ".count($expectedTestConfigs)." platforms)";
    }
    echo "</a>\n";
  } else {
    echo "<a href=\"testResults.php\" title=\"$boxesTitle\" style=\"text-decoration: none\">";
    $testimage="results.gif";
    $testalt="Logs from build";
    echo "<img style=\"border:0px\" src=\"../images/$testimage\" title=\"$testalt\" alt=\"$testalt\" />";
    if ($BUILD_TYPE == "P" || $BUILD_TYPE == "U") {
      echo "&nbsp;(No automated tests)";
    } elseif ($boxes == -2) {
      echo "&nbsp;(No expected tests)";
    } else {
      echo "&nbsp;(unexpected test boxes)";
    }
  }
  echo "</a>\n";
}

function fileSizeForDisplay($filename) {
  $onekilo=1024;
  $onemeg=$onekilo * $onekilo;
  $criteria = 10 * $onemeg;
  $scaleChar = " ";
  if (file_exists($filename)) {
    $zipfilesize=filesize($filename);
    if ($zipfilesize > $criteria) {
      $zipfilesize=round($zipfilesize/$onemeg, 0);
      $scaleChar = " MiB";
    }
    else {
      if ($zipfilesize > $onekilo) {
        $zipfilesize=round($zipfilesize/$onekilo, 0);
        $scaleChar = " KiB";
      } else {
        // use raw size in bytes if less that one 1K
        $scaleChar = " B";
      }
    }
  }
  else {
    $zipfilesize = 0;
  }
  $result =  "(" . $zipfilesize . $scaleChar . ")";
  return $result;
}

/* Deprecated. This function moved to "generate test results table" ant task.
This function "breaks" the full config string at meaningful 
underscores, for improved display in tables and similar.
Remember, some config values can have more than two underscores, 
such as ep46N-unit-lin64_linux.gtk.x86_64_8.0, which should 
be split as 
         ep46N-unit-lin64
         lin64_linux.gtk.x86_64
         8.0
 */
function computeDisplayConfig($config) {
  $lastUnderscore = strrpos ($config, "_");
  $firstUnderscore = strpos ($config, "_", strpos($config, "x86_64") + 6);
  $platformLength=$lastUnderscore - $firstUnderscore - 1;
  //echo "<br/>DEBUG: config: $config firstUnderscore: $firstUnderscore  lastUnderscore: $lastUnderscore  lastMinusFirst: $platformLength"
  $jobname = substr($config,0,$firstUnderscore);
  $platformconfig = substr($config,$firstUnderscore + 1,$platformLength);
  $vmused = substr($config,$lastUnderscore+1);
  //echo "DEBUG: jobname: ".$jobname."<br/>";
  //echo "DEBUG: platformconfig: ".$platformconfig."<br/>";
  //echo "DEBUG: vmused: ".$vmused."<br/>";
  return $jobname."<br/>".$platformconfig."<br/>".$vmused;

}

/* This function gets the first segment of the config
   which is 'jobname' on Hudson.
 */
function jobname($config) {
  $firstUnderscore = strpos ($config, "_", strpos($config, "x86_64") + 6);
  $jobname = substr($config,0,$firstUnderscore);
  return $jobname;
}

function substr_startswith($haystack, $needle) {
  return substr($haystack, 0, strlen($needle)) === $needle;
}

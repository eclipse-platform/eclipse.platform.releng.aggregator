<?php
# Begin: page-specific settings.
$pageTitle    = "Eclipse Project Downloads";
$pageKeywords = "eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide";
$pageAuthor   = "David Williams and Christopher Guindon";

//ini_set("display_errors", "true");
//error_reporting (E_ALL);
$eclipseStream="4";
include('dlconfig4.php');
$subdirDrops="drops4";
$expectedtestConfigs=0;
$testConfigs = array();

ob_start();

/*
 DL.thin.header.php.html was original obtained from

wget https://eclipse.org/eclipse.org-common/themes/solstice/html_template/thin/header.php

and then that file modified to suit our needs.
Occasionally, our version should be compared to the "standard" to see if anything has
changed, in the interest of staying consistent.

See https://eclipse.org/eclipse.org-common/themes/solstice/docs/
*/

require("DL.thin.header.php.html");

?>

<h1>The Eclipse Project Downloads</h1>
<p>On this
page you can find the latest builds produced by
the <a href="https://www.eclipse.org/eclipse/">Eclipse
Project</a>. To get started, run the program and go through the user and developer
documentation provided in the help system or
see the <a href="http://help.eclipse.org/">web-based help system</a>.
If you have problems installing or getting the workbench to run, <a href="https://wiki.eclipse.org/index.php/The_Official_Eclipse_FAQs">check
out the Eclipse Project FAQ,</a> or try posting a question to the <a href="https://www.eclipse.org/forums/">forum</a>.
</p>

<p>See the <a href="https://www.eclipse.org/downloads/">main Eclipse Foundation download site</a> for convenient all-in-one packages.
The <a href="http://archive.eclipse.org/eclipse/downloads/">archive site</a> contains older releases (including the last 3.x version, <a href="http://archive.eclipse.org/eclipse/downloads/drops/R-3.8.2-201301310800/">3.8.2</a>).
For reference, see also
<a href="https://wiki.eclipse.org/Eclipse_Project_Update_Sites">the p2 repositories provided</a>,
<a href="build_types.html">meaning of kinds of builds</a> (P,M,N,I,S, and R), and the
<a href="https://www.eclipse.org/eclipse/platform-releng/buildSchedule.html">build schedule</a>.
</p>
<p><img src="new.gif" alt="News Item 1" /> ﻿Eclipse support for Java&trade; 8 is built&ndash;in
to <a href="drops4/R-4.4-201406061215/">Luna (4.4)</a> so it, and all subsequent builds, contain full support
for <a href="http://www.oracle.com/technetwork/java/javase/overview/index.html">Java&trade; 8</a>.
For Kepler SR2 (4.3.2), a <a href="https://wiki.eclipse.org/JDT/Eclipse_Java_8_Support_For_Kepler">feature patch</a> with
preliminary Java&trade; 8 support is available (<a href="drops4/P20140317-1600/">P20140317-1600</a>).
</p>
<table class="downloads">
<tr>
<td class="latest">Latest Downloads</td>
</tr>
</table>

<?php

function startsWithDropPrefix($dirName, $dropPrefix)
{

  $result = false;
  // sanity check "setup" is as we expect
  if (isset($dropPrefix) && is_array($dropPrefix)) {
    // sanity check input
    if (isset($dirName) && strlen($dirName) > 0) {
      $firstChar = substr($dirName, 0, 1);
      //echo "first char: ".$firstChar;
      foreach($dropPrefix as $type) {
        if ($firstChar == "$type") {
          $result = true;
          break;
        }
      }
    }
  }
  else {
    echo "dropPrefix not defined as expected\n";
  }
  return $result;
}
function calcTestConfigsRan($buildName, $testResultsDirName) {

  global $subdirDrops;
  global $expectedtestConfigs;

  $boxes=0;
  // the include file, testConfigs.php defines 'testConfigs' array,
  // which consists of strings defining what platforms and vms we test.
  // For example, a testConfigs.php file might consist of
  // <?php
  // $expectedTestConfigs = array();
  // $expectedTestConfigs[]="linux.gtk.x86_64_8.0";
  // $expectedTestConfigs[]="macosx.cocoa.x86_64_7.0";
  // $expectedTestConfigs[]="win32.win32.x86_7.0";

  if (file_exists("$subdirDrops/$buildName/testConfigs.php")) {
    include "$subdirDrops/$buildName/testConfigs.php";
    $testConfigs = &$expectedTestConfigs;
  }
  else  {
    // minus 2 is code for "testConfigs not found"
    $boxes=-2;
    $testConfigs = array();
  }
  $expectedtestConfigs=count($testConfigs);

  if (file_exists("$subdirDrops/$buildName/buildproperties.php")) {
    // be sure any previous are reset
    unset ($BUILD_FAILED);
    include "$subdirDrops/$buildName/buildproperties.php";
    if (isset ($BUILD_FAILED) && strlen($BUILD_FAILED) > 0) {
      // minus 1 is taken as numeric code that "build failed".
      $boxes=-1;
      unset ($BUILD_FAILED);
    }
  }
  if ($boxes != -1 && $boxes != -2)  {

    // TEMP? appears "old style" builds had directories named "results", but now "testresults"
    // and we want to look in $testResultsDirName/consolelogs
    if (file_exists("$subdirDrops/$buildName/$testResultsDirName/consolelogs")) {
      $buildDir = dir("$subdirDrops/$buildName/$testResultsDirName/consolelogs");
      while ($file = $buildDir->read()) {
        for ($i = 0 ; $i < $expectedtestConfigs ; $i++) {
          if (strncmp($file, $testConfigs[$i], count($testConfigs[$i])) == 0) {
            $boxes++;
            break;
          }
        }
      }
    }
  }
  //echo "DEBUG: boxes: $boxes";
  return $boxes;
}
function printBuildColumns($fileName, $parts) {
  global $subdirDrops;
  // no file name, write empty column
  if ($fileName == "") {
    echo "<td status=\"status\">&nbsp;</td>\n";
    return;
  }
  // get build name, date and time
  $dropDir="$subdirDrops/$fileName";
  if (count($parts)==3) {
    $buildName=$parts[1];
    $buildDay=intval(substr($parts[2], 0, 8));
    $buildTime=intval(substr($parts[2], 8, 4));
    $buildType=$parts[0];
  }
  if (count($parts)==2) {
    $buildName=$fileName;
    $buildDay=intval(substr($buildName, 1, 8));
    $buildTime=intval(substr($buildName, 10, 2))*60+intval(substr($buildName, 12, 2));
    $buildType=substr($buildName, 0, 1);
  }
  // compute minutes elapsed since build started
  $day=intval(date("Ymd"));
  $time=intval(date("H"))*60+intval(date("i"));
  $diff=($day-$buildDay)*24*60+$time-$buildTime;
  // Add icons
  echo "<td class=\"status\">\n";
  // hard code for now the build is done
  // https://bugs.eclipse.org/bugs/show_bug.cgi?id=378706
  // but later, changed ...
  // compute build done based on "buildPending" file, but if not
  // present, assume build is done
  // https://bugs.eclipse.org/bugs/show_bug.cgi?id=382196
  $build_done=true;
  if (file_exists("$dropDir/buildPending")) {
    $build_done=false;
  }
  if ($build_done) {
    // test results location changed. 'testresults' is new standard
    // but we check for 'results' for older stuff.
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=379408
    $testResultsDirName="";
    if (file_exists("$dropDir/testresults")) {
      $testResultsDirName="testresults";
    } else {
      if (file_exists("$dropDir/results")) {
        $testResultsDirName="results";
      }
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
    echo "<a href=\"$dropDir/\"><img style=\"border:0px\" src=\"../images/$buildimage\" title=\"$buildalt\" alt=\"$buildalt\" /></a>\n";

    // set to zero globally, but computed in calcTestConfigsRan
    global $expectedtestConfigs;

    $boxesTitle="";

    // We skip the main "tests" part for patch builds, since don't expect any (for now).
    if ($buildType !== "P" && $boxes !== -2) {

      // always put in links, since someone may want to look at logs, even if not tests results, per se
      // don't forget to end link, after images decided.

      if ($boxes > -1) {
        $boxesTitle=$boxes." of ".$expectedtestConfigs." test platforms finished.";
      }
      if ($testResultsDirName === "results") {
        echo "<a href=\"$dropDir/results/testResults.html\" title=\"$boxesTitle\" style=\"text-decoration: none\">";
      } else {
        echo "<a href=\"$dropDir/testResults.php\" title=\"$boxesTitle\" style=\"text-decoration: none\">";
      }

      if ($boxes == -1) {
        $testimage="caution.gif";
        $testalt="Integration tests did not run due to failed build";
      } elseif ($boxes == 0 && $diff > 720) {
        // assume if no results at all, after 12 hours, assume they didn't run for unknown reasons
        $testimage="caution.gif";
        $testalt="Integration tests did not run, due to unknown reasons.";
      } elseif ($boxes > 0 && $boxes < $expectedtestConfigs) {
        if ($diff > 1440) {
          $testimage="junit.gif";
          $testalt="Tests results are available but did not finish on all machines";
        } else {
          $testimage="runtests.gif";
          $testalt="Integration tests are running ...";
        }
      } elseif ($boxes == $expectedtestConfigs) {
        $testimage="junit.gif";
        $testalt="Tests results are available";
      } else {
        $testimage="runtests.gif";
        $testalt="Integration tests are running ...";
      }
      echo "<img style=\"border:0px\" src=\"../images/$testimage\" title=\"$testalt\" alt=\"$testalt\" />\n";
      if ($boxes > -1) {
        echo "&nbsp;(".$boxes." of ".$expectedtestConfigs." platforms)\n";
      }
      echo "</a>\n";
    } else {
      echo "<a href=\"$dropDir/testResults.php\" title=\"$boxesTitle\" style=\"text-decoration: none\">";
      $testimage="results.gif";
      $testalt="Logs from build";
      echo "<img style=\"border:0px\" src=\"../images/$testimage\" title=\"$testalt\" alt=\"$testalt\" />";
      if ($buildType == "P") {
        echo "&nbsp;(No automated tests)";
      } elseif ($boxes == -2) {
        echo "&nbsp;(No expected tests)";
      } else {
        echo "&nbsp;(unexpected test boxes)";
      }
      echo "</a>\n";
    }
  }
  echo "</td>\n";
  return $buildName;
}
?>

<?php
// this is the main data computation part
$aDirectory = dir($subdirDrops);
while ($anEntry = $aDirectory->read()) {

  // Short cut because we know aDirectory only contains other directories.

  if ($anEntry != "." && $anEntry!=".." && $anEntry!="TIME" && startsWithDropPrefix($anEntry,$dropPrefix)) {
    $parts = explode("-", $anEntry);
    // echo "<p>an entry: $anEntry\n";
    // do not count hidden directories in computation
    // allows non-hidden ones to still show up as "most recent" else will be blank.
    if (!file_exists($subdirDrops."/".$anEntry."/buildHidden")) {
      if (count($parts) == 3) {
        $timePart = $parts[2];
        $year = substr($timePart, 0, 4);
        $month = substr($timePart, 4, 2);
        $day = substr($timePart, 6, 2);
        $hour = substr($timePart,8,2);
        $minute = substr($timePart,10,2);
        // special logic adds 1 second if build id contains "RC" ... this was
        // added for the M build case, where there is an M build and and RC version that
        // have same time stamp. One second should not effect desplayed values.
        $isRC = strpos($anEntry, "RC");
        if ($isRC === false) {
          $timeStamp = mktime($hour, $minute, 0, $month, $day, $year);
        } else {
          $timeStamp = mktime($hour, $minute, 1, $month, $day, $year);
        }
        $buckets[$parts[0]][$timeStamp] = $anEntry;
        $timeStamps[$anEntry] = date("D, j M Y -- H:i (O)", $timeStamp);
        // latestTimeStamp will not be defined, first time through
        if (!isset($latestTimeStamp) || !array_key_exists($parts[0],$latestTimeStamp)  || $timeStamp > $latestTimeStamp[$parts[0]]) {
          $latestTimeStamp[$parts[0]] = $timeStamp;
          $latestFile[$parts[0]] = $anEntry;
        }
      }

      if (count($parts) == 2) {
        $buildType=substr($parts[0],0,1);
        //$buckets[$buildType][] = $anEntry;
        $datePart = substr($parts[0],1);
        $timePart = $parts[1];
        $year = substr($datePart, 0, 4);
        $month = substr($datePart, 4, 2);
        $day = substr($datePart, 6, 2);
        $hour = substr($timePart,0,2);
        $minute = substr($timePart,2,2);
        $isRC = strpos($anEntry, "RC");
        if ($isRC === false) {
          $timeStamp = mktime($hour, $minute, 0, $month, $day, $year);
        } else {
          $timeStamp = mktime($hour, $minute, 1, $month, $day, $year);
        }
        $buckets[$buildType[0]][$timeStamp] = $anEntry;
        $timeStamps[$anEntry] = date("D, j M Y -- H:i (O)", $timeStamp);
        if (!isset($latestTimeStamp) || !array_key_exists($buildType,$latestTimeStamp) || $timeStamp > $latestTimeStamp[$buildType]) {
          $latestTimeStamp[$buildType] = $timeStamp;
          $latestFile[$buildType] = $anEntry;
        }
      }
    }
  }
}
?>

<!-- This is the summary section, showing latest of each -->

<table class="downloads table table-hover table-striped table-condensed">
<tr>
<th class="name">Build Name</th>
<th class="status">Build Status</th>
<th class="date">Build Date</th>
</tr>
<?php
foreach($dropType as $value) {

  $prefix=$typeToPrefix[$value];
  // if empty bucket, do not print this row
  if (array_key_exists($prefix,$buckets)) {


    if (array_key_exists($prefix,$latestFile)) {
      $fileName = $latestFile[$prefix];
    }
    $parts = explode("-", $fileName);

    // Uncomment the line below if we need click through licenses.
    // echo "<td><a href=license.php?license=$subdirDrops/$fileName>$parts[1]</a></td>\n";

    $buildName=$fileName;
    if (count($parts)==3) {
      $buildName=$parts[1];
    }
    if (!file_exists($subdirDrops."/".$fileName."/buildHidden")) {
      echo "<tr style=\"line-hieght:0.8;\">\n";
      if ($fileName == "") {
        echo "<td class=\"name\">&nbsp;</td>\n";
      } else {
        // Note: '$value' basically comes from dlconfig4.php and serves two purposes:
        // 1) the "tool tip" when hovering over the "Latest" build.
        // 2) the "title bar" of remaining sections.
        // In other words dlconfig4.php would have to be expanded if we ever wanted
        // "tool tip" and "section title" to be (slightly) different from each other.
        echo "<td class=\"name\"><a href=\"$subdirDrops/$fileName/\" title=\"$value\">$buildName</a></td>\n";
      }
      $buildName = printBuildColumns($fileName, $parts);
      echo "<td  class=\"date\">$timeStamps[$fileName]</td>\n";
      echo "</tr>\n";
    }
  }
}
?>
</table>


<?php
foreach($dropType as $value) {
  $prefix=$typeToPrefix[$value];
  // skip whole section, if bucket is empty
  if (array_key_exists($prefix,$buckets)) {

    echo "<table class=\"downloads\">\n";
    // header, colored row
    // name attribute can have no spaces, so we tranlate them to underscores
    // (could effect targeted links)
    $valueName=strtr($value,' ','_');
    echo "<tr id=\"$valueName\">\n";
    echo "<td class=\"main\">$value</td>\n";
    echo "</tr>\n";
    echo "</table>\n";

    echo "<table class=\"downloads table table-hover table-striped table-condensed\">\n";
    echo "<tr>\n";

    echo "<th class=\"name\">Build Name</th>\n";
    echo "<th class=\"status\">Build Status</th>\n";
    echo "<th class=\"date\">Build Date</th>\n";

    echo "</tr>\n";
    $aBucket = $buckets[$prefix];
    if (isset($aBucket)) {
      krsort($aBucket);
      foreach($aBucket as $innerValue) {
        if (!file_exists($subdirDrops."/".$innerValue."/buildHidden")) {
          $parts = explode("-", $innerValue);
          echo "<tr>\n";
          $buildName=$innerValue;
          if (count ($parts)==3) {
            echo "<td class=\"name\"><a href=\"$subdirDrops/$innerValue/\">$parts[1]</a></td>\n";
          } else if (count ($parts)==2) {
            echo "<td class=\"name\"><a href=\"$subdirDrops/$innerValue/\">$innerValue</a></td>\n";
          } else {
            echo "<td class==\"name\">Unexpected numberof parts?</td>\n";
          }
          $buildName = printBuildColumns($innerValue, $parts);
          echo "<td class=\"date\">$timeStamps[$innerValue]</td>\n";
          echo "</tr>\n";
        }
      }
    }
    echo "</table>\n";
  }
}
require("DL.footer.php.html");
$html = ob_get_clean();

#echo the computed content
echo $html;


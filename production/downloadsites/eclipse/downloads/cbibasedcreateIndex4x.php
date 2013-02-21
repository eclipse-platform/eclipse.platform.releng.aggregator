<?php  
  # Begin: page-specific settings.  Change these.
  $pageTitle    = "Eclipse Project Downloads";
  $pageKeywords = "";
  $pageAuthor   = "";
 
  //ini_set("display_errors", "true");
  //error_reporting (E_ALL);
  $eclipseStream="4";
  $otherIndexFile="eclipse3x.html";
  $otherStream="3";
  include('dlconfig4.php');
  $subdirDrops="drops4cbibased";

  # Use the basic white layout if the file is not hosted on download.eclipse.org
  $layout = (array_key_exists("SERVER_NAME", $_SERVER) && ($_SERVER['SERVER_NAME'] == "download.eclipse.org")) ? "default" : "html";
 
  ob_start();

  switch($layout){
    case 'html':
      #If this file is not on download.eclipse.org print the legacy headers.?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<link rel="stylesheet" href="../default_style.css" />
<title><?php echo $pageTitle;?></title></head>
<body><?php
      break;   
    default:
      #Otherwise use the default layout (content printed inside the nova theme).
      require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/app.class.php");
      require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/nav.class.php");
      require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/menu.class.php");
      $App  = new App();
      $Nav  = new Nav();
      $Menu   = new Menu();
      break;     
  }?>
<div class="container_<?php echo $layout;?>">
<table border="0" cellspacing="5" cellpadding="2" width="100%" >

<tr>

<td align="left" width="72%">
<font class="indextop"> Eclipse Project <?php echo $eclipseStream;?>.x Stream Downloads</font> <br />
<font class="indexsub">
Latest downloads from the Eclipse project
</font><br />
</td>

<td width="28%">

<!-- not sure, might need this "rowspan 2" then eclipsecon logo included?
<td width="19%" rowspan="2"></td>
-->
<img src="../images/friendslogo.jpg" alt="Friends of Eclipse Logo" /><br />Support Eclipse! Become a <a href="http://www.eclipse.org/donate/">friend</a>.<br />
</td>
<!--  <td width="19%" rowspan="2"><a href="http://www.eclipsecon.org/" target="_blank"><img src="../images/prom-eclipsecon1.gif" width="125" height="125" border="0" /></a></td> -->

</tr>

</table>

<table border="0" cellspacing="5" cellpadding="2" width="100%" >
<tr>
<td align="left" valign="top" colspan="2" bgcolor="#0080C0"><font color="#FFFFFF" face="Arial,Helvetica">Latest
Downloads</font></td></tr> <!-- The Eclipse Projects --> <tr> <td>
<p>On this
page you can find the latest <a href="build_types.html" target="_top">builds</a> produced by
the <a href="http://www.eclipse.org/eclipse" target="_top">Eclipse
Project</a>. To get started run the program and go through the user and developer
documentation provided in the online help system. If you have problems downloading
the drops, contact the <font size="-1" face="arial,helvetica,geneva"><a href="mailto:webmaster@eclipse.org">webmaster</a></font>.
If you have problems installing or getting the workbench to run, <a href="http://wiki.eclipse.org/index.php/The_Official_Eclipse_FAQs" target="_top">check
out the Eclipse Project FAQ,</a> or try posting a question to the <a href="http://www.eclipse.org/newsgroups" target="_top">newsgroup</a>.
All downloads are provided under the terms and conditions of the <a href="http://www.eclipse.org/legal/epl/notice.php" target="_top">Eclipse Foundation
Software User Agreement</a> unless otherwise specified. </p>


<p><a href="http://download.eclipse.org/eclipse/downloads/<?php echo $otherIndexFile;?>">Eclipse <?php echo $otherStream;?>.x downloads</a> are available.</p>
<p>See the <a href="http://www.eclipse.org/downloads/"> main Eclipse download site for other packages and projects</a>.</p>
<p>Help out with Eclipse translations - check out the <a href="http://babel.eclipse.org/babel/">Babel project</a>.</p>
<p>If you prefer, try downloading with the <a href="http://build.eclipse.org/technology/phoenix/torrents/SDK/">SDK Torrents</a> </p>

<p>
See also the <a href="http://www.eclipse.org/eclipse/platform-releng/buildSchedule.html">build schedule</a>, read information about different <a href="build_types.html">kinds of
builds</a>, access <a href="http://archive.eclipse.org/eclipse/downloads/">archived builds</a> (including language packs), or see a list of
<a href="http://wiki.eclipse.org/Eclipse_Project_Update_Sites">p2 update sites</a>.
</p>
</td></tr>
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
function runTestBoxes($buildName, $testResultsDirName) {
    // hard code for now the tests ran on one box (or, zero, if no testResultsDirName yet)
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=378706
    if ($testResultsDirName === "" ) {
       return 0;
    } else {
       return 1;
    }
    global $subdirDrops;
    $testBoxes=array("linux", "macosx", "win32");
    $length=count($testBoxes);
    $boxes=0;
    // TEMP? appears "old style" builds had directories named "results"
    if (file_exists("$subdirDrops/$buildName/$testResultsDirName")) {
        $buildDir = dir("$subdirDrops/$buildName/$testResultsDirName");
        while ($file = $buildDir->read()) {
            for ($i = 0 ; $i < $length ; $i++) {
                if (strncmp($file, $testBoxes[$i], count($testBoxes[$i])) == 0) {
                    $boxes++;
                    break;
                }
            }
        }
    }
    return $boxes;
}
function printBuildColumns($fileName, $parts) {
    global $subdirDrops;
    // no file name, write empty column
    if ($fileName == "") {
        echo "<td></td>\n";
        return;
    }
    // get build name, date and time
    $dropDir="$subdirDrops/$fileName";
    if (count($parts)==3) {
        $buildName=$parts[1];
        $buildDay=intval(substr($parts[2], 0, 8));
        $buildTime=intval(substr($parts[2], 8, 4));
    }
    if (count($parts)==2) {
        $buildName=$fileName;
        $buildDay=intval(substr($buildName, 1, 8));
        $buildTime=intval(substr($buildName, 10, 2))*60+intval(substr($buildName, 12, 2));
    }
    // compute minutes elapsed since build started
    $day=intval(date("Ymd"));
    $time=intval(date("H"))*60+intval(date("i"));
    $diff=($day-$buildDay)*24*60+$time-$buildTime;
    // Add icons
    echo "<td valign=\"baseline\">\n";
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
        } else
        if (file_exists("$dropDir/results")) {
            $testResultsDirName="results";
        }
        $boxes=runTestBoxes($fileName, $testResultsDirName);
        echo "<a href=\"$dropDir/\"><img border=\"0\" src=\"../images/build_done.gif\" title=\"Build is available\" alt=\"Build is available\" /></a>\n";

        switch ($boxes) {
        case 0:
            // if more than 12 hours then consider that the regression tests did not start
            if ($diff > 720) {
            // for now, hard code to "0" since we are not reunning tests
            //if ($diff > 0) {
                echo "<img src=\"../images/caution.gif\" title=\"Regression tests did not run!\" alt=\"Regression tests did not run!\" />\n";
            } else {
                echo "<img src=\"../images/runtests.gif\" title=\"Regression tests are running...\" alt=\"Regression tests are running...\" />\n";
            }
            break;

        case 5:
            if ($testResultsDirName === "testresults") {
                echo "<a href=\"$dropDir/testResults.php\">";
            } else {
                echo "<a href=\"$dropDir/results/testResults.html\">";
            }
            echo "<img border=\"0\" src=\"../images/junit.gif\" title=\"Tests results are available\" alt=\"Tests results are available\" /></a>\n";
            break;
        default:
            // if more than 24 hours then consider that the regression tests did not finish
            if ($diff > 1440) {
                if ($testResultsDirName === "testresults") {
                    echo "<a href=\"$dropDir/testResults.php\">";
                } else {
                    echo "<a href=\"$dropDir/results/testResults.html\">";
                }
                echo "<img border=\"0\" src=\"../images/junit.gif\" title=\"Tests results are available but did not finish on all machines\" alt=\"Tests results are available but did not finish on all machines\" /></a>\n";
            } else {
                echo "<img border=\"0\" src=\"../images/runtests.gif\" title=\"Tests are still running on some machines...\" alt=\"Tests are still running on some machines...\" />\n";
            }
        }
        //break;
    }
    //    $perfsDir="$dropDir/performance";
    //  if (file_exists("$perfsDir")) {
    //    $perfsFile="$perfsDir/performance.php";
    //  if (file_exists("$perfsFile")) {
    //     if (file_exists("$perfsDir/global.php")) {
    //echo "<a href=\"$perfsFile\"><img border=\"0\" src=\"../images/perfs.gif\" title=\"Performance tests are available\" alt=\"Performance tests are available\"/></a>\n";
    //    } else {
    //        echo "<img src=\"../images/caution.gif\" title=\"Performance tests ran and results should have been generated but unfortunately they are not available!\" alt=\"No Performance tests\"/>\n";
    ///   }
    // } else {
    //            if (file_exists("$perfsDir/consolelogs")) {
    // if more than one day then consider that perf tests did not finish
    //              if ($diff > 1440) {
    //                if (substr($buildName, 0, 1) == "I") {
    //                  $reason="see bug 259350";
    //            } else {
    //              $reason="either they were not stored in DB or not generated";
    //        }
    //  echo "<img src=\"../images/caution.gif\" title=\"Performance tests ran but no results are available: $reason!\" alt=\"No Performance Tests\" />\n";
    //} else {
    //   echo "<img src=\"../images/runperfs.gif\" title=\"Performance tests are running...\" alt=\"Performance tests are running\" />\n";
    // }
    // }
    // }
    //}
    //}
    else {
        // if more than 5 hours then consider that the build did not finish
        if ($diff > 300) {
            echo "<img src=\"../images/build_failed.gif\" title=\"Build failed!\" alt=\"Build failed!\" />\n";
        } else {
            echo "<img src=\"../images/build_progress.gif\" title=\"Build is in progress...\" alt=\"Build is in progress.\"/>\n";
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

                $buckets[$parts[0]][] = $anEntry;

                $timePart = $parts[2];
                $year = substr($timePart, 0, 4);
                $month = substr($timePart, 4, 2);
                $day = substr($timePart, 6, 2);
                $hour = substr($timePart,8,2);
                $minute = substr($timePart,10,2);
                $timeStamp = mktime($hour, $minute, 0, $month, $day, $year);

                $timeStamps[$anEntry] = date("D, j M Y -- H:i (O)", $timeStamp);
                // latestTimeStamp will not be defined, first time through
                if (!isset($latestTimeStamp) || !array_key_exists($parts[0],$latestTimeStamp)  || $timeStamp > $latestTimeStamp[$parts[0]]) {
                    $latestTimeStamp[$parts[0]] = $timeStamp;
                    $latestFile[$parts[0]] = $anEntry;
                }
            }

            if (count($parts) == 2) {

                $buildType=substr($parts[0],0,1);
                $buckets[$buildType][] = $anEntry;
                $datePart = substr($parts[0],1);
                $timePart = $parts[1];
                $year = substr($datePart, 0, 4);
                $month = substr($datePart, 4, 2);
                $day = substr($datePart, 6, 2);
                $hour = substr($timePart,0,2);
                $minute = substr($timePart,2,2);
                $timeStamp = mktime($hour, $minute, 0, $month, $day, $year);
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
<table width="100%" cellspacing="0" cellpadding="3" align="center">
<tr>
<td align="left">


<table  width="100%" cellspacing="0" cellpadding="3">
<tr>
<th width="30%">Build Type</th>
<th width="15%">Build Name</th>
<th width="15%">Build Status</th>
<th>Build Date</th>
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

        // Comment the line below if we need click through licenses.

        $buildName=$fileName;
        if (count($parts)==3) {
            $buildName=$parts[1];
        }
        if (!file_exists($subdirDrops."/".$fileName."/buildHidden")) {
            echo "<tr>\n";
            echo "<td width=\"30%\">$value</td>\n";
            if ($fileName == "") {
                echo "<td></td>\n";
            } else {
                echo "<td><a href=\"$subdirDrops/$fileName/\">$buildName</a></td>\n";
            }
            $buildName = printBuildColumns($fileName, $parts);
            echo "<td>$timeStamps[$fileName]</td>\n";
            echo "</tr>\n";
        }
    }
}
?>
    </table></td></tr></table>


<?php
foreach($dropType as $value) {
    $prefix=$typeToPrefix[$value];
    // skip whole section, if bucket is empty
    if (array_key_exists($prefix,$buckets)) {

        echo " <table width=\"100%\" cellspacing=\"0\" cellpadding=\"3\" align=\"center\" >\n";
        // header, colored row
        echo "<tr bgcolor=\"#999999\">\n";
        // name attribute can have no spaces, so we tranlate them to underscores
        // (could effect targeted links)
        $valueName=strtr($value,' ','_');
        echo "<td align=\"left\" width=\"30%\"><a name=\"$valueName\">\n";
        echo "<font color=\"#FFFFFF\" face=\"Arial,Helvetica\">$value\n";
        echo "</font></a></td>\n";
        echo "</tr>\n";

        echo "<tr>\n";
        echo "<td align=\"left\" >\n";
        echo "\n";
        echo "\n";
        echo "\n";
        echo "\n";
        echo "<table  width=\"100%\" cellspacing=\"0\" cellpadding=\"4\" >\n";
        echo "<tr>\n";

        echo "<th width=\"15%\">Build Name</th>\n";
        echo "<th width=\"15%\">Build Status</th>\n";
        echo "<th>Build Date</th>\n";

        echo "</tr>\n";

        $aBucket = $buckets[$prefix];
        if (isset($aBucket)) {
            rsort($aBucket);
            foreach($aBucket as $innerValue) {

                if (!file_exists($subdirDrops."/".$innerValue."/buildHidden")) {

                    $parts = explode("-", $innerValue);

                    echo "<tr>\n";

                    // Uncomment the line below if we need click through licenses.
                    // echo "<td><a href=\"license.php?license=$subdirDrops/$innerValue\">$parts[1]</a></td>\n";

                    // Comment the line below if we need click through licenses.
                    $buildName=$innerValue;
                    if (count ($parts)==3) {
                        echo "<td><a href=\"$subdirDrops/$innerValue/\">$parts[1]</a></td>\n";
                    } else if (count ($parts)==2) {
                        echo "<td><a href=\"$subdirDrops/$innerValue/\">$innerValue</a></td>\n";
                    } else {
                        echo "<td>Unexpected numberof parts?</td>\n";
                    }

                    $buildName = printBuildColumns($innerValue, $parts);
                    echo "<td>$timeStamps[$innerValue]</td>\n";
                    echo "</tr>\n";
                }
            }
        }
        echo "</table>\n";
        echo "</td></tr>\n";
        echo "</table>\n";

    }
}

echo '</div>';
$html = ob_get_clean();

switch($layout){
    case 'html':
      #echo the computed content with the body and html closing tag. This is for the old layout.
      echo $html;
      echo '</body>';
      echo '</html>';
      break;

	default:
      #For the default view we use $App->generatePage to generate the page inside nova.
	  $App->AddExtraHtmlHeader('<link rel="stylesheet" href="../default_style.css" />');
	  $App->Promotion = FALSE;
	  $App->generatePage('Nova', $Menu, NULL , $pageAuthor, $pageKeywords, $pageTitle, $html);
	  break;
}

